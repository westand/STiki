package edit_processing;

import mediawiki_api.api_retrieve;
import mediawiki_api.api_xml_user_perm;
import core_objects.metadata;
import core_objects.pair;
import db_server.db_country;
import db_server.db_edits;
import db_server.db_features;
import db_server.db_geolocation;
import db_server.db_hyperlinks;
import db_server.db_off_edits;

/**
 * Andrew G. West - rollback_handler.java - Given a recently committed edit,
 * it is the job of this class to determine if a permissioned rollback (RB) 
 * edit has taken place, and if so, find the offending edit (OE) and record a 
 * negative feedback against in in the backend database.
 */
public class rollback_handler{
		
	// **************************** PUBLIC FIELDS ****************************

	/**
	 * Rollbacks can be human-validated or be initiated by autonomous bots.
	 * Each offending-edit/rollback is tagged in this way. The NONE item
	 * is used in instances where an RB was not located.
	 */
	public static enum RB_TYPE{HUMAN, BOT, NONE};
	
	/**
	 * Regular expressions over `revision comments', which are indicative of
	 * rollback edits (assuming permission, and OE can be found). Matches
	 * are also indicative of origin (in-order): (0) Standard-RB, 
	 * (1,2) Linked-RB, (3) Huggle, (4) Twinkle, and (5,6) Cluebot.
	 */
	public static final String[] RB_REGEX = {
		"REVERTED EDIT.* BY .* TO LAST VERSION BY .*",
		"\\[\\[(WIKIPEDIA|WP):RBK\\|REVERTED\\]\\] EDIT.* BY .* TO LAST VERSION BY .*",
		"\\[\\[HELP:REVERTING\\|REVERTED\\]\\] EDIT.* BY .* TO LAST VERSION BY .*",
		"REVERTED EDIT.* BY .* TO LAST REVISION BY .*",
		"REVERTED .* EDIT.* BY .* IDENTIFIED AS \\[\\[(WIKIPEDIA|WP):VAND\\|VANDALISM\\]\\].*",
		"REVERTING POSSIBLE VANDALISM BY .* TO VERSION BY .* THANKS, \\[\\[USER:CLUEBOT\\|CLUEBOT\\]\\].*",
		"REVERTING POSSIBLE VANDALISM BY .* TO VERSION BY .* THANKS, \\[\\[USER:CLUEBOT NG\\|CLUEBOT NG\\]\\].*"
	};
	
	/**
	 * This array sets the RB_TYPE of the regular expressions in RB_REGEX.
	 * There should be 1-to-1 correspondence in the index positions.
	 */
	public static final RB_TYPE[] REGEX_TYPE = {
		RB_TYPE.HUMAN, // Standard RB
		RB_TYPE.HUMAN, // Linked-RB
		RB_TYPE.HUMAN, // Linked-RB (variety)
		RB_TYPE.HUMAN, // Huggle
		RB_TYPE.HUMAN, // Twinkle
		RB_TYPE.BOT,   // ClueBot
		RB_TYPE.BOT	   // ClueBot-NG
	};
	
