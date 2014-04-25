package gui_edit_queue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mysql.jdbc.CommunicationsException;

import mediawiki_api.api_retrieve;
import core_objects.metadata;
import core_objects.pair;
import core_objects.stiki_utils;
import core_objects.stiki_utils.SCORE_SYS;
import executables.stiki_frontend_driver;
import gui_support.diff_markup;
import gui_support.diff_whitespace;

/**
 * Andrew G. West - gui_display_pkg.java - This encapsulates all the items
 * necessary to display an edit in the GUI (the diff, the metadata, and the
 * edit token). Thus, multiple objects of this type can be created and cached,
 * such that the fetching of this pieces need not occur at the time of demand.
 */
public class gui_display_pkg{

	// ***************************** CONSTANTS *******************************

	/**
	 * Depth of article history for storage.
	 */
	public static final int HIST_DEPTH = 5;
	
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Metadata for page history to some depth. First edit in 
	 * the list is the primary edit of interest.
	 */
	public final List<metadata> page_hist;
	
	/**
	 * Convenience field for accessors. Simply the first element of
	 * the [page_hist] list variable created immediately above.
	 */
	public final metadata metadata;
	
	/**
	 * Colored HTML diff, which will be displayed in the diff-browser. 
	 */
	public final String content;
	
	/**
	 * Diff-browswer content identical to [content], except for the fact 
	 * this version has activated/clickable hyperlinks.
	 */
	public final String content_linked;
	
	/**
	 * If a roll-back edit were to be performed on the head edit of this
	 * object, this is the number of revisions affected. If a simple revert
	 * action is being performed, this value == 1. 
	 */
	public final int rb_depth;
	
	/**
	 * A set describing the permissions held by the author of page_hist[0].
	 */
	public final Set<String> user_perms;
	
	/**
	 * Whether the author of page_hist[0] has a "user_talk" page.
	 */
	public final boolean user_has_talkpage;
	
	/**
	 * Whether the author of page_hist[0] has a "user" page.
	 */
	public final boolean user_has_userpage;
	
	/**
	 * Queue from which the RID wrapped by this object was obtained.
	 */
	public final SCORE_SYS source_queue;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Edit token to detect conflicting edits and map edit to Wiki-user. 
	 * Edit token may need refreshed, thus it is private and non-final.
	 */
	private pair<String,String> edit_token;
	
