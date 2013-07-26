package db_server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import core_objects.stiki_utils;

/**
 * Andrew G. West - db_country.java - This class is a database-handler for
 * the [country] table, which is used in the calculation of country reputation.
 * 
 * Such reputation must be massively aggregated in order to be efficient.
 * Basically, a country's reputation is one minus the percentage of edits
 * that were vandalism in some specified time window.
 */
public class db_country{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * A countries reputation is calculated by examining edits in a
	 * historical window of the following duration (unit is DAYS).
	 */
	public static final int COUNTRY_REP_WINDOW = stiki_utils.HIST_WINDOW;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL incrementing the 'all edits' counter for some country, on some day.
	 */
	private PreparedStatement pstmt_inc_all;
	
	/**
	 * SQL incrementing the 'bad edits' counter for some country, on some day.
	 */
	private PreparedStatement pstmt_inc_bad;
	
	/**
	 * SQL checking to see if an entry exists for a country, on some day.
	 */
	private PreparedStatement pstmt_check_country;
	
	/**
	 * SQL initializing an entry for a particular country, on some day.
	 */
	private PreparedStatement pstmt_insert_country;
	
	/**
	 * SQL aggregating a countries history to produce a reputation/utilization.
	 */
	private PreparedStatement pstmt_agg_rep;

	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_country] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs).
	 */
	public db_country(stiki_con_server con_server) throws Exception{
		this.con_server = con_server;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Increment the num. of "all edits" assoc. to some country, on some day.
	 * @param country_code Two letter country-code for country to increment
	 * @param ts UNIX timestamp (seconds), when the edit was made
	 */
	public synchronized void increment_all(String country_code, long ts) 
			throws Exception{
		
			// If summary row does not exist; create. Then, increment
		long unix_day = stiki_utils.unix_day_at_unix_sec(ts);
		exists_or_create(country_code, unix_day);
		pstmt_inc_all.setLong(1, unix_day);
		pstmt_inc_all.setString(2, country_code);
		pstmt_inc_all.executeUpdate();
	}
	
	/**
	 * Increment the num. of "bad edits" assoc. to some country, on some day.
	 * @param country_code Two letter country-code for country to increment
	 * @param ts UNIX timestamp (seconds), when the edit was made
	 */
	public synchronized void increment_bad(String country_code, long ts) 
			throws Exception{
		
			// If summary row does not exist; create. Then, increment
		long unix_day = stiki_utils.unix_day_at_unix_sec(ts);
		exists_or_create(country_code, unix_day);
		pstmt_inc_bad.setLong(1, unix_day);
		pstmt_inc_bad.setString(2, country_code);
		pstmt_inc_bad.executeUpdate();
	}
	
	/**
	 * Return the current reputation for some country.
	 * @param country_code Two letter country-code for country to valuate
	 * @return The percentage of edits originating from 'country_code', over 
	 * interval [COUNTRY_REP_WINDOW] which were offending-edits. Should be
	 * on [0,1]. If a DBZ error would have occured, -1.0 is returned.
	 * If the empty string is passed as a country code, -1.0 is returned.
	 */
	public synchronized double cur_country_rep(String country_code)
			throws Exception{
		
		if(country_code.equals(""))
			return(-1.0); // If no country code provided
		
		long start_day = (stiki_utils.cur_unix_day()-COUNTRY_REP_WINDOW);
		pstmt_agg_rep.setString(1, country_code);
		pstmt_agg_rep.setLong(2, start_day);
		int all_edits = 0; int bad_edits = 0;
		
		ResultSet rs = pstmt_agg_rep.executeQuery();
		if(rs.next()){
			bad_edits = rs.getInt(1);
			all_edits = rs.getInt(2);
		} // At most there will be one result row
		
		if(all_edits != 0)
			return((bad_edits * 1.0) / (all_edits * 1.0));
		else return (-1.0);
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_inc_all.close();
		pstmt_inc_bad.close();
		pstmt_check_country.close();
		pstmt_insert_country.close();
		pstmt_agg_rep.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	

		String inc_all = "UPDATE " + stiki_utils.tbl_country + " SET ";
		inc_all += "ALL_EDITS=(ALL_EDITS+1) WHERE UNIX_DAY=? AND COUNTRY=?";
		pstmt_inc_all = con_server.con.prepareStatement(inc_all);
		
		String inc_bad = "UPDATE " + stiki_utils.tbl_country + " SET ";
		inc_bad += "BAD_EDITS=(BAD_EDITS+1) WHERE UNIX_DAY=? AND COUNTRY=?";
		pstmt_inc_bad = con_server.con.prepareStatement(inc_bad);
		
		String check = "SELECT UNIX_DAY FROM " + stiki_utils.tbl_country + " ";
		check += "WHERE UNIX_DAY=? AND COUNTRY=?";
		pstmt_check_country = con_server.con.prepareStatement(check);
		
		String insert = "INSERT INTO " + stiki_utils.tbl_country + " ";
		insert += "VALUES (?,?,0,0)";
		pstmt_insert_country = con_server.con.prepareStatement(insert);
		
		String rep = "SELECT SUM(BAD_EDITS),SUM(ALL_EDITS) FROM ";
		rep += stiki_utils.tbl_country + " WHERE COUNTRY=? AND UNIX_DAY>=?";
		pstmt_agg_rep = con_server.con.prepareStatement(rep);
	}
	
	/**
	 * Check to see if a summarization exists for a (country,day) combination,
	 * if it does not, insert an intial summary row.
	 * @param country_code Two letter country-code to check for existing row
	 * @param unix_day UNIX day to check for existing row
	 * @return TRUE if the summarization row existed prior to this method
	 * call, FALSE if a new row had to be inserted.
	 */
	private boolean exists_or_create(String country_code, long unix_day)
			throws Exception{
		
		pstmt_check_country.setLong(1, unix_day);
		pstmt_check_country.setString(2, country_code);
		ResultSet rs = pstmt_check_country.executeQuery();
		if(rs.next()){
			return true; // Any result-row indicates existing summarization
		} else{
			pstmt_insert_country.setLong(1, unix_day);
			pstmt_insert_country.setString(2, country_code);
			pstmt_insert_country.executeUpdate();
			return false; 
		} // Confirm summary row exists, if not, create one
	}

}