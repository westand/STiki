package gui_edit_queue;

import java.util.concurrent.LinkedBlockingQueue;

import core_objects.stiki_utils.SCORE_SYS;

import executables.stiki_frontend_driver;

/**
 * Andrew G. West - edit_queue_fetcher.java - This class fetches the display
 * object data (metadata/token/content) for a single RID. Most critically,
 * it does so in a threaded fashion (otherwise unremarkable).
 */
public class edit_queue_fetcher implements Runnable{
	
	//**************************** PRIVATE FIELDS ***************************

	/**
	 * GUI root class -- manages DB connections and handlers.
	 */
	private stiki_frontend_driver parent;
	
	/**
	 * Revision-ID whose display data should be obtained.
	 */
	private long rid;
	
	/**
	 * The page-ID (article-ID) on which revision 'rid' resides. Used to 
	 * ensure the 'rid' is the most recent edit on the page.
	 */
	private long pid;
	
	/**
	 * Whether or not an edit token should be obtained. Users using 
	 * rollback will not require this extra API call.
	 */
	private boolean using_native_rb;
	
	/**
	 * Queue from which the RID wrapped by this object was obtained.
	 */
	private SCORE_SYS source_queue;
	
	/**
	 * Cache/queue object, s.t. if this class is succesful in obtaining
	 * data for the RID, it should be added to this structure.
	 */
	private LinkedBlockingQueue<gui_display_pkg> shared_queue;
	
	
	//***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [edit_queue_fetcher]. The arguments to this constructor
	 * have already been described, as they are the private vars of this class.
	 */
	public edit_queue_fetcher(stiki_frontend_driver parent, long rid, long pid,
			boolean using_native_rb, SCORE_SYS source_queue,
			LinkedBlockingQueue<gui_display_pkg> shared_queue){
		
		this.parent = parent;
		this.rid = rid;
		this.pid = pid;
		this.using_native_rb = using_native_rb;
		this.source_queue = source_queue;
		this.shared_queue = shared_queue;
	}
	
	
	//**************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: The action to be completed in own thread. Just use the DB
	 * and MediaWiki API to obtain content/token/metadata for the RID.
	 */
	public void run(){
		
		try{
				// Fetch the data, a simple call to a static method
			gui_display_pkg cur_edit = gui_display_pkg.create_if_most_recent(
					parent, rid, pid, using_native_rb, source_queue);
			
				// Having obtained all data, we can also check the edit
				// against user-set filters from the STiki menu. 
				// Edit will be NULL'ed if an enabled filter is enacted
			cur_edit = edit_queue_filters.run_all_filters(parent, cur_edit);

				// Make sure we have not obtained a zero-diff (no change, 
				// a consequence of the diff browser showing rollback results)
				// If we have, don't make anyone else deal with it.
			if(cur_edit != null && cur_edit.has_zero_diff()){
				cur_edit = null;
				parent.client_interface.queues.queue_delete(rid);
			}
			
				// Do not allow a user to classify his/her own edits.
				// We also ignore such edits in the queue, so clients do
				// not keep re-fetching the RID at every reservation fetch.
			if(cur_edit != null && cur_edit.metadata.user.equalsIgnoreCase(
					parent.login_panel.get_editing_user())){
				cur_edit = null;
				parent.client_interface.queues.queue_ignore(
						rid, parent.login_panel.get_editing_user());
			}
				
				// If RID was most recent on page, we got data, and the diff
				// was non-zero -- then add it to the cache/queue.
			if(cur_edit != null)
				shared_queue.offer(cur_edit);

		} catch(Exception e){} // Things could go wrong. But given that 
							   // thread-pool will ignore and start another,
							   // we just call this RID dead and ignore
	}
	
}
