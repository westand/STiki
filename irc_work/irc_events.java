package irc_work;

import java.util.concurrent.DelayQueue;

import org.schwering.irc.lib.*;

import edit_processing.rid_queue_elem;

/**
 * Andrew G. West - irc_events.java - This class is the event-handler for
 * events being listened to on the IRC channel/connection specified by
 * [irc_listener.java]. Broadly, we are interested only in messages about
 * Wikipedia edits, and when we see these, we write the RIDs of interest
 * to a queue for processing. All other IRC traffic is ignored.
 */
public class irc_events implements IRCEventListener{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * IRC messages which have a (space-delimited) component matching the
	 * regex below are deemed to be of interest, and RID is parsed from URL.
	 */
	private static String DIFF_URL_REGEX = 
		"https://en.wikipedia.org/w/index\\.php\\?diff=(\\d)+&oldid=(\\d)+";
	
	/**
	 * Structure to which RIDs in need of processing should be written (this
	 * will be handled by another thread, thus the concurrent nature).
	 */
	private DelayQueue<rid_queue_elem> rid_queue;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [irc_events] object. 
	 * @param rid_queue Producer-consumer queue to which RIDs in need
	 * of processing should be written (the IRC channel is the producer).
	 */
	public irc_events(DelayQueue<rid_queue_elem> rid_queue){
		this.rid_queue = rid_queue;
	}
	
	
	// **************************** PUBLIC METHODS **************************
			
	/**
	 * The description of this method is superceeded by the interface. However,
	 * we note the name is a bit misleading, as this seems to be *any* non-
	 * administrative message receved in an IRC channel. We pull out the
	 * important parts and pass them on for further processing. 
	 */
	public void onPrivmsg(String chan, IRCUser u, String msg){
		
		// Channel and user are assumed, only message is of interest. 
		//
		// Seemingly, we could parse some interesting data out of the message
		// to do pre-processing prior to full API-lookup. Without a 
		// standardized format though, we simply look for the diff-url, parse
		// out the new revision-id (R_ID) and pass it off.
		
		long new_rid;
		String[] tokens = msg.split(" ");
		for(int i=0; i < tokens.length; i++){
			if(tokens[i].matches(DIFF_URL_REGEX)){
				
				new_rid = rid_from_diff_url(tokens[i]);	
				if(new_rid == -1)
					continue; // If parse error, don't attempt API
			
				rid_queue.offer(new rid_queue_elem(new_rid));
				
			} // If a new edit is countered, parse and add to queue
		} // Iterate over all message tokens, looking for diff-URL
		
	}
	
	// REFERENCE PURPOSES: Example IRC output lines: 
	//
	// #en.wikipedia> rc: [[Fittonia verschaffeltii]]  http://en.wikipedia.org
	//	  w/index.php?diff=343070556&oldid=343070456 * ShadowKinght * (+3) 
	
	// #en.wikipedia> rc: [[Kentucky Wildcats men's basketball]]  http://en.
	//	  wikipedia.org/w/index.php?diff=343070557&oldid=343009653 * .129.142 
	//	  .23 * (+0) /* Three Point Streak */ 
	
	// #en.wikipedia> rc: [[Special:Log/move]] move  * UnitedStatesian *  
	//	  moved [[Talk:Combat Zone (Studio)]] to [[Talk:Combat Zone (studio)]]:
	//	  Fix capitalization
	
	
	/**
	 * There are a host of methods required for interface compliance (all
	 * those below). However, we are not interested in their content, only
	 * that of the basic message format (above). These event-handlers
	 * simply ignore their input. 
	 */
	public void onRegistered() {}
	public void onDisconnected() {}
	public void onError(String msg) {}
	public void onError(int num, String msg) {}
	public void onInvite(String chan, IRCUser u, String nickPass) {}
	public void onJoin(String chan, IRCUser u) {}
	public void onKick(String chan, IRCUser u, String nickPass, String msg) {}
	public void onMode(IRCUser u, String nickPass, String mode) {}
	public void onMode(String chan, IRCUser u, IRCModeParser mp) {}
	public void onNick(IRCUser u, String nickNew) {}
	public void onNotice(String target, IRCUser u, String msg) {}
	public void onPart(String chan, IRCUser u, String msg) {}
	public void onQuit(IRCUser u, String msg) {}
	public void onReply(int num, String value, String msg) {}
	public void onTopic(String chan, IRCUser u, String topic) {}
	public void onPing(String p) {}	
	public void unknown(String a, String b, String c, String d) {}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Given a Wikipedia diff-URL (like those published on the IRC channel), 
	 * parse out the revision-ID (RID) of the new revision/edit.
	 * @param diff_url Wikipedia-formatted diff-URL.
	 * @return Revision-ID (RID) of most-recent revision from `diff_url', or
	 * -1 (negative one), if an error was encountered at parse-time.
	 */
	private  static long rid_from_diff_url(String diff_url){
		
		// Example URLs of the Wikipedia diff-format:
		//
		// http://en.wikipedia.org/w/index.php?diff=343070556&oldid=343070456
		// http://en.wikipedia.org/w/index.php?diff=343070557&oldid=343009653
		
		try{
			String[] url_parts = diff_url.split("(diff=)|(&oldid)");
			long rid = Long.parseLong(url_parts[1]);
			return (rid);
		} catch(Exception e){
			System.out.println("Failure to parse RID for diff-URL");
			System.out.println("String given: " + diff_url);
			e.printStackTrace();
			return -1;
		} // Output parse failures for manual inspection, return (-1)
		
	}


}
