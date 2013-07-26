package learn_frontend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import core_objects.metadata;
import core_objects.pair;
import core_objects.stiki_utils;
import core_objects.xlink_parser;

import db_server.db_hyperlinks;

/**
 * Andrew G. West - feature_hyperlinks.java - This class wraps the 
 * [xlink_parser.java] class. Whereas that class provides generic link
 * parsing functionality, this one handles the STiki engines specific
 * needs and inputs/outputs. 
 */
public class feature_hyperlinks{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Number of ex-links that *we* have parsed since class creation.
	 */
	private static int PARSE_ATTEMPTS = 0;
	
	/**
	 * Of [PARSE_ATTEMPTS], the quantity that passed gold-standard. That is,
	 * the link addition was verified by a Wikipedia API query.
	 */
	private static int PARSE_SUCCESS = 0;
	
		
	// ***************************** TEST HARNESS ***************************
	
	/**
	 * Simple test harness.
	 * @param args No arguments are taken
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception{
		
		String wikitext = "Once upon a time, a [http://www.example.com " +
				"hyperlink existed] on the Wikipedia";
	
		List<String> added_text = new ArrayList<String>();
		added_text.add(wikitext);
		List<String> rem_text = new ArrayList<String>();
		// process(api_retrieve.process_basic_rid(100), added_text, rem_text, null);
		
			// NOTE: That this harness does nothing at this time. With DB
			// connections, and etc. -- one should just comment out portions
			// of the public method in order to perform testing.
	}
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Process a wiki revision to determine any new external links added
	 * therein (note that a modification also counts as an add). 
	 * @param md Metadata of edit currently under inspection
	 * @param blocks_added See description in [feature_language]
	 * @param blocks_removed description in [feature_language]
	 * @param db_links DB-handler over the [hyperlinks] table
	 */
	public static void process(metadata md, 
			List<String> blocks_added, List<String> blocks_removed, 
			db_hyperlinks db_links){
		
			// The idea here is to ignore the individual tokens added
			// (which, in the case of URL modifications, might be fragments
			// of a URL). Instead, we look at the larger paragraphs/blocks
			// in which these are embedded.
			//
			// We parse all content removed for links and compare this
			// against the parse for all content added in order to deduce
			// the "new" links in the "added" version.
		
		Set<String> links_removed = new TreeSet<String>();		
		StringBuilder rem_text = new StringBuilder();		
		Iterator<String> iter_rem = blocks_removed.iterator();
		while(iter_rem.hasNext())
			rem_text.append(iter_rem.next() + " ");	
		String rem_text_str = rem_text.toString();
		links_removed.addAll(xlink_parser.parse_xlinks(rem_text_str));
		
		Set<String> links_added = new TreeSet<String>();
		StringBuilder added_text = new StringBuilder();	
		Iterator<String> iter_add = blocks_added.iterator();
		while(iter_add.hasNext())
			added_text.append(iter_add.next() + " ");
		String added_text_str = added_text.toString();	
		links_added.addAll(xlink_parser.parse_xlinks(added_text_str));

			// Do not want to include "moved" links
			// Once set determined, pass off to handler
		links_added.removeAll(links_removed);
		Set<pair<String,String>> url_and_desc = 
				get_descriptions(links_added, added_text_str);
		publish_links(url_and_desc, md, db_links);
		//measure_parse_success(links_added, md); // MUST BE LAST; DESTRUCTIVE
	}
	
	/**
	 * Return the success ratio of our link parsing efforts. That is, for
	 * the links we parse, what portion are verified by the API gold-standard?
	 * @return Percentage on [0,1] describing the success of our unassisted
	 * parsing efforts. Note that this only measures false positives, and
	 * there is no way at current to measure true negatives.
	 */
	public static double parse_success(){
		if(PARSE_ATTEMPTS > 0)
			return((1.0 * PARSE_SUCCESS)/(PARSE_ATTEMPTS));
		else return(1.0);
	}
	

	// *************************** PRIVATE METHODS **************************
	
	/**
	 * Given a set of URLs in a body of text, try to find their "descriptions"
	 * @param urls Set of URLs that were parsed out of 'added_text'
	 * @param added_text Body of text containing 'urls'
	 * @return A set of pairs, whose first elements are the members of 'urls'
	 * and whose second element is the "hyperlink text" of that text. This is
	 * a rudimentary attempt at the task of description parsing
	 */
	private static Set<pair<String,String>> get_descriptions(
			Set<String> urls, String added_text){
		
			// Here we only really attempt to find the description in one
			// case: "[http://www.example.com This is a description]"
			// We also assume well-formedness
		
		String url, regex, desc;
		Set<pair<String,String>> urls_and_desc = 
				new HashSet<pair<String,String>>();
		Iterator<String> url_iter = urls.iterator();
		while(url_iter.hasNext()){
			url = url_iter.next();
			regex = "\\[" + url + ".*?\\]";
			desc = stiki_utils.first_match_within(regex, added_text);
			if(desc == null)
				urls_and_desc.add(new pair<String,String>(url, ""));
			else{
				desc = desc.replace("[" + url, "");
				desc = desc.replace("]", "");
				urls_and_desc.add(new pair<String,String>(url, desc.trim()));
			} // only look for descriptions in the simple case
		} // iterate over all URLs added
		return(urls_and_desc);
	}
	
	/**
	 * Determine our API parse accuracy relative to API ground-truth. Given
	 * replag and other funniness, the API "truth" is sometimes latent
	 * and inaccurate at our calculation time. Thus, this method has been
	 * removed as a prerequisite to publishing links, and serves only
	 * to keep status/monitoring parameters updated. 
	 * @param links_added Set containing links parsed from edit on 'pid'
	 * @param md Metadata wrapping RID on which 'links_added' were added
	 */
	@SuppressWarnings("unused")
	private static void measure_parse_success(
			Set<String> links_added, metadata md){
		
		PARSE_ATTEMPTS += links_added.size();
		List<String> parse_api_agree = xlink_parser.api_set_agreement(
				 new ArrayList<String>(links_added), md.pid);
		PARSE_SUCCESS += parse_api_agree.size();
		links_added.removeAll(parse_api_agree);
		Iterator<String> fail_iter = links_added.iterator();
		while(fail_iter.hasNext())
			System.out.println("PID: " + md.pid + " - RID: " + 
					md.rid + " - We parsed: " + fail_iter.next());
	}
	
	/**
	 * Publish link addittions to the DB and IRC channels
	 * @param links Links found in edit wrapped by 'md'
	 * @param md Metadata of edit currently under inspection
	 * @param db_links DB-handler over the [hyperlinks] table
	 */
	private static void publish_links(Set<pair<String,String>> urls_and_desc, 
			metadata md, db_hyperlinks db_links){
		
		if(urls_and_desc.size() == 0){
			try{db_links.notify_no_links(md.rid, md.pid);}
			catch(Exception e){System.out.println("Trouble when notifying " +
					"of no links in RID: " + md.rid);}
		} else{
			try{db_links.insert_links(new ArrayList<pair<String,String>>(
					urls_and_desc), md.rid, md.pid, md.timestamp, md.user);}
			catch(Exception e){
				System.out.println("Trouble in parse/insertion of " +
					"external URL(s) with RID: " +  md.rid);}
		} // Publish all links to DB/IRC-feed, even if zero
	}

}