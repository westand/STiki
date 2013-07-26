package executables;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import core_objects.stiki_utils;

import learn_adtree.adtree_frontend;
import learn_frontend.feature_hyperlinks;
import learn_frontend.learn_interface;
import learn_frontend.train_sets;

import irc_work.irc_listener;
import irc_work.irc_output;

import db_server.db_category;
import db_server.db_country;
import db_server.db_edits;
import db_server.db_features;
import db_server.db_geolocation;
import db_server.db_hyperlinks;
import db_server.db_oe_migrate;
import db_server.db_off_edits;
import db_server.db_status;
import db_server.qmanager_server;
import db_server.stiki_con_server;
import edit_processing.edit_process_thread;
import edit_processing.rid_queue_elem;
import edit_processing.thread_manager;
import ext_queues.cluebotng_irc;

/**
 * Andrew G. West - stiki_backend_driver.java - This class opens up a
 * connection to the `Recent Changes' IRC channel, which writes edits
 * to be processed to a queue. Revisions are then popped-off this queue
 * and given to a handler which threads each process (which includes the
 * building of feature sets, and their "scoring").
 */
public class stiki_backend_driver{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Number of threads to use for parallel RID processing.
	 */
	private static final int NUM_RID_THREADS = 64;
	
	/**
	 * Learning module/strategy being applied.
	 */
	private static final learn_interface LEARNER = new adtree_frontend();
	
	/**
	 * The number of edits which should occur between migrations on the 
	 * [rollbacks] table (i.e., the archival of older rollbacks).
	 */
	private static final int NUM_REVS_MIGRATE_RB = 100000;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Critical structure. Contains RIDs in need of processing, and
	 * enables multi-threading. Queue is populated by IRC-listener
	 * and then consumed for processing herein. First pair element is RID
	 * to process, second element is number of re-attempts if query fails.
	 */
	private static DelayQueue<rid_queue_elem> rid_queue;
	
	/**
	 * Structure holding worker threads for RID processsing. Critically, the
	 * ExecutorService will restart threads to maintain a fixed size pool,
	 * even if exceptions or other service-halts are encountered.
	 */
	private static ExecutorService WORKER_THREADS; 
	
	/**
	 * By flipping this flag, the STiki processing would cleanly shutdown
	 * and exit (currently not implemented). 
	 */
	private static boolean break_proc = false;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Driver method. Start IRC listener and all back-end processing.
	 * @param args No arguments are required by this method.
	 */
	public static void main(String[] args) throws Exception{
		
			// Startup the IRC feeds which STiki writes
		irc_output irc_out = new irc_output();
		
			// Database handlers have connections; must be instantiated
		stiki_con_server server_con = new stiki_con_server();
		db_edits db_edits = new db_edits(server_con);
		db_off_edits db_oe = new db_off_edits(server_con);
		db_geolocation db_geo = new db_geolocation(server_con);
		db_features db_features = new db_features(server_con);
		db_category db_cat = new db_category(server_con);
		db_country db_country = new db_country(server_con);
		db_hyperlinks db_links = new db_hyperlinks(server_con, irc_out);
		db_status db_status_vars = new db_status(server_con);

			// Prepare structure to hold child-threads
		thread_manager tm = new thread_manager();
		WORKER_THREADS = Executors.newFixedThreadPool(NUM_RID_THREADS, tm);
		
			// Wrap edit queues, start population of external queues.
		qmanager_server qmanager = new qmanager_server(server_con, irc_out);
		cluebotng_irc cbng_irc = new cluebotng_irc(WORKER_THREADS, qmanager);
		
			// Create STiki produce-consume queue, and start IRC listening
		rid_queue = new DelayQueue<rid_queue_elem>();
		irc_listener irc_rc = new irc_listener(rid_queue);
		
			// A couple of RID trackers for periodic tasks, and statistics
		rid_queue_elem cur_element; // RID-element currently being handled.
		long rid_last_t = 0;		// RID which triggered last retraining
		long rid_last_mig = 0;		// RID which triggered last RB-migration
		long edits_processed = 0;
		long ts_status_updated = stiki_utils.cur_unix_time();
		
		while(!break_proc){
			if(rid_queue.peek() != null && // Peek can see unexpired entries
					rid_queue.peek().getDelay(TimeUnit.NANOSECONDS) < 0){
				
					// It would seem this null-check is redundant with above,
					// however, a null-exception here has caused server
					// shutdown -- so we make an explicit check after poll()
				cur_element = rid_queue.poll();
				if(cur_element == null)
					continue;
				
					// Read in, score, and queue the edit (STiki style).
					// This includes both STiki and WikiTrust processing
				WORKER_THREADS.submit(new edit_process_thread(
						db_edits, db_oe, db_geo, db_features, db_cat, 
						db_country, db_links, qmanager, LEARNER, 
						cur_element, rid_queue));	
				edits_processed++;
				
					// See if any periodic tasks need performed
				rid_last_t = retrain(server_con, cur_element.RID, rid_last_t);
				rid_last_mig = migrate_rb(cur_element.RID, rid_last_mig);
				
			} else{ // If element in P-C queue, pop-and-process
			
				if(ts_status_updated + 30 > stiki_utils.cur_unix_time()){
					update_status_vars(db_status_vars, rid_queue.size(), 
							tm.num_threads_created(), edits_processed, 
							cbng_irc.num_edits_processed(), irc_out.isUp());
					ts_status_updated = stiki_utils.cur_unix_time();
				} // Update status vars on a thirty second interval
				
				Thread.sleep(10); // Something to eliminate over-spinning?
				
			} // If not busy, update variables; possibly sleep

		} // Process edits until told otherwise
		
			// Shut-down all database connections, stmts, and IRC-listener
		WORKER_THREADS.shutdownNow();
		db_edits.shutdown();
		db_oe.shutdown();
		db_geo.shutdown();
		db_features.shutdown();
		db_cat.shutdown();
		db_country.shutdown();
		db_links.shutdown();
		db_status_vars.shutdown();
		cbng_irc.shutdown();
		qmanager.shutdown();
		irc_rc.shutdown();
		irc_out.shutdown();
		server_con.con.close();
	}
	
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Examine if a re-training should occur. If yes, initiate re-training.
	 * @param con_server Connection to the STiki database (full privs.)
	 * @param cur_rid RID which was last processed into the STiki system
	 * @param rid_last_retrain RID at which last retraining occured
	 * @return IF this class actually retrains the model, then 'cur_rid'
	 * will be returned. ELSE, variable 'rid_last_retrain' will be returned.
	 */
	private static long retrain(stiki_con_server con_server, long cur_rid, 
			long rid_last_retrain) throws Exception{
		
		if(LEARNER.retrain_interval() == -1)
			return(rid_last_retrain); // By rule; don't ever train
		else if((cur_rid - rid_last_retrain) < LEARNER.retrain_interval())
			return(rid_last_retrain); // Not needed; yet
			
			// Else, train over a "smart" training set -- note this is 
			// hard-coded option and different strategies are available
		LEARNER.train(train_sets.get_smart_set(con_server, cur_rid));
		return(cur_rid);
	}
	
