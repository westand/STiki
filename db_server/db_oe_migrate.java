package db_server;

import java.sql.PreparedStatement;
import java.util.Random;

import core_objects.stiki_utils;

/**
 * Andrew G. West - db_oe_migrate.java - Given the time-decaying nature of
 * feedback, offending-edits that happened long ago are no longer statistically
 * relevant to reputation calculation. In order to keep the [off_edits] table
 * small and efficient, these older rollbacks/feedback are migrated to 
 * [oes_archive], using this class/script.
 * 
 * This script should be scheduled to run periodically in order to maintain
 * query efficiency over the [offending_edits] table.
 */
public class db_oe_migrate implements Runnable{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL finding old entries in [off_edits], moving them to [oes_archive].
	 */
	private static PreparedStatement pstmt_migrate_old;
	
	/**
	 * SQL deleting old entries from the [off_edits] table.
	 */
	private static PreparedStatement pstmt_delete_old;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_oe_migrate], which requires no parameters. Start 
	 * the new thread, and immediately begin running migration in that thread.
	 */
	public db_oe_migrate(){
		Thread thread = new Thread(this, new Random().toString());
		thread.start();	
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Remove all entries in [off_edits] older than [HIST_WINDOW] seconds, 
	 * and migrate them to [oes_archive].
	 */
	public synchronized void run(){
		try{
			stiki_con_server server_con = new stiki_con_server();
			prep_statements(server_con);
			pstmt_migrate_old.executeUpdate(); 	// Migrate
			pstmt_delete_old.executeUpdate();	// Delete
			pstmt_migrate_old.close();			// Clean-up
			pstmt_delete_old.close();
			server_con.con.close();
		} catch(Exception e){
			System.out.println("");
			e.printStackTrace();
		}
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class.
	 * @param con Connection to the PreSTA-STiki database (privileged)
	 */
	private static void prep_statements(stiki_con_server server_con) 
			throws Exception{
		
		long old_time = (stiki_utils.cur_unix_time()-stiki_utils.HIST_WINDOW);
		
		String migrate_old = "INSERT INTO " + stiki_utils.tbl_oes_old + " ";
		migrate_old += "SELECT * FROM " + stiki_utils.tbl_off_edits + " ";
		migrate_old += "WHERE TS<=" + old_time;
		pstmt_migrate_old = server_con.con.prepareStatement(migrate_old);
		
		String delete_old = "DELETE FROM " + stiki_utils.tbl_off_edits + " ";
		delete_old += "WHERE TS<=" + old_time;
		pstmt_delete_old = server_con.con.prepareStatement(delete_old);
	}			
	
}
