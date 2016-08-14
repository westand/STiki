package core_objects;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import db_client.stiki_con_client;

/**
 * Andrew G. West - stiki_util.java - This is a class to provide the variable
 * constants and utility methods used throughout the STiki project.
 */
public class stiki_utils{
	
	// *************************** CRITICAL STUFF ****************************
	
	/**
	 * An enumeration listing those queues currently implemented by STiki:
	 * CBNG   Scores from Cluebot-NG system not resulting in a bot revert
	 * STIKI: Scores from ADTree processing of metadata (West et al.)
	 * WT:    Scores from WikiTrust reputation system (Adler et al.)
	 */
	public enum SCORE_SYS{CBNG, STIKI, WT, SPAM};
	
	/**
	 * An enumeration listing the broad queue "types" supported by STiki:
	 * VANDALISM:	Queues prioritized to locate vandalism
	 * LINK_SPAM:	Queues prioritized to locate external link spam
	 */
	public enum QUEUE_TYPE{VANDALISM, LINK_SPAM};
	
	
	// **************************** PUBLIC FIELDS ****************************
	
	// ******* QUEUE TABLES *******	
	
	/**
	 * Each queue has a table/priority-queue from which live edits are pulled.
	 */
	public static final String tbl_scores_stiki = "scores_stiki";
	public static final String tbl_scores_cbng = "scores_cbng";
	public static final String tbl_scores_wt = "scores_wt";
	public static final String tbl_scores_spam = "scores_spam";
	
	/**
	 * Similarly, a separate table scores all scores persistently. 
	 */
	public static final String tbl_queue_stiki = "queue_stiki";
	public static final String tbl_queue_cbng = "queue_cbng";  
	public static final String tbl_queue_wt = "queue_wt"; 
	public static final String tbl_queue_spam = "queue_spam"; 
	
	
	// ******* OTHER TABLES *******	
	
	/**
	 * Table of offending-edits and associated metadata. Subset of [tbl_edits].
	 */
	public static final String tbl_off_edits = "offending_edits";
	
	/**
	 * Table where offending-edits incapable of providing statistically 
	 * significant contributions are stored (for archival purposes).
	 */
	public static final String tbl_oes_old = "oes_archive";
	
	/**
	 * Table containing all edits and associated metadata.
	 */
	public static final String tbl_edits = "all_edits";
	
	/**
	 * Table where feature data is stored for all edits.
	 */
	public static final String tbl_features = "features";
	
	/**
	 * Table where human-initiated classification decisions are stored.
	 */
	public static final String tbl_feedback = "feedback";
	
	/**
	 * The raw Wikipedia category-links table is imported under this name.
	 * Some extensive processing is performed over it (cleaning it), only
	 * then can it be renamed and put into live use as "tbl_category" below.
	 */
	public static final String tbl_offline_cat_links = "categorylinks";
	
	/**
	 * Table where category-to-page links are stored (and the inverse).
	 */
	public static final String tbl_cat_links = "category_links";
	
	/**
	 * Wiki-dumped table containing category-ID to category-name mappings.
	 */
	public static final String tbl_category = "category";
	
	/**
	 * Table pertinent to the computation of country-reputation.
	 */
	public static final String tbl_country = "country";
	
	/**
	 * Table storing IP->location mappings at country granularity.
	 */
	public static final String tbl_geo_country = "geo_country";
	
	/**
	 * Table storing IP->location,timezone mappings at city granularity.
	 */
	public static final String tbl_geo_city = "geo_city";
	
	/**
	 * Table containing external-links added by Wikipedia edits.
	 */
	public static final String tbl_links = "hyperlinks";
	
	/**
	 * Table where STiki status variables are stored/updated/viewed.
	 */
	public static final String tbl_status = "stiki_status";
	
	/**
	 * Table where client stored procedure calls are logged, so the log
	 * can be inspected from a security perspective, if required. Insertions
	 * to this table should only be possible from within stored procedures.
	 * Only procedures using UPDATE/DELETE should will be logged.
	 */
	public static final String tbl_log = "log_client";
	
