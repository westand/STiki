package irc_work;


import java.io.IOException;
import java.util.concurrent.DelayQueue;

import org.schwering.irc.lib.*;

import edit_processing.rid_queue_elem;

/**
 * Andrew G. West - irc_listener.java - The purpose of this class is to 
 * listen to an IRC channel (presumably one publishing `Recent Changes' on
 * Wikipedia), and listen/parse for actions that initiate STiki processing.
 */
public class irc_listener extends Thread{
	
	// *************************** PRIVATE FIELDS ****************************
	
	/**
	 * This enumeration lists the different channels (flavors) of `Recent 
	 * Changes' are made available by Wikipedia (different languages, 
	 * subsets of user types, and possible vandalism). 
	 */
	private enum CHANNELS{EN_WIKI_ALL};
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Connection to the IRC Sserver
	 */
	private IRCConnection con_irc;
	
	
	// ***************************** CONSTRUCTORS ****************************

	/**
	 * Construct an [irc_listener] object -- which establishes a connection
	 * to the server and implements a listener/event-handler on top of it,
	 * to signal all the necessary back-end processsing.
	 * @param rid_queue Queue to which new RIDs should be added
	 */
	public irc_listener(DelayQueue<rid_queue_elem> rid_queue){
		
			// IRC settings perceived as static/inconsequentail
		String host = "irc.wikimedia.org";
		int p_min = 6667; // Port-range minimum
		int p_max = 6669; // Port-range maximum
		String pass = null;
		String user = "STikiQueuer";

			// Prepare connection, event-handler, and basic-settings
		con_irc = new IRCConnection(host, p_min, p_max, pass, user, user, user);
		con_irc.addIRCEventListener(new irc_events(rid_queue)); 
		con_irc.setDaemon(true);
		con_irc.setColors(false); 

		try{ con_irc.connect(); } catch(IOException ioexc){
			System.out.println("Error establishing RC IRC connection:");
			ioexc.printStackTrace(); 
		} // Establish connection to IRC server
		
			// With connection established, join channel, fork thread
		con_irc.doJoin(this.enum_to_string(CHANNELS.EN_WIKI_ALL));
		start(); // Start as own thread, so processing can occur
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Close the connection to the IRC-server
	 */
	public void shutdown(){
		con_irc.close();
	}
	

	 // *************************** PRIVATE METHODS ***************************

	 /**
	  * Convert a user-friendly IRC-channel identifier (per an enumeration),
	  * into the actual IRC channel string. 
	  * @param enum_channel IRC-channel ID, per an enumeration in this class
	  * @return Actual name of the IRC channel that should be joined
	  */
	 private String enum_to_string(CHANNELS enum_channel){
		 if(enum_channel.equals(CHANNELS.EN_WIKI_ALL))
			 return("#en.wikipedia");
		 else return "";
	 }

}
