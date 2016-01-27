package gui_edit_queue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import core_objects.pair;
import core_objects.stiki_utils.SCORE_SYS;
import executables.stiki_frontend_driver;

/**
 * Andrew G. West - edit_queue_filler.java - This class is in charge of
 * keeping the RID queue/cache filled, so that edits are always available
 * to the GUI client. Ultimately, edit RIDs are fetched from the DB, the
 * associated content/metadata is fetched from MediaWiki, and then stored.
 * When one is popped, a new one is obtained.
 */
public class edit_queue_filler implements Runnable{

	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * CRTICAL variable setting. We add elements to the queue/cache until it
	 * is at LEAST this size (as latency permits). Given multi-threading 
	 * considerations, the  MAXIMUM SIZE will be this number plus the number 
	 * of available threads in the thread pool.
	 * 
	 * A high-value will always ensure a user does not have to wait on the
	 * network for an edit to display. However, too high a number will mean
	 * the edit-tokens will be "old" by the time an edit gets displayed,
	 * increasing the chance of a conflicting edit, and a failed reversion.
	 */
	public static final int MIN_QUEUE_SIZE = 10;

	/**
	 * Number of prior reservation IDs to remember (and clear, if req'd).
	 * This should be calculated based on [MIN_QUEUE_SIZE] and the default
	 * queue pop size (per that stored procedure). Given random generation
	 * of IDs, a little bit of conservativeness does no harm.
	 */
	public static final int RES_HIST_SIZE = 3;
	
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Root GUI class maintaining all DB-handlers (i.e., the server-queue).
	 */
	private stiki_frontend_driver parent;
	
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
	 * Queue entries from the server-side. These are simply RIDs which are
	 * in-line to be data-fetched and cached (in the rid_queue_cache).
	 */
	private Queue<pair<Long,Long>> server_q;
	
	/**
	 * STiki who is doing classifiying. This is important so that we can avoid
	 * showing this indvidual edits they have 'ignored' in the past.
	 */
	private String stiki_user;
	
	/**
	 * Is the 'stiki_user' using the native rollback  functionality to 
	 * undo edits? (precondition: they have the privileges to do so)
	 */
	private boolean using_native_rb;
	
	/**
	 * Queue from which edits are currently being fetched.
	 */
	private SCORE_SYS queue_in_use;
	
	/**
	 * Data structure tracking work-in-progress by threads.
	 */
	private List<pair<Future<?>,Long>> futures;
	
