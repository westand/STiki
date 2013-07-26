package core_objects;

/**
 * Andrew G. West - feature_set.java - This class wraps the features
 * associated with a single-edit into a single object for easily handling.
 */
public class feature_set{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Vandalism [true], or not-vandalism [false] label applied to the 
	 * feature set for training purposes. Default to [false] if unknown.
	 */
	public final boolean LABEL;
	
	/**
	 * Revision-ID whose feature set is encoded by this object.
	 */
	public final long R_ID;
	
	/**
	 * Whether the user who committed this edit was anonymous/IP.
	 */
	public final boolean IS_IP;
	
	/**
	 * Non-normalized user reputation of [R_ID]'s editor, per PreSTA style.
	 */
	public final double REP_USER;
	
	/**
	 * Non-normalized article reputation of [R_ID]'s page, per PreSTA style.
	 */
	public final double REP_ARTICLE;
	
	/**
	 * Time-of-day (local) at which [R_ID] was made, a float on [0,24).
	 */
	public final float TOD;
	
	/**
	 * Day-of-week on which [R_ID] was made on [1,7]. SATURDAY=7, SUNDAY=1.
	 */
	public final int DOW;
	
	/**
	 * Time-since registration. For the editor who made, [R_ID], what is
	 * the interval between the first edit ever seen by the editor (their
	 * estimated registration time), and the time at which [R_ID] was made?
	 */
	public final long TS_R;
	
	/**
	 * Time-since last page edit. For the page on which [R_ID] was made,
	 * how many seconds prior to [R_ID] was the page last edited? Should be
	 * (-1) if [R_ID] was first edit on the article/page of interest.
	 */
	public final long TS_LP;
	
	/**
	 * Time-since last user rollback. For the editor who made [R_ID], how
	 * many seconds ago did they last committ an edit which was rolled-back
	 * (flagged as vandalism)? Should be (-1) if no such edit exists. 
	 */
	public final long TS_RBU;
	
	/**
	 * Length in characters of revision comment left with [R_ID].
	 */
	public final int COMM_LENGTH;
	
	/**
	 * Given an edit, this feature records the size, in bytes, of the article
	 * after this edit was made, relative to the size of the article before it
	 * was made. For example, "+4" indicates an addition of 4 bytes.
	 */
	public final int BYTE_CHANGE;
	
	/**
	 * Reputation of the country of origin for an IP address, per geolocation
	 * data (only possible for anonymous users identified by IP).
	 */
	public final double REP_COUNTRY;
	
	
	// ********** NLP FEATURES ********
	
	/**
	 * Whether or not the text-added by a revision matches any dirty/offensive
	 * regexes. The feature value is the number of regexes violated. 
	 */
	public final int NLP_DIRTY;
	 
	/**
	 * The maximum number of consecutive repetitions of a single character in
	 * the text added by the revision. (i.e., addition of "aaaaaa" = 6).
	 */
	public final int NLP_CHAR_REP;
	
	/**
	 * The percentage of the edit addition which is capitalized (upper-case).
	 */
	public final double NLP_UCASE;
	
	/**
	 * Percentage of the edit addition which is alpha characters (i.e.,
	 * textual rather than numerical or format based).
	 */
	public final double NLP_ALPHA;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [feature_set] object by providing all fields. We forgoe
	 * an at lenth description of the arguments here; it is provided above. 
	 */
	public feature_set(boolean label, long r_id, boolean is_ip, double rep_user, 
			double rep_article, float tod, int dow, long ts_r, long ts_lp,
			long ts_rbu,  int comm_length, int byte_change, 
			double rep_country,  int nlp_dirty,int nlp_char_rep, 
			double nlp_ucase, double nlp_alpha){
		
		LABEL = label;
		R_ID = r_id;
		IS_IP = is_ip;
		REP_USER = rep_user;
		REP_ARTICLE = rep_article;
		TOD = tod;
		DOW = dow;
		TS_R = ts_r;
		TS_LP = ts_lp;
		TS_RBU = ts_rbu;
		COMM_LENGTH = comm_length;
		BYTE_CHANGE = byte_change;
		REP_COUNTRY = rep_country;
		NLP_DIRTY = nlp_dirty;
		NLP_CHAR_REP = nlp_char_rep;
		NLP_UCASE = nlp_ucase;
		NLP_ALPHA = nlp_alpha;
	}

}
		