	/**
	 * Examine if RB migration should occur. If yes, initiate migration.
	 * @param cur_rid RID which was last processed into the STiki system
	 * @param rid_last_migrate RID at which last migration occured
	 * @return IF this class actually initiates migrartion, then 'cur_rid'
	 * will be returned. ELSE, variable 'rid_last_migration' will be returned.
	 */
	private static long migrate_rb(long cur_rid, long rid_last_migrate)
			throws Exception{
		
			// If migration not needed, break and exit immediately
		if((cur_rid - rid_last_migrate) < NUM_REVS_MIGRATE_RB)
			return(rid_last_migrate);
		else // Else, perform migration (in own thread)
			new db_oe_migrate();
		return(cur_rid);
	}
	
	/**
	 * Update status variables, which speak to the health of processing.
	 * @param db_status_vars DB-handler object for status variables.
	 * @param rid_queue_size Number of RIDs in queue to be processed
	 * @param num_threads_created Number of (worker) threads created (may not 
	 * correspond to the number of active threads).
	 * @param stiki_edits_processed Number of edits processed (worker tasks 
	 * assigned, note this includes edits not in name-space zero (NS0))
	 * @param cbng_edits_processed Number of edits processed by CBNG in session
	 * @param irc_up Whether or not the output IRC connection is active
	 */
	private static void update_status_vars(db_status db_status_vars, 
			int q_size, int num_threads_created, long stiki_edits_processed,
			long cbng_edits_processed, boolean irc_up) throws Exception{
		
		db_status_vars.update_status_var(
				db_status.BE_QUEUE_SIZE, q_size);
		db_status_vars.update_status_var(
				db_status.THREADS_CREATED, num_threads_created);
		db_status_vars.update_status_var(
				db_status.EDITS_PROC_STIKI, stiki_edits_processed);
		db_status_vars.update_status_var(
				db_status.EDITS_PROC_CBNG, cbng_edits_processed);
		db_status_vars.update_status_var(
				db_status.OUTPUT_IRC_UP, irc_up);
		db_status_vars.update_status_var(
				db_status.LINK_PARSE_ACC, 
				Math.round(100 * feature_hyperlinks.parse_success()));
	}
	
}
