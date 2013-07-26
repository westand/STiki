package offline_review_tool;

import java.io.BufferedReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import gui_edit_queue.gui_display_pkg;

/**
 * Andrew G. West - ort_edit_queue_filler.java - This class is a stripped
 * down version of [edit_queue_filler.java] in the [gui_edit_queue]
 * packge, for the offline review tool (ORT).
 * 
 * This class is in charge of keeping the RID queue/cache filled, so that 
 * edit diffs are always available to the GUI client. RIDs come from
 * a user provided text file, wrapped as a BufferedReader object.
 */
public class ort_edit_queue_filler implements Runnable{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * CRTICAL variable setting. We add elements to the queue/cache until it
	 * is at LEAST this size (as latency permits). Given multi-threading 
	 * considerations, the  MAXIMUM SIZE will be this number plus the number 
	 * of available threads in the thread pool.
	 */
	private static final int MIN_QUEUE_SIZE = 10;
	
	/**
	 * Reader over RID file which defines the static edit queue. Its format
	 * should be plain-text, one RID per line. Cursor set to next RID to show.
	 */
	private BufferedReader rid_file;
	
	/**
	 * The producer-consumer queue which is enqueued and popped in
	 * multi-threaded fashion. It contains (cached) edits to be displayed.
	 */
	private LinkedBlockingQueue<gui_display_pkg> rid_queue_cache;
	
	/**
	 * Thread-pool for use in fetching-data; populating the queue.
	 */
	private ExecutorService threads;
	
	/**
	 * Data structure tracking work-in-progress by threads.
	 */
	private List<Future<?>> futures;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [ort_edit_queue_filler]. Arguments are not java-doc'ed, as
	 * all are described as private variables.
	 */
	public ort_edit_queue_filler(BufferedReader rid_file, 
			LinkedBlockingQueue<gui_display_pkg> rid_queue_cache, 
			ExecutorService threads){
		
		this.rid_file = rid_file;
		this.rid_queue_cache = rid_queue_cache;
		this.threads = threads;
		futures = new LinkedList<Future<?>>(); // Tasks in progress list
	}
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: The infinite loop in charge of keeping the edit queue full.
	 */
	public void run(){
		String next_rid_file_line;
		try{long rid;
			while(true){
				if(futures.size() + rid_queue_cache.size() < MIN_QUEUE_SIZE){

						// Go to text file to get next RID, send to proc.
					next_rid_file_line = rid_file.readLine();
					if(next_rid_file_line != null){
						rid = Long.parseLong(next_rid_file_line);
						futures.add(threads.submit(new ort_edit_queue_fetcher(
								rid, rid_queue_cache)));	
					} else{ // If file has RIDs remaining, process
						rid_queue_cache.add(gui_display_pkg.create_end_pkg());
					} // Else add the empty queue placeholder	

				} else
					Thread.sleep(10); // Prevent over-spinning if full queue

					// Pop finished tasks from the 'futures' list, so
					// that it only contains tasks still in computation
				futures = remove_done(futures);
			
			} // Populate the queue until interrupted by shutdown

		} catch(Exception e){}	// Only will occur at interruption
	}
	
	/**
	 * Shutdown any lingering objects created by this class, or its children.
	 */
	public void shutdown() throws Exception{
		rid_file.close();
	}
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Given a list of Future objects, remove all that have completed.
	 * @param futures_in List of future objects
	 * @return List 'futures_in', without all entries mapping to 'done' tasks
	 */
	private synchronized static List<Future<?>> remove_done(
			List<Future<?>> futures_in){
		
		List<Future<?>> futures_live = new LinkedList<Future<?>>();
		for(int i=0; i < futures_in.size(); i++){
			if(!futures_in.get(i).isDone())
				futures_live.add(futures_in.get(i));
		} // Iterate over all tasks, checking "done" status
		return(futures_live);
	}
	
}
