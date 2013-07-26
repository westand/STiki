package ext_queues;

import mediawiki_api.api_retrieve;
import core_objects.metadata;
import core_objects.stiki_utils;
import core_objects.stiki_utils.SCORE_SYS;
import db_server.qmanager_server;

/**
 * Andrew G. West - cluebotng_process.java - Given a line from the CBNG feed,
 * this class parses that line and makes the necessary DB/queue modifications.
 * This class exists primarily so this can be threaded, per RID. 
 */
public class cluebotng_process implements Runnable{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Queue mananger -- to update the CBNG edit queue.
	 */
	private qmanager_server qmanager;
	
	/**
	 * A line from the CBNG feed, describing the scoring of some RID.
	 */
	private String msg;
	
	/**
	 * If set to FALSE, then edits made by bots will not be enqueued.
	 */
	private static final boolean ENQUEUE_BOT = false;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Process a single line from the CBNG feed, in threaded fashion. 
	 * @param qmanager Queue mananger -- to update the CBNG edit queue
	 * @param msg A line from the CBNG feed, describing the scoring of some RID
	 */
	public cluebotng_process(qmanager_server qmanager, String msg){
		this.qmanager = qmanager;
		this.msg = msg;	
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
		// Example lines from the CBNG feed:
		// [[Evan Tanner]] http://en.wikipedia.org/w/index.php?
		// 		diff=403838186&oldid=403838136 * .149.1.104 * (-11274) /* 
		// 		Biography */  # 0.977775 # Reverted before # Not reverted
		//
		// [[Wikipedia talk:Featured article candidates]] MB http://en.
		//		wikipedia.org/w/index.php?diff=403838177&oldid=403658843 * 
		//		MiszaBot II * (-2475) Archiving 2 thread(s) (older than 21d) 
		//		to [[Wikipedia talk:Featured article candidates/archive48]]. 
		//		# N/A # Outside of valid namespaces # Not reverted
		//
		// [[Disney Sing Along Songs]]  http://en.wikipedia.org/w/index.php?
		//		diff=403838179&oldid=403838060 * Diannaa * (+0) Reverted 4 
		//		edits by [[Special:Contributions/66.8.245.223|66.8.245.223]] 
		//		to last revision by 98.238.120.147 ([[WP:HG|HG]]) # 
		//		0.00752599 # Below threshold # Not reverted
		//
		// [[Ed Reynolds]]  http://en.wikipedia.org/w/index.php?n 
		//		diff=403838457&oldid=400401904 * .70.215.54 * (-17)  # 
		//		0.951492 # Default revert # Reverted
	
	/**
	 * Overriding: Method to be run when class is "executed". Given an IRC 
	 * feed message, process that message, parsing out its various fields 
	 * and making queue modifications where necessary
	 */
	public void run(){
		
		if(msg == null)
			return; // Sanity check to make sure we have a message
		
		String title = stiki_utils.first_match_within("\\[\\[.*?\\]\\]", msg);
		String rid_str = stiki_utils.first_match_within("diff=\\d*", msg);
		if(title == null || title.contains(":") || rid_str == null)
			return; // Concerned only with well-formed edits in NS0
		
		try{ 	// Work backwards on String; delimiter more predictable
			String[] cbng_parts = msg.split("#");
			String cbng_outcome = cbng_parts[cbng_parts.length-1].trim();
			String cbng_comment = cbng_parts[cbng_parts.length-2].trim();
			String cbng_score = cbng_parts[cbng_parts.length-3].trim();
			String wiki_portion = cbng_parts[cbng_parts.length-4].trim();
			String username = wiki_portion.split("\\*")[1].trim();
			
				// Parse out the RID and PID. Page-ID requires a MW-API call;
				// the primary reason this class is threaded.
			long rid = Long.parseLong(rid_str.replace("diff=", ""));
			metadata md = api_retrieve.process_basic_rid(rid);
			if(md == null)
				return;
			long pid = md.pid;
			
				// If the edit was un-scorable or a REVERT, we don't enqueue,
				// but remove the previous entry on the PID. Similarly, don't
				// allow CBNG to enqueue its own edits. We also have a no-bot
				// enqueue option. Otherwise, enqueue the RID/score.
			if(cbng_score.equalsIgnoreCase("N/A"))
				qmanager.delete_pid(SCORE_SYS.CBNG, pid);
			else if(cbng_outcome.equalsIgnoreCase("REVERTED") || 
					cbng_comment.equalsIgnoreCase("User is myself") ||
					wiki_portion.contains("ClueBot") || 
					wiki_portion.contains("STiki")){
					// NEVER EVER SELF ENQUEUE
				qmanager.delete_pid(SCORE_SYS.CBNG, pid); 
				qmanager.insert_score(SCORE_SYS.CBNG, 
						rid, pid, Double.parseDouble(cbng_score), false);
			} else if(!ENQUEUE_BOT && username.toUpperCase().contains("BOT")){
					// Check username to see if a bot
				qmanager.delete_pid(SCORE_SYS.CBNG, pid); 
				qmanager.insert_score(SCORE_SYS.CBNG, 
						rid, pid, Double.parseDouble(cbng_score), false);				
			} else qmanager.insert_score(SCORE_SYS.CBNG, 
					rid, pid, Double.parseDouble(cbng_score), true);
			
		} catch(Exception e){
			System.out.println("Error in CBNG parse:");
			e.printStackTrace();
		} // Try-catch the whole process. Failure is no big deal if
		  // something goes wrong; It's only a single RID.
		
	} // End run() method

}