	/**
	 * Reservations are made distinct through the use of a random key, which
	 * can later be used to release reservation(s), if required. This 
	 * structure is operated as an LRU cache with fixed size.
	 */
	private List<Long> resid_history;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [edit_queue_filler]. Most arguments are described as 
	 * private variables, only "default_queue" is not. It is the initial
	 * queue from which edits should be pulled.
	 */
	public edit_queue_filler(
			stiki_frontend_driver parent,
			LinkedBlockingQueue<gui_display_pkg> rid_queue_cache,
			SCORE_SYS default_queue,
			ExecutorService threads){
		
		this.parent = parent;
		this.rid_queue_cache = rid_queue_cache;
		this.threads = threads;
		this.futures = new LinkedList<pair<Future<?>,Long>>();
		this.resid_history = new LinkedList<Long>();
		
			// Initialize all queue-determinant fields to default criteria
		this.stiki_user = "";
		this.using_native_rb = false;
		this.queue_in_use = default_queue;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: The infinite loop in charge of keeping the edit queue full.
	 */
	public void run(){
		long resid;
		try{
			pair<Long,Long> rid_pid_pair;
			while(true){
				
				if(futures.size() + rid_queue_cache.size() < MIN_QUEUE_SIZE){

					while(this.server_q == null || this.server_q.isEmpty()){
						resid = Math.abs(new Random().nextInt());
						this.server_q = parent.client_interface.queues.
								queue_fetch(queue_in_use, stiki_user, resid);
						this.add_resid_to_hist(resid);
					}	// While the queue has not reached the minimum size, 
						// Keep popping RIDs from the server, obtaining data
						// and caching it (in threaded fashion).
					
					rid_pid_pair = this.server_q.poll();
					futures.add(new pair<Future<?>,Long>(
							threads.submit(new edit_queue_fetcher(
							parent, rid_pid_pair.fst, rid_pid_pair.snd, 
							using_native_rb, queue_in_use, 
							rid_queue_cache)), rid_pid_pair.fst));				
				} else
					Thread.sleep(10); // Prevent over-spinning if full queue

					// Pop finished tasks from the 'futures' list, so
					// that it only contains tasks still in computation
				futures = remove_done(futures);
			
			} // Populate the queue until interrupted by shutdown

		} catch(Exception e){}	// Only will occur at interruption
	}
	
	/**
	 * If the STiki user/queue/token-basis have changed, we notify 
	 * the population loop via this method. This way, it can query s.t. 
	 * the user will not see edits he/she has ignored, we will know if edit
	 * tokens need to be obtained, and if so, those tokens will map to
	 * the correct user. THE CHALLENGE, HOWEVER, IS MAKING SURE THESE CHANGES
	 * ARE ALSO REFLECTED IN CACHED/ENQUEUED EDITS AS WELL
	 * @param stiki_user Current STiki user
	 * @param using_native_rb Is the 'stiki_user' using the native rollback
	 * functionality to undo edits (precondition: they have the privilege)
	 * @param queue Queue from which next edit should be fetched
	 */
	public void new_user_settings(String stiki_user,
			boolean using_native_rb, SCORE_SYS queue){
		
			// Changing this variables should cause all new fetch-requests
			// to occur under new parameters. Clear
		this.stiki_user = stiki_user;
		this.using_native_rb = using_native_rb;
		this.queue_in_use = queue;

			// Now wipe previous reservations. This should make the top-
			// scoring edits re-available for the new user/style/queue.
		wipe_recent_res();
		
			// Now, there might be enqeued/in-progress edits fetched under
			// old params. Cancel all tasks currently in progress, wipe the
			// queue, and then throw away the top of the queue (as this may
			// be old, but not yet been in future-list when we cleared it).
		this.server_q.clear(); // Don't forget there are TWO queues
		cancel_all(futures);
		rid_queue_cache.clear();
		while(rid_queue_cache.poll() == null){};
		rid_queue_cache.poll(); // Throw away one once available
	}
	
	/**
	 * Shutdown this object. Really, the only thing to do is clean-up the
	 * DB side of things. Thread will eventually be interuppted and shutdown 
	 * when the pool is closed by the root GUI object.
	 */
	public void shutdown(){
		this.wipe_recent_res();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Wipe recently held reservations.
	 */
	private void wipe_recent_res(){
		try{ // Just iterate over all recently held IDs
			for(int i=0; i < resid_history.size(); i++)
				parent.client_interface.queues.queue_wipe(resid_history.get(i));
		} catch(Exception e){};
	}
	
	/**
	 * Add an res-ID to the "historical structure". This action will maintain
	 * capacity by removing that entry least recently added.
	 * @param resid Reservation ID to be added to structure
	 */
	private void add_resid_to_hist(long resid){
		if(resid_history.size() != RES_HIST_SIZE){		
			resid_history.add(0, resid);
		} else{
			resid_history.remove(RES_HIST_SIZE - 1);
			resid_history.add(0, resid);
		} // Branch in order to maintain a maximum list size
	}

	/**
	 * Given a list of Future objects, remove all that have completed.
	 * @param futures_in List of future objects
	 * @return List 'futures_in', without all entries mapping to 'done' tasks
	 */
	private synchronized static List<pair<Future<?>,Long>> remove_done(
			List<pair<Future<?>,Long>> futures_in){
		
		List<pair<Future<?>,Long>> futures_live = 
				new LinkedList<pair<Future<?>,Long>>();
		for(int i=0; i < futures_in.size(); i++){	
			if(!futures_in.get(i).fst.isDone())
				futures_live.add(futures_in.get(i));
			// else System.out.println(futures_in.get(i).snd + " not done");
		} // Iterate over all tasks, checking "done" status
		return(futures_live);
	}
	
	/**
	 * Given a list of Future objects (submitted tasks), cancel/kill
	 * all those tasks which have no yet completed.
	 * @param futures List of Future objects
	 */
	private synchronized static void cancel_all(
			List<pair<Future<?>,Long>> futures){
		for(int i=0; i < futures.size(); i++)
			futures.get(i).fst.cancel(true); // Cancel via interrupt if needed
	}
	
}
