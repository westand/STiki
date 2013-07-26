package db_server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import core_objects.escape_string;
import core_objects.metadata;
import core_objects.stiki_utils;
import edit_processing.rollback_handler.RB_TYPE;

/**
 * Andrew G. West - db_off_edits.java - This class performs all DB functions
 * that pertain to the deletion/update/insertion of offending edits. Crucially,
 * SELECT queries run over this data are handled by the reputation module.
 * 
 * Offending edits include bad-edits identified by any mechanism. This
 * includes both (1) rollback-edits, as are found by automatically parsing
 * edit comments, and (2) feedback provided at the STiki front-end.
 * 
 * Note that many methods are synchronized, preventing their simultaneous
 * execution. Such mutual exclusion is not always necessary (e.g., one could
 * query article and user OEs simultaneously). However, this Java mutex is of
 * little significance given that these statements share a MySQL connection,
 * and therefore some form of mutex is being applied at the server.
 */
public class db_off_edits{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * The schema of the [offending_edits] table requires that the 
	 * 'flagging RID' of an offending-edit (OE) be given. This works fine
	 * if the OE is rollback sourced. However, if the OE was found on the 
	 * STiki front-end, we assign the field to the value below.
	 * Why? (Because the revert has not yet been comitted or is in processing,
	 * we don't yet know the RID -- and we don't want to wait for it).
	 */
	public static final long FLAG_RID_CLIENT = 0;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL inserting a row into the [offending_edits] table. 
	 */
	private PreparedStatement pstmt_insert;
	
	/**
	 * SQL returning the timestamp when some user last committed an OE.
	 */
	private PreparedStatement pstmt_ts_last_user_oe;
	
	/**
	 * SQL returning temporally relevant OEs mapping to some user.
	 */
	private PreparedStatement pstmt_oes_user;
	
