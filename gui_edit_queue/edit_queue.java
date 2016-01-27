package gui_edit_queue;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import core_objects.pair;
import core_objects.stiki_utils.SCORE_SYS;
import db_client.client_interface;

import executables.stiki_frontend_driver;

/**
 * Andrew G. West - edit_queue.java - This class provides GUI support
 * in deciding which RIDs are displayed to the user. This class is a frontend,
 * providing a simple interface to the complex multi-threading which keeps the
 * queue populated at maximum efficiency and minimum latency.
 */
public class edit_queue{

	// **************************** PRIVATE FIELDS ***************************
	
	// *********** VARIABLE PROPERTIES **********
	
	/**
	 * STiki user who last requested an edit to examine. Critically, if a
	 * different user asks for an edit, the then the queue will need
	 * re-populated so that edit-tokens are accurate; user sees no 'ignores'.
	 */
	private String stiki_user;
	
	/**
	 * Is the 'stiki_user' using the native rollback functionality to undo 
	 * edits? (precondition: they have the privilege). If this property 
	 * changes, queue needs cleaned, as this property affects whether
	 * edit or rollback tokens are obtained for each edit in queue. 
	 */
	private boolean using_native_rb = false;
	
	/**
	 * Queue from which edits are currently being fetched.
	 */
	private SCORE_SYS queue_in_use;

	
	// **************** STRUCTURES  *************
	
	/**
	 * The producer-consumer queue which is enqueued and popped in
	 * multi-threaded fashion. This class is in charge of popping...
	 * It contains (cached) edits which will be displayed.
	 */
	private LinkedBlockingQueue<gui_display_pkg> rid_queue_cache;
	
	/**
	 * Class keeping the [rid_queue_cache] populated.
	 */
	private edit_queue_filler queue_filler;
	
	/**
	 * Table maintaining of list that has become "inactive" since being
	 * enqueued. Will be checked prior to popping edit to client.
	 */
	private SortedSet<Long> inactive_rids;
	
	/**
	 * Class maintaining the [rid_queue_cache] via the [inactive_rids] list. 
	 * Helps maintain the "only shown most recent edits on page" condition.
	 */
	private edit_queue_maintain queue_maintainer;

	
	// ************** ACTIVE & SHOWN ************
	
	/**
	 * All data associated with current edit (that last popped from queue).
	 */
	private gui_display_pkg cur_edit;
	
	/**
	 * Structure to handle edit-advancement if the "back" button is used. 
	 * 
	 * If the first element of this pair is TRUE, then the second element is
	 * the edit which should be next displayed (and was shown before "back").
	 * 
	 * If the first element is FALSE, then the second element is the edit
	 * which was previously displayed (that which one would go "back" to).
	 */
	private pair<Boolean, gui_display_pkg> back_helper;
	
