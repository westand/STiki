package db_server;

import java.sql.PreparedStatement;

import core_objects.escape_string;
import core_objects.metadata;
import core_objects.stiki_utils;

/**
 * Andrew G. West - db_edits.java - This class performs all DB functions
 * that pertain to the deletion/update/insertion/query of edits into the main
 * edits table [main_edits]. 
 */
public class db_edits{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL inserting a row into the [main_edits] table. 
	 */
	private PreparedStatement pstmt_insert;
	
	/**
	 * SQL setting the offending-edit (OE) flag associated with an edit.
	 */
	private PreparedStatement pstmt_set_oe_flag;
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_edits] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 */
	public db_edits(stiki_con_server con_server) throws Exception{
		this.con_server = con_server;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Insert a row into the [main_edits] table by providing all fields. 
	 * @param edit_metadata Metadata object containing data on edit
	 */
	public synchronized void insert_edit(metadata edit_metadata)
			throws Exception{
		
			// NOTICE that String fields are escaped here, so as to make
			// sure no characters are lost when stored in DB. Anytime such
			// a field is queried, it should be unescaped for consistency.
		pstmt_insert.setLong(1, edit_metadata.rid);
		pstmt_insert.setLong(2, edit_metadata.pid);
		pstmt_insert.setLong(3, edit_metadata.timestamp);
		pstmt_insert.setInt(4, edit_metadata.namespace);
		pstmt_insert.setString(5, escape_string.escape(edit_metadata.title));
		pstmt_insert.setString(6, escape_string.escape(edit_metadata.user));
		pstmt_insert.setBoolean(7, edit_metadata.user_is_ipv4_or_ipv6);
		pstmt_insert.setString(8, escape_string.escape(edit_metadata.comment));
		pstmt_insert.setString(9, edit_metadata.country);
		pstmt_insert.setBoolean(10, edit_metadata.get_is_rb());
		pstmt_insert.setBoolean(11, false); // OE status unknown
		pstmt_insert.executeUpdate();
	}
	
	/**
	 * Mark an edit as being an OE (offending-edit).
	 * @param rid Revision-ID of edit identified as an offending edit
	 */
	public synchronized void mark_oe(long rid) throws Exception{
		pstmt_set_oe_flag.setLong(1, rid);
		pstmt_set_oe_flag.executeUpdate();
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_insert.close();
		pstmt_set_oe_flag.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		String insert = "INSERT INTO " + stiki_utils.tbl_edits + " ";
		insert += "VALUES (?,?,?,?,?,?,?,?,?,?,?)"; // 11 params
		pstmt_insert = con_server.con.prepareStatement(insert);
		
		String flag_oe = "UPDATE " + stiki_utils.tbl_edits + " SET OE=1 ";
		flag_oe += "WHERE R_ID=?";
		pstmt_set_oe_flag = con_server.con.prepareStatement(flag_oe);
	}

}
