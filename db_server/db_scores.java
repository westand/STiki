package db_server;

import java.sql.PreparedStatement;

/**
 * Andrew G. West - db_scores.java - This class performs all DB functions
 * that pertain to the rows in a "scores" table. It is simple; 
 * storing all RID's and their classification scores for posterity.
 * 
 * Note that this handler encompasses MULTIPLE tables. Given the inclusion
 * of multiple edit queues, this handles all [classify_*] tables. The
 * specific table an instance handles is determined at construction.
 */
public class db_scores{
		
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL inserting a row into a classification table. 
	 */
	private PreparedStatement pstmt_insert;
	
	/**
	 * Name of table being handled by this class. Table must be of appropriate
	 * classification structure in order to be functional.
	 */
	private String table;
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_classify] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 * @param table Classification table handled by constructed class.
	 */
	public db_scores(stiki_con_server con_server, String table) 
			throws Exception{
		this.con_server = con_server;
		this.table = table;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Insert a row into a classification table by providing all fields. 
	 * @param rid Revision-ID whose classification is being written
	 * @param class_score Real-valued classification score of 'rid'. Higher
	 * scores are more indicative of vandalism, and vice-versa.
	 */
	public synchronized void insert_classification(long rid,
			double class_score) throws Exception{
		
		pstmt_insert.setLong(1, rid);
		pstmt_insert.setDouble(2, class_score);
		pstmt_insert.executeUpdate();
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_insert.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		String insert = "INSERT INTO " + table + " ";
		insert += "VALUES (?,?)"; // 2 params
		pstmt_insert = con_server.con.prepareStatement(insert);
	}
	
}
