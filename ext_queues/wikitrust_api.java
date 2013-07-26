package ext_queues;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import core_objects.stiki_utils;

/**
 * Andrew G. West - wikitrust_api.java - This class uses the 
 * WikiTrust API to fetch WikiTrust scores for particular RIDs.
 */
public class wikitrust_api{
	
	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * If set to FALSE, calls to this class will not attempt to even contact
	 * the WikiTrust API -- but instead return the error code. This is useful
	 * if the WikiTrust servers are known to be down for a long period (or
	 * there is another issue) -- and this flag can save bandwidth.
	 */
	public static boolean WIKITRUST_ACTIVATED = false;
	
	/**
	 * Error code to return if any part of the WikiTrust fetch fails. 
	 */
	public static final double ERROR_CODE = -1.0;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Base URL for the WikiTrust API (zero-delay version).
	 */
	private static final String WT_API = "http://en.collaborativetrust.com/" +
			"WikiTrust/RemoteAPI?method=vandalismZD&revid=#&pageid=#";
	
	/**
	 * Default number of retry attempts for contacting WikiTrust server.
	 */
	private static final int NUM_RETRIES = 0;
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Fetch the WikiTrust score for a particular revision-ID.
	 * @param rid Revision ID whose WikiTrust score is desired
	 * @return WikiTrust score for `rid' (range should be on [0,1]). Public 
	 * variable "ERROR_CODE" will be returned if the WikiTrust 
	 * servers cannot be contacted, or the WikiTrust servers return an error.
	 */
	public static double wikitrust_score(long rid, long pid){
		
		if(!WIKITRUST_ACTIVATED) // If not even querying API
			return(ERROR_CODE);
		
		String url = WT_API;
		url = url.replaceFirst("#", rid + "");
		url = url.replaceFirst("#", pid + "");
		String response = string_from_url(url, NUM_RETRIES);
		if(response == null)
			return(ERROR_CODE);
		else{
			try{double score = Double.parseDouble(response);
				return(score);
			} catch(NumberFormatException e){
				return(ERROR_CODE);
			} // Errors can happen in an attempt to parse ...
		} // ... or they can occur if server contact fails
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Retrieve a String from a URL, possibly requiring retries.
	 * @param url String format of the URL to connect to
	 * @param retries If stream cannot be established, the number of times to
	 * retry before accepting the data will not be received.
	 * @return  A string containing the content at 'str_url' or NULL if a 
	 * connection could not be established after 'retries' attempts
	 * @throws InterruptedException 
	 */
	private static String string_from_url(String str_url, int retries){
		
		try{URL url = new URL(str_url);
			URLConnection conn = url.openConnection();
			InputStream is = conn.getInputStream();
			String str_stream = stiki_utils.capture_stream(is);
			is.close();
			return(str_stream);
		} catch(Exception e){
			if(retries == 0)
				return null;
			else return(string_from_url(str_url, retries-1));
		} // Retry attempts work recursively until base-case
	}
	
}
