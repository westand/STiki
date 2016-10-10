package irc_work;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.schwering.irc.lib.IRCConnection;

/**
 * Andrew G. West - irc_echo_test.java - Simple echo listener on an IRC
 * feed in order to debug connections and output. 
 */
public class irc_echo_test extends Thread{

	// ***************************** MAIN METHOD ****************************
		
	/**
	 * Test harness
	 * @param args No arguments are taken by this method
	 */
	public static void main(String[] args) throws Exception{
		ExecutorService pool = Executors.newFixedThreadPool(4);
		new irc_echo_test(pool);
		Thread.sleep(50000000);
	}	
	
		
	// ***************************** CONSTRUCTORS ****************************	
	
	/**
	 * Construct a [irc_echo_test] class, connecting to the en.wikipedia
	 * recent changes feed and mirroring all output to STDOUT. This 
	 * is envisioned primarily as a debugging tool.
	 */
	public irc_echo_test(ExecutorService threads){
		
		String host = "irc.cluenet.org"; // irc.cluenet.org = irc.wikimedia.org
		int p_min = 6667; // Port-range minimum
		int p_max = 6669; // Port-range maximum
		String pass = null;
		String user = "STikiQueue99";

			// Prepare connection, event-handler, and basic-settings
		IRCConnection con_irc;
		con_irc = new IRCConnection(host, p_min, p_max, pass, user, user, user);
		con_irc.addIRCEventListener(new irc_echo_events(con_irc)); 
		con_irc.setDaemon(true);
		con_irc.setColors(false);
		con_irc.setPong(true);

		try{ con_irc.connect(); } catch(IOException ioexc){
			System.out.println("Error establishing RC IRC connection:");
			ioexc.printStackTrace(); 
		} // Establish connection to IRC server
		
			// With connection established, join channel, fork thread
		//con_irc.doJoin("#cluebotng-spam"); // #cluebotng-spam	= en.wikipedia
		start(); // Start as own thread, so processing can occur
	}

}
