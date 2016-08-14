package learn_frontend;

import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

import mediawiki_api.api_retrieve;

import core_objects.feature_set;
import core_objects.metadata;
import core_objects.stiki_utils;
import db_server.db_category;
import db_server.db_country;
import db_server.db_geolocation;
import db_server.db_hyperlinks;
import db_server.db_off_edits;

/**
 * Andrew G. West - feature_handler.java - This class takes in metadata of 
 * an edit (presumably the most recent one), and does the scoring and 
 * reputation valuation necessary to build the feature set.
 * 
 * Where possible, features are handled internally to this class -- many
 * require DB handlers -- and the complex broadly-grouped reputations
 * have dedicated classes for their computation.
 */
public class feature_builder{

	// ***************************** PUBLIC VARS *****************************
	
	/**
	 * At current, hyperlink features are not part of STiki's classification
	 * process. However, setting this flag to TRUE enables data collection
	 * which may eventually be used to build an anti-spam classifier.
	 */
	private static final boolean PROCESS_HYPERLINKS = true;
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Given an edit's metadata, compute its feature set. 
	 * @param md Metadata associated with an edit. Critically, this
	 * method should be run immediately after the edit has been comitted.
	 * This method is not designed to handle prior edits.
	 * @param db_oe Handler for DB queries involving offending edits.
	 * @param db_geo Handler for DB queries involving geo-location data
	 * @param db_cat Handler for DB queries involving category data
	 * @return Feature set for edit encoded by 'md'. 
	 */
	public static feature_set score_edit(metadata md, db_off_edits db_oe,
			db_geolocation db_geo, db_category db_cat, db_country db_country,
			db_hyperlinks db_links) throws Exception{
		
			// Straightforward reputations and other simple queries
		double rep_user = user_reputation(md.user, db_oe);
		double rep_article = article_reputation(md.pid, db_oe);
		int size_change = api_retrieve.process_size_change(md.pid, md.rid);
		int comm_length = md.comment.length();
		
			// Handle geo-location based features
		float tod; int dow; double rep_country;
		if(md.user_is_ipv4){
			double gmt_offset = db_geo.get_gmt_offset(
					stiki_utils.ip_to_long(md.user));
			if(Double.isNaN(gmt_offset))
				gmt_offset = 0.0; // Ugly hack: should map to own grouping
			Calendar cal = get_unix_set_cal(md.timestamp, gmt_offset);
			tod = time_of_day(cal); 
			dow = day_of_week(cal);
			rep_country = db_country.cur_country_rep(md.country);
			db_country.increment_all(md.country, md.timestamp);
		} else{ // Reg'd users ineligible, take on error values 
			tod = -1.0F; dow = -1; rep_country = -1.0;
		}
		
			// Next handle features of the 'time since' form
		long ts_r = (md.timestamp - 
				api_retrieve.process_user_first_edit_ts(md.user));
		long ts_lp =  api_retrieve.process_prior_page_edit_ts(md.pid, md.rid);
		if(ts_lp != -1) 
			ts_lp = (md.timestamp - ts_lp);
		long ts_rbu = db_oe.ts_last_user_oe(md.user);
		if(ts_rbu != -1) 
			ts_rbu = (md.timestamp - ts_rbu);
		
			// NLP-feature calculation handled in helper-class
		feature_language lang_feats = new feature_language(md.rid);
		int nlp_dirty = lang_feats.dirty_regex_score();
		int nlp_char_rep = lang_feats.longest_char_repetition();
		double nlp_ucase = lang_feats.percentage_uppercase();
		double nlp_alpha = lang_feats.percentage_alpha();
		
		if(PROCESS_HYPERLINKS){
			feature_hyperlinks.process(md, 
					lang_feats.get_added_blocks(),
					lang_feats.get_removed_blocks(), db_links);
		} // Collect data concerning any external-hyperlinks added.
		
		return (new feature_set(false, md.rid, md.user_is_ipv4_or_ipv6, 
				rep_user, rep_article, tod, dow, ts_r, ts_lp, ts_rbu, 
				comm_length, size_change, rep_country, nlp_dirty, nlp_char_rep, 
				nlp_ucase, nlp_alpha));
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Return a Calendar object initialized to a Unix time, with offset
	 * @param unix_ts Unix timestamp of time (GMT) to be interpreted
	 * @param offset Offset from GMT, in locality, in hours
	 * @return Calendar object encoding 'unix_ts' offset by 'offset' hours
	 */
	private static Calendar get_unix_set_cal(long unix_ts, double offset){
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
		cal.setTimeInMillis((long) ((unix_ts + 60*60*offset) * 1000));
		return(cal);
	}

	/**
	 * Return the time-of-day per a Calendar object. Time-of-day is represented 
	 * as a floating point number, [0,24). The integer portion is the hour on
	 * a 24-hour clock. The decimal portion corresponds to (minute/60).
	 * @param cal Calendar object initialized to a particular date/time
	 * @return Time-of-day represented by 'cal', per format described above.
	 */
	private static float time_of_day(Calendar cal){
		float time_of_day = cal.get(Calendar.HOUR_OF_DAY);
		time_of_day += (cal.get(Calendar.MINUTE) / 60.0);
		time_of_day += (cal.get(Calendar.SECOND) / 3600.0);		
		return(time_of_day);
	}
	
	/**
	 * Return the day-of-week which a Calendar object encodes.
	 * @param cal Calendar object initialized to a particular date/time
	 * @return Integer [1,7] describing day-of-week encoded by Cal. Java's
	 * date format is s.t. SATURDAY=7 and SUNDAY=1.
	 */
	private static int day_of_week(Calendar cal){
		return (cal.get(Calendar.DAY_OF_WEEK));
	}
	
	/**
	 * Calculate the raw-reputation of a user at timestamp 'now'.
	 * @param user Identifier of user whose reputation is being valuted
	 * @param db_rb DB handler for looking up past poor behavior
	 * @return Reputation of user 'user' at time 'calc_ts'
	 */
	private static double user_reputation(String user, 
			db_off_edits db_oe) throws Exception{
		
		double raw_rep = 0.0;
		long ts_now = stiki_utils.cur_unix_time();
		Iterator<Long> iter = db_oe.recent_user_oes(user).iterator();
		while(iter.hasNext()){
			raw_rep += stiki_utils.decay_event(ts_now, iter.next(), 
					stiki_utils.HALF_LIFE);
		} // Iterate over all temporally relevant OE's, adding to rep
		return (raw_rep);
	}
	
	/**
	 * Calculate the raw-reputation of an article at timestamp 'now'
	 * @param pid Identifier of article whose reputation is being valuated
	 * @param db_rb DB handler for looking up past poor behavior
	 * @return Reputation of article 'pid' at time 'calc_ts'
	 */
	private static double article_reputation(long pid, 
			db_off_edits db_oe) throws Exception{
	
		double raw_rep = 0.0;
		long ts_now = stiki_utils.cur_unix_time();
		Iterator<Long> iter = db_oe.recent_article_oes(pid).iterator();
		while(iter.hasNext()){
			raw_rep += stiki_utils.decay_event(ts_now, iter.next(), 
					stiki_utils.HALF_LIFE);
		} // Iterate over all temporally relevant OE's, adding to rep
		return (raw_rep);
	}
	
	/*
	 * Calculate the raw-reputation of a category at timestamp 'now'
	 * @param pid Article of the edit whose category-rep is being scored
	 * @param db_cat DB handler for category membership data
	 * @param db_rb DB handler for looking up past poor behavior
	 * @return The reputation of the category of which 'pid' is a member,
	 * that has the worst normalized (by number of members) reputation.
	 *
	private synchronized static double category_reputation(long pid, 
			db_category db_cat, db_off_edits db_oe) throws Exception{
		
		List<Long> cat_members;		// List containing a categories members
		Iterator<Long> iter_mems;	// Iterator over 'cat_members' instance
		double cat_rep;				// Reputation for single category
		double max_rep = 0.0;		// Max-normal rep. of all categories
		
		List<Long> cats = db_cat.get_page_memberships(pid);
		Iterator<Long> iter_cat = cats.iterator();
		while(iter_cat.hasNext()){
	
			cat_rep = 0.0;
			cat_members = db_cat.get_category_members(iter_cat.next());
			iter_mems = cat_members.iterator();
			while(iter_mems.hasNext()) // Sum cat-reps from article-reps
				cat_rep += article_reputation(iter_mems.next(), db_oe);
			max_rep = Math.max(max_rep, (cat_rep / cat_members.size()));
			
		} // Outer-loop, iterate over all cat's in which page is member
		return(max_rep);
	} */

}
