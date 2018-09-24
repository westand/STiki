package ext_queues;

import java.util.concurrent.ExecutorService;

import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;

import db_server.qmanager_server;

/**
 * Andrew G. West - cluebotng_listener.java - This class implements an
 * EventListener over an IRC channel to the CBNG feed. It passes the messages
 * received to a threaded handler, which processes them.
 */
public class cluebotng_listener implements IRCEventListener{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * IRC connection over which this listener operates. Needed to initiate
	 * the channel-join action once the server reply is confirmed.
	 */
	private IRCConnection con_irc;
	
	/**
	 * Executor to be given individual RIDs to process.
	 */
	private ExecutorService threads;
	
	/**
	 * Queue mananger -- to update the CBNG edit queue.
	 */
	private qmanager_server qmanager;
	
	/**
	 * Counter tracking number of IRC lines processed in this session.
	 */
	private long edits_processed = 0;
	
	/**
	 * Boolean tracking if the channel-join action has been completed.
	 */
	private boolean has_joined_channel = false;
	
	
	// ***************************** CONSTRUCTORS ****************************

	/**
	 * Construct an EventListener for CBNG's IRC feed.
	 * @param con_irc IRC connection over which this listener operates
	 * @param threads Executor to be given individual RIDs to process
	 * @param qmanager Queue manager, so that the parse of the IRC feed
	 * can be used to update the "Cluebot-NG" specific queue.
	 */
	public cluebotng_listener(IRCConnection con_irc, ExecutorService threads, 
			qmanager_server qmanager){
		this.con_irc = con_irc;
		this.threads = threads;
		this.qmanager = qmanager;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: A server reply via this message confirms a full connection,
	 * which indiciates it is now legal to issue our channel join request.
	 */
	public void onReply(int num, String value, String msg){
		if(!has_joined_channel){
			con_irc.doJoin(cluebotng_irc.CHANNEL);
			has_joined_channel = true;
		} // Only want to join the channel once
	}
	
	/**
	 * Overriding: This is the main CBNG feed processing method.
	 */
	public void onPrivmsg(String chan, IRCUser u, String msg){
		threads.execute(new cluebotng_process(qmanager, msg));
		edits_processed++;
	}
	
	/**
	 * Return the number of IRC lines processed in this session.
	 * @return number of IRC lines processed in this session
	 */
	public long num_edits_complete(){
		return(this.edits_processed);
	}
	
	/**
	 * If there are errors, this is likely how they will be reported out
	 */
	public void onNotice(String target, IRCUser u, String msg){
		System.out.println("NOTICE: " + msg);
	}
		
		// There are a host of methods required for interface compliance (all
		// those below). However, we are not interested in their content, only
		// that of the basic message format (above). These event-handlers
		// simply ignore their input. 
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
	public void onPart(String chan, IRCUser u, String msg) {}
	public void onQuit(IRCUser u, String msg) {}
	public void onTopic(String chan, IRCUser u, String topic) {}
	public void onPing(String p) {}	
	public void unknown(String a, String b, String c, String d) {}

}
