package gui_edit_queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.LinkedBlockingQueue;

import core_objects.stiki_utils;
import db_client.client_interface;

import mediawiki_api.api_retrieve;


/**
 * Andrew G. West - edit_queue_maintain.java - While classes exist to 
 * "fill" and "pop" the edit queue -- this one is in charge of queue
 * maintenance. Primarily, it ensures that enqueued edits maintain the
 * "most recent on page" property.
 * 
 * Being a queue -- this class uses a clone method to copy the entire queue
 * (enabling inspection past the head). In this manner, it can inspect all
 * queue elements and store their status. Then, just before [edit_queue.java]
 * pops an element to the client, it can check its status against this class.
 */
public class edit_queue_maintain implements Runnable{
	
	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * Enqueued edits will have their "most recent on page" status checked
	 * every [SECS_REFRESH_CURRENCY] seconds.
	 */
	public static final int SECS_REFRESH_CURRENCY = 10; 

	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * The queue this class is in charge of mainting. Contains cached
	 * edit data enqueued -- which will/can be shown to client.
	 */
	private LinkedBlockingQueue<gui_display_pkg> rid_queue_cache;

	/**
	 * Structure to which inactive RIDs are added. Note that in the parent
	 * class [edit_queue.java] -- this struct is wrapped for concurrency.
	 */
	private SortedSet<Long> inactive_rids;
	
	/**
	 * Access to client stored procedures. This will allow us to dequeue
	 * those RIDs found to be inactive (so no one else pops them).
	 */
	private client_interface client_iface;
	
	/**
	 * Timestamp at which queue currency was last examined.
	 */
	private long ts_last_check;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Create a [edit_queue_maintain] object.
	 * @param rid_queue_cache Structure containing enqueued RIDs in need 
	 * of inspection (to be copied and inspected offline)
	 * @param inactive_rids Structure to which RIDs discovered to be 
	 * inactive will be written.
	 * @param client_interface Access to client stored procedures
	 */
	public edit_queue_maintain(LinkedBlockingQueue<gui_display_pkg> 
			rid_queue_cache, SortedSet<Long> inactive_rids, 
			client_interface client_iface){
		this.rid_queue_cache = rid_queue_cache;
		this.inactive_rids = inactive_rids;
		this.client_iface = client_iface;
		this.ts_last_check = stiki_utils.cur_unix_time();
	}

	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Infinite loop checking the status of enqueued RIDs,
	 * and updating a status table accordingly.
	 */
	public void run(){
		
			// Init all structures needed for loop
		gui_display_pkg cur_pkg;
		Iterator<gui_display_pkg> queue_iter;
		Map<Long,Long> queued_pairs;
		Map<Long,Long> actual_pairs;
		Iterator<Long> compare_iter;
		long compare_pid, pot_del_pid;
		List<Long> new_inactives;
		
		while(true){
			
			if(stiki_utils.cur_unix_time() >= 
				(ts_last_check + SECS_REFRESH_CURRENCY)){
			
				try{queued_pairs = new HashMap<Long,Long>(); // clean
					queue_iter = rid_queue_cache.iterator();
					while(queue_iter.hasNext()){
						cur_pkg = queue_iter.next();
						queued_pairs.put(cur_pkg.metadata.pid, 
								cur_pkg.metadata.rid);
					} // Get pids/rids of enqueued edits (iter is thread safe)
				
						// Query API for actual most recent on PIDs
					actual_pairs = api_retrieve.process_latest_page(
							queued_pairs.keySet());
					ts_last_check = stiki_utils.cur_unix_time();
					
						// Compare actual and enqeued sets
					new_inactives = new ArrayList<Long>();
					compare_iter = queued_pairs.keySet().iterator();
					while(compare_iter.hasNext()){
						compare_pid = compare_iter.next();
						pot_del_pid = queued_pairs.get(compare_pid).longValue();
						if((pot_del_pid != actual_pairs.get(compare_pid).
								longValue()) && 
								!inactive_rids.contains(pot_del_pid)){
							inactive_rids.add(pot_del_pid); // Insert
							new_inactives.add(pot_del_pid);
						} // If new inactive, add to reject lists
					} // Compare actual and enqueued sets
					
						// Now delete inactives back on server. Note that this
						// was not done in-loop for latency reasons.
					for(int i=0; i < new_inactives.size(); i++)
						client_iface.queues.queue_delete(new_inactives.get(i));
				
				} catch(Exception e){}; // If something goes wrong (likely the 
										// Wiki-API call). Just ignore, and 
										// loop will retry automatically.
			} else{
				try{Thread.sleep(1000);} catch(Exception e){}
			} // Don't spin to death while waiting for refresh
		
		} // Do queue maintenance until interrupted at shutdown
	}
	
	/**
	 * This method will determine whether or not an edit is most recent on 
	 * an article (within some error window -- per inf. loop above).
	 * @param rid Revision-ID of the edit whose status is being checked
	 * @param discard Whether or not status information should be discarded.
	 * If the edit will never be checked again (i.e., is about to be
	 * popped to the client), this will reduce mem. requirements.
	 * @return TRUE if 'rid' is most recent on 'pid' (within error window
	 * determined by [SECS_REFRESH_CURRENCY]). FALSE, otherwise. 
	 */
	public boolean active(long rid, boolean discard){
		
			// Note: The lack of a set-entry indicates an "active" edit
			// because (1) only "inactive" edits or scored, AND
			// (2) it could simply means the RID hasn't aged sufficiently
			// since its insertion (where currency was checked) -- meaning
			// it falls within the "window epsilon" constraint.
	
		if(discard)
			return(!inactive_rids.remove(rid));
		else return(!inactive_rids.contains(rid));
	}

}
