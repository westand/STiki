package edit_processing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Andrew G. West - thread_manager.java - This class creates the threads
 * which are used by the ThreadPool, and are assigned edit-processing
 * tasks by that object. Crucually, by allowing this class to create
 * the Threads, we can achieve some degree of tracking over their use.
 */
public class thread_manager implements ThreadFactory{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * For the actual creation of threads, we just use a default object.
	 */
	ThreadFactory thread_factory = Executors.defaultThreadFactory();
	
	/**
	 * List of all Thread objects created by this class.
	 */
	List<Thread> thread_list;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Create a [thread_manager] object, initializing all structures.
	 */
	public thread_manager(){
		this.thread_list = new ArrayList<Thread>();
	}
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Overriding: Create a new thread -- and also store data 
	 * such that this thread can be tracked in the future.
	 */
	public Thread newThread(Runnable task){
		Thread t = thread_factory.newThread(task);
		thread_list.add(t);
		return(t);
	}
	
	/**
	 * Count the number of threads created by this instance.
	 * @return Number of threads created by this instance (regardless of
	 * whether they are still active, running, or not).
	 */
	public int num_threads_created(){
		return(thread_list.size());
	}

}
