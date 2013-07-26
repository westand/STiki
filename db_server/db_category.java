package db_server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import core_objects.stiki_utils;

/**
 * Andrew G. West - db_category.java - This class is the DB-handler for the
 * [category_links] table. This table is provided by WikiMedia, thus there
 * are no insertion/update methods. It's primary purpose is, given an RID,
 * to determine the number of categories in which RID is a member, and
 * determine all the PIDs in those member category (to calculate cat-rep).
 */
public class db_category{
		
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL returning the categories of which some page is a member.
	 */
	private PreparedStatement pstmt_get_pid_memberships;
	
	/**
	 * SQL returning the pages which are a member of some category.
	 */
	private PreparedStatement pstmt_get_cat_memberships;
	
	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_classify] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 */
	public db_category(stiki_con_server con_server) throws Exception{
		this.con_server = con_server;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Return the categories of which some page is a member.
	 * @param pid Page (article), by ID, whose memberships are desired
	 * @return List of categories, by ID, that 'pid' is a member of
	 */
	public synchronized List<Long> get_page_memberships(long pid) 
			throws Exception{
		
		pstmt_get_pid_memberships.setLong(1, pid);
		ResultSet rs = pstmt_get_pid_memberships.executeQuery();
		List<Long> cat_list = new ArrayList<Long>();
		
		while(rs.next()) // Transfer ResultSet to List
			cat_list.add(rs.getLong(1));
		rs.close();
		return(cat_list);
	}
	
	/**
	 * Return a set of article-members for a list of categories.
	 * @param cat_list Listing of categories, by ID
	 * @return A set containing all articles, by ID, of pages which
	 * are a member of at least one category in 'cat_list'
	 */
	public synchronized Set<Long> get_category_members(List<Long> cat_list) 
			throws Exception{
		
		Iterator<Long> iter = cat_list.iterator();
		Set<Long> pid_list = new HashSet<Long>();
		
		while(iter.hasNext()) // Just make a set of individual-cat queries
			pid_list.addAll(this.get_category_members(iter.next()));
		return(pid_list);
	}
	
	/**
	 * Return the article-members of a single category.
	 * @param cat_id Category, by ID, whose members are desired
	 * @return A list of all article-members of category 'cat_id'
	 */
	public synchronized List<Long> get_category_members(long cat_id) 
			throws Exception{
		
		pstmt_get_cat_memberships.setLong(1, cat_id);
		ResultSet rs = pstmt_get_cat_memberships.executeQuery();
		List<Long> pid_list = new ArrayList<Long>();
		
		while(rs.next()) // Transfer ResultSet to List
			pid_list.add(rs.getLong(1));
		rs.close();
		return(pid_list);
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_get_pid_memberships.close();
		pstmt_get_cat_memberships.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{

		String pid_mems = "SELECT CAT_ID FROM " + stiki_utils.tbl_cat_links;
		pid_mems += " WHERE P_ID=?";
		pstmt_get_pid_memberships = con_server.con.prepareStatement(pid_mems);
		
		String cat_mems = "SELECT P_ID FROM " + stiki_utils.tbl_cat_links;
		cat_mems += " WHERE CAT_ID=?";
		pstmt_get_cat_memberships = con_server.con.prepareStatement(cat_mems);
	}

}
