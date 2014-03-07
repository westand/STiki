package mediawiki_api;

import gui_support.gui_settings;
import gui_support.gui_settings.SETTINGS_BOOL;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

import core_objects.metadata;
import core_objects.pair;
import core_objects.stiki_utils;
import db_client.client_interface;
import db_server.db_geolocation;

/**
 * Andrew G. West - api_retrieve.java - This class takes in a simple
 * piece of identifying data (e.g. RID), then uses it to build a call
 * to the MediaWiki-API, and then hands the results to the proper
 * XML-parser, to extract the necessary data.
 * 
 * So far the different formats implemented by this class include:
 * 
 * 		[x]:  "basic rid" -- given an RID, we can determine the timestamp, 
 * 			  pid, editor, namespace, comment, etc. of that revision
 * 		[x]:  "multiple rids" -- given a list of RIDS, get their metadata. This
 * 			  is less lightweight than the singular version above.
 * 		[x]:  "offender search" -- given a RB-ID, the name of the offender,
 * 			  and the page of offense -- we look for the guilty edit RID.
 * 		[x]:  "user perms" -- given a user name, we check to see if the
 * 			  permissions that user has above the norm
 * 		[x]:  "user first edit ts" -- given a user, we determine the timestamp
 * 			  at which that user committed his/her first ever edit 
 * 		[x]:  "prior page edit ts" -- given a page, determine when that page
 * 			  was last edited (relative to a more current RID).
 *		[x]:  "next page edit ts" -- given a page, determine when that page
 * 			  was next edited (relative to some RID).
 * 		[x]:  "size change" -- given an edit, determine the size of the page
 * 			  after that edit, relative to the previous version (in bytes).
 * 		[x]:  "diff text prior" -- given an RID, output the HTML annotated
 *            diff between the edit, and the prior one on the same article.
 *    	[xx]:  "diff text current" -- given an RID, output the HTML annotated
 *            diff between the edit, and the current one on the same article.
 *      [xx]: "latest rid on page" -- given a PID (article-ID), return
 *            the RID corresponding to the most recent edit on that article
 *      [xx]: "get edit token" -- given a page, secure the editing token
 *      	  necessary to make an edit on that page (which is specific to
 *      	  the user editing, if an active session is open).
 *      [xx]: "get page content" -- get current content, given title
 *      [xx]: "get page content" -- get (maybe historical) content given RID
 *      [xx]: "block status" -- return whether or not some user/IP is blocked
 *      [xx]: "page flagged" -- if page is under "pending changes" protection
 *      [xx]: "page protections -- if page is under standard protection
 *      [xx]: "user autoconfirmed" -- if a user has 'autoconfirmed' status
 *      [xx]: "prev [n] on page" -- get data for [n] previous page edits
 *      [xx]: "next [n] on page" -- get data for [n] next page edits
 *      [xx]: "is badrevid" -- whether a RID is "bad" or not    
 *      [xx]: "page is missing" -- whether a PID is "missing" or not
 *      [xx]: "xlinks on page" -- external links; valid for CURRENT version.
 *      [xx]: "block history" -- archived blocks for a user
 *      [xx]: "pages last touched" -- last edit date for a group of pages
 *      [xx]: "joint contribs" -- getting data about the contribuitions
 *            from multiple users in a single, large query.
 *  	[xx]: "total user edit count" -- edit count for a provided user.
 *  		  this call comes in multiple flavors.
 *  	[xx]: "pages missing" -- given a set of pages, return the subset
 *  		  of pages that do not exist on the wiki.
 *  	[xx]: "category members" -- provided a category, produce a set that
 *  		  contains all article members of that category.
 *  	[xx]: "deleted revs" -- produce deleted (article) revisions by
 *  		  some user. Not this is not RevDelete.
 *  	[xx]: "page cats" -- produce the category memberships of a page.
 *  	[xx]: "size at time" -- size of a page in bytes at some timestamp
 *  
 */
public class api_retrieve{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Given that an HTTP connection fails to establish between the client,
	 * this number of re-connects should be attempted before failure.
	 */
	public static final int NUM_HTTP_RETRIES = 2;
	
	/**
	 * The [base_url()] function over which all API calls operate is 
	 * specific to English Wikipedia. Should we wish to override it, this
	 * value can be set or overwritten. It's contents will be interpeted
	 * if it is a non-null value.
	 */
	public static String BASE_URL_OVERRIDE = null;
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Return the base URL for making API retrievals over en.wp. Note that 
	 * this is fixed aside from the (HTTP|HTTPS) protocol choice, which
	 * is implemented as a persistent user option
	 * @return Base URL for making API retrievals over en.wp.
	 */
	public static String base_url(){
		if(BASE_URL_OVERRIDE != null)
			return(BASE_URL_OVERRIDE);
		else{
			String base = "en.wikipedia.org/w/api.php?action=query";
			if(gui_settings.get_bool_def(SETTINGS_BOOL.options_https, false))
					return("https://" + base);
			else return("http://" + base);
		} // alllow manual override of this logic
	}
	
	
	// ************
	
