package db_server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import core_objects.stiki_utils;

/**
 * Andrew G. West - db_geolocation.java - This class handles all queries
 * pertaining to the retrieval of geolocation data from the DB.
 */
public class db_geolocation{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL retrieving the GMT offset of some IP address origin.
	 */
	private PreparedStatement pstmt_gmt_offset;
	
	/**
	 * SQL retrieving the two-letter country code associated with some IP.
	 */
	private PreparedStatement pstmt_country_code;
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_geolocation] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 */
	public db_geolocation(stiki_con_server con_server) throws Exception{
		this.con_server = con_server;
		prep_statements();
	}
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Given a IP address, return the GMT offset at the (physical) IP origin.
	 * @param int_ip IP address, in integer format
	 * @return GMT offset at location where 'int_ip' physically resides.
	 * If our database does nto contain this information, return NaN
	 */
	public synchronized double get_gmt_offset(long int_ip) throws Exception{
		pstmt_gmt_offset.setLong(1, int_ip);
		ResultSet rs = pstmt_gmt_offset.executeQuery();
		if(rs.next()){
			if(rs.getString(1).equals(""))
				return (Double.NaN);
			else return (Double.parseDouble(rs.getString(1)));
		} // We assume there is exactly one result
		return (Double.NaN); // If no result returned
	}
	
	/**
	 * Given an IP address, return the two-letter country-code of its origin.
	 * @param int_ip IP address, in integer format
	 * @return Two letter country-code where 'int_ip' resides, or the empty
	 * string if there was an error or such data was not available.
	 */
	public synchronized String get_country_code(long int_ip) throws Exception{
		pstmt_country_code.setLong(1, int_ip);
		ResultSet rs = pstmt_country_code.executeQuery();
		if(rs.next()) // Result should be guaranteed, but just in case...
			return(rs.getString(1));
		else return ("");
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_gmt_offset.close();
		pstmt_country_code.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		String offset = "SELECT timezone FROM " + stiki_utils.tbl_geo_city;
		offset += " WHERE ip_start<=? ORDER BY ip_start DESC LIMIT 1";
		pstmt_gmt_offset = con_server.con.prepareStatement(offset);
		
		String country = "SELECT country_code FROM ";
		country += stiki_utils.tbl_geo_country + " WHERE ip_start<=? ";
		country += "ORDER BY ip_start DESC LIMIT 1";
		pstmt_country_code = con_server.con.prepareStatement(country);
	}

}