	/**
	 * Table specifying which queue should be selected by default. This
	 * is done dynamically to prevent problems arising from service outages.
	 */
	public static final String tbl_def_queue = "default_queue";
	
	
	
	// ******** CONSTANTS *********
	
	/**
	 * Regular expression defining the timestamps used in Wikipedia signatures. 
	 */
	public static final String WIKI_TS_REGEXP = "\\d\\d:\\d\\d, (\\d\\d|\\d) " +
			"(January|February|March|April|May|June|July|August|September|" +
			"October|November|December) \\d\\d\\d\\d \\(UTC\\)";
		
	/**
	 * Half-life, in seconds, which should be used to decay reputation-events.
	 */
	public static final int HALF_LIFE = 60*60*24*5;
	
	/**
	 * The time duration that an event has statistically significant weight,
	 * assuming its weight is decayed expotentially using HALF_LIFE. Events
	 * occuring more than HIST_WINDOW ago need not be included in rep. calc. 
	 */
	public static final int HIST_WINDOW = 60*60*24*45;
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Return the integer constant/label code for a scoring system (queue)
	 * Note this mapping should be equivalent to DB table [default_queue]
	 * @param queue Queue of interest
	 * @return Integer code associated with that queue
	 */
	public static int queue_to_constant(SCORE_SYS queue){
		if(queue.equals(SCORE_SYS.STIKI)) return(1);
		else if(queue.equals(SCORE_SYS.CBNG)) return(2);
		else if(queue.equals(SCORE_SYS.WT)) return(3);
		else if(queue.equals(SCORE_SYS.SPAM)) return(4);
		return(0); // Catch-all if something weird happens
	}
	
	/**
	 * Return a scorying system (queue), given its integer code
	 * Note this mapping should be equivalent to DB table [default_queue]
	 * @param qcode Integer code identifying a queue
	 * @return The 'SCORE_SYS' associated with 'qcode'. NULL will be returned
	 * if the 'qcode' does not map to a system.
	 */
	public static SCORE_SYS constant_to_queue(int qcode){
		if(qcode == 1) return(SCORE_SYS.STIKI);
		else if(qcode == 2) return(SCORE_SYS.CBNG);
		else if(qcode == 3) return(SCORE_SYS.WT);
		else if(qcode == 4) return(SCORE_SYS.SPAM);
		return(null);
	}
	
	/**
	 * Given a scoring system, return its "type" (objective function)
	 * @param queue Scoring system (queue), per enumeration
	 * @return What 'queue' targets. That is, what type of edit does 
	 * the scoring system prioritize to find (vandalism, spam, etc.)
	 */
	public static QUEUE_TYPE queue_to_type(SCORE_SYS queue){
		if(queue.equals(SCORE_SYS.STIKI)) return(QUEUE_TYPE.VANDALISM);
		else if(queue.equals(SCORE_SYS.CBNG)) return(QUEUE_TYPE.VANDALISM);
		else if(queue.equals(SCORE_SYS.WT)) return(QUEUE_TYPE.VANDALISM);
		else if(queue.equals(SCORE_SYS.SPAM)) return(QUEUE_TYPE.LINK_SPAM);
		return(null);
	}
	
	/**
	 * Test if the DB is reachable by trying to establish a connection. More 
	 * generally, this can be thought of as a network connectivity test.
	 * @return TRUE if the DB could be reached; FALSE, otherwise
	 */
	public static boolean test_db_connectivity(){
		try{stiki_con_client con_client= new stiki_con_client();
			con_client.con.close();
			return true;
		} catch(Exception e){
			return false;
		} // TRUE if connection established. Exceptions return FALSE.
	}
	
