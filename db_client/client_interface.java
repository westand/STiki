package db_client;

import java.sql.CallableStatement;

import core_objects.escape_string;
import core_objects.metadata;
import core_objects.stiki_utils;
import core_objects.stiki_utils.SCORE_SYS;
import edit_processing.rollback_handler.RB_TYPE;

/**
 * Andrew G. West - client_interface.java - Rather than allowing clients 
 * direct SQL query access onto the STiki servers, all requests are
 * handled by stored procedures. This class provides a Java interface
 * to those stored procedures.
 * 
 * These calls should be made as high-level as possible. As a security 
 * measure, this will prevent exposing low-level primitives to clients.
 * Moreover, this makes it easier to log and audit all calls made by clients.
 */
public class client_interface{
		
	// ***************************** PUBLIC FIELDS ***************************

	/**
	 * Persistent connection to the STiki server (limited permissions).
	 */
	public final stiki_con_client con_client;
	
	/**
	 * This PUBLIC manager handles all stored-procedures relating to the 
	 * queuing system. It is treated separate since the various queues
	 * sometimes have dependent behaviors, and the stored procedures are
	 * dynamic in terms of the table(s) over which they operate.
	 */
	public final qmanager_client queues;
	
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * SQL call inserting a classification (feedback) for an edit.
	 */
	private CallableStatement cstmt_feedback_insert;
	
	/**
	 * SQL call determinining the country-code mapping for an IP address.
	 */
	private CallableStatement cstmt_geo_country;
	
	/**
	 * SQL call inserting an "offending edit" -- and many subsequent triggers.
	 */
	private CallableStatement cstmt_oe_insert;
	
	/**
	 * SQL call determining which queue to use, by default (at program start).
	 */
	private CallableStatement cstmt_default_queue;
	
	/**
	 * SQL call to determine if a user has *explicit" permission to use tool.
	 */
	private CallableStatement cstmt_users_explicit;
	
	/**
	 * SQL call to report on recent usage of STiki (and specific queues).
	 */
	private CallableStatement cstmt_recent_use;
	
	/**
	 * SQL call to compile a simplified learboard for client purposes.
	 */
	private CallableStatement cstmt_leaderboard;
	
