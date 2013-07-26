package db_server;

import java.sql.Statement;

import core_objects.stiki_utils;

/**
 * Andrew G. West - db_status.java - DB handler class where STATUS variables
 * of the STiki runtime are handled. Insertions should be handled manually,
 * and viewing will primarily be initatiated manually. Thus, the main purpose
 * of this class is to keep the variables UPDATED.
 */
public class db_status{
	
	// **************************** PUBLIC FIELDS ****************************
	
		// All updates take identical key/value pair. We provide a public
		// list of the available keys -- then all updates can share a method.
	
	public static final String BE_QUEUE_SIZE =    "BACKEND_QUEUE_SIZE";
	public static final String THREADS_CREATED =  "WORKER_THREADS_CREATED";
	public static final String EDITS_PROC_STIKI = "EDITS_PROCESSED";
	public static final String EDITS_PROC_CBNG =  "EDITS_PROC_CBNG";
	public static final String OUTPUT_IRC_UP =    "OUTPUT_IRC_UP";
	public static final String LINK_PARSE_ACC =   "LINK_PARSE_ACC";
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_status] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 */
	public db_status(stiki_con_server con_server) throws Exception{
		this.con_server = con_server;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Update a status variable in the DB.
	 * @param key String name of the variable being updated. The available
	 * variable names are available as public variables to this class.
	 * @param value Value to which 'key' should be updated.
	 */
	public synchronized void update_status_var(String key, long value) 
			throws Exception{
		Statement stmt = con_server.con.createStatement();
		String sql = "UPDATE " + stiki_utils.tbl_status + " SET VALUE=";
		sql += value + " WHERE NAME='" + key + "'";
		stmt.executeUpdate(sql);
		stmt.close(); // Critical for memory purposes
	}
	
	/**
	 * Update a status variable in the DB.
	 * @param key String name of the variable being updated. The available
	 * variable names are available as public variables to this class.
	 * @param value Boolean value to which the 'key' should be updated.
	 * FALSE will be mapped to 0, and TRUE to 1.
	 */
	public synchronized void update_status_var(String key, boolean value) 
			throws Exception{
		if(value == true)
			update_status_var(key, 1);
		else update_status_var(key, 0);
	}

	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{}

}
