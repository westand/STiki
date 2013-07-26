package offline_review_tool;

import java.util.concurrent.LinkedBlockingQueue;

import gui_edit_queue.gui_display_pkg;

/**
 * Andrew G. West - ort_edit_queue_fetcher.java - This class is a reduction
 * from [edit_queue_fetcher.java] in the [gui_edit_queue] package, as needed
 * for the offline review tool (ORT).
 * 
 * This class fetches the display object data (metadata/token/content) for a 
 * single RID. Most critically, it does so in a threaded fashion. Notable
 * from the STiki version of the class, it does not care about IP 
 * geolocation, or if the edit is most-recent-on-page.
 */
public class ort_edit_queue_fetcher implements Runnable{
	
	//**************************** PRIVATE FIELDS ***************************

	/**
	 * Revision-ID whose display data should be obtained.
	 */
	private long rid;
	
	/**
	 * Cache/queue object, s.t. if this class is succesful in obtaining
	 * data for the RID, it should be added to this structure.
	 */
	private LinkedBlockingQueue<gui_display_pkg> shared_queue;
	
	
	//***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [ort_edit_queue_fetcher]. The arguments to this constructor
	 * have already been described, as they are the private cars of this class.
	 */
	public ort_edit_queue_fetcher(long rid, 
			LinkedBlockingQueue<gui_display_pkg> shared_queue){
		this.rid = rid;
		this.shared_queue = shared_queue;
	}
	
	
	//**************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: The action to be completed in own thread. Just use the
	 * MediaWiki API to obtain content/metadata for the RID.
	 */
	public void run(){
		
		try{	// Fetch the data, a simple call to a static method
				// (again, this class exists only for threading purposes).
			gui_display_pkg cur_edit = gui_display_pkg.create_offline(rid);
			
				// Add obtained data to the cache/queue.
			if(cur_edit != null)
				shared_queue.offer(cur_edit);
			
		} catch(Exception e){} // Things could go wrong. But given that 
							   // thread-pool will ignore and start another,
							   // we just call this RID dead and ignore
	}
	
}