	/**
	 * Open an input reader over a file whose location/name is provided.
	 * @param filename File which is to read in
	 * @return A reader capable of reading the file line-by-line, or NULL if 
	 * an error occured during the creation of said reader
	 */
	@SuppressWarnings("resource")
	public static BufferedReader create_reader(String filename){
		BufferedReader in = null;
		try{ // If an exception is raised, return NULL
			File inFile = new File(filename);
			FileInputStream fis = new FileInputStream(inFile);
			DataInputStream dis = new DataInputStream(fis);
			InputStreamReader isr = new InputStreamReader(dis);
			in = new BufferedReader(isr);
		} catch (Exception e){
			System.err.println("Error opening reader over file: " + filename);
		} // And output a message concerning nature of error
		return in;
	}
	
	/**
	 * Open an output writer over a file whose location/name is provided.
	 * @param filename File which is to be written
	 * @param append TRUE if data should be appending to file; FALSE, otherwise
	 * @return A string writer suitable for file authoring, or NULL if an
	 * error occurred during the creation of said writer. 
	 */
	public static BufferedWriter create_writer(String filename, boolean append){
		BufferedWriter out = null;
		try{ // If an exception is raised, return NULL
			File outFile = new File(filename);
			out = new BufferedWriter(new FileWriter(outFile, append));
		} catch(Exception e){
			System.err.println("Error opening writer over file: " + filename);
		} // And output a message concerning nature of error 
		return out;
	}
	
