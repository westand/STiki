package db_server;

import irc_work.irc_output;
import core_objects.stiki_utils;
import core_objects.stiki_utils.SCORE_SYS;

/**
 * Andrew G. West - qmanager_server.java - This class wraps all [queue_*] and
 * [classify_*] handlers implemented by STiki. This particular version is 
 * specific to SERVER-side operation.
 */
public class qmanager_server{
	
	// **************************** PRIVATE FIELDS ***************************
	
		// A [db_queue] and [db_scores] object are opened over each queue,
		// which have their own unique database tables. Not java'docing this.
	private db_queue db_queue_stiki;
	private db_queue db_queue_cbng;
	private db_queue db_queue_wt;
	private db_queue db_queue_spam;

	private db_scores db_scores_stiki;
	private db_scores db_scores_cbng;
	private db_scores db_scores_wt;
	private db_scores db_scores_spam;
	
	/**
	 * IRC handler, via which STiki scores will be written to a public feed.
	 */
	private irc_output irc_out;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a "queue manager" by constructing the handlers over all
	 * database tables used by the various implemented queues.
	 * @param con Connection to STiki servers (fully privileged)
	 * @param irc_out IRC handler, to which STiki scores will be written
	 */
	public qmanager_server(stiki_con_server con, irc_output irc_out) 
			throws Exception{
		
		db_queue_stiki = new db_queue(con, stiki_utils.tbl_queue_stiki);
		db_queue_cbng = new db_queue(con, stiki_utils.tbl_queue_cbng);
		db_queue_wt = new db_queue(con, stiki_utils.tbl_queue_wt);
		db_queue_spam = new db_queue(con, stiki_utils.tbl_queue_spam);
		
		db_scores_stiki = new db_scores(con, stiki_utils.tbl_scores_stiki);
		db_scores_cbng = new db_scores(con, stiki_utils.tbl_scores_cbng);
		db_scores_wt = new db_scores(con, stiki_utils.tbl_scores_wt);
		db_scores_spam = new db_scores(con, stiki_utils.tbl_scores_spam);
		this.irc_out = irc_out;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Having scored an edit, persistnetly write that the score to the 
	 * appropriate [classify_*] table, and maybe add to the [queue_*].
	 * @param sys System that generated the score. This helps direct RID/score
	 * to proper storage table, and queuing table.
	 * @param rid Revision ID of the edit being scored
	 * @param pid Page ID of the edit being scored
	 * @param score Real-valued score speaking to vandalism propability
	 * @param enqueue If TRUE, then the RID/score will be added to appropriate
	 * queue. If false, score is stored, but not enqueued.
	 */
	public void insert_score(SCORE_SYS sys, long rid, long pid, 
			double score, boolean enqueue) throws Exception{
		
			// NOTE: All scores are stored in [scores_*]. However, if
			// an edit is not enqueued, the previous edit in the queue
			// associated with that PID will be removed, by virtue of the
			// fact a more recent edit exists on the article.
			
		 if(sys.equals(SCORE_SYS.STIKI)){
			if(enqueue)
				db_queue_stiki.insert_classification(rid, pid, score);
			else delete_pid(sys, pid);
			db_scores_stiki.insert_classification(rid, score);
			irc_out.msg(irc_output.CHANNELS.STIKI_SCORES, 
					rid + " " + score + " " + "https://en.wikipedia.org/w/" +
					"index.php?oldid=" + rid + "&diff=prev");
		} else if(sys.equals(SCORE_SYS.CBNG)){
			if(enqueue)
				db_queue_cbng.insert_classification(rid, pid, score);
			else delete_pid(sys, pid);
			db_scores_cbng.insert_classification(rid, score);
		} else if(sys.equals(SCORE_SYS.WT)){
			if(enqueue)
				db_queue_wt.insert_classification(rid, pid, score);
			else delete_pid(sys, pid);
			db_scores_wt.insert_classification(rid, score);
		} else if(sys.equals(SCORE_SYS.SPAM)){
			if(enqueue)
				db_queue_spam.insert_classification(rid, pid, score);
			else delete_pid(sys, pid);
			db_scores_spam.insert_classification(rid, score);
		} // Branch based on system that generated score
	}
	
	/**
	 * Remove an element from a particular queue, by providing its PID.
	 * Note this removes an element from only ONE queue.
	 * @param sys Queue from which the element should be removed
	 * @param pid Page identifier of element to be removed. 
	 */
	public void delete_pid(SCORE_SYS sys, long pid) throws Exception{
		if(sys.equals(SCORE_SYS.CBNG))
			db_queue_cbng.delete_pid(pid);
		else if(sys.equals(SCORE_SYS.STIKI))
			db_queue_stiki.delete_pid(pid);
		else if(sys.equals(SCORE_SYS.WT))
			db_queue_wt.delete_pid(pid);
		else if(sys.equals(SCORE_SYS.SPAM))
			db_queue_spam.delete_pid(pid);
	}
	
	/**
	 * Delete a PID from ALL queues.
	 * @param pid Page identified of the element to be removed
	 */
	public void delete_pid(long pid) throws Exception{
		db_queue_cbng.delete_pid(pid);
		db_queue_stiki.delete_pid(pid);
		db_queue_wt.delete_pid(pid);
		db_queue_spam.delete_pid(pid);
	}
	
	/**
	 * Shutdown all objects created by this class (DB handlers). 
	 */
	public void shutdown() throws Exception{
		db_queue_cbng.shutdown();
		db_queue_stiki.shutdown();
		db_queue_wt.shutdown();
		db_queue_spam.shutdown();
		
		db_scores_cbng.shutdown();
		db_scores_stiki.shutdown();
		db_scores_wt.shutdown();
		db_scores_spam.shutdown();
	}

}
