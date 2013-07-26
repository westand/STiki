package utilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import core_objects.stiki_utils;
import db_server.stiki_con_server;

/**
 * Andrew G. West - clean_db_categories.java - The [category_links] table is
 * a large one, and one used-intensively during reputation calculation. Thus,
 * it is desireable to keep it as small and optimized as possible.
 * 
 * Thus, this class processes that table (in raw, direct from Wiki form), 
 * deleting membership links corresponding to 'uninteresting' categories. A 
 * category may be uninteresting if it (1) is not in NS0, (2) has two or 
 * fewer members, or (3) is determined to be of an administrative, and not 
 * content-based nature. Our attempts at removing un-interesting categories
 * are best-effort, and far from complete.
 * 
 * Wikipedia dumps the basis [categorylinks] table which is modified by this
 * class, with the assistance of the [category] table, which must also be
 * imported from Wikipedia. When ready for production use, the [categorylinks]
 * table should be renamed to the production-version [category_links].
 */
public class clean_db_categories{
	
	// **************************** PRIVATE FIELDS ***************************

		// The following are not general-purpose statements. They do 
		// large amounts of engine-changing, column-renaming, column-deletion,
		// and index building in order to prepare for further optimization.
		// They also make the raw Wiki-tables suitable for local use.
	private static PreparedStatement pstmt_category_setup;
	private static PreparedStatement pstmt_cat_link_setup;
	
	/**
	 * Any category name which matches a REG-EX (SQL-style) in this array
	 * will be considered non-interesting on the basis that it likely
	 * is administrative in nature.
	 */
	private static String[] ADMIN_REGEXES={"%CLASS%", "%WIKIPEDIA%", "%STUB%",
		"%IMPORTANCE%", "%TEMPLATE%", "%IMAGE%", "%UNKNOWN%", "%REDIRECT%",
		"%REFERENCE%", "%PRIORITY%", "%UNSOURCE%", "%GEOCOORDINATE_DATA",
		"ALL_ARTICLES_%", "%_MISSING", "%MISSING%", "%_DISAMBIGUATION_%",
		"%INCOMPLETE%", "%CLEANUP%", "%LACKING%", "ALL_SET_INDEX_ARTICLES",
		"%DEPRECATED%", "%GEOLINKS%", "%DEATHS", "%DISAMBIGUATION%",
		"ARTICLES_%", "%BIRTHS%", "%NEEDING%", "%ORPHAN%FROM%", "%ORPHANED%",
		"%INFOBOX%", "%NPOV%", "%ASSESSED%", "%FREE%", "USER%", 
		"%TASK_FORCE%", "%WORK_GROUP%"};
	
	/**
	 * SQL deleting all "non-interesting" categores from consideration per
	 * the REGEXs above, and all those without 2+ page members.
	 */
	private static PreparedStatement pstmt_clean_cats;
	
	/**
	 * SQL returning IDs and names of interesting categories.
	 */
	private static PreparedStatement pstmt_get_int_cats;
	
	/**
	 * SQL assigning a cat-ID to a membership link defined by cat-TITLE.
	 */
	private static PreparedStatement pstmt_id_link;
	
	/**
	 * SQL removing links that describe an "uninteresting" membership.
	 */
	private static PreparedStatement pstmt_clean_links;
	
