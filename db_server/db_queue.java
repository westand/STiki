package db_server;

import java.sql.PreparedStatement;

/**
 * Andrew G. West - db_queue.java - This class performs all DB functions
 * pertaining to a queue table -- which is a table containing RIDs that
 * NEED TO BE CLASSIFIED by a human, with the following criteria:
 * 
 *		[1]: This table shall not contain edits that have been classified
 *			 by a human. Once an edit has been classified, it should be 
 *			 deleted from this table, and the details of that classification 
 *			 should be written into table [feedback].
 *		[2]: This table should contain only edits which are the 'most recent'
 *			 edit on the page they were made. If an edit does not meet this
 *			 criteria, it should be deleted from the table.
 *
 * Note that this "server-side" portion of database access is concerned
 * primarily with queue additions and maintenance. The queue is also a focal
 * point for client-side action, conducted through the client interface.
 * 
 * Note that this handler encompasses MULTIPLE tables. Given the inclusion
 * of multiple edit queues, this handles all [queue_*] tables. The
 * specific table an instance handles is determined at construction.
 */
public class db_queue{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL inserting a row into a queue table. 
	 */
	private PreparedStatement pstmt_in_up;
	
	/**
	 * SQL removing all rows from this table having a particular PID.
	 */
	private PreparedStatement pstmt_delete_pid;
	
	/**
	 * Name of table being handled by this class. Table must be of 
	 * appropriate queue structure in order to be functional.
	 */
	private String table;
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_queue] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 * @param table Queue table handled by constructed class.
	 */
	public db_queue(stiki_con_server con_server, String table) throws Exception{
		this.con_server = con_server;
		this.table = table;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Insert a row into a queue table by providing those fields wich 
	 * can be known at insertion (i.e., ignore reservation ones). Note the
	 * tables primary key is on PID. Thus, we simply update the row ON
	 * DUPLICATE KEY, so we don't have to do a DELETE->INSERT.
	 * @param rid Revision-ID the row concerns.
	 * @param pid Page-identifier (article) on which 'rid' was made
	 * @param class_score Real-valued classification score of 'rid'. Higher
	 * scores are more indicative of vandalism, and vice-versa.
	 */
	public synchronized void insert_classification(long rid, long pid, 
			double class_score) throws Exception{
		
			// Users using the front-end application generally help enforce
			// constraint (2) above. However, much of this can also be handled
			// here, by enforcing PID as a UNIQUE PRIMARY key.
		
			// When inserting a row, provide known variables
			// the remainder of fields are simply default values
		pstmt_in_up.setLong(1, rid);
		pstmt_in_up.setLong(2, pid);
		pstmt_in_up.setDouble(3, class_score);
		
			// And re-issue in case of duplicate key
		pstmt_in_up.setLong(4, rid);
		pstmt_in_up.setDouble(5, class_score);
		pstmt_in_up.executeUpdate();
	}
	
	/**
	 * Delete any rows associated with some page-ID (at most one). Presumably
	 * this should be called if either of the criteria described in the 
	 * introduction to this class fail to be met.
	 * @param pid Page-ID whose row should be deleted (if present).
	 */
	public synchronized void delete_pid(long pid) throws Exception{
		pstmt_delete_pid.setLong(1, pid);
		pstmt_delete_pid.executeUpdate();
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_in_up.close();
		pstmt_delete_pid.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		
		String in_up = "INSERT INTO " + table  + " (R_ID,P_ID,SCORE)";
		in_up += "VALUES (?,?,?)"; // 3 params
		in_up += "ON DUPLICATE KEY UPDATE ";
		in_up += "R_ID=?,SCORE=?,RES_EXP=0,RES_ID=0,PASS=''"; // 2 addl.
		pstmt_in_up = con_server.con.prepareStatement(in_up);
		
		String delete_pid = "DELETE FROM " + table + " ";
		delete_pid += "WHERE P_ID=?";
		pstmt_delete_pid = con_server.con.prepareStatement(delete_pid);
	}
	
}
