package utilities;

import java.sql.Statement;
import java.sql.ResultSet;

import core_objects.stiki_utils;

import db_server.qmanager_server;
import db_server.stiki_con_server;

/**
 * Andrew G. West - scrub_passes.java - This is a script designed to 
 * de-queue edits that have too many "pass" classifications. It can/should
 * be run via a cron-script of some kind. If enough people decide to "pass"
 * an edit, it is presumably too borderline to be worth anothers time.
 */
public class scrub_passes{

	// **************************** PRIVATE FIELDS ***************************	
	
	/**
	 * Threshold that determines number of "passes" that amount to de-queuing.
	 */
	private static int DEQ_THRESHOLD = 2;
	
	
	// **************************** PUBLIC METHODS ***************************	
	
	/**
	 * @param args One optional argument is permitted: (1) an integer
	 * denoting the "pass threshold" that determines de-queueing. I.e., "if
	 * an edit has [x] pass actions, it should be de-queued." If not provided,
	 * script will default to some script-provided value.
	 */
	public static void main(String[] args) throws Exception{
		
		//int dqs = 0;
		if(args.length > 1)
			DEQ_THRESHOLD = Integer.parseInt(args[0]);
		stiki_con_server con = new stiki_con_server();
		qmanager_server server_q = new qmanager_server(con, null);
		Statement stmt = con.con.createStatement();
		ResultSet rs = stmt.executeQuery(
				"SELECT P_ID,PASS FROM queue_stiki WHERE LENGTH(PASS)!=0");
		
			// Note: On the assumption that the "STiki" queue is the most
			// central to our processing, and should be most up to date.
			// We only scan this queue, assuming that the cascading
			// nature will appropriately update all other queues. 
		
		String passes; long pid;
		while(rs.next()){
			pid = rs.getLong(1);
			passes = rs.getString(2);
			if(stiki_utils.char_occurences('|', passes) >= DEQ_THRESHOLD*2){
				server_q.delete_pid(pid);
				//dqs++;
			} // Block specific to dequeue actions	
		} // Iterate over all queue entries with "pass" instances

		//System.out.println("Dequeue actions: " + dqs);
		
		rs.close();
		stmt.close();
		con.con.close();
	}
	
}
