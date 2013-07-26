package ext_queues;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.schwering.irc.lib.IRCConnection;

import db_server.qmanager_server;

/**
 * Andrew G. West - cluebotng_irc.java - Connect to the Cluebot-NG scoring
 * feed via IRC. Then, attach a listener/parser object onto this feed
 * which processes the edits into a queue on the STiki server.
 */
public class cluebotng_irc extends Thread{
	
	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * The channel to which the listener needs to connect.
	 */
	public static final String CHANNEL = "#cluebotng-spam";
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Connection to the IRC Server (#cluebotng-spam @ irc.cluenet.org).
	 */
	private IRCConnection con_irc;
	
	/**
	 * Event listener to be attached to the IRC connection.
	 */
	private cluebotng_listener irc_listener;
	
	
	// ***************************** CONSTRUCTORS ****************************

	/**
	 * Construct an [cluebotng_irc] object -- which establishes a connection
	 * to the Cluebot servers and implements a listener/event-handler on top 
	 * of it, to signal all the necessary back-end processsing.
	 * @param threads Executor to be given individual RIDs to process
	 * @param qmanager Queue manager, so that the parse of the IRC feed
	 * can be used to update the "Cluebot-NG" specific queue.
	 */
	public cluebotng_irc(ExecutorService threads, qmanager_server qmanager) 
			throws Exception{
		
			// Connection and username settings
		String host = "irc.cluenet.org";
		int p_min = 6667; // Port-range minimum
		int p_max = 6669; // Port-range maximum
		String pass = null;
		String user = "STikiQueuer";

			// Prepare connection, event-handler, and basic-settings
		con_irc = new IRCConnection(host, p_min, p_max, pass, user, user, user);
		irc_listener = new cluebotng_listener(con_irc, threads, qmanager);
		con_irc.addIRCEventListener(irc_listener);
		con_irc.setDaemon(true);
		con_irc.setColors(false); 
		con_irc.setPong(true);

		irc_connect(50);	// ClueNet servers seem to be funky as of late:
							// a very large, but functional, retry count
		
			// Fork the thread. Notice that the connection has not yet
			// joined a channel. The semantics of the ClueBot's IRC requires
			// that we be FULLY connected before the channel join occurs,
			// so this is implemented internal to the EventListener.
		start(); 
	}
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Return the number of IRC lines processed in this session.
	 * @return the number of IRC lines processed in this session.
	 */
	public long num_edits_processed(){
		return(irc_listener.num_edits_complete());
	}
	
	/**
	 * Close the connection to the IRC-server
	 */
	public void shutdown(){
		con_irc.close();
	}

	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Establish an IRC connection. This version is method-wrapped so 
	 * it can be recursively called in case of error.
	 * @param num_retries Maximum number of retries to attempt.
	 */
	public void irc_connect(int num_retries){
		try{ con_irc.connect(); } 
		catch(IOException ioexc){
			if(num_retries == 0){
				System.out.println("Error at ClueNet IRC connection:");
				ioexc.printStackTrace(); 
			} else irc_connect(num_retries - 1);
		} // If an exception occurs, consider a retry attempt
	}
	
}
