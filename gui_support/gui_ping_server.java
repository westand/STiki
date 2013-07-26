package gui_support;

import executables.stiki_frontend_driver;

/**
 * Andrew G. West - gui_ping_server.java - Some GUI users receive 
 * ConnectionExceptions and broken pipes. However, these only occur after
 * extensive pauses w/o communication. Seemingly, a timeout is being hit which
 * as of this point remains unidentified. Thus, this class, in its own thread
 * will periodically ping the DB-server in hopes of maintaining the connection.
 */
public class gui_ping_server implements Runnable{

	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * The STiki backend server will be pinged every [n] milliseconds:
	 */
	public static final int PING_INTERVAL_MS = 5000;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * The main GUI object, which maintains the connection and hands it
	 * out to all DB-handlers and other objects that need it.
	 */
	private stiki_frontend_driver parent;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_ping_server].
	 * @param parent Root GUI class. Maintains shared DB connection, and 
	 * also contains method to reset that connection if necessary.
	 */
	public gui_ping_server(stiki_frontend_driver parent){
		this.parent = parent;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Ping the server on a selected interval.
	 */
	public void run(){
		
		while(true){
			try{parent.client_interface.ping();
				Thread.sleep(PING_INTERVAL_MS);
			} catch(Exception e){
				try{ parent.reset_connection(false);
				} catch(Exception e2){}
			} // Ping the connection, if it fails, try to reset the connection.
			  // If unable to reset (?) -- bigger problems are forthcoming

		} // Ping to infinity. (When main GUI window is closed, this thread
		  // will be interuppted and shutdown by thread-pool manager).
	}

}
