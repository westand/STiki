package edit_processing;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Andrew G. West - rid_queue_elem.java - This class represents an element
 * of the RID-queue. It exists as a class so that [Delayed] can be 
 * extended, allowing the RID-Queue to be a java DelayQueue, rather than
 * a home-brewed implentation (which was problematic). It also streamlines
 * the use of several metadata fields (i.e., num. of re-attempts).  
 */
public class rid_queue_elem implements Delayed{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * The revision-ID wrapped by this object. Other fields are metadata.
	 */
	public final long RID;
	
	/**
	 * The default insertion-delay to be associated with this object (i.e.,
	 * this object cannot be popped before [ELEMENT_DELAY] time units pass).
	 * 
	 * Currently set to 10 seconds. 
	 * 
	 * STiki shouldn't be in too big a hurry to calculate vandalism 
	 * probabilities. Because of client-side pre-caching and queues, 
	 * a human will never see a very very young edit. Thus, we should give 
	 * anti-vandal bots a chance to do their work before we pre-fetch 
	 * all that data only to discard it. Moreover, things like external
	 * link parsing which are done by Wikipedia have some latency, and their
	 * API returns can be incorrect if we ping them just after an edit.
	 * Replication lag might cause similar behavior.
	 */
	public static final long ELEMENT_DELAY = 10000;
	
	/**
	 * The time-unit which is applied to variable [ELEMENT_DELAY].
	 */
	public static final TimeUnit DELAY_UNIT = TimeUnit.MILLISECONDS;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Due to data-distribution lag, attempts to query for new RID data
	 * sometimes fail. This variable contains the number of reattempts.
	 */
	private int reattempts = edit_process_thread.NEW_RID_ATTEMPTS;
	
	/**
	 * This variable contains the UNIX millisecond at which this element
	 * 'expires' (i.e., that it is legal to pop this element from the queue).
	 */
	private long msec_exp;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [rid_queue_elem] by providing the revision-ID.
	 * @param rid Revision-ID which this queue-element should wrap
	 */
	public rid_queue_elem(long rid){
		this.RID = rid;
		this.msec_exp = System.currentTimeMillis() + ELEMENT_DELAY;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Return the num. of re-attempts remaining, then decrement that variable.
	 * @return Number of query-attempts remaining for this RID.
	 */
	public int get_decr_reattempts(){
		int old_reattempts = this.reattempts;
		this.reattempts--;
		return(old_reattempts);	
	}
	
	/**
	 * Overriding: Return the time duration until this element 'expires' 
	 * (is eligible for popping), in a time-unit passed in as an argument.
	 */
	public long getDelay(TimeUnit unit){
		long msec_remaining = (this.msec_exp - System.currentTimeMillis());
		return(unit.convert(msec_remaining, DELAY_UNIT));
	}

	/**
	 * Overriding: Compare this [rid_queue_elem] object to another. Comparison
	 * is done based on the RID field, solely.
	 */
	public int compareTo(Delayed other){
		rid_queue_elem rid_other = (rid_queue_elem) other;
		if(this.RID == rid_other.RID)
			return(0);
		else if(this.RID < rid_other.RID)
			return(-1);
		else return (1);
	}

}
