package offline_review_tool;

import java.io.BufferedReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import gui_edit_queue.gui_display_pkg;

/**
 * Andrew G. West - ort_edit_queue.java - This is a stripped down version
 * of [edit_queue.java] in the [gui_edit_queue] package, designed for use
 * with the offline review tool (ORT). It makes sure edit-diffs are 
 * fetched as needed in an efficient and multi-threaded fashion.
 * 
 * In particular, the RIDS to-review are expected to come from a user 
 * provided text file, whose formatting dictates one RID per line.
 */
public class ort_edit_queue{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * The producer-consumer queue which is enqueued and popped in
	 * multi-threaded fashion. This class is in charge of popping...
	 * It contains (cached) edits which will be displayed.
	 */
	private LinkedBlockingQueue<gui_display_pkg> rid_queue_cache;
	
	/**
	 * Class whose responsibility it is to keep the [shared_queue] populated.
	 */
	private ort_edit_queue_filler queue_filler;

	/**
	 * All data associated with current edit (that last popped from queue).
	 */
	private gui_display_pkg cur_edit;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [ort_rid_queue] object handlers.
	 * @param threads Available threads for work
	 * @param rid_file Reader over file which defines the static edit queue. 
	 * Its format should be plain-text, one RID per line. Cursor over next RID.
	 */
	public ort_edit_queue(ExecutorService threads, BufferedReader rid_file) 
			throws Exception{	
		this.rid_queue_cache = new LinkedBlockingQueue<gui_display_pkg>();
		this.queue_filler = new ort_edit_queue_filler(rid_file, 
				rid_queue_cache, threads);
		threads.submit(queue_filler); // Start population
	}

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Obtain the next RID to display (and store in local variable).
	 */
	public void next_rid(){
			// Once the queue is non-empty, we can pop an edit.
		while(rid_queue_cache.peek() == null){
			try{Thread.sleep(10);} 
			catch(Exception e) {}
		} // Ensure we do not spin to death while waiting
		cur_edit = rid_queue_cache.poll();
	}
	
	/**
	 * Accessor: Return all date associated with "current RID"
	 * @return [gui_display_pkg] associated with the current RID
	 */
	public gui_display_pkg get_cur_edit(){
		return (this.cur_edit);
	}
	
	/**
	 * Shutdown any lingering objects created by this class, or its children.
	 */
	public void shutdown() throws Exception{
		this.queue_filler.shutdown();
	}
	
}