	/**
	 * Read the contents of an InputStream into a String.
	 * @param in InputStream whose content is being captured
	 * @return String object containing all data in 'in'
	 */
	public static String capture_stream(InputStream in) throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		StringBuilder sb = new StringBuilder();
		while((line = br.readLine()) != null)
			sb.append(line);
		return(sb.toString());
	}
	
	/**
	 * Retrieve a String from a URL destination, possibly requiring retries.
	 * @param str_url URL from which data should be fetched
	 * @param retries If connection cannot be established, the number of times 
	 * to retry before accepting the data will not be received.
	 * @return String of data at 'url', or NULL if a connection could
	 * not be established after 'retries' attempts at connecting.
	 */
	public static String str_from_url(String str_url, int retries) 
			throws InterruptedException{
		
		try{URL wrapped_url = new URL(str_url);
			URLConnection con = wrapped_url.openConnection();
			con.connect();
			InputStream is = con.getInputStream();			 
			return(stiki_utils.capture_stream(is));
		} catch(Exception e){
			e.printStackTrace();
			Thread.sleep(50); // Slight pause, server might correct issue
			if(retries == 0){
				return null;
			} else{return(str_from_url(str_url, retries-1));}
		} // Retry attempts work recursively until base-case
	}
	
	/**
	 * Given a string of an IP address in conventional ???.???.???.????
	 * decimal octet format, compute the 32-bit decimal (sans octet) equivalent. 
	 * Note: Return type is a LONG because Java integers are signed. 
	 * @param ip String representation of an IP address ("???.???.???.???")
	 * @return Long containing decimal representation of the IP
	 */
	public static long ip_to_long(String ip){
		String[] ip_octets = ip.split("\\.");
		
			// Integers used because 'byte' type is signed
		int octet1 = Integer.parseInt(ip_octets[0]);
		int octet2 = Integer.parseInt(ip_octets[1]);
		int octet3 = Integer.parseInt(ip_octets[2]);
		int octet4 = Integer.parseInt(ip_octets[3]);
		
			// Long used because 'int' types are signed
		long dec_rep = octet1;
		dec_rep <<= 8;
		dec_rep |= octet2;
		dec_rep <<=8;
		dec_rep |= octet3;
		dec_rep <<=8;
		dec_rep |= octet4;
		return dec_rep;
	}
	
	/**
	 * Given an integer format IP address, convert to a string representation.
	 * Against, a long is used because Java integers are always signed.
	 * @param int_ip 32-bit IP address, in long format
	 * @return Parameter 'int_ip' in conventional ???.???.???.??? format
	 */
	public static String ip_to_string(long int_ip){
		
		int mask= 255; // (2^8 - 1)
		long octet4, octet3, octet2, octet1;
		
		octet4 = int_ip & mask;
		mask <<= 8;
		octet3 = (int_ip & mask) >> 8;
		mask <<= 8;
		octet2 = (int_ip & mask) >> 16;
		mask <<= 8;
		octet1 = (int_ip & mask) >> 24;
		
		return(octet1 + "." + octet2 + "." + octet3 + "." + octet4); 
	}
	
	/**
	 * Given a String representation of a boolean; convert it to a native one
	 * @param value String representation of a boolean. Any casing of "true"
	 * or "false" is accepted, as are "0" and "1"
	 * @return native boolean representation of 'val'
	 */
	public static boolean str_to_bool(String val){
		try{if(Integer.parseInt(val) == 0)
				return false;
		} catch(Exception e){}
		if(val.equalsIgnoreCase("false") || val.equals(""))
			return false;
		return true;
	}
	
	/**
	 * Return the result of the expression (2^exp). Method implemented due
	 * to the fact the Math.pow() method requires doubles, which seems risky
	 * when dealing with large (signed) integer results.
	 * @param exp Positive integer power to which 2 should be raised
	 * @return Result of the expression (2^exp)
	 */
	public static long power_of_2(int exp){
		long result = 1;
		for(int i = exp; i != 0; i--)
			result *= 2;
		return result;
	}
	
	/**
	 * Find the substrings that match some regex, in a larger string. 
	 * Note that the regex will be compiled as "case insensitive"
	 * @param regex Regex by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return All substrings of 'corpus' matching 'regex'
	 */
	public static List<String> all_pattern_matches_within(String regex, 
			String corpus){
		List<String> matches = new ArrayList<String>();
		Matcher match = Pattern.compile(regex, 
				Pattern.CASE_INSENSITIVE).matcher(corpus);
		while(match.find())
			matches.add(match.group());
		return(matches);
	}
	
	/**
	 * Find the UNIQUE substrings that match some pattern, in a larger string.
	 * Note that the regex will be compiled as "case insensitive"
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return All UNIQUE substrings of 'corpus' matching 'regex'
	 */
	public static Set<String> unique_matches_within(String regex, 
			String corpus){
		Set<String> matches = new HashSet<String>();
		Matcher match = Pattern.compile(regex, 
				Pattern.CASE_INSENSITIVE).matcher(corpus);
		while(match.find())
			matches.add(match.group());
		return(matches);
	}
	
	/**
	 * Return the first substring that matches some pattern, in a larger string.
	 * Note that the regex will be compiled as "case insensitive"
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return First substring of 'corpus' matching 'regex'. Return NULL if
	 * no matches are present.
	 */
	public static String first_match_within(String regex, String corpus){
		Matcher match = Pattern.compile(regex,  
				Pattern.CASE_INSENSITIVE).matcher(corpus);
		while(match.find())
			return(match.group());
		return(null);
	}
	
	/**
	 * Determine if there is a pattern match in some larger string
	 * Note that the regex will be compiled as "case insensitive"
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return True if 'regex' is matched within 'corpus' .
	 */
	public static boolean has_match_within(Pattern regex, String corpus){
		Matcher match = regex.matcher(corpus);
		while(match.find())
			return(true);
		return(false);
	}
	
	/**
	 * Number of substrings that match some pattern, in a larger string.
	 * Note that the regex will be compiled as "case insensitive"
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return Number of substrings of 'corpus' matching 'regex'
	 */
	public static int num_matches_within(String regex, String corpus){
		int matches = 0;
		Matcher match = Pattern.compile(regex,  
				Pattern.CASE_INSENSITIVE).matcher(corpus);
		while(match.find())
			matches++;
		return(matches);
	}
	
	/**
	 * Number of substrings that match some pattern, in a larger string.
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return Number of substrings of 'corpus' matching 'regex'
	 */
	public static int num_matches_within(Pattern pattern, String corpus){
		int matches = 0;
		Matcher match = pattern.matcher(corpus);
		while(match.find())
			matches++;
		return(matches);
	}
	
	/**
	 * Determine the number of times some character appears in a string
	 * @param letter Letter/character of interest
	 * @param input Input string
	 * @return Number of times that "letter" occurs in "input"
	 */
	public static int char_occurences(char letter, String input){
		int matches = 0;
		for(int i=0; i < input.length(); i++){
			if(input.charAt(i) == letter)
				matches++;
		} // Just iterate over entire string by character
		return(matches);
	}
	
	/**
	 * Set the system clipboard such that it contains some string.
	 * @param str Content to reside on clipboard after method completes
	 */
	public static void set_sys_clipboard(String str){
		StringSelection selection = new StringSelection(str);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}
	
	/**
	 * Given the output from a logistically generated function (i.e., ADTrees),
	 * determine the CDF probability of that output, or a lower output. More 
	 * intuitively, given a score [x], return [y] such that [y]% of all 
	 * scores (in the distribution) should probabilistically score [x] or lower.
	 * @param value Score output being analyzed.
	 * @return CDF value (on [0,1]) of "value" per simple logistic dist. 
	 */
	public static double logistic_cdf(double value){
		
			// IN 3.80 -> OUT 0.98
			// IN -8.1 -> OUT 0.01
			// IN 0.00 -> OUT 0.50
		return(1.0 - ((1.0) / (1.0 + Math.exp(value))));	
	}
	
	/**
	 * Determine whether a provided string is in IPv4 or IPv6 format.
	 * @param addy String to test for IP address matching
	 * @return TRUE if 'addy' is a well-formed IPv4 or IPv6 address.
	 * FALSE otherwise.
	 */
	public static boolean is_v4_v6_ip(String addy){
		return(addy.matches("(\\d)+\\.(\\d)+\\.(\\d)+\\.(\\d)+") || 
				addy.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"));
	}
	

	// ***** TIME-CENTRIC METHODS *****

	/**
	 * Time decay an event, from ''calc_time', using the half-life param.
	 * @param calc_ts UNIX timestamp when calculation is being made
	 * @param event_ts UNIX timestamp at which event in question occured
	 * @param hl Half-life of expotential decay, in seconds
	 * @return Using the half-life, decay the duration from 
	 * (this.calc_time-event_time), using the base-quantity of 1.
	 */
	public static double decay_event(long calc_ts, long event_ts, long hl){
		double decay = Math.pow(0.5,((calc_ts-event_ts)/(hl * 1.0)));
		if(decay > 1.0)
			return (1.0);
		return(decay); 
	}
	
	/**
	 * Provide the number of elapsed seconds from UNIX epoch until the 
	 * time specified by the arguments (in GMT/UTC time zone). 
	 * @param y Year (in UTC locale) from which to calculate time
	 * @param mon Month (in UTC locale) from which to calculate time
	 * @param d Day (in UTC locale) from which to calculate time
	 * @param h Hour (in UTC locale) from which to calculate time
	 * @param min Min (in UTC locale) from which to calculate time
	 * @return Number of seconds between UNIX epoch at UTC date provided
	 */
	public static long arg_unix_time(int y, int mon, int d, int h, 
			int min, int sec){
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
		cal.set(y, (mon-1), d, h, min, sec); // ZERO-INDEXED months?! WTF?!?!
		return (cal.getTimeInMillis() / 1000);
	}
	
	/**
	 * Convert a Wikipedia timestamp (2001-01-21T02:12:21Z) into a UNIX one
	 * @param wiki_ts Wikipedia timestamp, in string format
	 * @return The same time as 'wiki_ts' expressed in UNIX format
	 */
	public static long wiki_ts_to_unix(String wiki_ts){
		
		int year=0, month=0, day=0, hour=0, min=0, sec=0;
		
			// Being a fixed format, we can parse the parts out easily
		if(wiki_ts.contains("Z") || wiki_ts.contains("-")){
			year = Integer.parseInt(wiki_ts.substring(0, 4));
			month = Integer.parseInt(wiki_ts.substring(5,7));
			day = Integer.parseInt(wiki_ts.substring(8, 10));
			hour = Integer.parseInt(wiki_ts.substring(11, 13));
			min = Integer.parseInt(wiki_ts.substring(14, 16));
			sec = Integer.parseInt(wiki_ts.substring(17, 19));
		} else{
			year = Integer.parseInt(wiki_ts.substring(0, 4));
			month = Integer.parseInt(wiki_ts.substring(4,6));
			day = Integer.parseInt(wiki_ts.substring(6, 8));
			hour = Integer.parseInt(wiki_ts.substring(8, 10));
			min = Integer.parseInt(wiki_ts.substring(10, 12));
			sec = Integer.parseInt(wiki_ts.substring(12, 14));
		}
		
		return (stiki_utils.arg_unix_time(year, month, day, hour, min, sec));
	}
	
	/**
	 * Convert a UNIX timestamp into one on the Wikipedia format
	 * @param unix_ts Time represented in UNIX format.
	 * @return Time 'unix_ts' in Wikipedia format (2001-01-21T02:12:21Z)
	 */
	public static String unix_ts_to_wiki(long unix_ts){
		
		String wiki_ts = "";
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+00"));
		cal.setTimeInMillis(1000 * unix_ts);
		wiki_ts += cal.get(Calendar.YEAR) + "-"; 
		wiki_ts += cal_pad("" + (cal.get(Calendar.MONTH)+1)) + "-"; 
		wiki_ts += cal_pad("" + cal.get(Calendar.DAY_OF_MONTH)) + "T"; 
		wiki_ts += cal_pad("" + cal.get(Calendar.HOUR_OF_DAY)) + ":"; 
		wiki_ts += cal_pad("" + cal.get(Calendar.MINUTE)) + ":"; 
		wiki_ts += cal_pad("" + cal.get(Calendar.SECOND)) + "Z";
		return(wiki_ts);
	}

	/**
	 * Given a full-month name (i.e., January), convert it to its
	 * integer month number (i.e., 1)
	 * @param month Fully written out name of some month
	 * @return Integer equivalent of `month', or zero (0) if error.
	 */
	public static int month_name_to_int(String month){
		if(month.equals("January")) return 1;
		else if(month.equals("February")) return 2;
		else if(month.equals("March")) return 3;
		else if(month.equals("April")) return 4;
		else if(month.equals("May")) return 5;
		else if(month.equals("June")) return 6;
		else if(month.equals("July")) return 7;
		else if(month.equals("August")) return 8;
		else if(month.equals("September")) return 9;
		else if(month.equals("October")) return 10;
		else if(month.equals("November")) return 11;
		else if(month.equals("December")) return 12;
		else return(0);
	}
	
	/**
	 * Return the number of UTC seconds elapsed since UNIX epoch.
	 * @return number of seconds elapsed since UNIX epoch
	 */
	public static long cur_unix_time(){
		return (System.currentTimeMillis() / 1000);
	}
	
	/**
	 * Return the current 'unix day'. The number of days elapsed since
	 * Jan. 1, 1970, which is considered day 0 (zero).
	 * @return Number of days elapsed since 1970/1/1.
	 */
	public static long cur_unix_day(){
		return(cur_unix_time()/(60*60*24));	
	}
	
	/**
	 * Return the 'unix day' at some unix second.
	 * @param unix_sec Unix timestamp (in the unit of seconds)
	 * @return Number of days between 1970/1/1 and 'unix_sec'
	 */
	public static long unix_day_at_unix_sec(long unix_sec){
		return(unix_sec/(60*60*24));	
	}
	
	
	// ************************** PRIVATE METHODS ***************************
	
	private static String cal_pad(String str){
		if(str.length() == 2)
			return(str);
		else return ("0" + str);
	}
	
}
