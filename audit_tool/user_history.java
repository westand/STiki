package audit_tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import core_objects.metadata;
import core_objects.pair;
import core_objects.stiki_utils;


/**
 * Andrew G. West - user_history.java - Object wrapping the user history
 * of a single IP/user.
 */
public class user_history implements Comparable<user_history>{

	// ************************ PUBLIC-STATIC FIELDS ************************
	
	/**
	 * A list of properties that a user-page could exhibit.
	 */
	public static enum UPAGE_PROP{SHARED_IP, VANDALISM, SPAM, OTHER_ISSUE};
	

	// **************************** PUBLIC FIELDS ***************************

	// ******* PROVIDED FIELDS ********
	
	/**
	 * Username or IP address of the user whose history is stored here.
	 */
	public final String USER;
	
	/**
	 * List storing prior edits of 'USER.' Should be stored in ascending
	 * order by timestamp. The 'metadata' stores actual edit data. The 
	 * second boolean element encodes whether heuristics indicate
	 * whether or not the edit is vandalism.
	 */
	public List<pair<metadata,Boolean>> EDITS;
	
	/**
	 * Content of 'USER's talk page.
	 */
	public String USER_TALK_PAGE = null;
	
	/**
	 * Last block action affecting 'USER'. First element is the timestamp of 
	 * the block action, the second is the action string, 
	 * i.e., "block" or "unblock"
	 */
	public pair<Long,String> BLOCK_LAST = null;
	
	
	// ********** CALCULATED **********
	
	/**
	 * Set of properties exhibited by document 'USER_TALK_PAGE'
	 */
	public Set<UPAGE_PROP> UPAGE_FEATS = null;
	
	/**
	 * Number of [EDITS] objects that were reverted
	 */
	public int REVERTED_EDITS = 0;
	
	
	// ***************************** CONSTRUCTORS ***************************
	
	/**
	 * Construct a [user_history]
	 * @param username Username whose history we are constructing
	 */
	public user_history(String username){
		this.USER = username;
		this.EDITS = new ArrayList<pair<metadata,Boolean>>();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	// *********** BUILDERS ***********
	
	/**
	 * Add an edit to this users contribution list
	 * @param edit Metadata wrapping details about the contribution
	 * @param next_rev_comment Revision comment of the next revision on the
	 * same article. Should be NULL if no such revision exists.
	 */
	public void add_edit(metadata edit, String next_rev_comment){
		boolean next_bad = comment_indicates_trouble(next_rev_comment);
		this.EDITS.add(new pair<metadata,Boolean>(edit, next_bad));
		if(next_bad)
			this.REVERTED_EDITS++;
	}
	
	/**
	 * Associate a user talk page with this user. 
	 * @param user_talk_page User talk page content
	 */
	public void add_talk_page(String user_talk_page){
		this.USER_TALK_PAGE = user_talk_page;
		this.UPAGE_FEATS = user_page_properties();
	}
	
	/**
	 * Associate a "last block action" to this user. 
	 * @param block_last Data about users last block action. Specific 
	 * format is described in the class variable "BLOCK_LAST"
	 */
	public void add_block_last(pair<Long,String> block_last){
		this.BLOCK_LAST = block_last;
	}
	
	@Override
	public int compareTo(user_history other){
		try{long this_ip = stiki_utils.ip_to_long(this.USER);
			long that_ip = stiki_utils.ip_to_long(other.USER);
			if(this_ip < that_ip) 
				return -1;
			else if(this_ip > that_ip) 
				return 1;
			else return 0;
		} catch(Exception e){
			return(this.USER.compareTo(other.USER));
		} // Prefer to sort IP addresses numerically
		  // If either party not IP, default to alphabetical
	}		
		
	
	// *************************** PRIVATE METHODS ***************************

	
	/**
	 * Determine the set of "properties" this user page exhibits.
	 * @return A set containing the properties of this user's user talk 
	 * page. These are primarily template-driven (vandalism, shared-ip, etc.)
	 */
	private Set<UPAGE_PROP> user_page_properties(){
		Set<UPAGE_PROP> props = new TreeSet<UPAGE_PROP>();
		
		if(this.USER_TALK_PAGE == null)
			return(props);
		String CAP_TP = this.USER_TALK_PAGE.toUpperCase();
		
		if(CAP_TP.contains("TEMPLATE:UW-VANDALISM") || 
				CAP_TP.contains("TEMPLATE:HUGGLE/WARN"))
			props.add(UPAGE_PROP.VANDALISM);
		if(CAP_TP.contains("TEMPLATE:UW-SPAM") || 
				CAP_TP.contains("TEMPLATE:HUGGLE/WARN-SPAM"))
			props.add(UPAGE_PROP.SPAM);
		if(CAP_TP.contains("TEMPLATE:SHAREDIP"))
			props.add(UPAGE_PROP.SHARED_IP);
		return(props);
	}
	
	
	/**
	 * Determine whether some comment indicates vandalism reversion.
	 * @param rev_comment Revision comment to analyze
	 * @return TRUE if the vandalism indicates undo/revert (of a previous
	 * edit). FALSE, otherwise.
	 */
	private static boolean comment_indicates_trouble(String rev_comment){
		
			// This comment heuristic is a cheap one
		if(rev_comment == null)
			return(false);
		rev_comment = rev_comment.toUpperCase();
		if(rev_comment.contains("VANDALISM") || 
				rev_comment.contains("REVERT") ||
				rev_comment.contains("UNDID") ||
				rev_comment.contains("UNDO") ||
				rev_comment.contains("RV "))
			return(true);
		else return(false);
	}
	
}
