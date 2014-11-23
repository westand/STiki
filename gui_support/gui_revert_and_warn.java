package gui_support;

import executables.stiki_frontend_driver.FB_TYPE;
import gui_edit_queue.gui_display_pkg;
import gui_panels.gui_revert_panel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JOptionPane;

import mediawiki_api.api_post;
import mediawiki_api.api_post.EDIT_OUTCOME;
import mediawiki_api.api_retrieve;

import core_objects.metadata;
import core_objects.stiki_utils;
import core_objects.stiki_utils.QUEUE_TYPE;

/**
 * Andrew G. West - gui_revert_and_warn.java - The class handles the
 * reversion of bad edits, and the warning of the offending users.
 * 
 * Crucially, the actions of the class are threaded. POST edit actions on 
 * WikiMedia are painfully (perhaps, intentionally), slow. In its own thread, 
 * the revert can be latent, without affecting the GUI progression onto the 
 * next RID for review. Further, this class examines if the revert was 
 * successfully made, and if so, is able to warn the offending user.
 * 
 * The warning of users is non-trivial. In particular, warning-messages 
 * escalate if a user is caught multiple times. Thus, one must check for
 * previous warnings before issuance.
 */
public class gui_revert_and_warn implements Runnable{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Descriptions of why a warning did/did not take place. If the edit did
	 * take place, then the user-warning provided is encoded.
	 */
	public enum WARNING{NO_EDIT_TOO_OLD, NO_USER_BLOCK, NO_BEATEN, 
		NO_ERROR, NO_OPT_OUT, NO_AIV_TIMING, NO_CUSTOM_MSG, YES_UW1, 
		YES_UW2, YES_UW3, YES_UW4, YES_AIV, YES_AIV_4IM, };
		
	/**
	 * Description of the type of undo/revert/rollback performed.
	 */
	public enum RV_STYLE{NOGO_RB, NOGO_SW, SIMPLE_UNDO, RB_NATIVE_ONE, 
		RB_NATIVE_MANY, RB_SW_ONE, RB_SW_MULTIPLE}
	
	/**
	 * Page where extreme vandals are reported, so admins may block them.
	 */
	public final String AIV_PAGE = "Wikipedia:Administrator_" +
			"intervention_against_vandalism";
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * The type of feedback being left. At this stage it will be "guilty"
	 * or "AGF". This is necessary given different revert styles. e.g.,
	 * "AGF" rollbacks must be labeled as minor, so they need to proceed
	 * with software rollback rather than the native version.
	 */
	private FB_TYPE fb_type;
	
	/**
	 * Wrapper for the edit which is about to be reverted. 
	 */
	private gui_display_pkg edit_pkg;
	
	/**
	 * Metadata object which contains information about edit being reverted
	 */
	private metadata metadata;
	
	/**
	 * Revision which should be left with the reversion-action. 
	 */
	private String revert_comment;
	
	/**
	 * Cookie headers which should be included when making the POST.
	 */
	private String cookie;
		
	/**
	 * Whether or not the editing user has the native rollback permission.
	 */
	private boolean user_has_native_rb;
	
	/**
	 * Whether or not watchlisting should be explicitly prevented.
	 */
	private boolean no_watchlist;
	
	/**
	 * If the revert succceeds, whether the offending user should be warned.
	 */
	private boolean warn;
	
	/**
	 * Message to be posted to the offending user's talk page. Can be
	 * used as a softer alternative to automated warning mechanism.
	 */
	private String usr_talk_msg;
	
	/**
	 * GUI panel displaying revert/warning result to end-users.
	 */
	private gui_revert_panel gui_revert_panel;
	
	/**
	 * Type of queue which produced reversion; maps to warning type.
	 */
	private QUEUE_TYPE queue_type;
	
	
	// ********** CONSTANTS ***********