	/**
	 * SQL returning temporally relevant OEs mapping to some article.
	 */
	private PreparedStatement pstmt_oes_article;
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_off_edits] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs).
	 */
	public db_off_edits(stiki_con_server con_server) throws Exception{
		this.con_server = con_server;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Insert a row into the [offending_edit] table by providing all fields,
	 * as well as DB-handlers for tables with triggered relationships.
	 * @param off_edit Metadata object containing data on GUILTY edit
	 * @param flag_rid RID of flagging edit. if the offending edit was located
	 * via ROLLBACK. If the offending edit was detected using the STiki
	 * front-end tool, then the 'flag_rid' field should be zero.
	 * @param db_edits DB-handler for trigger into the [all_edits] table
	 * @param db_feat DB-handler for trigger into the [features] table
	 * @param db_country DB-handler for trigger into the [country] table
	 * @param db_links DB-handler for trigger into the [hyperlinks] table
	 */
	public synchronized void new_oe(metadata off_edit, long flag_rid,
			RB_TYPE rb_type, db_edits db_edits, db_features db_feat, 
			db_country db_country, db_hyperlinks db_links) throws Exception{
		
			// Any errors at insertion are likely due to a duplicate primary
			// key (multiple flaggings for single OE), which are just ignored
			//
			// Note that these errors are caught and the method returns, 
			// without handling the trigger-calls. Given that this method is
			// the only one to flag OE's, it is assumed these calls would
			// also result in redundancy (i.e., unneccessary updates).
			//
			// Note that the user-name is escaped when written to the DB --
			// queries against that field must follow the same policy
		try{
			pstmt_insert.setLong(1, off_edit.rid);
			pstmt_insert.setLong(2, off_edit.pid);
			pstmt_insert.setLong(3, off_edit.timestamp);
			pstmt_insert.setInt(4, off_edit.namespace);
			pstmt_insert.setString(5, escape_string.escape(off_edit.user));
			pstmt_insert.setLong(6, flag_rid);
			pstmt_insert.setInt(7, -1); // Research: Unknown # of views
			pstmt_insert.executeUpdate();
		} catch(Exception e){return;};

			// New OE's trigger flags and increments in other tables
		db_edits.mark_oe(off_edit.rid);
		db_feat.set_guilty_label(off_edit.rid);
		db_country.increment_bad(off_edit.country, off_edit.timestamp);
		db_links.flag_as_oe(off_edit.rid, rb_type);
	}
	
	/**
	 * Return the timestamp at which some user last committed an OE.
	 * @param user User-name or IP of user whose behavior is being examined
	 * @return Timestamp at which 'user' last committed an OE, or -1 if
	 * no such offending edit exists.
	 */
	public synchronized long ts_last_user_oe(String user) throws Exception{
		
			// Note that the user-argument is escaped before querying
		pstmt_ts_last_user_oe.setString(1, escape_string.escape(user));
		ResultSet rs = pstmt_ts_last_user_oe.executeQuery();
		if(rs.next()){
			long ts_last_oe = rs.getLong(1);
			if(ts_last_oe == 0)
				return (-1); // Catch the null-case
			else return (ts_last_oe);
		} // We expect there to be a row, legitimate, or `null'
		return (-1); // If no-rows or null returned, default to fail
	}
	
	/**
	 * Return all temporally relevant offending-edits mapping to some user.
	 * @param user User whose recent OE history is being examined
	 * @return List of timestamps on ['now'-HIST_WINDOW,'now'], at 
	 * which user 'user' committed an offending edit. These time bounds
	 * are enforced without constraint by [db_rb_migrate.java].
	 */
	public synchronized List<Long> recent_user_oes(String user) 
			throws Exception{
		
			// Obvious question: Why copy ResultSet to List when we could
			// just return the former? Its a multi-threading issue. The
			// ResultSet is tied to the statement -- thus a re-use of the
			// stmt breaks the ResultSet, whose access may not be synched.
		List<Long> oes_list = new LinkedList<Long>();
		pstmt_oes_user.setString(1, escape_string.escape(user)); // Escaped
		ResultSet rs = pstmt_oes_user.executeQuery();
		while(rs.next())
			oes_list.add(rs.getLong(1));
		return(oes_list);
	}
	
	/**
	 * Return all temporally relevant offending-edits mapping to some article.
	 * @param pid Page (by ID), whose recent OE history is being examined
	 * @return List of timestamps on ['now'-HIST_WINDOW,'now'], at 
	 * which user 'user' committed an offending edit. These time bounds
	 * are enforced without constraint by [db_rb_migrate.java].
	 */
	public synchronized List<Long> recent_article_oes(long pid) 
			throws Exception{
			
			// Obvious question: Why copy ResultSet to List when we could
			// just return the former? Its a multi-threading issue. The
			// ResultSet is tied to the statement -- thus a re-use of the
			// stmt breaks the ResultSet, whose access may not be synched.
		List<Long> oes_list = new LinkedList<Long>();
		pstmt_oes_article.setLong(1, pid);
		ResultSet rs = pstmt_oes_article.executeQuery();
		while(rs.next())
			oes_list.add(rs.getLong(1));
		return(oes_list);
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_insert.close();
		pstmt_ts_last_user_oe.close();
		pstmt_oes_user.close();
		pstmt_oes_article.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		String insert = "INSERT INTO " + stiki_utils.tbl_off_edits + " ";
		insert += "VALUES (?,?,?,?,?,?,?)"; // 7 params
		pstmt_insert = con_server.con.prepareStatement(insert);
		
		String ts_last_user_oe = "SELECT MAX(TS) FROM ";
		ts_last_user_oe += stiki_utils.tbl_off_edits + " WHERE USER=?";
		pstmt_ts_last_user_oe = con_server.con.prepareStatement(ts_last_user_oe);
		
		String oes_user = "SELECT TS FROM " + stiki_utils.tbl_off_edits + " ";
		oes_user += "WHERE USER=?";
		pstmt_oes_user = con_server.con.prepareStatement(oes_user);
		
		String oes_article = "SELECT TS FROM " + stiki_utils.tbl_off_edits;
		oes_article += " WHERE P_ID=?";
		pstmt_oes_article = con_server.con.prepareStatement(oes_article);
	}

}