	/**
	 * If a flagging edit has ID [x], then the guilty edit should be ID [x-1]. 
	 * If this is not the case, we search backwards until [x-SEARCH_DEPTH].
	 */
	public static final int SEARCH_DEPTH = 10;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Determine the rollback-status of an edit; making the appropriate
	 * database changes if a rollback/offending-edit pair can be found.
	 * @param cur_rev_md Metadata of edit to-be-examined.
	 * @param db_oe Database handler performing offending-edit requests
	 * @param db_geo DB-handler for mapping IP address to geographic locales
	 * @param db_edits DB-handler for OE-trigger into the [all_edits] table
	 * @param db_feat DB-handler for OE-trigger into the [features] table
	 * @param db_country DB-handler for OE-trigger into the [country] table
	 * @param db_links DB-handler for OE-trigger into the [hyperlinks] table
	 */
	public static void new_edit(metadata cur_rev_md, db_off_edits db_oe, 
			db_geolocation db_geo, db_edits db_edits, db_features db_feat, 
			db_country db_country, db_hyperlinks db_links) throws Exception{

		pair<Long, RB_TYPE> oe_result = find_oe(cur_rev_md);
		if(oe_result.fst == -1){
			cur_rev_md.set_is_rb(false);
			return; 
		} // RB-detection or offender-location failed

			// Now that we have RB, flag and store accordingly
		cur_rev_md.set_is_rb(true);
		metadata guilty_md;
		guilty_md = api_retrieve.process_basic_rid(oe_result.fst, db_geo);
		if(guilty_md != null && cur_rev_md.rid != guilty_md.rid){ 
			db_oe.new_oe(guilty_md, cur_rev_md.rid, oe_result.snd, db_edits, 
					db_feat, db_country, db_links);
		} // If no error getting guilty edit, and not self-identifying
	}

	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Determine if an edit is a rollback (and the offending-edit, if so)
	 * @param cur_rev_md Metadata of edit under examination
	 * @return A pair. The first element is the RID of the offending-edit (OE), 
	 * or -1 if the OE could not be located (or may not exist), for any reason.
	 * The second element is an RB_TYPE object encoding whether the the RB
	 * was located via human-validated, or autonomous (bot) means.
	 */
	private static pair<Long, RB_TYPE> find_oe(metadata cur_rev_md) 
			throws Exception{
		
			// Failure to match an RB-regex is instant rejection
		RB_TYPE rb_type = RB_TYPE.NONE;
		boolean potential_rb = false;;
		String uc_comment = cur_rev_md.comment.toUpperCase();
		for(int i=0; i < RB_REGEX.length; i++){
			if(uc_comment.matches(RB_REGEX[i])){
				rb_type = REGEX_TYPE[i];
				potential_rb = true;
				break; // No need to keep searching
			} // Check for REGEX matching
		} // Iterate over all OE-REGEXes
			
		if(!potential_rb) // If no REGEX was matched, abandon
			return (new pair<Long, RB_TYPE>(-1L, RB_TYPE.NONE));
	
		if(!rb_type.equals(RB_TYPE.BOT)){
			boolean rb_perm = api_xml_user_perm.has_rollback(
					api_retrieve.process_user_perm(cur_rev_md.user));
			if(!rb_perm) // If user not permissioned, abandon
				return (new pair<Long, RB_TYPE>(-1L, RB_TYPE.NONE));
		} // Check rollback permissions of flagging user (but not bots)
		
			// Next parse out the offender (and check for self-RB)
		String uc_offender = parse_offender(uc_comment);
		if(uc_offender.equals("") || uc_offender.equals(cur_rev_md.user))
			return (new pair<Long, RB_TYPE>(-1L, RB_TYPE.NONE));
		
			// Having offender, search for offending edit RID
		long oe_rid = api_retrieve.process_offender_search(uc_offender, 
				cur_rev_md.pid, cur_rev_md.rid, SEARCH_DEPTH);
		if(oe_rid == -1) // If offender-location fails, abandon
			return (new pair<Long, RB_TYPE>(-1L, RB_TYPE.NONE)); 
		
			// If successful, return RID and type of offending-edit
		return (new pair<Long, RB_TYPE>(oe_rid, rb_type)); 
	}

	/**
	 * Given a comment string of rollback form, return the offending user.
	 * @param comment Full (escaped, uppercase) comment of a Wikipedia edit
	 * @return The username/IP of the offending user, or the empty string
	 * if the process failed for any reason (ill-formed format).
	 */
	private static String parse_offender(String comment) throws Exception{
	
		try{
				// Relatively easily to get "offender" portion given 
				// the fixed (starting) structure of the REGEXs
				// Must be careful not to be to forgiving, else one of these
				// split-words could appear inside of a user-name
			String[] comm = comment.split(" BY | TO LAST |" +
					" TO VERSION | IDENTIFIED ");
			String offend = comm[1].trim();
			
				// End of comment may contain misc. parenthetical data; remove
				// BUT, we don't want to affect those with '(',')' in username
			if(offend.matches(".*\\(.*\\)")){
				if(offend.matches("\\[\\[.*\\]\\].*\\(.*\\)")){
					offend = offend.split("\\]\\]")[0];
					offend += "]]";                 
				} // The common-case can be explitly handled	
				else offend = offend.substring(0, offend.indexOf('(')).trim();
			} // If not common-case, make a best guess
			
				// Handle specially formed offender strings
			String re_sp1 = "\\[\\[.*\\|.*\\]\\]";
			String re_sp2 = "\\[\\[USER:.*\\]\\]";
			String re_sp3 = "\\[\\[.*\\]\\]";
			String re_sp4 = ".* (TALK)";
			
			if(offend.matches(re_sp1)) // [[Special:Contributions/bob|bob]]
				return(offend.split("\\||\\]\\]")[1].trim());	
			else if(offend.matches(re_sp2)) // [[User:bob]]
				return(offend.substring(7, offend.length()-2));
			else if(offend.matches(re_sp3)) // [[bob]]
				return(offend.substring(2, offend.length()-2));
			else if(offend.matches(re_sp4)) // bob (talk)
				return(offend.substring(0, offend.length()-7));
	
			return (offend); // If no regex matched, assume simple case
			
		} catch(Exception e){
			System.out.println("Offender-parse failed: " + comment);
			return ("");
		} // If reg-ex successful parsing fails, report	
	}
	
}