	/**
	 * For the user under inspection (i.e., the author of page_hist[0]).
	 * This is the number of edits that author has made in all history.
	 * Useful in ensuring we "don't template the regulars." This field 
	 * only intends to be useful for registered editors. In the case
	 * of a non-registered editor it should be negative one (-1).
	 * 
	 * This is private and non-final because we might not want to take
	 * the penalty up front of computing this for all edits. Be aware
	 * of the fact it could be set to NULL; see its accessor method herein.
	 * 
	 * Note that this should be checked for NULL; as it may be set to NULL
	 * if we don't to set the penalty of pre-calculating it.
	 */
	private Integer user_edit_count;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_display_pkg] by providing all fields. Field
	 * descriptions are not java doc'ed -- are identical to private vars.
	 */
	public gui_display_pkg(List<metadata> page_hist, 
			pair<String,String> edit_token, String content, 
			String content_linked, SCORE_SYS source_queue, 
			int rb_depth, Set<String> user_perms, boolean user_has_talkpage,
			boolean user_has_userpage, Integer user_edit_count){
		
		this.page_hist = page_hist;
		this.metadata = page_hist.get(0);
		this.edit_token = edit_token;
		this.content = content;
		this.content_linked = content_linked;
		this.rb_depth = rb_depth;
		this.source_queue = source_queue;
		this.user_perms = user_perms;
		this.user_has_talkpage = user_has_talkpage;
		this.user_has_userpage = user_has_userpage;
		this.user_edit_count = user_edit_count;
	}
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Given an RID, make the MediaWiki calls needed to obtain all details
	 * and content necessary to display that edit in the main window, with
	 * a null edit-token (i.e., the use of this object should result
	 * in no changes to the live Wikipedia). FURTHER, this version does
	 * not populate the COUNTRY field of the METADATA object, making
	 * it unsuitable for use in typical STIKI and LIVE applications.
	 * @param rid Revision-ID of edit under inspection
	 * @return A [gui_display_pkg] will be returned, with the [edit_token]
	 * object set to null, per the introduction to this method
	 */
	public static gui_display_pkg create_offline(long rid) throws Exception{
			
		try{	// MediaWiki API all the necessary data
			metadata md = api_retrieve.process_basic_rid(rid);
			Set<String> perms;
			if(!md.user_is_ip) // IPs have no permissions
				perms = api_retrieve.process_user_perm(md.user);
			else perms = new HashSet<String>(0);
			
			Set<String> titles_missing = new HashSet<String>();
			titles_missing.add("User_talk:" + md.user);
			titles_missing.add("User:" + md.user);
			titles_missing = api_retrieve.process_pages_missing(titles_missing);
			boolean user_has_talkpage = !titles_missing.contains("User_talk:" + md.user);
			boolean user_has_userpage = !titles_missing.contains("User:" + md.user);
			
				// And manipulate the diff-content as needed
			String diff = api_retrieve.process_diff_prev(rid);
			String content = diff_markup.beautify_markup(
					diff, md.title, "", false); 
			content = diff_whitespace.whitespace_diff_html(content);
			String con_link = diff_markup.beautify_markup(
					diff, md.title, "", true); 
			con_link = diff_whitespace.whitespace_diff_html(con_link);	
			
			List<metadata> meta_list = new ArrayList<metadata>(1);
			meta_list.add(md); // Just create a one element list
			return(new gui_display_pkg(meta_list, null, content, 
					con_link, null, 0, perms, user_has_talkpage, 
					user_has_userpage, null));
			
		} catch(Exception e){
			return null;
		} // If error encountered -- just skip the edit
	}
		
	/**
	 * Given an RID, make the MediaWiki calls needed to obtain all details
	 * and content necessary to display that edit in the main window. Further,
	 * return NULL if 'rid' is not the most recent revision on page 'pid'.
	 * @param parent Root GUI class containing all DB-handlers
	 * @param rid Revision-ID of edit under inspection
	 * @param pid Page-ID (article) on which 'rid' resides
	 * @param cookie Cookie encoding user-mapping data
	 * @param using_native_rb Whether or not an edit token should be obtained.
	 * One is not needed if rollback being used (will be set to NULL)
	 * @return A fully populated [gui_display_pkg] will be returned. If a 
	 * data-acquisition error is encountered, then NULL will be returned. Also,
	 * return NULL if 'rid' is not the most recent revision on page 'pid'.
	 */
	public static gui_display_pkg create_if_most_recent(
			stiki_frontend_driver parent, long rid, long pid, String cookie, 
			boolean using_native_rb, SCORE_SYS source_queue) throws Exception{
			
		try{	// Begin by fetching recent page metadata
				// Multiple RIDs are returned to determine rollback-depth
			List<metadata> page_hist = api_retrieve.process_page_hist_meta(
					pid, gui_display_pkg.HIST_DEPTH, cookie, 
					parent.client_interface);
			
			if((page_hist.size() == 0 && (api_retrieve.process_badrevid(rid) ||  
					api_retrieve.process_page_missing(pid))) || 
					rid != page_hist.get(0).rid){
				parent.client_interface.queues.queue_delete(rid);
				return null;
			}	// If the enqueued RID has been deleted/redacted ...
				// ... or the PID is invalid ...
				// ... or it is no longer most recent on the page ...
				// Delete from queues and abandon in GUI
				
			pair<String,String> edit_token;
			if(!using_native_rb) // Fetch edit token, if not using native RB
				edit_token = api_retrieve.process_edit_token(pid, cookie);
			else edit_token = null;
						
			int edits_to_rb = 0;
			long rid_to_diff = 0;
			metadata meta = page_hist.get(0); // convenience
			for(int i=0; i < page_hist.size(); i++){
				if(page_hist.get(i).user.equals(meta.user))
					edits_to_rb++;
				else{
					rid_to_diff = page_hist.get(i).rid;
					break;
				} // Need to go one-past author contribs to find diff-to RID
			} // Determine quantity of consecutive user edits
			if(edits_to_rb == 0 || rid_to_diff == 0){
				parent.client_interface.queues.queue_delete(rid);
				return(null);
			} // If page-hist only has one author, RB will fail
			
			String note = "";
			if(edits_to_rb > 1){
				note = "Below is displayed a combined diff for " + 
						(edits_to_rb) + " edits by the same user<BR>" +
						"The edit properties box shows information " +
						"for the most recent of these edits<BR>" +
						"If instructed, STiki will revert ";
				if(edits_to_rb == 2)
					note += "both edits";
				else note += "all " + (edits_to_rb) + " edits";
			} // If our diff presentation spans multiple edits, we need
			  // to make visual note of this (just below title)
			
				// And manipulate the diff-content as needed
				// On the API call, note that we already know at least part
				// of the rollback is current, making the "diff_current" safe			
			String diff = api_retrieve.process_diff_current(rid_to_diff);
			
			String content = diff_markup.beautify_markup(
					diff, page_hist.get(0).title, note, false); 
			content = diff_whitespace.whitespace_diff_html(content);
			String con_link = diff_markup.beautify_markup(
					diff, page_hist.get(0).title, note, true); 
			con_link = diff_whitespace.whitespace_diff_html(con_link);
			
			Set<String> perms;
			if(!meta.user_is_ip) // IPs have no permissions
				perms = api_retrieve.process_user_perm(meta.user);
			else perms = new HashSet<String>(0);
			
				// Determine existence of user/talk pages
			Set<String> titles_missing = new HashSet<String>();
			titles_missing.add("User_talk:" + meta.user);
			titles_missing.add("User:" + meta.user);
			titles_missing = api_retrieve.process_pages_missing(titles_missing);
			boolean user_has_talkpage = !titles_missing.contains("User_talk:" + meta.user);
			boolean user_has_userpage = !titles_missing.contains("User:" + meta.user);
			
			Integer edit_count = null;
			if(meta.user_is_ip)
				edit_count = -1;
			else if(parent.menu_bar.get_options_menu().get_dttr_policy()){
				edit_count = (int) api_retrieve.process_user_edits(meta.user);
			} // If user is registered (i.e., possibly a "regular", compute
			  // his/her edit count. But only take this performance
			  // penalty if we expected this to be used in DTTR warning

			return(new gui_display_pkg(page_hist, edit_token, content, 
					con_link, source_queue, edits_to_rb, perms, 
					user_has_talkpage, user_has_userpage, edit_count));
			
		} catch(CommunicationsException e){
			parent.reset_connection(false);
			return(create_if_most_recent(parent, rid, pid, 
					cookie, using_native_rb, source_queue));
		} // If a CommunicationsFailure, reset the connection and retry 
		catch(Exception e){
			// e.printStackTrace();
			return null;
		} // Any other error is MediaWiki assumed -- just skip the edit
	}
	
	/**
	 * Determine whether or not this package wraps a "zero diff"; that is,
	 * one where there are no differences (and the HTML 'content' field
	 * would display only a page title, but no changes)
	 * @return TRUE if the wrapped difference is zero; FALSE otherwise.
	 */
	public boolean has_zero_diff(){
		
			// Note this is a consequence of the diff browser showing
			// rollback actions. We don't want to display zero-diffs to 
			// end users (what's the point? no change). However, we can't
			// check for this at queuing time because the server-side
			// does its an analysis at the per-edit (not rollback) level.
		
			// Moreover, this is a hacky way to detect it. If there are no
			// colored HTML tables, then no comparison is being made.
		return(!content.contains("bgcolor"));
	}
	
	/**
	 * Create an "end" queue [gui_display_pkg]. Given that [gui_display_pkg]
	 * objects are en-queued for STiki inspection/display -- it may be the
	 * case that the queue runs empty. If this is ever the case (and re-
	 * population is not automatic), then the queue should be filled with 
	 * objects of this type -- which are essentially null and present
	 * visual notification that the queue is empty.
	 */
	public static gui_display_pkg create_end_pkg(){
		String end_con = "<HTML><HEAD></HEAD><BODY><DIV ALIGN=\"center\"> ";
		end_con += "All edits from the provided queue have been exhausted. ";
		end_con += "Please restart STiki or the offline-research-tool ";
		end_con += "(ORT), as required for your usage."; 
		end_con += "</DIV></BODY><HTML>";
		
		List<metadata> meta_list = new ArrayList<metadata>(1);
		meta_list.add(new metadata()); // Just create a one element list
		return(new gui_display_pkg(meta_list, null, end_con, 
				end_con, null, 0, new HashSet<String>(0), false, false, null));
	}
	
	
	// ***** ACESSOR AND UPDATE METHODS
	
	/**
	 * Determine the number of edits made by the user under inspection
	 * Crucially, if this was pre-queried at package construction, we can 
	 * now quickly make use of that value. Otherwise, a query is performed.
	 * @return The number of edits made by the user under investigation
	 * (i.e., that wrapped by metadata). If the user is unregistered, then
	 * the value negative one (-1) will be returned.
	 */
	public int get_user_edit_count() throws Exception{
		if(this.user_edit_count == null){
			if(this.metadata.user_is_ip)
				return(-1);
			else{
				this.user_edit_count = (int) api_retrieve.process_user_edits(
						this.metadata.user, 0, stiki_utils.cur_unix_time(), 
						0, 50, null, 50);
				return(this.user_edit_count);
			} // If we do have to compute, store persistently
					
		} else return(this.user_edit_count);
	}
	
	/**
	 * Accessor method: Return the [edit_token] field of this object.
	 * @return return the [edit_token] field of this object
	 */
	public pair<String,String> get_token(){
		return (this.edit_token);
	}

	/**
	 * Update method: Update RB token of the metadata object
	 * @param session_cookie Cookie with which update should take place
	 */
	public void refresh_rb_token(String session_cookie) throws Exception{
		this.metadata.refresh_rb_token(session_cookie);
	}
	
	/**
	 * Update method: Update the edit tokens of this object.
	 *  @param session_cookie Cookie with which update should take place
	 */
	public void refresh_edit_token(String session_cookie) throws Exception{
		this.edit_token = api_retrieve.process_edit_token(
				this.metadata.pid, session_cookie);
	}
	
}
