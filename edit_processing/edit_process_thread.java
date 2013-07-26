package edit_processing;

import java.util.concurrent.DelayQueue;

import core_objects.feature_set;
import core_objects.metadata;
import core_objects.stiki_utils.SCORE_SYS;
import db_server.db_category;
import db_server.db_country;
import db_server.db_edits;
import db_server.db_features;
import db_server.db_geolocation;
import db_server.db_hyperlinks;
import db_server.db_off_edits;
import db_server.qmanager_server;
import ext_queues.wikitrust_process;
import learn_frontend.feature_builder;
import learn_frontend.learn_interface;
import mediawiki_api.api_retrieve;

/**
 * Andrew G. West - edit_process_thread.java - This class takes a revision,
 * and performs the steps necessary to:
 * 
 *  	(1) parses out the edit data into primitive form
 * 		(2) scores the edit (assigns feature values)
 * 		(3) instructs the edit to be classified against an ML model.
 * 		(4) writes the edit/features to a persistent database
 * 
 * This is the bulk of the work performed in backend processing. Revisions
 * to be processed are provided by [stiki_backend_driver.java]. This class
 * is standalone so each revision-process can be given its own thread.
 */
public class edit_process_thread implements Runnable{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Number of times to retry querying for new edit data. This is a practical
	 * requirement given that it takes some time for data to distribute
	 * across MediaWiki servers and be available for our query. This variable
	 * is used wherever RID-queue insertions are made (IRC handler).
	 */
	public static final int NEW_RID_ATTEMPTS = 2;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * DB handler for all queries/updates pertaining to the [main_edits] table.
	 */
	private db_edits db_edits;
	
	/**
	 * DB handler for all queries/updates of the [offending_edits] table.
	 */
	private db_off_edits db_oe;
	
	/**
	 * DB handler for all queries/updates pertaining to the [geo_*] tables.
	 */
	private db_geolocation db_geo;
	
	/**
	 * DB handler for all queries/updates pertaining to the [features] table.
	 */
	private db_features db_feat;
	
	/**
	 * DB handler for all queries pertaining to the [category_links] table.
	 */
	private db_category db_cat;
	
	/**
	 * DB handler for all queries/updates pertaining to the [country] table.
	 */
	private db_country db_country;
	
	/**
	 * DB handler for all inserts/updates pertaining to the [hyperlinks] table.
	 */
	private db_hyperlinks db_links;
	
	/**
	 * Manager wrapping access to all [queue_*] and [classify_*] tables.
	 */
	private qmanager_server qmanager;
	
	/**
	 * Handler for classification (i.e., "scoring") of feature-sets.
	 */
	private learn_interface learn_module;
	
	/**
	 * RID-queue element (wrapping RID, number of re-attempts, etc.)
	 */
	private rid_queue_elem rid_element;
	
	/**
	 * Queue containing RIDs in need of processing.
	 */
	private DelayQueue<rid_queue_elem> rid_queue;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [stiki_backend_thread] object. The articles are self
	 * explanatory per the global arguments except for [rid], which is
	 * the revision this instance should process.
	 */
	public edit_process_thread(db_edits db_edits, db_off_edits db_oe, 
			db_geolocation db_geo, db_features db_feat, db_category db_cat, 
			db_country db_country, db_hyperlinks db_links, 
			qmanager_server qmanager, learn_interface learn_module, 
			rid_queue_elem rid_element, DelayQueue<rid_queue_elem> rid_queue)
			throws Exception{

		this.db_edits = db_edits;
		this.db_oe = db_oe;
		this.db_geo = db_geo;
		this.db_feat = db_feat;
		this.db_cat = db_cat;
		this.db_country = db_country;
		this.db_links = db_links;
		this.qmanager = qmanager;
		this.learn_module = learn_module;
		this.rid_element = rid_element;
		this.rid_queue = rid_queue;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Begin the process of scoring the RID enconded within the fields of this 
	 * object. Initiation of this method will start a new thread.
	 */
	public void run(){
		
		try{
			metadata meta; // Begin by getting basic edit metadata
			meta = api_retrieve.process_basic_rid(this.rid_element.RID, db_geo);
			if(!safe_to_process_metadata(meta))
				return; // If metadata problem, do not continue
			
			rollback_handler.new_edit(meta, db_oe, db_geo, db_edits, 
					db_feat, db_country, db_links); // Handle RBs
			
				// Score the feature-set, determine queue eligibility
			feature_set cur_features = feature_builder.score_edit(
					meta, db_oe, db_geo, db_cat, db_country, db_links);
			db_feat.insert_feature_row(cur_features);
			double score = learn_module.classify(cur_features);
			boolean should_queue = should_queue(meta);
			
				// Historically store the queue, possibly enqueue the
				// edit, and persistently store the feature set
			qmanager.insert_score(SCORE_SYS.STIKI, 
					meta.rid, meta.pid, score, should_queue);
			db_edits.insert_edit(meta);
			
				// Here we API out for the WikiTrust score. It would be
				// nice if this were more high-level, but here we have the
				// data to prevent addl. queries (and we're already threaded).
				// Also gives the WT folks a slight processing delay.
			new wikitrust_process(qmanager, meta.rid, meta.pid).score();
			
		} catch(Exception e){
			System.out.println("Error encountered in RID-process thread:");
			e.printStackTrace();
		} // Must try-catch, cannot 'throw' per interface compliance.	
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Given the metadata returned from a MediaWiki API call, inspect it to
	 * determine if processing of the edit/metadata should continue.
	 * @param cur_rev_md Metadata (maybe null), from MediaWiki API.
	 * @return TRUE if the edit processing should continue, FALSE if
	 * it should aborted as the result of some metadata property. 
	 */
	private boolean safe_to_process_metadata(metadata cur_rev_md){
	
		if(cur_rev_md == null){
			
				// If new RID metadata is unavailable. Could be due to 
				// distribution delay, re-query as permitted.
			int num_reattempts = (this.rid_element.get_decr_reattempts());
			if(num_reattempts != 0)
				rid_queue.add(new rid_queue_elem(this.rid_element.RID));
			else // if(num_reattempts == 0)
				System.out.println("Failed to obtain new RID metadata");
			return false;
			
		} else if(cur_rev_md.namespace != 0)
			return false; // Only interested in NS0 edits at this time
		else return true; // If non-null and NS0, enable processing
	}
	
	/**
	 * Some metadata properties are highly indicative of the fact an edit is
	 * NOT vandalism. This practical consideration allows us to 'short-circuit'
	 * such edits, so users will never see them, but are else handled as usual.
	 * @param cur_rev_md Metadata associated with current edit
	 * @return FALSE if some property of 'cur_rev_md' is highly indicative of 
	 * the fact the associated edit is NOT vandalism. TRUE, otherwise.
	 */
	private boolean should_queue(metadata cur_rev_md){
		
			// Rollbacks, normal-reverts, and STiki-reverts are unlikely to be 
			// vandalism, yet sometimes are flagged as such because they appear 
			// on controversial pages. We prevent the queueing of such edits.
			// Also exclude edits made by bots
		boolean is_bot = cur_rev_md.user.toUpperCase().contains("BOT");
		String uc_comment = cur_rev_md.comment.toUpperCase();
		if(is_bot || 
				cur_rev_md.get_is_rb() ||  
				uc_comment.matches("UNDID REVISION .* BY .*") ||
				uc_comment.matches(".*STIKI.*") ||
				cur_rev_md.user.equals("ClueBot NG"))
			return false;
		
			// Failure to meet any above criteria; queue the edit
		return true;
	}
	
}