	/**
	 * Given an RID, retrieve basic edit metadata.
	 * @param rid Revision-ID (RID) of the edit of interest
	 * @param db_geo DB-handler so edit source can be determined
	 * @return Metadata of edit 'rid', nor null if it could not be found.
	 */
	public static metadata process_basic_rid(long rid, db_geolocation db_geo) 
			throws Exception{
		api_xml_basic_rid handler = new api_xml_basic_rid(db_geo);
		do_parse_work(new URL(url_basic_rid(rid)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given an RID, retrieve basic edit metadata. Note this version does
	 * not handle the GEOLOCATION portion, and sets the COUNTRY field
	 * of the returned metadata object to the empty string.
	 * @param rid Revision-ID (RID) of the edit of interest
	 * @param session_cookie Cookie unique to fetching user (for RB token)
	 * @return Metadata of edit 'rid', nor null if it could not be found.
	 */
	public static metadata process_basic_rid(long rid) throws Exception{
		api_xml_basic_rid handler = new api_xml_basic_rid(null);
		do_parse_work(new URL(url_basic_rid(rid)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given an RID, retrieve basic edit metadata.
	 * @param rid Revision-ID (RID) of the edit of interest
	 * @param session_cookie Cookie unique to fetching user (for RB token)
	 * @param db_geo DB-handler so edit source can be determined
	 * @return Metadata of edit 'rid', nor null if it could not be found.
	 */
	public static metadata process_basic_rid(long rid, String session_cookie, 
			db_geolocation db_geo) throws Exception{
		api_xml_basic_rid handler = new api_xml_basic_rid(db_geo);
		do_parse_work(new URL(url_basic_rid(rid)), session_cookie, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given an RID, retrieve basic edit metadata. Note this version does
	 * not handle the GEOLOCATION portion, and sets the COUNTRY field
	 * of the returned metadata object to the empty string.
	 * @param rid Revision-ID (RID) of the edit of interest
	 * @param session_cookie Cookie unique to fetching user (for RB token)
	 * @return Metadata of edit 'rid', nor null if it could not be found.
	 */
	public static metadata process_basic_rid(long rid, String session_cookie) 
			throws Exception{
		api_xml_basic_rid handler = new api_xml_basic_rid(null);
		do_parse_work(new URL(url_basic_rid(rid)), session_cookie, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given a LIST of RIDs, retrieve their edit metadata. This is a more
	 * effecient version than if one were to call the above 
	 * [process_basic_rid()] method multiple times.
	 * @param rid_list List of RIDs whose metadata is desired
	 * @param db_geo DB-handler so edit source can be determined
	 * @return Set of [metadata] objects encoding the metadata of those
	 * RIDs in 'rid_list'. If data was not available for some RID, then
	 * the corresponding metadata will not be available in the return set.
	 */
	public static List<metadata> process_multiple_rids(List<Long> rid_list, 
			db_geolocation db_geo) throws Exception{
		api_xml_multiple_rids handler = new api_xml_multiple_rids(db_geo);
		do_parse_work(new URL(url_multiple_rids(rid_list)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given a rollback (RB) edit, attempt to find the offending edit (OE) ID.
	 * @param uc_offender User that RB indicates committed guilty edit
	 * @param pid Page on which RB and (assumingly) OE took place
	 * @param flag_rid RID of the flagging edit that initiated this search.
	 * Crucially, this param allows historical searches to be done.
	 * @param search_depth Number of historical edits to examine in OE search
	 * @return The RID of the OE, if it can found. -1 (negative one), otherwise
	 */
	public static long process_offender_search(String uc_offender, long pid,
			long flag_rid, int search_depth) throws Exception{
		
		api_xml_find_off handler = new api_xml_find_off(uc_offender);
		String offender_url = url_offender_search(pid, flag_rid, search_depth);
		do_parse_work(new URL(offender_url), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given a username, determine the permissions he/she has.
	 * @param user User whose rights are being examined
	 * @return Set containing privileges held by 'user'
	 */
	public static Set<String> process_user_perm(String user) throws Exception{
		api_xml_user_perm handler = new api_xml_user_perm(user);
		do_parse_work(new URL(url_user_perm(user)), null, handler);
		return(handler.get_result()); // Return result from object
	}
	
	/**
	 * Return the timestamp at which 'user' committed his/her first edit.
	 * @param user User whose first edit time is being looked-up
	 * @return Timestamp at which 'user' performed his/her first edit
	 */
	public static long process_user_first_edit_ts(String user) throws Exception{
		api_xml_user_first handler = new api_xml_user_first();
		do_parse_work(new URL(url_user_first_edit(user)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Return the timestamp at which a page was last edited (prior to some RID).
	 * @param pid Page-ID of page under investigation (and the page of 'rid')
	 * @param rid Revision-ID whose prior-edit is being found. Crucially,
	 * this allows queries of a historical nature to be made
	 * @return Timestamp at which 'pid' was last edited, before 'rid'.
	 */
	public static long process_prior_page_edit_ts(long pid, long rid) 
			throws Exception{
		api_xml_page_prior handler = new api_xml_page_prior();
		do_parse_work(new URL(url_prior_page_edit(pid, rid)), null, handler);
		return(handler.get_result()); // Return result from parser
	}

	/**
	 * Return the timestamp at which a page was next edited (rel. to some RID).
	 * @param pid Page-ID of page under investigation (and the page of 'rid')
	 * @param rid Revision-ID whose next-edit is being found.
	 * @return Timestamp at which 'pid' was next edited, after 'rid', or -1
	 * if 'rid' is the most current edit on 'pid'
	 */
	public static long process_next_page_edit_ts(long pid, long rid) 
			throws Exception{
			// Note the same XML-handler is used as in the "prior TS" case.
		api_xml_page_prior handler = new api_xml_page_prior();
		do_parse_work(new URL(url_newer_page_edit(pid, rid)), null, handler);
		return(handler.get_result()); // Return result from parser
	}

	/**
	 * Return the size-change (in bytes), which an edit made relative to
	 * the previous version of the same page. This is NOT an edit distance,
	 * but only a measure of the number of bytes needed to store the page.
	 * @param pid Page-ID of page under investigation (and the page of 'rid')
	 * @param rid Revision-ID whose size-change is being queried
	 * @return The number of bytes of size-change that revision 'rid' made,
	 * relative to the previous version on the same page. The returned integer
	 * may be positive or negative, indicating the addition and removal of 
	 * bytes, respectively. Size data may not be available for very old
	 * edits, and in such cases, zero will be returned.
	 */
	public static int process_size_change(long pid, long rid) throws Exception{
		api_xml_size_change handler = new api_xml_size_change();
		do_parse_work(new URL(url_size_change(pid, rid)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Return the diff-text between and a revision and that previous. 
	 * @param rid Revision-ID that should be compared against previous edit
	 * @return HTML-marked up diff text, as a String.
	 */
	public static String process_diff_prev(long rid) throws Exception{
		api_xml_diff_text handler = new api_xml_diff_text();
		do_parse_work(new URL(url_diff_prev(rid)), null, handler);
		return(handler.get_result()); // Return result from parser	
	}
	
	/**
	 * Return the diff between and an RID and the current one on same page. 
	 * @param rid Revision-ID that should be compared to "current"
	 * @return HTML-marked up diff text, as a String.
	 */
	public static String process_diff_current(long rid) throws Exception{		
		api_xml_diff_text handler = new api_xml_diff_text();
		do_parse_work(new URL(url_diff_current(rid)), null, handler);
		return(handler.get_result()); // Return result from parser	
	}
	
	/**
	 * Given a set of PIDs, get the RIDs of the last edit made those pages.
	 * @param pid_set Set of Page-ID's, whose last-edit RID is desired.
	 * @return Map of PID/RID pairs. Where "RID" is the most recent revision
	 * to "PID". RID will be set to -1 if the the page DNE or error occcurs.
	 */
	public static Map<Long,Long> process_latest_page(
			Set<Long> pid_set) throws Exception{
		api_xml_latest_page handler = new api_xml_latest_page();
		do_parse_work(new URL(url_latest_page(pid_set)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given a PID, get the RID of the last edit made on that article.
	 * @param pid Page-ID (article), whose last-edit RID is desired.
	 * @return RID of the last edit made on 'pid', or -1 if one does not
	 * exist, or there was an error in processing.
	 */
	public static long process_latest_page(long pid) throws Exception{
		
			// Note that due to use of <Set> and iterators, this method
			// just be streamlined to a single-RID version, if anything
			// more than casual use is expected.
		
		Set<Long> pid_set = Collections.singleton(pid);
		api_xml_latest_page handler = new api_xml_latest_page();
		do_parse_work(new URL(url_latest_page(pid_set)), null, handler);
		return(handler.get_result().values().iterator().next());
	}
	
	/**
	 * Given a PID, return (1) the edit token necessary to edit that page 
	 * (which is session specific to the user logged-in), as well as (2) the 
	 * timestamp at which the token was obtained (to detect edit conflicts).
	 * @param pid Page-ID (article) on which token should be obtained.
	 * @param session_cookie User-specific list of semicolon separated values
	 * of the form "key=value", which are a cookie on MediaWiki requests an
	 * encode user-login data. Cookies are obtained at log-in. If a user is
	 * editing anonymously, either the empty string or null may be passed.
	 * @return Pair whose first element is the edit token, and whose second
	 * element is the time the token was obtained (in YYYYMMDDHHMMSS format). 
	 */
	public static pair<String,String> process_edit_token(long pid, 
			String session_cookie) throws Exception{
		api_xml_edit_token handler = new api_xml_edit_token();
		do_parse_work(new URL(url_edit_token(pid)), session_cookie, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * This method is overloaded. The parametrization/output is identical
	 * to the methods which share this name, EXCEPT this version expects
	 * the page TITLE rather than an identifying PID.
	 */
	public static pair<String,String> process_edit_token(String page, 
			String session_cookie) throws Exception{
		api_xml_edit_token handler = new api_xml_edit_token();
		do_parse_work(new URL(url_edit_token(page)), session_cookie, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given a page-title, get the current content of that article.
	 * @param title Title of the page with desired content
	 * @return Entire current content of article with title 'title'
	 */
	public static String process_page_content(String title) throws Exception{
		api_xml_page_content handler = new api_xml_page_content();
		do_parse_work(new URL(url_page_content(title)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Retrieve (possibly) past page content, by providing an RID.
	 * @param rid Revision-ID whose changes will be reflect in return.
	 * @return Entire current content of the article article on which
	 * 'rid' resides, immediately after edit 'rid' was committed
	 */
	public static String process_page_content(long rid) throws Exception{
			// Note that handler for current content can be re-used
		api_xml_page_content handler = new api_xml_page_content();
		do_parse_work(new URL(url_page_content(rid)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given a user-name return whether or not that user is currently blocked.
	 * @param user Wikipedia user-name or IP of some user
	 * @param is_ip Pass TRUE is 'user' is an IP address, FALSE otherwise
	 * @return TRUE if 'user' is currently blocked, FALSE, otherwise
	 */
	public static boolean process_block_status(String user, boolean is_ip) 
			throws Exception{
		api_xml_block_status handler = new api_xml_block_status();
		do_parse_work(new URL(url_block_status(user, is_ip)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Determine if an article is "flagged" (under "pending changes" protect).
	 * @param pid Unique page-identifier to article under examination
	 * @return TRUE if the page is under any kind of "flagging" or 
	 * "pending changes" style protection. FALSE; otherwise.
	 */
	public static boolean process_page_flagged(long pid) throws Exception{
		api_xml_page_flagged handler = new api_xml_page_flagged();
		do_parse_work(new URL(url_page_flagged(pid)), null, handler);
		return(handler.get_result());
	}
	
	/**
	 * Determine if an article is under formal page-protection (for editing).
	 * @param pid Unique page-identifier to article under examination
	 * @return TRUE if the page is under any kind of edit protection
	 * (standard-style, not related to pending changes). FALSE; otherwise.
	 */
	public static boolean process_page_protected(long pid) throws Exception{
		api_xml_page_protected handler = new api_xml_page_protected();
		do_parse_work(new URL(url_page_protected(pid)), null, handler);
		return(handler.get_result());
	}
	
	/**
	 * Determine if a user has 'auto-confirmed' status. Note this is an
	 * educated guess that assumes the IP is not a Tor exit-node.
	 * @param user Username about which an inquiry is being made
	 * @return TRUE if 'user' is thought to be 'autoconfirm'. Else, false.
	 * Also return false if the user DNE.
	 */
	public static boolean process_autoconfirmed_status(String user) 
			throws Exception{
		api_xml_autoconfirmed handler = new api_xml_autoconfirmed(user);
		do_parse_work(new URL(url_autoconfirmed(user)), null, handler);
		return(handler.get_result());
	}
	
	/**
	 * Get metadata for the last [n] edits made to an article
	 * @param pid Unique ID which identifies a particular article
	 * @param n Depth-of-search into article history
	 * @param cookie Unique cookie associated with editing user
	 * @param db_geo DB-handler so edit source can be determined
	 * @return List of up to 'n' metadata objects, from most recent to
	 * least recent, from the history of article 'pid'
	 */
	public static List<metadata> process_page_hist_meta(long pid, int n, 
			String cookie, db_geolocation db_geo) throws Exception{
		api_xml_multiple_rids handler = new api_xml_multiple_rids(db_geo);
		do_parse_work(new URL(url_last_n_page_meta(pid, n)), cookie, handler);
		return(handler.get_result());
	}
	
	/**
	 * Get metadata for the next [n] edits made to an article
	 * @param pid Unique ID identifying the article of interest
	 * @param rid RID (on PID) at which to begin enumerating edits (inclusive)
	 * @param n Maximum number of future edits to return
	 * @param cookie Unique cookie associated with editing user
	 * @param db_geo DB-handler so edit source can be determined
	 * @return List of up to 'n' metadata objects, from least recent to 
	 * most recent, beginning at 'rid', from the history of article 'pid'
	 */
	public static List<metadata> process_page_next_meta(long pid, long rid, 
			int n, String cookie, db_geolocation db_geo) throws Exception{
		api_xml_multiple_rids handler = new api_xml_multiple_rids(db_geo);
		do_parse_work(new URL(url_next_n_page_meta(pid, rid, n)), cookie, handler);
		return(handler.get_result());
	}
	
	/**
	 * Get metadata for the last [n] edits made to an article
	 * @param pid Unique ID which identifies a particular article
	 * @param n Depth-of-search into article history
	 * @param cookie Unique cookie associated with editing user
	 * @param client Client DB connection (for geo-location purposes)
	 * @return List of up to 'n' metadata objects, from most recent to
	 * least recent, from the history of article 'pid'
	 */
	public static List<metadata> process_page_hist_meta(long pid, int n, 
			String cookie, client_interface client) throws Exception{
		api_xml_multiple_rids handler = new api_xml_multiple_rids(client);
		do_parse_work(new URL(url_last_n_page_meta(pid, n)), cookie, handler);
		return(handler.get_result());
	}
	
	/**
	 * Given an RID, determine if it is a "badrevid"
	 * @param rid Revision-ID (RID) of the edit of interest
	 * @return TRUE if 'rid' is a "badrevid" per Wikipedia. FALSE, otherwise.
	 */
	public static boolean process_badrevid(long rid) throws Exception{
		api_xml_badrevid handler = new api_xml_badrevid();
		do_parse_work(new URL(url_basic_rid(rid)), null, handler);
			// This could be handled with a slightly more minimal URL
			// generator, but for now, sticking with code-reuse
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Determine if some PID is "missing" (invalid).
	 * @param pid Unique page-identifier to article under examination
	 * @return TRUE if the PID is invalid/missing. FALSE, otherwise.
	 */
	public static boolean process_page_missing(long pid) throws Exception{
		api_xml_page_missing handler = new api_xml_page_missing();
		do_parse_work(new URL(url_page_protected(pid)), null, handler);
			// This could be handled with a slightly more minimal URL
			// generator, but for now, sticking with code-reuse
		return(handler.get_result());
	}
	
	/**
	 * Produce a set of the external links appearing on the
	 * CURRENT VERSION of some page, identified by PID.
	 * @param pid Page-ID of article whose external links are desired
	 * @param offset Begin producing the set after the 'offset'-th link
	 * in the document. To get all links, set offset=0. 
	 * @return Set containing the xlinks currently on page 'pid'
	 */
	public static Set<String> process_xlinks(long pid, int offset) 
			throws Exception{
		api_xml_xlink handler = new api_xml_xlink(pid);
		do_parse_work(new URL(url_xlink(pid, offset)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Produce the block history for some user.
	 * @param user Username/IP for individual of interest
	 * @return A list of all block/unblock actions. The list goes from
	 * newer->older. Elements are pairs, whose first element is the
	 * UNIX timestamp of the action, and the second element is either
	 * "block" or "unblock".
	 */
	public static List<pair<Long,String>> process_block_hist(String user) 
			throws Exception{
		api_xml_block_hist handler = new api_xml_block_hist();
		do_parse_work(new URL(url_block_hist(user)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Produce the times at which some list of pages was last touched/edited
	 * @param pages Set of page titles.
	 * @return A map from page titles to to the last timestamp at which
	 * they were touched. Pages that DNE will not have a map entry.
	 */
	public static Map<String,Long> process_pages_touched(Set<String> pages) 
			throws Exception{
		api_xml_page_touched handler = new api_xml_page_touched();
		do_parse_work(new URL(url_page_touched(pages)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Produce a list of user->RID contributions for MULTIPLE users.
	 * @param users List of users whose contributions are sought.
	 * @param timestamp Timestamp in Mediawiki format, "2007-12-09T19:53:52Z",
	 * so that only contributions NEWER than this will be returned.
	 * @return A list containing pairs. The first element will be an RID.
	 * The second element will be an PID. The output will contain these pairs
	 * for ALL edits made by a user in 'users' after 'timestamp'.
	 */
	public static List<pair<Long,Long>> process_joint_contribs(
			List<String> users, String timestamp) throws Exception{
		api_xml_joint_contribs handler = new api_xml_joint_contribs(users);
		do_parse_work(new URL(url_joint_contribs(users, timestamp)), null, handler);
		return(handler.get_result()); // Return result from parser
	}	
	
	/**
	 * Count the total number of edits for a user, relative to some timestamp. 
	 * Note that this is a extremely time and bandwidth intensive task for 
	 * users with many edits (recursive calls are required). If you just 
	 * want to know the quantity of edits in all history, the simplified
	 * [process_user_edits(user)] is preferred. Note that 0 will be returned
	 * even if the input user does not exist. Note that this works for 
	 * both REGISTERED AND REGISTERED USERS. It seems this version does
	 * not count deleted revisions.
	 * @param user String form of a user-name
	 * @paran ns Namespace in which to count edits. Use Integer.MAX_VALUE
	 * to count contributions across all namepaces
	 * @param ts_prev The timestamp at which to count the total number
	 * of contributions. Do not use this method if "ts_prev = NOW"
	 * @param start_num Quantity at which to start counting (typically =0).
	 * Used primarily for internal recursive alls.
	 * @param break_num If the quantity of edits exceeds this threshold,
	 * break the recursion without exhaustive counting. Note that
	 * Integer.MAX_INT can be passed to disable this setting. Also, the
	 * return value may be greater than 'break_num' (slightly)
	 * @param batch_size Number on [1,500] indicating how many edits to 
	 * fetch at once. Useful when setting low 'break_num'
	 * @return  The number of edits by 'user' at time 'ts_prev' or before,
	 * perhaps as constrained by other provided optionss
	 */
	public static long process_user_edits(String user, int ns, long ts_prev, 
			long start_num, long break_num, int batch_size) throws Exception{
		api_xml_user_edits_date handler = 
				new api_xml_user_edits_date(user, ns, 
						start_num, break_num, batch_size);
		do_parse_work(new URL(url_user_edits_date(
				user, ns, ts_prev, batch_size)), null, handler);
		return(handler.get_result()); // Return result from parser
	} 
	
	/**
	 * Count the total number of edits for a user, in all history, in all 
	 * namespaces. Note this works only for REGISTERED USERS. This version
	 * does count deleted revisions.
	 * @param user String form of a user-name
	 * @return  The number of edits by 'user' (as of "now"). If an invalid
	 * user name is passed, -1 will be returned.
	 */
	public static long process_user_edits(String user) throws Exception{
		api_xml_user_edits_ever handler = new api_xml_user_edits_ever();
		do_parse_work(new URL(url_user_edits_ever(user)), null, handler);
		return(handler.get_result()); // Return result from parser
	} 
	
	/**
	 * Given a list of (possible) pages; return those that not exist.
	 * @param page_list List of pages. List size should be at most 50.
	 * @return A set containing all those page titles from the input list
	 * 'page_list' which do not exist on the wiki.
	 */
	public static Set<String> process_pages_missing(Set<String> page_list) 
			throws Exception{
		api_xml_pages_missing handler = new api_xml_pages_missing();
		do_parse_work(new URL(url_pages_missing(page_list)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Given a category name, produce a set containing all article 
	 * titles which are a member of that category.
	 * @param category Category title; Must have a "Category:" prefix
	 * @param recursive If TRUE then the processor will crawl into all
	 * sub-categories (and their sub-categories...) and add their members
	 * to the return set. If FALSE, then only the top-level members will
	 * be returned (which would be just the names of sub-categories). 
	 * @param cats_traversed (sub)-Categories already visisted; avoid cycles.
	 * This could alternatively be used as a traversal blacklist.
	 * @param continue_key This key exists for pagination purposes. If one 
	 * wishes to start a new query, pass NULL for this value
	 * @return A set containing all elements in 'category', which may or not
	 * traverse sub-categories based on the value of 'recursive'
	 */
	public static Set<String> process_cat_members(String category, 
			boolean recursive, Set<String> cats_traversed, 
			String continue_key) throws Exception{
		api_xml_cat_members handler = 
				new api_xml_cat_members(category, recursive, cats_traversed);
		do_parse_work(new URL(url_cat_members(
				category, continue_key)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Produce the deleted revisions (article deletion; not RevDelete) which 
	 * were made by a particular user. Note that the calling user MUST have
	 * sufficient permissions to make this call!!
	 * @param user User whose deleted revisions are desired
	 * @param do_content If TRUE, revision content will be provided in the output
	 * @param start_time If a previous query exceeded the return limit, 
	 * this field can be used to continue that query from a particular
	 * timestamp. To start a new query, NULL should simply be provided
	 * @param cookie Login cookie; this is validated to show that the user
	 * has enough permissions to make this protected call
	 * @return A list of deleted revisions by 'user'. The list elements are 
	 * pairs, whose first element is the revision metadata object, and the 
	 * second element is revision content in String form (if content=false, 
	 * then the second element will be NULL). No guarantees are made 
	 * regarding list order.
	 */
	public static List<pair<metadata, String>> process_deleted_revs(String user, 
			boolean do_content, String start_time, String cookie) 
			throws Exception{
		api_xml_deleted_revs handler = new api_xml_deleted_revs(
				user, do_content, cookie);
		do_parse_work(new URL(url_deleted_revs(
				user, do_content, start_time)), cookie, handler);
		return(handler.get_result()); // Return result from parser
	}
	 
	/**
	 * Given a page, return all categories which that page is a member of.
	 * @param page Title of some Wikipedia page
	 * @param hidden Whether we are displaying "hidden" categories or
	 * "not hidden" ones. These are mutually exclusive; to get "all"
	 * categories one would need to query twice with both values.
	 * @param continue_key This key exists for pagination purposes. If one 
	 * wishes to start a new query, pass NULL for this value.
	 * @return Set containing all categories of which 'page' is a member.
	 */
	public static Set<String> process_page_cats(String page, boolean hidden,
			String continue_key) throws Exception{
		api_xml_page_cats handler = 
				new api_xml_page_cats(page, hidden);
		do_parse_work(new URL(url_page_cats(
				page, hidden, continue_key)), null, handler);
		return(handler.get_result()); // Return result from parser
	}
	
	/**
	 * Return the size of a page in bytes, per a particular timestamp
	 * @param page Page title
	 * @param time Timestamp of the form "2014-02-01T09:45:36Z"
	 * @param title_encoded Whether or not 'title' is already encoded for API
	 * @return The size in bytes of 'page' at time 'time'
	 */
	public static int process_size_at_time(String page, String time, 
			boolean title_encoded) throws Exception{
		
		api_xml_size_time handler = new api_xml_size_time();
		do_parse_work(new URL(url_size_time(
				page, time, title_encoded)), null, handler);
		return(handler.get_result()); // Return result from parser	
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Handle the XML parsing of a document, given a URL and XML-handler.
	 * @param url URL of the MediaWiki XML that needs to be parsed
	 * @param cookie String of semicolon-separated pairs of the form 
	 * "key=value", which are cookie elements sent with the HTTP request.
	 * If the request doesn't require these session variables, or if the
	 * user is anonymous, either null or the empty string may be passed.
	 * @param dh XML handler for data contained in 'url'
	 */
	private static void do_parse_work(URL url, String cookie, 
			DefaultHandler handler) throws Exception{
	
			// Get connection, insert cookie header data (if applicable)
		URLConnection conn = url.openConnection();
		if((cookie != null) && (!cookie.equals("")))
			conn.setRequestProperty("Cookie", cookie);

			// Open up InputStream to URL, method handles retries
		InputStream in = stream_from_url(conn, api_retrieve.NUM_HTTP_RETRIES);
		if(in == null) // Most common STiki error, connection failure
			throw new Exception("Exception thrown to terminate thread");
		
			// Debugging code to see actual XML:
		//URLConnection conn2 = url.openConnection();
		//if((cookie != null) && (!cookie.equals("")))
		//	conn2.setRequestProperty("Cookie", cookie);
		//InputStream in2 = stream_from_url(conn2, api_retrieve.NUM_HTTP_RETRIES);
		//System.out.println(wiki_utils.capture_stream(in2));
		
			// Having an InputStream, parse the XML it contains
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		parser.parse(in, handler); // Parse
		in.close(); // Close up the URL-connection
	}
	
	/**
	 * Retrieve an InputStream for a URL, possibly requiring retries.
	 * @param con Connection object which has been 'opened' to a URL
	 * @param retries If stream cannot be established, the number of times to
	 * retry before accepting the data will not be received.
	 * @return InputStream over data in 'con', or NULL if a connection could
	 * not be established after 'retries' attempts at connecting.
	 */
	private static InputStream stream_from_url(URLConnection con, 
			int retries) throws InterruptedException{
		
		try{InputStream is = con.getInputStream();
			return(is);
		} catch(Exception e){
			Thread.sleep(50); // Slight pause, server might correct issue
			if(retries == 0){
				System.out.println("Error: HTTP error at URL: " + 
						con.getURL().toExternalForm());
				e.printStackTrace();
				return null;
			} else{return(stream_from_url(con, retries-1));}
		} // Retry attempts work recursively until base-case
	}
	
	
	// *** URL CREATORS FOR REQUESTS ***
	
	/**
	 * Given a RID, produce the API-URL which will output metadata for edit.
	 * @param rid Revision-ID (RID) of the edit of interest
	 * @param MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_basic_rid(long rid){ 
		String url = base_url() + "&prop=revisions&revids=" + rid; 
		url += "&rvtoken=rollback"; 
		url += "&rvprop=ids|timestamp|user|comment|tags&format=xml";
		return (url);
	}
	
	/**
	 * Given a list of RIDs, produce the URL to get their metadata.
	 * @param rid_list List of RIDs whose metadata should be obtained. The
	 * MediaWiki API may impose limits on how many can be queried at once.
	 * @param MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_multiple_rids(List<Long> rid_list){
		String url = base_url() + "&prop=revisions&revids=";
		Iterator<Long> iter = rid_list.iterator();
		while(iter.hasNext()) // Just append each RID to the URL
			url += iter.next() + "|";
		url = url.substring(0, url.length()-1); // Trim off trailing bar
		url += "&rvtoken=rollback";
		url += "&rvprop=ids|timestamp|user|comment|tags&format=xml";
		return (url);
	}
	
	/**
	 * Produce the API-URL necessary for a rollback offender search.
	 * @param pid Page on which the rollback edit took place
	 * @param flag_rid RID of the flagging edit which initiated this off-search
	 * @param search_depth Number of edits to search in 'pid' history
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_offender_search(long pid, long flag_rid, 
			int search_depth){
		String url = base_url() + "&prop=revisions&pageids=" + pid;
		url += "&rvstartid=" + flag_rid;
		url += "&rvlimit=" + (search_depth+1);
		url += "&rvprop=ids|user&rvdir=older&format=xml";
		return (url);
	}

	/**
	 * Produce the API-URL necessary to determine user permissions.
	 * @param uc_user User whose rights are being examined
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_user_perm(String user) throws Exception{
		String url = base_url() + "&list=users&ususers=";
		url += URLEncoder.encode(user, "UTF-8");
		url += "&usprop=groups&format=xml";
		return (url);
	}
	
	/**
	 * Produce the API-URL to determine the timestamp of a user's first edit.
	 * @param user User whose first edit time is desired
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_user_first_edit(String user) throws Exception{
		String url = base_url() + "&list=usercontribs&ucuser=";
		url += URLEncoder.encode(user, "UTF-8") + "&uclimit=1&ucdir=newer&";
		url += "ucnamespace=0&ucprop=timestamp&format=xml";
		return(url);
	}
	
	/**
	 * Produce the API-URL to determine the time a page was last edited 
	 * (prior to some RID, which is provided).
	 * @param pid Page-ID of the page whose prior edit time-stamp is desired
	 * @param rid RID s.t. we are are looking for the edit prior to 'rid'
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_prior_page_edit(long pid, long rid){
		String url = base_url() + "&prop=revisions&pageids=" + pid; 
		url += "&rvstartid=" + rid;
		url += "&rvlimit=2&rvprop=timestamp&rvdir=older&format=xml";
		return(url);
	}
	
	/**
	 * Produce the API-URL to determine the time a page was NEXT edited 
	 * (relative to some RID, which is provided).
	 * @param pid Page-ID of the page whose next edit time-stamp is desired
	 * @param rid RID s.t. we are are looking for the edit after 'rid'
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_newer_page_edit(long pid, long rid){
		String url = base_url() + "&prop=revisions&pageids=" + pid; 
		url += "&rvstartid=" + rid;
		url += "&rvlimit=2&rvprop=timestamp&rvdir=newer&format=xml";
		return(url);
	}
	
	/**
	 * Return the size-change in bytes which was initiated by some RID.
	 * @param pid Page-ID on which edit 'rid' resides
	 * @param rid Revision whose change-size is desired
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_size_change(long pid, long rid){
		String url = base_url() + "&prop=revisions&pageids=" + pid;
		url += "&rvstartid=" + rid;
		url += "&rvlimit=2&rvprop=size&rvdir=older&format=xml";
		return(url);
	}

	/**
	 * Produce the URL to return the diff between an edit and that previous.
	 * @param rid Revision-ID that should be compared against previous edit
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_diff_prev(long rid){
		String url = base_url() + "&prop=";
		url += "revisions&revids=" + rid + "&rvdiffto=prev&format=xml";
		return(url);
	}	
	
	/**
	 * Produce the URL to return the diff between an edit and the current 
	 * on that same page/article..
	 * @param rid Revision-ID to be diffed against "current"
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_diff_current(long rid){
		String url = base_url() + "&prop=";
		url += "revisions&revids=" + rid + "&rvdiffto=cur&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL to get the RID of the most recent edit on some page(s).
	 * @param pid_set Page-IDs (articles), whose most recent edit is desired.
	 * At most 500 PIDs should be contained in the list.
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_latest_page(Set<Long> pid_set){
		String url = base_url() + "&prop=info&pageids=";
		Iterator<Long> iter = pid_set.iterator();
		while(iter.hasNext())
			url += iter.next() + "|";
		url = url.substring(0, url.length()-1); // Clip trailing "|"
		url += "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL necessary to obtain an edit token on some page 
	 * @param pid Page-ID (article), for which the edit token is desired
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_edit_token(long pid){
		String url = base_url();
		url += "&prop=info&pageids=" + pid + "&intoken=edit&format=xml";
		return(url);	
	}
	
	/**
	 * Produce the URL necessary to obtain an edit token on some page 
	 * @param Page Page name for which edit token should be obtained
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_edit_token(String page) throws Exception{
		String url = base_url() + "&prop=info&titles=";
		url += URLEncoder.encode(page, "UTF-8") + "&intoken=edit&format=xml";
		return(url);	
	}
	
	/**
	 * Produce the URL necessary to obtain an article's (current) content.
	 * @param title Name of the article whose content is desired
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_page_content(String title) throws Exception{
		String url = base_url() + "&prop=revisions&titles=";
		url += URLEncoder.encode(title, "UTF-8") + "&rvlimit=1";
		url += "&rvprop=content&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL necessary to obtain an article's content.
	 * @param rid Revision ID, which will be reflected in content
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_page_content(long rid){
		String url = base_url() + "&prop=";
		url += "revisions&rvprop=content&revids=" + rid + "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL necessary to obtain the block-status of some user.
	 * @param user Username or IP address of user to investigate
	 * @param is_ip TRUE if the 'user' field is an IP; FALSE, otherwise
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_block_status(String user, boolean is_ip) 
			throws Exception{
			
			// Format changes slightly depending on user-type being queried
		String url = base_url() + "&list=blocks&";
		if(is_ip) url += "bkip=" + URLEncoder.encode(user, "UTF-8") + "&";
		else url += "bkusers=" + URLEncoder.encode(user, "UTF-8") + "&";
		url += "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL necessary to obtain the flag-status of some page.
	 * @param pid Page-identified of the article under examination
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_page_flagged(long pid){
		String url = base_url();
		url += "&prop=flagged&pageids=10000" + pid + "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL necessary to obtain the protect-status of some page.
	 * @param pid Page-identified of the article under examination
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_page_protected(long pid){
		String url = base_url();
		url += "&prop=info&inprop=protection&pageids=" + pid + "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL necessary to obtain the autoconfirm status of a user.
	 * @param user Username about which query is being made
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_autoconfirmed(String user) throws Exception{
		String url = base_url();
		url += "&list=allusers&auprop=editcount|registration&aufrom=";
		url += URLEncoder.encode(user, "UTF-8") + "&aulimit=1&format=xml";
		return(url);
	}
	
	/**
	 * Prooduce the URL necessary to obtain metadata for
	 * the last [n] edits to some particular page.
	 * @param pid Page-identifier of article being queried
	 * @param n Number of edits in the past to retrieve
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_last_n_page_meta(long pid, int n){
		String url = base_url() + "&prop=revisions&pageids=" + pid; 
		url += "&rvlimit=" + n;
		url += "&rvtoken=rollback"; 
		url += "&rvprop=ids|timestamp|user|comment|tags&rvdir=older&format=xml";
		return(url);
	}
	
	/**
	 * Prooduce the URL necessary to obtain metadata for the next [n] edits 
	 * to some particular page, from a particular RID
	 * @param pid Page-identifier of article of interest
	 * @param rid RID at which to start forward enumeration (will be included)
	 * @param n Number of edits in the future to retrieve (if available)
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_next_n_page_meta(long pid, long rid, int n){
		String url = base_url() + "&prop=revisions&pageids=" + pid; 
		url += "&rvlimit=" + n;
		url += "&rvstartid=" + rid;
		url += "&rvtoken=rollback"; // Not pertinent, but allows parser re-use 
		url += "&rvprop=ids|timestamp|user|comment|tags&rvdir=newer&format=xml";
		return(url);		
	}
	
	/**
	 * Produce the URL necessary to obtain a list of external links on a page.
	 * @param pid Page-ID whose external links are of interest
	 * @param offset Only list links after the point in the document where
	 * the "#offset"-th link appears.
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_xlink(long pid, long offset) throws Exception{
		String url = base_url() + "&prop=extlinks&pageids=" + pid;
		url += "&ellimit=500&eloffset=" + offset + "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL necessary to obtain block history for some user
	 * @param username/IP of the individual of interest
	 * @returnu URL to obtain, containing relevant data
	 */
	private static String url_block_hist(String user) throws Exception{
		String url = base_url() + "&list=logevents&letype=block&letitle=User";
		url += URLEncoder.encode(":" + user, "UTF-8");
		url += "&lelimit=500&ledir=older&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL to obtain the timestamps page(s) were last touched.
	 * @param users Set of pages. Max size == 50.
	 * @return URL to obtain, containing relevant data
	 */
	private static String url_page_touched(Set<String> pages) 
			throws Exception{
		Iterator<String> iter = pages.iterator();
		String url = base_url() + "&prop=info&titles=";
		while(iter.hasNext()) // Just append each page to the URL
			url += URLEncoder.encode(iter.next(), "UTF-8") + "|";
		url = url.substring(0, url.length()-1); // Trim off trailing bar
		url += "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL to obtain the contributions of multiple users
	 * @param users List of users whose contributions will be returned.
	 * Size limit of this is unknown, but should probably be < 50.
	 * @param timestamp Timestamp in Mediawiki format, "2007-12-09T19:53:52Z",
	 * so that only contributions NEWER than this will be returned.
	 * @return URL to obtain, containing relevant data
	 */
	private static String url_joint_contribs(List<String> users, 
			String timestamp) throws Exception{
		Iterator<String> iter = users.iterator();
		String url = base_url() + "&list=usercontribs&ucuser=";
		while(iter.hasNext()) // Just append each RID to the URL
			url += URLEncoder.encode(iter.next(), "UTF-8") + "|";
		url = url.substring(0, url.length()-1); // Trim off trailing bar
		url += "&uclimit=500&ucstart=" + timestamp;
		url += "&ucprop=ids&ucdir=newer&format=xml";
		return(url);	
	}

	/**
	 * Produce the URL to count the number of edits for some account, 
	 * relative to a provided timestamp.
	 * @param user String form of a user-name
	 * @param ts_before The timestamp at which to count the total number
	 * of contributions. To count *all* contributions in history, just
	 * entire the UNIX timestamp of "now" or some future time.
	 * @param batch_size Integer on [1,500] to set batch size
	 * @return URL to obtain, containing relevant data.
	 */
	private static String url_user_edits_date(String user, int ns, 
			long ts_before, int batch_size) throws Exception{
		
		String url = base_url() + "&list=usercontribs";
		url += "&ucuser=" + URLEncoder.encode(user, "UTF-8");
		url += "&ucprop=timestamp";
		url += "&ucdir=older";
		url += "&uclimit=" + batch_size;
		url += "&ucstart=" + stiki_utils.unix_ts_to_wiki(ts_before);
		if(ns != Integer.MAX_VALUE)
			url += "&ucnamespace=" + ns;
		url += "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL to count the number of edits for some account.
	 * @param user String form of a user-name
	 * @return URL to obtain, containing relevant data.
	 */
	private static String url_user_edits_ever(String user) throws Exception{
		String url = base_url() + "&list=users";
		url += "&ususers=" + URLEncoder.encode(user, "UTF-8");
		url += "&usprop=editcount";
		url += "&format=xml";
		return(url);
	}
	
	/**
	 * Produce a URL to determine whether some set of pages exists or not.
	 * @param page_list List of (possible) page titles. Length 50 at maximum.
	 * @return URL to obtain, containing relevant data
	 */
	private static String url_pages_missing(Set<String> page_list) 
			throws Exception{
		StringBuilder url = new StringBuilder();
		url.append(base_url() + "&titles=");
		Iterator<String> page_iter = page_list.iterator();
		while(page_iter.hasNext())
			url.append(URLEncoder.encode(page_iter.next(), "UTF-8") + "|");
		return(url.substring(0, url.length()-1) + "&format=xml");
	}
	
	/**
	 * Produce the URL to produce the article members of some category.
	 * @param category Category whose members to produce; 
	 * must prefix with "Category:"
	 * @param continue_key This key exists for pagination purposes. If one 
	 * wishes to start a new query, pass NULL for this value
	 * @return URL to obtain, containing relevant data.
	 */
	private static String url_cat_members(String category, 
			String continue_key) throws Exception{
		
		String url = base_url() + "&list=categorymembers";
		url += "&cmtitle=" + URLEncoder.encode(category,"UTF-8");
		if(continue_key != null)
			url += "&cmcontinue=" + continue_key;
		url += "&cmlimit=500";
		url += "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the API-URL to list out deleted revisions for a user.
	 * @param user User whose deleted revisions are desired 
	 * @param do_content Whether or not content should be included in output
	 * @param start_time Key for cotinuing queries; NULL if a new query
	 * @return MediaWiki URL to obtain, containing relevant data
	 */
	private static String url_deleted_revs(String user, boolean do_content, 
			String start_time) throws Exception{
		String url = base_url() + "&list=deletedrevs";
		url += "&druser=" + URLEncoder.encode(user, "UTF-8");
		url += "&drprop=comment|revid";
		if(do_content)
			url += "|content";
		if(start_time != null)
			url+= "&drstart=" + start_time;
		url += "&drlimit=1&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL to produce the category memberships of an article.
	 * @param page Wikipedia page whose category memberships are desired.
	 * @param hidden Whether we are displaying "hidden" categories or
	 * "not hidden" ones. These are mutually exclusive; to get "all"
	 * categories one would need to query twice with both values.
	 * @param continue_key This key exists for pagination purposes. If one 
	 * wishes to start a new query, pass NULL for this value
	 * @return URL to obtain, containing relevant data.
	 */
	private static String url_page_cats(String page, boolean hidden, 
			String continue_key) throws Exception{
		
		String url = base_url();
		url += "&prop=categories";
		url += "&titles=" + URLEncoder.encode(page,"UTF-8");
		if(hidden)
			url += "&clshow=hidden";
		else url += "&clshow=!hidden";
		if(continue_key != null)
			url += "&clcontinue=" + continue_key;
		url += "&cllimit=500";
		url += "&format=xml";
		return(url);
	}
	
	/**
	 * Produce the URL to obtain the size of a page at a given timestamp.
	 * @param page Page title
	 * @param time Timestamp of the form ""2014-02-01T09:45:36Z"
	 * @param title_encoded Whether or not the title has already been encoded
	 * @return URL to obtain, containing relevant data.
	 */
	private static String url_size_time(String page, String time, 
			boolean title_encoded) throws Exception{
		
		String url = base_url();
		url += "&prop=revisions";
		
		if(title_encoded)
			url += "&titles=" + page;
		else url += URLEncoder.encode(page,"UTF-8");
		
		url += "&rvstart=" + time;
		url += "&rvdir=older";
		url += "&rvlimit=1";
		url += "&rvprop=size";
		url += "&format=xml";
		return(url);	
	}
		
}