		// Finally some table clean-up stuff. likely not of general-purpose
		// interest, so we omit the Java-docing of these statements.
	private static PreparedStatement pstmt_del_name_col;
	private static PreparedStatement pstmt_optimize_links;
	private static PreparedStatement pstmt_del_cat_table;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Driver method. Delete un-neccesary/un-interesting membership links.
	 * @param args No arguments are required by this method
	 */
	public static void main(String[] args) throws Exception{
			
		stiki_con_server con_server = new stiki_con_server();
		prep_statements(con_server);
		
			// These two statements significantly alter the operating tables, 
			// their engines, column names, indexes, etc.
		//pstmt_category_setup.execute();
		//pstmt_cat_link_setup.execute();
			
			// Delete all non-interesting cats. It would be nice to knock
			// out all non-NS0 at this point -- but that's a lot of work, and
			// there are no non-NS0 OEs, so its not a practical concern.
		//pstmt_clean_cats.executeUpdate(); 
		
			// Intuitively, we now use the interesting IDs to label the 
			// membership links defined by title. This is nothing more than
			// an UPDATE-JOIN, but given the size of the tables, MySQL 
			// chokes in a huge way. Instead, we implement the join piece-wise
		int cats_complete = 0;
		ResultSet rs = pstmt_get_int_cats.executeQuery();
		while(rs.next()){
			if(cats_complete++ % 1000 == 0)
				System.out.println(cats_complete + " categories completed!");
			pstmt_id_link.setLong(1, rs.getLong(1));
			pstmt_id_link.setString(2, rs.getString(2));
			pstmt_id_link.executeUpdate();
			
		} // Just iterate over all category titles, setting IDs
	
			// Delete any cat-link which did not get assigned an ID above
			// (i.e., delete those mapping to un-interesting categories)
		pstmt_clean_links.executeUpdate();
		
			// Clean-up, minimize, and optimize the tables
		pstmt_del_name_col.executeUpdate();
		pstmt_optimize_links.execute();
		pstmt_del_cat_table.executeUpdate();
		
			// Shutdown statements and terminate connection
		pstmt_category_setup.close();
		pstmt_cat_link_setup.close();
		pstmt_clean_cats.close();
		pstmt_clean_links.close();
		pstmt_del_name_col.close();
		pstmt_optimize_links.close();
		pstmt_del_cat_table.close();
		con_server.con.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this instance.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 */
	private static void prep_statements(stiki_con_server con_server) 
			throws Exception{
		
		String cat = "ALTER TABLE " + stiki_utils.tbl_category + " ";
		cat += "CHANGE COLUMN cat_id CAT_ID INTEGER UNSIGNED NOT NULL,";
		cat += "CHANGE COLUMN cat_title CAT_TITLE varchar(255) NOT NULL,";
		cat += "CHANGE COLUMN cat_pages PAGES INTEGER  NOT NULL DEFAULT 0,";
		cat += "DROP COLUMN cat_subcats,";
		cat += "DROP COLUMN cat_files,";
		cat += "DROP COLUMN cat_hidden,";
		cat += "DROP PRIMARY KEY,";
		cat += "ADD PRIMARY KEY USING BTREE(CAT_ID),";
		cat += "DROP INDEX cat_title,";
		cat += "DROP INDEX cat_pages,";
		cat += "ADD INDEX ind_title(CAT_TITLE),";
		cat += "CHARACTER SET latin1 COLLATE latin1_swedish_ci,";
		cat += "ENGINE = MyISAM;";
		pstmt_category_setup = con_server.con.prepareStatement(cat);
				
		String link = "ALTER TABLE " + stiki_utils.tbl_offline_cat_links;
		link += " CHANGE COLUMN cl_from P_ID INT(8) UNSIGNED NOT NULL,";
		link += "CHANGE COLUMN cl_to CAT_TITLE varchar(255) NOT NULL,";
		link += "ADD COLUMN CAT_ID INT(8) UNSIGNED NOT NULL AFTER P_ID,";
		link += "DROP COLUMN cl_sortkey,";
		link += "DROP COLUMN cl_timestamp,";
		link += "DROP INDEX cl_from,";
		link += "ADD INDEX cat_ind(CAT_ID),";
		link += "ADD INDEX pid_ind(P_ID),";
		link += "ADD INDEX title_ind(CAT_NAME),";
		link += "CHARACTER SET latin1 COLLATE latin1_swedish_ci,";
		link += "ENGINE = MyISAM;";
		pstmt_cat_link_setup = con_server.con.prepareStatement(link);
		
		String clean_cats = "DELETE FROM " + stiki_utils.tbl_category + " ";
		clean_cats += "WHERE PAGES<2 ";
		for(int i=0; i < ADMIN_REGEXES.length; i++){
			clean_cats += "OR UPPER(CAT_TITLE) LIKE \"" + 
				ADMIN_REGEXES[i] + "\" ";
		} // Delete any category with < 2 members, or matching an admin-REGEX
		pstmt_clean_cats = con_server.con.prepareStatement(clean_cats);
		
		String int_cats = "SELECT CAT_ID,CAT_TITLE FROM ";
		int_cats += stiki_utils.tbl_category;
		pstmt_get_int_cats = con_server.con.prepareStatement(int_cats);
		
		String id_link = "UPDATE " + stiki_utils.tbl_offline_cat_links + " ";
		id_link += "SET CAT_ID=? WHERE CAT_TITLE=?";
		pstmt_id_link = con_server.con.prepareStatement(id_link);
		
		String clean_links = "DELETE FROM " + stiki_utils.tbl_offline_cat_links;
		clean_links += " WHERE CAT_ID=0";
		pstmt_clean_links = con_server.con.prepareStatement(clean_links);
		
		String name_col = "ALTER TABLE " + stiki_utils.tbl_offline_cat_links;
		name_col += " DROP COLUMN CAT_TITLE";
		pstmt_del_name_col = con_server.con.prepareStatement(name_col);
		
		String opt_link = "OPTIMIZE TABLE " + stiki_utils.tbl_offline_cat_links;
		pstmt_optimize_links = con_server.con.prepareStatement(opt_link);
		
		String drop_cats = "DROP TABLE " + stiki_utils.tbl_category;
		pstmt_del_cat_table = con_server.con.prepareStatement(drop_cats);
	}
	
}