	/**
	 * SQL call which pings STiki server (a meaningless stored procedure call).
	 */
	private CallableStatement cstmt_ping;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Create a client interface (which creates the connection to server),
	 * and prepare all SP calls that will be used. Note that after this class
	 * is initialized, the public connection should be checked for NULL.
	 */
	public client_interface() throws Exception{
		this.con_client = new stiki_con_client();
		if(this.con_client.con != null){
			this.queues = new qmanager_client(this.con_client);
			prep_statements();
		} else{ // If connection fails, don't let errors cascade
			this.queues = null;
		}
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Insert "feedback" -- a classification performed on the STiki frontend
	 * @param rid Revision-ID whose feedback is being written
	 * @param label Label of edit (0=innocent, 1=vandalism).
	 * @param user Username of the individual leaving feedback.
	 */
	public synchronized void feedback_insert(long rid, int label, String user) 
			throws Exception{	
		cstmt_feedback_insert.setLong(1, rid);
		cstmt_feedback_insert.setInt(2, label);
		cstmt_feedback_insert.setString(3, user);
		cstmt_feedback_insert.execute();
	}
	
	/**
	 * Given an IP address, return the two-letter country-code of its origin.
	 * @param int_ip IP address, in integer format
	 * @return Two letter country-code where 'int_ip' resides, or the empty
	 * string if there was an error or such data was not available.
	 */
	public synchronized String geo_country(long int_ip) throws Exception{
		cstmt_geo_country.setLong(1, int_ip);
		cstmt_geo_country.execute();
		return(cstmt_geo_country.getString(2));
	}
	
	/**
	 * Insert a row into [offending_edits], indicating a guilty edit.
	 * @param md Metadata object containing data on GUILTY edit
	 * @param flag_rid RID of the flagging edit, if the offending edit was 
	 * located via ROLLBACK. If the offending edit was detected using the STiki
	 * front-end tool, then the 'flag_rid' field should be zero.
	 * @param rb_code Integer code indicating the nature of the ROLLBACK
	 * (for example, whether it was HUMAN or BOT initiated).
	 */
	public synchronized void oe_insert(metadata md, long flag_rid, 
			RB_TYPE rbtype) throws Exception{
		cstmt_oe_insert.setLong(1, md.rid);
		cstmt_oe_insert.setLong(2, md.pid);
		cstmt_oe_insert.setLong(3, md.timestamp);
		cstmt_oe_insert.setInt(4, md.namespace);
		cstmt_oe_insert.setString(5, escape_string.escape(md.user));
		cstmt_oe_insert.setLong(6, flag_rid);
		cstmt_oe_insert.setString(7, md.country);
		if(rbtype.equals(RB_TYPE.HUMAN))
			cstmt_oe_insert.setInt(8, 1);
		else // if(rbtype.equals(RB_TYPE.BOT))
			cstmt_oe_insert.setInt(8, 2);
		cstmt_oe_insert.execute();
	}
	
	/**
	 * Determine the default queue which should be selected at STiki startup.
	 * @return Enumeration identifying which queue should be default
	 */
	public synchronized SCORE_SYS default_queue() throws Exception{
		cstmt_default_queue.execute();
		return(stiki_utils.constant_to_queue(cstmt_default_queue.getInt(1)));
	}
	
	/**
	 * Determine if some Wikipedia/STiki user has explicit tool rights
	 * (as opposed to the implicit ones per qualification conditions)
	 * @param uname Username of some individual
	 * @return TRUE if the 'uname' has explicit permission. FALSE, otherwise
	 */
	public synchronized boolean user_explicit(String uname) throws Exception{
		cstmt_users_explicit.setString(1, uname);
		cstmt_users_explicit.execute();
		return(cstmt_users_explicit.getInt(2) > 0);
	}
	
	/**
	 * Retrieve recent usage statistics for STiki and its individual queues
	 * (perhaps to inform usage strategy moving forward).
	 * @param time_ago. Unix timestamp determing threshold for 'recent' events
	 * @return An 8-tuple containing usage information relative to 
	 * 'time_ago'. (1) Recent STiki uses, and (2) recent STiki reverts.
	 * This pattern continues for (3,4) CBNG, (5,6) WikiTrust, and (7,8) SPAM.
	 */
	public synchronized int[] recent_use(long time_ago) throws Exception{
		int[] recent_array = new int[8];
		cstmt_recent_use.setLong(1, time_ago);
		cstmt_recent_use.execute();
		recent_array[0] = cstmt_recent_use.getInt(2);
		recent_array[1] = cstmt_recent_use.getInt(3);
		recent_array[2] = cstmt_recent_use.getInt(4);
		recent_array[3] = cstmt_recent_use.getInt(5);
		recent_array[4] = cstmt_recent_use.getInt(6);
		recent_array[5] = cstmt_recent_use.getInt(7);
		recent_array[6] = cstmt_recent_use.getInt(8);
		recent_array[7] = cstmt_recent_use.getInt(9);
		return(recent_array);
	}
	
	/**
	 * Return a CSV format of a simplified leaderboard.
	 * @return CSV string of a simplified leadeboard. It is all a single
	 * CSV, but should be itemized into 4 columns: (1) username,
	 * (2) classification quantity, (3) # vandalism, (4) # AGF
	 */
	public synchronized String leaderboard() throws Exception{
		cstmt_leaderboard.execute();
		return(cstmt_leaderboard.getString(1));
	}
	
	/**
	 * Ping the STiki server by calling a meaningless stored procedure.
	 */
	public synchronized void ping() throws Exception{
		cstmt_ping.execute();
	}
		
	/**
	 * Close all connections and other DB infrastructure opened by this class.
	 */
	public synchronized void shutdown() throws Exception{
		cstmt_feedback_insert.close();
		cstmt_geo_country.close();
		cstmt_oe_insert.close();
		cstmt_default_queue.close();
		cstmt_recent_use.close();
		cstmt_ping.close();
		con_client.con.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare the databse (stored procedure) calls to be used by this class.
	 */
	private void prep_statements() throws Exception{
		
		cstmt_feedback_insert = con_client.con.prepareCall(
			"{CALL client_feedback_insert(?,?,?)}"); // 3 IN params
		
		cstmt_geo_country = con_client.con.prepareCall(
			"{CALL client_geo_country(?,?)}");  // 1 IN, 1 OUT params
		cstmt_geo_country.registerOutParameter(2, java.sql.Types.CHAR);
		
		cstmt_oe_insert = con_client.con.prepareCall(
			"{CALL client_oe_insert(?,?,?,?,?,?,?,?)}"); // 8 IN params
		
		cstmt_default_queue = con_client.con.prepareCall(
			"{CALL client_default_queue(?)}"); // 1 OUT param
		cstmt_default_queue.registerOutParameter(1, java.sql.Types.INTEGER);
		
		cstmt_users_explicit = con_client.con.prepareCall(
			"{CALL client_users_explicit(?,?)}"); // 1 IN, 1 OUT params
		cstmt_users_explicit.registerOutParameter(2, java.sql.Types.INTEGER);
		
		cstmt_recent_use = con_client.con.prepareCall( // 1 IN, 8 OUT params
				"{CALL client_recent_use(?,?,?,?,?,?,?,?,?)}"); 
		cstmt_recent_use.registerOutParameter(2, java.sql.Types.INTEGER);
		cstmt_recent_use.registerOutParameter(3, java.sql.Types.INTEGER);
		cstmt_recent_use.registerOutParameter(4, java.sql.Types.INTEGER);
		cstmt_recent_use.registerOutParameter(5, java.sql.Types.INTEGER);
		cstmt_recent_use.registerOutParameter(6, java.sql.Types.INTEGER);
		cstmt_recent_use.registerOutParameter(7, java.sql.Types.INTEGER);
		cstmt_recent_use.registerOutParameter(8, java.sql.Types.INTEGER);
		cstmt_recent_use.registerOutParameter(9, java.sql.Types.INTEGER);
		
		cstmt_leaderboard = con_client.con.prepareCall(
				"{CALL client_leaderboard(?)}"); // 1 OUT param
		cstmt_leaderboard.registerOutParameter(1, java.sql.Types.VARCHAR);
		
		cstmt_ping = con_client.con.prepareCall("{CALL client_ping()}");
	}

}