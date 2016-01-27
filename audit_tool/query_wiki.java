package audit_tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mediawiki_api.api_retrieve;

import core_objects.metadata;
import core_objects.pair;
import core_objects.stiki_utils;

/**
 * Andrew G. West - query_wiki.java - Given a range of IP addresses, this
 * class determines the contributions of those addresses, and makes all
 * related MediaWiki queries (user-based, block-based, etc.)
 */
public class query_wiki{	
	
	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * When issuing contribution-based user queries, this is the number
	 * of users that will be combined into one query.
	 */
	public static final int BATCH_SIZE = 50;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Larger audit in which this sub-audit is taking place. Provides
	 * multiple useful paramaters about the audit environment.
	 */
	private audit PARENT_AUDIT; 
	
	/**
	 * Buffer storing IPs in need of processing until enough are present
	 * to issue a full and efficient query to Mediawiki API.
	 */
	private List<String> IP_BUFFER;

	/**
	 * Buffer storing pages in need of processing until enough are present
	 * to issue a full and efficient query to Mediawiki API.
	 */
	private Set<String> TOUCH_BUFFER;
	
	
	// ***************************** CONSTRUCTORS ***************************
	
	/**
	 * Construct an [query_wiki] object
	 * @param parent Larger audit in which this one is taking place
	 */
	public query_wiki(audit parent){
		this.PARENT_AUDIT = parent;
		this.IP_BUFFER = new ArrayList<String>(BATCH_SIZE);
		this.TOUCH_BUFFER = new TreeSet<String>();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Produce a history of user contributions to the wiki.
	 * @return A list of user history objects
	 */
	public List<user_history> create_history() throws Exception{
		
		int ips_done = 0;
		ip_range cur_range;
		List<pair<Long,Long>> contrib_list = 
				new ArrayList<pair<Long,Long>>();
		
		for(int i=0; i < this.PARENT_AUDIT.IP_RANGES.size(); i++){
			cur_range = this.PARENT_AUDIT.IP_RANGES.get(i);
			for(long j=cur_range.IP_BEG_INT; j <= cur_range.IP_END_INT; j++){
				contrib_list.addAll(batch_contribs(stiki_utils.ip_to_string(j), false));
				ips_done++;
				if(ips_done % 500 == 0)
					System.out.println("Done with " + ips_done + 
							" of " + PARENT_AUDIT.NUM_IPS + 
							" IP addresses for first-pass analysis");
			} // Iterate internal to the range sets
		} // Iterate over all range sets
		contrib_list.addAll(batch_contribs(null, true)); // Flush
		
		int TOTAL_EDITS = contrib_list.size();
		System.out.println("\nFound " + TOTAL_EDITS + " revisions in range(s)");
		System.out.println("Now conducting deeper analysis of those edits\n");
		
		long rid, pid;
		int contribs_done = 0;
		Map<Long, pair<metadata,metadata>> edit_data = 
				new TreeMap<Long, pair<metadata,metadata>>();
		for(int i=0; i < contrib_list.size(); i++){
			rid = contrib_list.get(i).fst;
			pid = contrib_list.get(i).snd;
			edit_data.put(rid, query_edit_this_next(pid,rid));
			contribs_done++;
			if(contribs_done % 25 == 0)
				System.out.println("Done with " + contribs_done + 
						" of " + TOTAL_EDITS + " contributions");
		} // Get additional data about all contributions
		
		metadata cur_md;
		Set<String> users = new TreeSet<String>();
		Iterator<pair<metadata,metadata>> iter = edit_data.values().iterator();
		while(iter.hasNext()){
			cur_md = iter.next().fst;
			if(cur_md == null)
				continue;
			users.add(cur_md.user);
		} // Spin over all contributions to determine unique editors
		
		int TOTAL_USERS = users.size();
		System.out.println("\nFound " + TOTAL_USERS + " active editors");
		
			// Get the content of user-pages, where they exist
		Map<String,String> user_pages = user_pages(users);
		
			// Check for users last block action. Notice we only consider
			// users that have a user-page, on the assumption that block
			// notices are typically cross-posted there
		System.out.println("\nPerforming block investigations for " + 
			user_pages.keySet().size() +  " users \n");
		Map<String,pair<Long,String>> block_last = 
				 user_blocks(user_pages.keySet());

				 
		///////////////////////////////////////////////////
				 
			// At this point, we have multiple dis-organized structures
			// with edit/user/block data. Their creation was designed to
			// minimize bandwidth, we now put them into something more
			// query and output friendly
		
		Map<String,user_history> WIKI_HIST = new TreeMap<String,user_history>();
		Iterator<String> iter_users = users.iterator();
		String user;
		while(iter_users.hasNext()){
			user = iter_users.next();
			WIKI_HIST.put(user, new user_history(user));
		} // Intialize the user objects and history
		  // Assume all users exist for remainder of WIKI_HIST building
		
		long key; String next_comment;
		List<Long> edit_list = new ArrayList<Long>(edit_data.keySet());
		Collections.sort(edit_list); // RID order should be timestamp order
		Iterator<Long> iter_edits = edit_list.iterator();
		while(iter_edits.hasNext()){
			key = iter_edits.next();
			if(edit_data.get(key).snd != null)
				next_comment = edit_data.get(key).snd.comment;
			else next_comment = null;
			WIKI_HIST.get(edit_data.get(key).fst.user).
					add_edit(edit_data.get(key).fst, next_comment);
		} // Add edits to user-centric histories
		
		Iterator<String> iter_upages = user_pages.keySet().iterator();
		while(iter_upages.hasNext()){
			user = iter_upages.next();
			WIKI_HIST.get(user).add_talk_page(user_pages.get(user));
		} // Add user pages to user histories
		
		Iterator<String> iter_blks = block_last.keySet().iterator();
		while(iter_blks.hasNext()){
			user = iter_blks.next();
			WIKI_HIST.get(user).add_block_last(block_last.get(user));
		} // Add block histories to user histories
		
		List<user_history> WIKI_HIST_LIST = 
				new ArrayList<user_history>(WIKI_HIST.values());
		Collections.sort(WIKI_HIST_LIST); // Put users in-order
		return(WIKI_HIST_LIST);
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Add an IP address to the "edit" processing batch
	 * @param user Username/IP whose contributions are desired
	 * @param force Whether or not to force the query. Forcing is equivalent
	 * to a forced flush of the buffer.
	 * @return A list containing pairs. The first element will be a RID.
	 * The second element will be an PID. The output will contain these pairs
	 * for ALL edits made by a user in an interest set.
	 */
	@SuppressWarnings("unchecked")
	private List<pair<Long,Long>> batch_contribs(String user, boolean force) 
			throws Exception{
		
		if(user != null)
			IP_BUFFER.add(user);
		if(IP_BUFFER.size() >= BATCH_SIZE || force){
			List<pair<Long,Long>> contribs = 
					api_retrieve.process_joint_contribs(IP_BUFFER, 
					stiki_utils.unix_ts_to_wiki(PARENT_AUDIT.LIMIT_TIME_UNIX));
			IP_BUFFER.clear();
			return(contribs);
		} // Commit query if buffer full or requested
		else return(Collections.EMPTY_LIST);
	}

	/**
	 * Add a page for "touched" batch processing
	 * @param page Page to determine existence and last "touch" time. 
	 * @param force Whether or not to force the query. Forcing is equivalent
	 * to a forced flush of the buffer.
	 * @return A map from page titles to to the last timestamp at which
	 * they were touched. Pages that DNE will not have a map entry.
	 */
	@SuppressWarnings("unchecked")
	private Map<String,Long> batch_page_touched(String page, boolean force) 
			throws Exception{
		
		if(page != null)
			TOUCH_BUFFER.add(page);
		if(TOUCH_BUFFER.size() >= BATCH_SIZE || force){
			Map<String,Long> touch_list = 
					api_retrieve.process_pages_touched(TOUCH_BUFFER);
			TOUCH_BUFFER.clear();
			return(touch_list);
		} // Commit query if buffer full or requested
		else return(Collections.EMPTY_MAP);
	}
		
	/**
	 * Get metdata information about a particular edit, as well as the 
	 * next edit on the same page/article.
	 * @param pid Page identifier of article of interest
	 * @param rid Revision-id (on 'pid') from which to enumerate.
	 * @return A pair of metadata objects. The first element is for edit
	 * 'rid'. The second element is for the edit AFTER 'rid' on 'pid'. If
	 * 'rid' is most recent edit, the second element will be null.
	 */
	private pair<metadata,metadata> query_edit_this_next(long pid, long rid) 
			throws Exception{
		List<metadata> md_list = 
				api_retrieve.process_page_next_meta(pid, rid, 2, null);
		if(md_list.size() == 0)
			return(new pair<metadata,metadata>(null, null));
		else if(md_list.size() == 1)
			return(new pair<metadata,metadata>(md_list.get(0), null));
		else //if(md_list.size() == 2)
			return(new pair<metadata,metadata>(md_list.get(0), md_list.get(1)));
	}
	
	/**
	 * Given a set of users, fetch their user-pages (where they exist)
	 * @param users Set of wiki users (their usernames)
	 * @return A map from usernames to the current version of their user
	 * page. If a user does not have a user-page 
	 */
	private Map<String,String> user_pages(Set<String> users) throws Exception{
		
			// From users, enumerate all "User_Talk:" pages
		Iterator<String> iter = users.iterator();
		Map<String,Long> touches = new TreeMap<String,Long>();
		while(iter.hasNext())
			touches.putAll(batch_page_touched("User_talk:" + iter.next(), false));
		touches.putAll(batch_page_touched(null, true));
		int TALKS_TO_PROBE = touches.size();
		System.out.println("Of these " + TALKS_TO_PROBE + " users have " +
				"talk/profile pages to be analyzed\n");

			// Only query for actual user-page content when, (a) the user-
			// page exists, and (b) has been touched per time bounds.
		int talks_handled = 0;
		long touched;
		String page, user, user_page;
		Map<String,String> user_page_map = new TreeMap<String,String>();
		Iterator<String> iter_touches = touches.keySet().iterator();
		while(iter_touches.hasNext()){
			page = iter_touches.next();
			touched = touches.get(page);
			if(touched >= PARENT_AUDIT.LIMIT_TIME_UNIX){
				user_page = api_retrieve.process_page_content(page);
				user = page.split(":")[1];
				user_page_map.put(user, user_page);
			} // Confirm time bounds on user-page modification/touch
			
			talks_handled++;
			if(talks_handled % 25 == 0)
				System.out.println("Done with " + talks_handled + 
						" of " + TALKS_TO_PROBE + " user pages");
		} // Iterate over all user-talk pages that actually exist
		return(user_page_map);
	}
	
	/**
	 * Produce the most recent block action for a set of users.
	 * @param users Users whose recent block histories are desired
	 * @return A map from users to their last block action. No map entry will
	 * exist for a user w/o a block history. The first element is a UNIX 
	 * timestamp, the second element is an action string, either "block" or 
	 * "unblock". In combination, these tell us all we need to know.
	 */
	private Map<String,pair<Long,String>> user_blocks(Set<String> users) 
			throws Exception{
		
		String user;
		List<pair<Long,String>> temp_list;
		Map<String,pair<Long,String>> block_map = 
				new TreeMap<String,pair<Long,String>>();
		Iterator<String> iter = users.iterator();
		int blocks_investigated = 0;
		while(iter.hasNext()){
			user = iter.next();
			temp_list = api_retrieve.process_block_hist(user);
			if(temp_list == null || temp_list.size() == 0){
				// Do nothing. Do not "continue" so we can hit progress-meter
			} else if(temp_list.get(0).snd.equals("block") || 
					temp_list.get(0).fst >= PARENT_AUDIT.LIMIT_TIME_UNIX)
				block_map.put(user, temp_list.get(0));
			
			blocks_investigated++;
			if(blocks_investigated % 25 == 0)
				System.out.println("Done with " + blocks_investigated + 
						" of " + users.size() + " block investigations");
		} // Investigate all requested users
		return(block_map);
	}
	
}