	/**
	 * STiki may present a user's vandalism which occured long in the past.
	 * However, especially with IP edits, it may be inappropriate to warn
	 * someone for actions they took long ago. This var. sets the warn-window.
	 */
	private final long WARN_WINDOW_SECS = (60 * 60 * 24);
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Handle the reversion (and possibly warning) for an instance of vandalism.
	 * @param fb_type Type of reversion being performed (guilty vs. AGF)
	 * @param edit_pkg Wrapper around RID(s) to be reverted
	 * @param revert_comment Edit comment to be left with reversion action
	 * @param cookie Cookie headers to include when POST-ing (user-mapping)
	 * @param user_has_native_rb If editing user has native rollback permission
	 * @param rollback Whether or not rollback is being used to revert
	 * @param no_watchlist Should watchlisting should be explicitly prevented?
	 * @param warn If offending-user should be warned on revert-success
	 * @param usr_talk_msg Custom message to be posted to offending user's
	 * talk page. Presumably should not be used in conjuction with 
	 * 'warn = true'. NULL may be passed to leave no message.
	 * @param gui_revert_panel GUI object displaying revert/warning result
	 */
	public gui_revert_and_warn(FB_TYPE fb_type, gui_display_pkg edit_pkg, 
			String revert_comment, String cookie, boolean user_has_native_rb, 
			boolean rollback, boolean no_watchlist, boolean warn, String
			usr_talk_msg, gui_revert_panel gui_revert_panel){
		
		this.fb_type = fb_type;
		this.edit_pkg = edit_pkg;
		this.metadata = edit_pkg.page_hist.get(0); // convenience
		this.revert_comment = revert_comment;
		this.cookie = cookie;
		this.user_has_native_rb = user_has_native_rb;
		this.no_watchlist = no_watchlist;
		this.warn = warn;
		this.usr_talk_msg = usr_talk_msg;
		this.gui_revert_panel = gui_revert_panel;
		this.queue_type = stiki_utils.queue_to_type(edit_pkg.source_queue);
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Make the POST request. If the edit commit succeeds, then
	 * potentially warn the user who made the offending edit. Nothing complex
	 * here, this class just exists to wrap this into its own thread.
	 * 
	 * This method, on-or-before completion, must update the GUI panel.
	 */
	public void run(){
		try{
			RV_STYLE revert_type;
			EDIT_OUTCOME revert_outcome;
			if(user_has_native_rb && fb_type.equals(FB_TYPE.GUILTY)){
				
					// Only "guilty" edits should make use of native RB
				InputStream in = api_post.edit_rollback(metadata.title, 
						metadata.user, revert_comment,
						metadata.rb_token, cookie, no_watchlist, true);
				long earliest_rb_undone = api_post.rollback_response(in);
				if(earliest_rb_undone < 0){
					if(earliest_rb_undone == -2){
						revert_outcome = EDIT_OUTCOME.ERROR;
						bad_rbtoken_handler();
					} else if(earliest_rb_undone == -3){
						revert_outcome = EDIT_OUTCOME.ASSERT_FAIL;
						assert_fail_dialog();
					} else revert_outcome = EDIT_OUTCOME.ERROR;
				} else if(earliest_rb_undone == 0)
					revert_outcome = EDIT_OUTCOME.BEATEN;
				else // anything else (it would be an RID)
					revert_outcome = EDIT_OUTCOME.SUCCESS;
				
				if(earliest_rb_undone > 0){
					if(this.edit_pkg.rb_depth == 1)
						revert_type = RV_STYLE.RB_NATIVE_ONE;
					else revert_type = RV_STYLE.RB_NATIVE_MANY;
				} else revert_type = RV_STYLE.NOGO_RB;
				in.close();
			
			} else{
					// All AGF cases fall under this case;
					// rollback can't be used due to "minor" settings
					// And "guilty" reverts where user w/o native rollback
				boolean minor = false;
				if(fb_type.equals(FB_TYPE.GUILTY))
					minor = true;
				
				int sw_rb_code = gui_soft_rollback.software_rollback(
						edit_pkg, revert_comment, minor, cookie, 
						no_watchlist, true);
				
				if(sw_rb_code == -2){
					revert_outcome = EDIT_OUTCOME.ASSERT_FAIL;
					assert_fail_dialog();
				} else if(sw_rb_code == -1)
					revert_outcome = EDIT_OUTCOME.ERROR;
				else if(sw_rb_code == 0)
					revert_outcome = EDIT_OUTCOME.BEATEN;
				else // anything else (it would be an RID)
					revert_outcome = EDIT_OUTCOME.SUCCESS;
				
				if(sw_rb_code > 0){
					if(sw_rb_code == 1)
						revert_type = RV_STYLE.RB_SW_ONE;
					else // if(sw_rb_code > 1)
						revert_type = RV_STYLE.RB_SW_MULTIPLE;
				} else revert_type = RV_STYLE.NOGO_SW;
			
			} /*else{	// Basic "undo" revert (no rollback)
				InputStream in = api_post.edit_revert(metadata.rid, 
					metadata.title, revert_comment, minor, 
					edit_pkg.get_token(), cookie, no_watchlist); 
				revert_outcome = api_post.edit_was_made(in);
				if(revert_outcome.equals(EDIT_OUTCOME.SUCCESS))
					revert_type = RV_STYLE.SIMPLE_UNDO;
				else revert_type = RV_STYLE.NOGO_SW;
				in.close();
			} // Decide between undo paths */
			
				// If reversion successful, warn user if requested
				// No matter the case, set final outcome variable
			WARNING revert_warn_outcome;
			if(warn && revert_outcome.equals(EDIT_OUTCOME.SUCCESS))
				revert_warn_outcome = warn();
			else if(revert_outcome.equals(EDIT_OUTCOME.SUCCESS))
				revert_warn_outcome = WARNING.NO_OPT_OUT;
			else if(revert_outcome.equals(EDIT_OUTCOME.BEATEN))
				revert_warn_outcome = WARNING.NO_BEATEN;
			else //if(revert_outcome.equals(EDIT_OUTCOME.ERROR))
				revert_warn_outcome = WARNING.NO_ERROR;
			
			if(revert_outcome.equals(EDIT_OUTCOME.SUCCESS) 
					&& this.usr_talk_msg != null 
					&& !this.usr_talk_msg.equals("")){
				user_talk_append(
						this.usr_talk_msg, 
						agf_comment(this.queue_type));
				revert_warn_outcome = WARNING.NO_CUSTOM_MSG;		
			} // There is also the option to pass a custom message to
			  // the offending users talk page. Code logic permits this
			  // only in the "AGF" case when warning is not enabled, so
			  // the interaction between the two is simplified. Only do 
			  // this if revert actually succeeds. 
		
				// Use the OUTCOME data to update user-facing GUI
			gui_revert_panel.update_display(this.edit_pkg,
					revert_type, revert_warn_outcome);
		} 
		catch (Exception e){
			System.out.println("Threaded-POST attempt (reversion) failed:");
			e.printStackTrace();
		} // Simple method call, inward response stream is discarded
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Warn the offending-user. This is a complex and unfortunately-fragile
	 * process. Summarily, we go look at the UserTalk page of the offending-
	 * user to determine if there are previous warnings. We then increment
	 * the severity one-notch and issue the warning (levels 1--4). If a user
	 * has exceeded warnings, a request for admin-intervention is made.
	 * Much of this analysis depends on well-formatted pages, and regular
	 * expressions which might be changed by Wikipedia administravia.
	 * @return Constant defining the outcome of the warning.
	 */
	private WARNING warn() throws Exception{
		
			// If old vandalism BY AN IP, do not warn_offender
			// Always warn for edits made by registered users
		long time_elapsed = (stiki_utils.cur_unix_time() - metadata.timestamp);
		if(time_elapsed > WARN_WINDOW_SECS && metadata.user_is_ip)
			return(WARNING.NO_EDIT_TOO_OLD); 
		
			// If blocked already, do not warn offender
		if(api_retrieve.process_block_status(
				metadata.user, metadata.user_is_ip))
			return(WARNING.NO_USER_BLOCK);
		
		String sec_content = "";
		String talk_content = "";
		String date_header = cur_utc_month_year();
		String talk_page = "User talk:" + metadata.user;
		if(edit_pkg.user_has_talkpage){ 
			talk_content = api_retrieve.process_page_content(talk_page);
			if(talk_content.startsWith("#REDIRECT ")){
				talk_page = stiki_utils.first_match_within("\\[\\[.*?\\]\\]", 
						talk_content).replace("[[", "").replace("]]", "");
				talk_content = api_retrieve.process_page_content(talk_page);
			} // Accomodate possibility of user-page redirect
			  // No plans to accomodate redirects of nested depth.
			sec_content = get_section_content(talk_content, date_header);
		} // Go to offending-user's Talk page, get month's content
		  // We know from package building whether usertalk exists

			// We now know a warning will likely be placed; need edit token
			// Can re-use one from the main article if we have it(?)
		if(edit_pkg.get_token() == null) 
			edit_pkg.refresh_edit_token(this.cookie);
		
			// Determine what vandalism warnings were issued in the current
			// month -- we plan to issue the next most severe
			// This is all done type specific (vandalism, spam, etc.)
		int highest_level = highest_warn_level(queue_type, sec_content);
		int warning_level = (highest_level+1);
		boolean imm_non_van_warn = im_warn_present(queue_type, sec_content);		
	
			// OKAY: TIME TO ACTUALLY PLACE THE WARNING/AIV-POST:
		if(imm_non_van_warn || warning_level == 5){
			
				// Do not AIV if offending edit prior to last warning
			if(metadata.timestamp < last_message_ts(sec_content))
				return(WARNING.NO_AIV_TIMING);
			
			String aiv_msg = aiv_text(queue_type, metadata.user, metadata.rid,
					!metadata.user_is_ip, imm_non_van_warn);
			api_post.edit_append_text(AIV_PAGE, aiv_comment(queue_type, 
					metadata.user, metadata.rid, !metadata.user_is_ip), aiv_msg, 
					false, this.edit_pkg.get_token(), this.cookie, true, 
					no_watchlist, true);
			
			if(imm_non_van_warn)
				return(WARNING.YES_AIV_4IM);
			else return(WARNING.YES_AIV);
		} // If warning level 5 vandalism, or non-vandal "4im" previous -> AIV
		
		else{ // if(warning_level <= 4){
			
			String warning = warning_template(queue_type, 
					warning_level, metadata.title, metadata.user_is_ip);
			if(sec_content.equals("")){
				warning = "\r== " + date_header + " ==\r" + warning;
			} else{ // If header doesn't exist, need to make one
				warning = "\r\r" + warning + "\r\r";
			} // Just a nice little space buffer if section exists
			
				// We assume current month/year is last on page, so it
				// is always safe to append content.
			api_post.edit_append_text(talk_page, warning_comment(queue_type), 
					warning, false, this.edit_pkg.get_token(), this.cookie, 
					true, no_watchlist, true);
			
				// Output which warning level was issued
			if (warning_level == 1) return(WARNING.YES_UW1);
			else if (warning_level == 2) return(WARNING.YES_UW2);
			else if (warning_level == 3) return(WARNING.YES_UW3);
			else return (WARNING.YES_UW4);
			
		} // Warning levels < 5 get issued a UserTalk warning
	}
	
	/**
	 * Produce the warning message (template) that should be appended to the
	 * UserTalk of offending users -- by providing the template fields
	 * @param warn_type Type of warning to place (vandalism, spam, etc.)
	 * @param level Number [1--4], indicating warning severity
	 * @param page_title Title of the page which was vandalized
	 * @param is_ip Whether or not the user being warned is an IP address.
	 * @return String containing warning, which should be put on UserTalk.
	 */
	private static String warning_template(QUEUE_TYPE warn_type, int level, 
			String page_title, boolean is_ip){
		
		// Below was original (asked to remove by User):
		// String template_note = "This particular incident was located with "
		// "the assistance of [[Wikipedia:STiki|STiki]]. Thank you!";
		
		String template_note = "";
		String template = "";
		if(warn_type.equals(QUEUE_TYPE.VANDALISM))
			template += "{{subst:uw-vandalism";
		else template += "{{subst:uw-spam";
		
		template += level + 
				"|" + page_title + 
				"|" + template_note + 
				"|" + "subst=subst:}}  ~~~~"; // Signature
		
		if(is_ip) // IP address users get a note about DHCP
			template += "\n{{subst:Shared IP advice}}";
		return(template);
	}
	
	/**
	 * Produce a notification/warning of the AIV (Administrator Intervention
	 * against Vandalism). After enough warnings, we ignore the user, and
	 * append text of the form below to a special page, so admin's can block.
	 * @param type Type of behavior that led to block request (vandalism, etc.)
	 * @param user Username or IP address of the offending editor
	 * @param rid RID of the incident producing this request
	 * @param registered TRUE if offender is registered; FALSE, otherwise
	 * @param unrel_imm TRUE if the last warning a user received was a 4im
	 * unrelated to cause. FALSE if a more typical procession.
	 * @return Text which should be appended to the AIV notice-board
	 */
	private static String aiv_text(QUEUE_TYPE type, String user, long rid, 
			boolean registered, boolean unrel_imm){
		
		String aiv_text = "";
		if(!unrel_imm){
			if(registered){
				aiv_text = "\r* {{Vandal|" + user + "}} ### " +
					"({{diff2|" + rid + "|diff}}) " +
					"after recently receiving last warning ~~~~ \r"; 
			} else{
				aiv_text = "\r* {{IPvandal|" + user + "}} ### " +
					"({{diff2|" + rid + "|diff}}) " +
					"after recently receiving last warning ~~~~ \r";
			} // Registration status alters template used
		} else{
			if(registered){
				aiv_text = "\r* {{Vandal|" + user + "}} ### " +
					"({{diff2|" + rid + "|diff}}) " +
					"after recently receiving unrelated 4im warning " +
					"(may require administrative attention) ~~~~ \r"; 
			} else{
				aiv_text = "\r* {{IPvandal|" + user + "}} ### " +
					"({{diff2|" + rid + "|diff}}) " +
					"after recently receiving unrelated 4im warning " +
					"(may require administrative attention) ~~~~ \r";
			} // Registration status alters template used
		} // Note made when AIV'ing after an vandalism-unrelated 4im
		
		if(type.equals(QUEUE_TYPE.VANDALISM))
			aiv_text = aiv_text.replace("###", "Vandalized");
		else if(type.equals(QUEUE_TYPE.LINK_SPAM))
			aiv_text = aiv_text.replace("###", "Link spammed");
		return(aiv_text);
	}
	
	/**
	 * Produce the edit summary which should accompany an AIV posting.
	 * @param type Type of behavior that led to block request (vandalism, etc.)
	 * @param user Username or IP address of the offending editor
	 * @param rid RID of the vandalism incident producing this request
	 * @param registered TRUE if offender is registered; FALSE, otherwise
	 * @return Text which should form the "comment" for the AIV post
	 */
	private static String aiv_comment(QUEUE_TYPE type, String user, long rid, 
			boolean registered){
		
		String comment = "";
		if(registered)
			comment = "Reporting [[User_talk:" + user + "|" + user + "]] " +
				"for ###, found using [[WP:STiki|STiki]].";
		else // Just a simple branch based on registration status 
			comment = "Reporting [[Special:Contributions/" + 
				user + "|" + user + "]] " + "for ###, " +
				"found using [[WP:STiki|STiki]].";
		
		if(type.equals(QUEUE_TYPE.VANDALISM))
			comment = comment.replace("###", "repeated vandalism");
		else if(type.equals(QUEUE_TYPE.LINK_SPAM))
			comment = comment.replace("###", "spam behavior");
		return(comment);
	}

	/**
	 * Given the full content of a Wiki article, extract a single section. 
	 * Note: This method assumes well-formed article format, behavior
	 * is un-defined if that is not the case.
	 * @param content Full content of some Wikipedia article
	 * @param section_title Section title whose content should be extracted,
	 * this field is case-sensitive
	 * @return Subset of 'content' which is under section 'section_title'.
	 * If the specified section does not exist, return the empty string.
	 */
	private static String get_section_content(String content, 
			String section_title){
		
			// Section headers come in several forms.
		String sec_start_form1 = "== " + section_title + " ==";
		String sec_start_form2 = "==" + section_title + "==";
		String sec_start_form3 = "=== " + section_title + " ===";
		String sec_start_form4 = "====" + section_title + "====";
		
			// Find last matching form on the page (likely the ONLY)
		int start_ind = Math.max(Math.max(Math.max(
				content.lastIndexOf(sec_start_form1), 
				content.lastIndexOf(sec_start_form2)), 
				content.lastIndexOf(sec_start_form3)), 
				content.lastIndexOf(sec_start_form4));
		if(start_ind == -1)
			return(""); // If section doesn't exist, return empty string
		
			// Chop off anything before start of the section (including the
			// header itself). Cut is a likely an over-estimate (2 char), but 
			// will not be punished because templates are not first to appear.
		String sec_content;
		sec_content = content.substring(start_ind + sec_start_form3.length());
		
			// Start of new section determines current, take substring
		int end_ind = sec_content.indexOf("=="); // Reduction of all forms
		if(end_ind == -1)
			return(sec_content); // If no other sections
		else return(sec_content.substring(0, end_ind));		
	}
	
	/**
	 * Determine the highest level of warning (1--4), that has
	 * been issued on some content of a "User Talk" page. Such that 1+level
	 * is the next logical warning to isssue (level 5 = AIV).
	 * 
	 * We classify two types:
	 * # Primary warnings (uw-spam or uw-vandalism) are the common templates
	 * and capture precisely the behavior we are looking to punish.
	 * # Secondary warnings (uw-test, uw-joke, etc.) are somewhat
	 * orthogonal to the behavior we detected, but are problematic nonetheless.
	 * Even though level 4 warnings exist for these, we cap their return
	 * value at 3 so the user will receive at least one behavior-specific 
	 * warning before being reported to AIV. "4im" warnings are handled
	 * by a separate function in this class.
	 * 
	 * @param type Type of warnings to search for (vandalism, spam, etc.)
	 * @param content UserTalk page content in which to search
	 * @return An integer on [0,4] will be returned. Zero is indicative of 
	 * no warnings present. Otherwise, higher numbers indicate severity.
	 */
	private static int highest_warn_level(QUEUE_TYPE type, String content){
		
			// Template:uw-vandalism1
			// Template:uw-spam1
			// Template:Huggle/warn-spam-1
			// Template:Huggle/warn-1
		
		int highest_warning = 0;
		for(int i=1; i <= 4; i++){
			
			if(type.equals(QUEUE_TYPE.VANDALISM)){
				
					// Primary templates
				if(content.contains("Template:uw-vandalism" + i) ||
						content.contains("Template:Huggle/warn-" + i))
					highest_warning = i;
					
					// Related templates
				else if(content.contains("Template:uw-test" + i) ||
						content.contains("Template:uw-delete" + i) ||
						content.contains("Template:uw-error" + i) ||
						content.contains("Template:uw-joke" + i) ||
						content.contains("Template:uw-notcensored" + i) ||
						content.contains("Template:uw-defamatory" + i))
					if(i==4) highest_warning = 3; else highest_warning = i;
				
			} else if(type.equals(QUEUE_TYPE.LINK_SPAM)){
				
					// Primary templates
				if(content.contains("Template:uw-spam" + i) ||
						content.contains("Template:Huggle/warn-spam-" + i))
					highest_warning = i;
				
					// Related templates
				else if(content.contains("Template:uw-advert" + i))
					if(i==4) highest_warning = 3; else highest_warning = i;
		
			} // Branch based on type of template beings searched	
		} // Iterate over possible levels, looking for templates
		  // Recall there is the possibility for "4im" templates
		
		return(highest_warning);
	}
	
	/**
	 * Determine the presence of "4im" warnings in some content, but
	 * do not include those of a particular type (i.e., if being of that
	 * type would result in a block request, regardless).
	 * @param type Format of "4im" warning to IGNORE if found,
	 * @param content UserTalk page content in which to search
	 * @return Whether or not a "4im" warning exists in 'content', but NOT
	 * a "4im" warning template of "type".
	 */
	private static boolean im_warn_present(QUEUE_TYPE type, String content){
		if(type.equals(QUEUE_TYPE.VANDALISM))
			return(content.contains("4im") && !content.contains("vandalism4im"));
		else // if(type.equals(QUEUE_TYPE.LINK_SPAM))
			return(content.contains("4im") && !content.contains("spam4im"));
	}
	
	/**
	 * Output the current UTC month/year in string format. It seems to be a
	 * de-facto standard that this should be heading for vandalism warnings.
	 * @return Current UTC month/year, format: "March 2010"
	 */
	private static String cur_utc_month_year(){
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT+0000")); // UTC locale
		int month = cal.get(Calendar.MONTH);
		
		String date_str = "";
		switch(month){ // Months start counting at zero
	        case Calendar.JANUARY: date_str = "January"; break;
	        case Calendar.FEBRUARY: date_str = "February"; break;
	        case Calendar.MARCH: date_str = "March"; break;
	        case Calendar.APRIL: date_str = "April"; break;
	        case Calendar.MAY: date_str = "May"; break;
	        case Calendar.JUNE: date_str = "June"; break;
	        case Calendar.JULY: date_str = "July"; break;
	        case Calendar.AUGUST: date_str = "August"; break;
	        case Calendar.SEPTEMBER: date_str = "September"; break;
	        case Calendar.OCTOBER: date_str = "October"; break;
	        case Calendar.NOVEMBER: date_str = "November"; break;
	        case Calendar.DECEMBER: date_str = "December"; break;}
		return(date_str + " " + cal.get(Calendar.YEAR));	
	}
	
	/**
	 * Determine the UNIX timestamp of the last signature in some content.
	 * @param content Content with sig-timestamps (i.e., a talk page)
	 * @return The last timestamp in 'content.' Note that "last" is 
	 * determined by physical page position, not actual time.
	 * Zero (0) will be returned if no such timestamp exists.
	 */
	private static Long last_message_ts(String content){
		List<Long> ts_list = sig_timestamps(content);
		if(ts_list.size() == 0)
			return(0L);
		else return(ts_list.get(ts_list.size()-1));
	}
	
	/**
	 * Create a list of all timestamps found on a Wikipedia page. Presumably,
	 * this would be a talk page, where posts are signed with timestamps.
	 * While we parse out Wikipedia timestamps, they are stored as UNIX ones.
	 * @param content Content which should be searched for timestamps.
	 * @return List of well-formed timestamps (converted to UNIX format), 
	 * in the order in which they were encountered in the document. 
	 */
	private static List<Long> sig_timestamps(String content){
		
			// Example timestamp: 17:41, 17 July 2010 (UTC)
		List<Long> unix_ts_list = new ArrayList<Long>();
		List<String> wiki_ts_list = stiki_utils.all_pattern_matches_within(
				stiki_utils.WIKI_TS_REGEXP, content);
		
		long cur_unix_ts;
		String cur_wiki_ts;
		String[] cur_wiki_ts_split;
		for(int i=0; i < wiki_ts_list.size(); i++){
			try{ // Attempt to parse out a unix timestamp
				cur_wiki_ts = wiki_ts_list.get(i);
				cur_wiki_ts = cur_wiki_ts.replace(",", "");
				cur_wiki_ts = cur_wiki_ts.replace(":", " ");
				cur_wiki_ts_split = cur_wiki_ts.split(" ");
				cur_unix_ts = stiki_utils.arg_unix_time(
						Integer.parseInt(cur_wiki_ts_split[4]), 
						stiki_utils.month_name_to_int(cur_wiki_ts_split[3]), 
						Integer.parseInt(cur_wiki_ts_split[2]), 
						Integer.parseInt(cur_wiki_ts_split[0]), 
						Integer.parseInt(cur_wiki_ts_split[1]), 0);
				unix_ts_list.add(cur_unix_ts);
			} catch(Exception e){};
		} // Convert all wiki-formatted timestamps to UNIX ones
		return(unix_ts_list);
	}

	/**
	 * Return the comment to be associated with WARNING PLACEMENT
	 * @param type Type of warning being left (vandalism, spam, etc.)
	 * @return Textual comment to associate with the warning action
	 */
	private static String warning_comment(QUEUE_TYPE type){
		if(type.equals(QUEUE_TYPE.VANDALISM))
			return("User warning for unconstructive editing " +
					"found using [[WP:STiki|STiki]]");
		else // if(type.equals(QUEUE_TYPE.LINK_SPAM))
			return("User warning for unconstructive external link " +
					"found using [[WP:STiki|STiki]]");
	}
	
	/**
	 * Return the comment to be associated with customized AGF messages
	 * @param type Queue type of warning being left
	 * @return Textual comment to associate with the notification action
	 */
	private static String agf_comment(QUEUE_TYPE type){
		return("Notification of [[WP:AGF|good faith]] revert " +
				"found using [[WP:STiki|STiki]]");
	}
	
	/**
	 * Append a message the offending user's talk page.
	 * @param message Wikitext content to be appended
	 * @param comment Comment to be associated with message posting
	 */
	private void user_talk_append(String message, String comment) 
			throws Exception{
		String talkpage = "User_talk:" + this.metadata.user;
		api_post.edit_append_text(talkpage, comment, message, false, 
				this.edit_pkg.get_token(), this.cookie, true, 
				no_watchlist, true);
	}
	
	/**
	 * Code to execute if the rollback response was "badtoken." This is a
	 * problematic error case, so we have some dedicated debugging output.
	 */
	private void bad_rbtoken_handler(){
		try{
			System.err.println("Bad token when trying to RB RID:" + metadata.rid);
			System.err.println("Token was: " + metadata.rb_token);
			System.err.println("Token obtained at: " + edit_pkg.get_token().snd);
			System.err.println("Session cookie was: " + cookie);
			System.err.println("Refetching token obtained: " + 
					api_retrieve.process_basic_rid(metadata.rid, cookie).rb_token);
			System.err.println();
		} catch(Exception e){};
	}
	
	/**
	 * Dialog to pop the user if the "assertuser" condition fails. That is,
	 * this code should only be called if the WMF session has been
	 * unexpectedly terminated/severed. This dialog will inform the user
	 * of what has happened and force a shutdown of STiki. THIS METHOD
	 * DOES NOT RETURN; IT KILLS THE PROGRAM!!
	 */
	private void assert_fail_dialog(){
		
		JOptionPane.showMessageDialog(this.gui_revert_panel.parent,
				"A check associated with your last revert/AGF action \n" +
				"found your login session has been unexpectedly \n" +
				"terminated. This is believed to be a bug on the WMF \n" +
				"server-side, not within STiki itself.\n" +
				"\n" +
				"Regardless, to protect your privacy (i.e., your IP \n" +
				"address), your last revert and warnings did not commit.\n" +
				"\n" +
				"When you click \"OK\" below, STiki will shut down. \n" +
				"Restarting the program and logging in again will \n" +
				"initiate a new session and correct the issue.\n\n",
				"Error: WMF has dropped session",
	 		    JOptionPane.ERROR_MESSAGE);

		this.gui_revert_panel.parent.exit_handler(true); // Kill program
	}
	
}