	/**
	 * Set containing edits displayed this session; to ensure no duplicates.
	 */
	private Set<Long> edits_shown;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_rid_queue] object.
	 * @param parent Root GUI class -- to be notified of any DB errors, also
	 * provides accessibility to some shared DB-handlers.
	 * @param threads Available threads for work
	 * @param client_interface Client connection to the database
	 * @parma default_queue Queue to be loaded at program start
	 */
	public edit_queue(stiki_frontend_driver parent, ExecutorService threads, 
			client_interface client_interface, SCORE_SYS default_queue) 
			throws Exception{	
	
			// First the structures are init'ed. Note where concurrent
			// security is ensured over [inactive_rids] struct.
		this.back_helper = new pair<Boolean, gui_display_pkg>(false, null);
		this.edits_shown = new TreeSet<Long>();
		this.rid_queue_cache = new LinkedBlockingQueue<gui_display_pkg>();
		this.inactive_rids = Collections.synchronizedSortedSet(
				new TreeSet<Long>());
		
			// Then their population/maintenance classes and threads
		this.queue_in_use = default_queue;
		this.queue_filler = new edit_queue_filler(
				parent, rid_queue_cache, default_queue, threads);
		this.queue_maintainer = new edit_queue_maintain(
				rid_queue_cache, inactive_rids, client_interface);
		threads.submit(queue_filler); // Start population
		threads.submit(queue_maintainer); // Start maintenance
	}

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Obtain the next RID to display (and store in local variable).
	 * @param stiki_user STiki user who is obtaining the RID
	 * @param using_native_rb Is the 'stiki_user' using the native rollback
	 * functionality to undo edits? (precondition: they have the privilege)
	 * @param using_native_rb Is the 'stiki_user' using the native rollback
	 * functionality to undo edits? (precondition: they have the privilege)
	 * @param queue Queue from which next edit should be fetched
	 * @param prev If TRUE, instruct the queue to re-show the previous
	 * edit, rather than advancing onward (the FALSE case).
	 * @param reattempt If TRUE, this is a first attempt to get a revision.
	 * If FALSE, it indicates the failure of the immediately prior attempt.
	 */
	public void next_rid(String stiki_user, 
			boolean using_native_rb, SCORE_SYS queue, boolean prev, 
			boolean reattempt){
		
		if(stiki_user != this.stiki_user || 
				using_native_rb != this.using_native_rb ||
				queue != this.queue_in_use){
			this.stiki_user = stiki_user;
			this.using_native_rb = using_native_rb;
			this.queue_in_use = queue;
			this.queue_filler.new_user_settings(
					stiki_user, using_native_rb, queue);
		}	// If not the same base settings as last edit, we need
			// to inform filler, as queue may need cleared, re-filled.
		
		if(!prev){
			
			if(this.back_helper.fst){
				this.back_helper.fst = false;
				gui_display_pkg swap = this.back_helper.snd;
				this.back_helper.snd = cur_edit;
				cur_edit = swap;
			} else{ // If the back button was used, queue is not needed
			
					// Once the queue is non-empty, we can pop an edit.
				while(rid_queue_cache.peek() == null){
					try{Thread.sleep(10);} 
					catch(Exception e) {}
				} // Ensure we do not spin to death while waiting
				
					// Do not display an edit that has been shown before.
					// Do not display if became inactive while enqueued.
				if(!reattempt)
					this.back_helper.snd = cur_edit; // Save as "back"
				cur_edit = rid_queue_cache.poll();
				if(edits_shown.contains(cur_edit.metadata.rid) || 
						!queue_maintainer.active(cur_edit.metadata.rid, true)){
					next_rid(stiki_user, using_native_rb, queue, prev, true);
				} else edits_shown.add(cur_edit.metadata.rid);
				
			} // If back button not involved, advance normally queue
			
		} else{ // If not previous, go to queue and pop
			this.back_helper.fst = true;
			gui_display_pkg swap = this.back_helper.snd;
			this.back_helper.snd = cur_edit;
			cur_edit = swap;
		} // If "advancing" to previous, use helper structure
	}
	
	/**
	 * Refresh the rb-token associated with the current-RID (that last 
	 * popped off the queue). This is needed, for example, if a user's login
	 * status changes internally to viewing a single edit.
	 */
	public void refresh_rb_token() throws Exception{
		this.cur_edit.refresh_rb_token();
	}
	
	/**
	 * Refresh the edit-token associated with the current-RID (that last 
	 * popped off the queue). This is needed, for example, if a user's login
	 * status changes internally to viewing a single edit.
	 */
	public void refresh_edit_token() throws Exception{
		this.cur_edit.refresh_edit_token();
	}
	
	/**
	 * Accessor: Return all data associated with "current RID"
	 * @return [gui_display_pkg] associated with the current RID
	 */
	public gui_display_pkg get_cur_edit(){
		return (this.cur_edit);
	}
	
	/**
	 * Shutdown this class, and all those backing it
	 */
	public void shutdown(){
		this.queue_filler.shutdown();
	}

}
