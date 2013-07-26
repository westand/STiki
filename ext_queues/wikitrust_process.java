package ext_queues;

import core_objects.stiki_utils.SCORE_SYS;
import db_server.qmanager_server;

/**
 * Andrew G. West - wikitrust_process.java - Given an RID/PID pair, this
 * class wraps the API call and database-handling for "WikiTrust" queue.
 * This class can become threaded with minimal changes.
 */
public class wikitrust_process{

	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Queue mananger -- to update the WikiTrust edit queue.
	 */
	private qmanager_server qmanager;
	
	/**
	 * Revision ID of revision whose WikiTrust score is being obtained.
	 */
	private long rid;
	
	/**
	 * Page ID of 'rid' -- whose WikiTrust score is being obtained.
	 */
	private long pid;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a call to the WikiTrust API
	 * @param qmanager Queue mananger -- to update the WikiTrust edit queue
	 * @param rid Revision ID of rev. whose WikiTrust score is being obtained
	 * @param pid Page ID of 'rid' -- whose WikiTrust score is being obtained
	 */
	public wikitrust_process(qmanager_server qmanager, long rid, long pid){
		this.qmanager = qmanager;
		this.rid = rid;
		this.pid = pid;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Make a WikiTrust API call for an RID. Then, update the 
	 * local WikiTrust edit queue as appropriate.
	 */
	public void score(){
		
		try{	// If the edit was un-scorable or an API, we don't enqueue --
				// but remove the previous entry on the PID. Otherwise,
				// enqueue the RID along with its score (whose method
				// will dequeue any previous RID on the same PID).
			
			double score = wikitrust_api.wikitrust_score(rid, pid);
			if(score == wikitrust_api.ERROR_CODE)
				qmanager.delete_pid(SCORE_SYS.WT, pid);
			else qmanager.insert_score(SCORE_SYS.WT, 
					rid, pid, score, true);
				
		} catch(Exception e){
			System.out.println("Error in WT parse:");
			e.printStackTrace();
		} // Try-catch the whole process. Failure is no big deal if
		  // something goes wrong; It's only a single RID.
	}
	
}