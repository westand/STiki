package db_server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import core_objects.feature_set;
import core_objects.stiki_utils;

/**
 * Andrew G. West - db_features.java - This class performs all DB functions
 * that pertain to edit-features (i.e., the maintenance of table [features]). 
 */
public class db_features{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL inserting a row into the [features] table. 
	 */
	private PreparedStatement pstmt_insert;
	
	/**
	 * SQL setting the 'label' of a feature set. Edits are initialized
	 * as innocent, so only discovered vandalism needs flagged.
	 */
	private PreparedStatement pstmt_set_label;
	
	/**
	 * SQL fetching all RIDs that exist in a specified RID interval
	 */
	private PreparedStatement pstmt_rids_only_interval;
	
	/**
	 * SQL fetching feature-row for a single RID.
	 */
	private PreparedStatement pstmt_fetch_rid;
	
	/**
	 * SQL fetching feature-rows on a specified RID interval.
	 */
	private PreparedStatement pstmt_fetch_rid_interval;
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [features] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 */
	public db_features(stiki_con_server con_server) throws Exception{
		this.con_server = con_server;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Insert a row into the [features] table by providing all fields. 
	 * @param feature_set Metadata object containing data on edit
	 */
	public synchronized void insert_feature_row(feature_set feature_set)
			throws Exception{
		pstmt_insert.setBoolean(1, feature_set.LABEL);
		pstmt_insert.setLong(2, feature_set.R_ID);
		pstmt_insert.setBoolean(3, feature_set.IS_IP);
		pstmt_insert.setDouble(4, feature_set.REP_USER);
		pstmt_insert.setDouble(5, feature_set.REP_ARTICLE);
		pstmt_insert.setFloat(6, feature_set.TOD);
		pstmt_insert.setInt(7, feature_set.DOW);
		pstmt_insert.setLong(8, feature_set.TS_R);
		pstmt_insert.setLong(9, feature_set.TS_LP);
		pstmt_insert.setLong(10, feature_set.TS_RBU);
		pstmt_insert.setInt(11, feature_set.COMM_LENGTH);
		pstmt_insert.setInt(12, feature_set.BYTE_CHANGE);
		pstmt_insert.setDouble(13, feature_set.REP_COUNTRY);
		pstmt_insert.setInt(14, feature_set.NLP_DIRTY);
		pstmt_insert.setInt(15, feature_set.NLP_CHAR_REP);
		pstmt_insert.setDouble(16, feature_set.NLP_UCASE);
		pstmt_insert.setDouble(17, feature_set.NLP_ALPHA);
		pstmt_insert.executeUpdate();
	}
	
	/**
	 * Set the edit-label to identify an edit as being an OE (offending-edit). 
	 * Note that edit labels are intialized to innocent.
	 * @param rid Revision-ID of edit identified as an offending edit
	 */
	public synchronized void set_guilty_label(long rid) throws Exception{
		pstmt_set_label.setLong(1, rid);
		pstmt_set_label.executeUpdate();
	}
	
	/**
	 * Output a list of RIDs that occur internal to some RID interval (those
	 * not in NS0, not by anonymous users, not true edits -- won't exist). 
	 * @param start_rid Lower bound of RIDs to be included (inclusive)
	 * @param end_rid Upper bound of RIDs to be included (inclusive)
	 * @return List of all RIDs on 'start_rid' <= R_ID <= 'end_rid', about
	 * which the database has data.
	 */
	public synchronized List<Long> get_rids_in_interval(long start_rid, 
			long end_rid) throws Exception{
		
			// First, query to get the ResultSet
		pstmt_rids_only_interval.setLong(1, start_rid);
		pstmt_rids_only_interval.setLong(2, end_rid);
		ResultSet rs = pstmt_rids_only_interval.executeQuery();
		
			// Then turn raw ResultSet into Java-list
		List<Long> rid_list = new ArrayList<Long>();
		while(rs.next())
			rid_list.add(rs.getLong(1));
		rs.close();
		return(rid_list);	
	}
	
	/**
	 * Return a single feature-set by providing an RID.
	 * @param rid Revision-ID whose feature set is desired
	 * @return Feature-set associated with revision 'rid', or NULL if the
	 * backend database contains no data for 'rid'
	 */
	public synchronized feature_set feature_set_by_rid(long rid) 
			throws Exception{
		
		pstmt_fetch_rid.setLong(1, rid);
		ResultSet rs = pstmt_fetch_rid.executeQuery();
		if(rs.next()) // At most one result
			return(convert_db_row_to_fs(rs));
		return(null);
	}
	
	/**
	 * Output a list of [feature_set] objects containing all feature rows
	 * from the DB with a revision-ID between provided bounds.
	 * @param start_rid Lower bound of RIDs to be included (inclusive)
	 * @param end_rid Upper bound of RIDs to be included (inclusive)
	 * @return List of all feature rows from the DB where 
	 * 'start_rid' <= R_ID <= 'end_rid', wrapped as [feature_set objects]
	 */
	public synchronized List<feature_set> get_features_in_interval(
			long start_rid, long end_rid) throws Exception{
		
			// First, query to get the ResultSet
		pstmt_fetch_rid_interval.setLong(1, start_rid);
		pstmt_fetch_rid_interval.setLong(2, end_rid);
		ResultSet rs = pstmt_fetch_rid_interval.executeQuery();
		
			// Then turn raw ResultSet into list of Java objects
		List<feature_set> feature_list = new ArrayList<feature_set>();
		while(rs.next())
			feature_list.add(convert_db_row_to_fs(rs));
		rs.close();
		return(feature_list);
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_insert.close();
		pstmt_set_label.close();
		pstmt_rids_only_interval.close();
		pstmt_fetch_rid.close();
		pstmt_fetch_rid_interval.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		String insert = "INSERT INTO " + stiki_utils.tbl_features + " ";
		insert += "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"; // 17 params
		pstmt_insert = con_server.con.prepareStatement(insert);
		
		String set_label = "UPDATE " + stiki_utils.tbl_features + " SET ";
		set_label += "LABEL=1 WHERE R_ID=?";
		pstmt_set_label = con_server.con.prepareStatement(set_label);
		
		String rids_only = "SELECT R_ID FROM " + stiki_utils.tbl_features;
		rids_only += " WHERE R_ID>=? AND R_ID<=?";
		pstmt_rids_only_interval = con_server.con.prepareStatement(rids_only);
		
		String fetch_rid = "SELECT * FROM " + stiki_utils.tbl_features + " ";
		fetch_rid += " WHERE R_ID=?";
		pstmt_fetch_rid = con_server.con.prepareStatement(fetch_rid);
		
		String fetch_rid_interval = "SELECT * FROM ";
		fetch_rid_interval += stiki_utils.tbl_features + " WHERE ";
		fetch_rid_interval += "R_ID>=? AND R_ID<=?";
		pstmt_fetch_rid_interval = con_server.con.prepareStatement(
				fetch_rid_interval);
	}
	
	/**
	 * Convert a row (containg all fields) from the [features] table, into 
	 * a [feature_set] Java object -- simplifying data handling.
	 * @param rs ResultSet object which was generated using a "SELECT *"
	 * query over the [features] table. The output object will encode that
	 * row to which the ResultSet cursor is currently pointing.
	 */
	private static feature_set convert_db_row_to_fs(ResultSet rs) 
			throws Exception{
		
		return (new feature_set(rs.getBoolean(1), rs.getLong(2), 
				rs.getBoolean(3), rs.getDouble(4), rs.getDouble(5), 
				rs.getFloat(6), rs.getInt(7), rs.getLong(8), 
				rs.getLong(9), rs.getLong(10), rs.getInt(11), 
				rs.getInt(12), rs.getDouble(13), rs.getInt(14), 
				rs.getInt(15), rs.getDouble(16), rs.getDouble(17)));
	}

}
