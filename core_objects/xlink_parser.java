package core_objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;

import mediawiki_api.api_retrieve;

/**
 * Andrew G. West and Dr. Allen Smith (http://www.drallensmith.org/ ;
 * [[User:Allens]] on en.wp) - xlink_parser.java - This class, given 
 * some wikitext, will parse out external links. 
 * 
 * A special thanks to Dr. Smith for his regexp wizardry!
 */
public class xlink_parser{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Regexp for removing comments.
	 */
	private static final String REGEXP_COMMENT = "<!--(?:[^-]+|-)*?-->";
	
	/**
	 * Regexp for splitting into sections, without devouring the delimiters.
	 */
	private static final String REGEXP_SPLIT = "(?=(?:\\{\\{|\\}\\}))";
	
	/**
	 * Regexp for protocols that MediaWiki translates into working URI/URLs
	 */
	private static final String REGEXP_PROTS = "(?:(?:https?|ftp|irc|ircs|" +
			"gopher|telnet|nntp|worldwind|svn|git|mms)://|news:|mailto:)";
	
	/**
	 * Regexps for characters in URLs - first is taken directly from MediaWiki
	 * MediaWiki notes: "\p{Zs} is unicode 'separator, space' category"
	 */
	private static final String URL_CHAR = 
			"[^\\]\\[<>\"\\x00-\\x20\\x7F\\p{Zs}]";
	private static final String URL_CHAR_MINUS_PIPE = 
			"[^\\]\\[<>\"\\x00-\\x20\\x7F\\p{Zs}|]";
	
	/**
	 * Regexps to recognize plain URLs and simply brackted "[]" ones:
	 */
	private static final String REGEXP_PLAIN = 
			"(\\b" + REGEXP_PROTS + URL_CHAR + "+)";
	private static final String REGEXP_PLAIN_TEMPLATE = 
			"(\\b" + REGEXP_PROTS + URL_CHAR_MINUS_PIPE + "+)";
	
	/**
	 * Regexps for characters to strip from the end - per MediaWiki
	 */
	private static final String REGEXP_STRIP_CHAR = "[,;.:!?]+$";
	private static final String REGEXP_STRIP_CHAR_PLUS_PAREN = "[,;.:!?)]+$";
	
	/**
	 * Regexp for templates that force "http://" if no protocol present
	 */
	private static final String REGEXP_URL_TEMPLATE = 
			"^\\{\\{\\s*(?:URL|Official website)\\s*\\|";
	
	/**
	 * Regexps to get URLs from the REGEXP_URL_TEMPLATE formats
	 */
	private static final String REGEXP_URL_TEMPLATE_PLAIN =	
			REGEXP_URL_TEMPLATE + "\\s*(?:(?:1|url|mobile)\\s*=\\s*)?(" + 
			REGEXP_PROTS + "?" + URL_CHAR_MINUS_PIPE + "+)";
	private static final String REGEXP_URL_TEMPLATE_NAMED = 
			"|\\s*(?:1|url|mobile)\\s*=\\s*(" + REGEXP_PROTS + "?" + 
			URL_CHAR_MINUS_PIPE + "+)";	
	private static final String REGEXP_URL_TEMPLATE_URL = 
			"(?:" + REGEXP_URL_TEMPLATE_PLAIN + ")|(?:" + 
			REGEXP_URL_TEMPLATE_NAMED + ")";

	/**
	 * Regexp for template that makes its input a link
	 */
	private static final String REGEXP_PLAIN_LINK_TEMPLATE = 
			"^\\{\\{\\s*plain\\s*link\\s*\\|";
	
	/**
	 * Regexps to extract URLs bsed on REGEXP_PLAIN_LINK_TEMPLATE
	 */
	private static final String REGEXP_PLAIN_TEMPLATE_PLAIN = 
			REGEXP_PLAIN_LINK_TEMPLATE + "\\s*(?:(?:1|url)\\s*=\\s*)?(" + 
			REGEXP_PROTS + URL_CHAR_MINUS_PIPE + "+)";
	private static final String REGEXP_PLAIN_TEMPLATE_NAMED = 
			"|\\s*(?:1|url)\\s*=\\s*(" + REGEXP_PROTS + 
			URL_CHAR_MINUS_PIPE + "+)";	
	private static final String REGEXP_PLAIN_TEMPLATE_URL = 
			"(?:" +	REGEXP_PLAIN_TEMPLATE_PLAIN + ")|(?:" + 
			REGEXP_PLAIN_TEMPLATE_NAMED + ")";
	
	/**
	 * Regexps to detect valid hostnames
	 */
	private static final String REGEXP_HOSTNAME = 
			"^(?:(?:[a-zA-Z0-9][-.a-zA-Z0-9]+)|(?:\\[[a-fA-F9:.]{2,}\\]))$";
	
	
	// ***** COMPILED REGEXPS
	
		// These are uncontroversial compilations of the above.
		// Doing this statically uptop should have some time.
	private static final Pattern split_pattern = 
			Pattern.compile(REGEXP_SPLIT); // Insensitivity uneeded
	private static final Pattern plain_pattern = 
			Pattern.compile(REGEXP_PLAIN); // Insensitivity uneeded
	private static final Pattern url_template_pattern = 
			Pattern.compile(REGEXP_URL_TEMPLATE, Pattern.CASE_INSENSITIVE);
	private static final Pattern url_template_url_pattern = 
			Pattern.compile(REGEXP_URL_TEMPLATE_URL, Pattern.CASE_INSENSITIVE);
	private static final Pattern url_template_plain_pattern = 
			Pattern.compile(REGEXP_PLAIN_TEMPLATE_URL, Pattern.CASE_INSENSITIVE);
	private static final Pattern plain_link_pattern = 
			Pattern.compile(REGEXP_PLAIN_LINK_TEMPLATE, Pattern.CASE_INSENSITIVE);
	private static final Pattern plain_template_pattern = 
			Pattern.compile(REGEXP_PLAIN_TEMPLATE, Pattern.CASE_INSENSITIVE);
	private static final Pattern prots_pattern = 
			Pattern.compile(REGEXP_PROTS, Pattern.CASE_INSENSITIVE);
	private static final Pattern hostname_pattern = 
			Pattern.compile(REGEXP_HOSTNAME); // Insensitivity uneeded
	
	
	// ***************************** TEST HARNESS ****************************
	
	/**
	 * Simple test-harness for testing this classes functionality.
	 * @param args No arguments are taken by this method
	 */
	public static void main(String[] args) throws Exception{
		
			// Some basic settings, data
		String ALLENS_SANDBOX = "User:Allens/sandbox";
		long ALLENS_SANDBOX_PID = 33658968L;
		String wikitext = api_retrieve.process_page_content(ALLENS_SANDBOX);
		
			// Compare our parse to API return
			// Assuming no change to above page == 176 right now (2012-06-01)
		List<String> urls = parse_xlinks(wikitext);
		List<String> api_set_agree = api_set_agreement(
				urls, ALLENS_SANDBOX_PID);
		System.out.println("URLs API/parse in common: " +  api_set_agree.size());
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Parse out all external links from a body of wikitext
	 * @param wikitext Body of text in wiki syntax format
	 * @return All external links (URLs) contained in 'wikitext'
	 */
	public static List<String> parse_xlinks(String wikitext){
		
			// Preliminaries: Remove comments, standardize ampersand (per API)
		List<String> urls = new ArrayList<String>(); // return set
		wikitext = wikitext.replaceAll(REGEXP_COMMENT, "");
		wikitext = wikitext.replace("&amp;", "&");
		wikitext = wikitext.replace("&lt;", "<");
		wikitext = wikitext.replace("&gt;", ">");
		
		int bracket_level = 0;
		String parts[] = split_pattern.split(wikitext);
		for(int i = 0; i < parts.length; i++){
			if(parts[i].startsWith("{{")){
				bracket_level++;
			} else if(parts[i].startsWith("}}")){
				bracket_level--;
				if(bracket_level < 0)
					bracket_level=0; // Issue?
			} // Determine how nested we are in brackets
			
			if(bracket_level == 0) // Plain patterns
				urls.addAll(parse_pattern(parts[i], plain_pattern));
			else if(stiki_utils.has_match_within(url_template_pattern, parts[i]))
				urls.addAll(parse_pattern(parts[i], url_template_url_pattern));
			else if(stiki_utils.has_match_within(plain_link_pattern, parts[i]))
				urls.addAll(parse_pattern(parts[i], url_template_plain_pattern));
			else // if [plain_template_pattern] match
				urls.addAll(parse_pattern(parts[i], plain_template_pattern));		
		} // URLS are parsed per plain/template/etc. appearance locale
		return(urls);
	}
	
	/**
	 * Determine the agreement between a set of passed in links (those 
	 * parsed locally), and that computed by the WP parser (via API)
	 * @param inlist User provided set of URLs
	 * @param pid Page-ID (on wiki) which will be WP parsed
	 * @return All URLs in 'inlist' that the API agrees are on article 'pid.'
	 * NULL will be returned in an error is encountered.
	 */
	public static List<String> api_set_agreement(List<String> inlist, 
			long pid){
		
			// Straightforward. However do note the "&amp;"->"&" replacement.
			// below. This is something the call to [process_xlinks()]
			// does explicitly in creating its set, which seems to help
			// out HTTP clients in resolving links (?)
		
		try{String parsed_url;
			List<String> agreed = new ArrayList<String>();
			Set<String> gold_urls = api_retrieve.process_xlinks(pid, 0);
			for(int i=0; i < inlist.size(); i++){
				parsed_url = inlist.get(i);
				parsed_url = parsed_url.replace("&amp;", "&");
				if(gold_urls.contains(parsed_url)){
					agreed.add(parsed_url);
				} else{}
			} // Iterate over all links
			return(agreed);
		} catch(Exception e){
			System.out.println("Error in hyperlink-API agreement check:");
			e.printStackTrace();
			return(null);
		}
	}
	
	
	// **************************** PRIVATE METHODS ***************************
	
	/**
	 * Parse out wikilinks from text, dependent on the type of environment
	 * they reside in (i.e., plain text, vs. bracketed, vs. template).
	 * @param wikitext Body of text in wiki-syntax format
	 * @param pattern Regex specific to type of environment being parsed
	 * @return All external links in 'wikitext' per 'pattern'
	 */
	private static List<String> parse_pattern(String wikitext, 
			Pattern pattern){
		
		String url;
		List<String> urls = new ArrayList<String>();
		List<String> matches = all_matches_within(pattern, wikitext, 1);
		Iterator<String> match_iter = matches.iterator();
		while(match_iter.hasNext()){
			url = match_iter.next();
			if(url == null)
				continue; // why does this happen?
			if(url.indexOf("(") == -1)
				url = url.replaceAll(REGEXP_STRIP_CHAR_PLUS_PAREN, "");
			else url = url.replaceAll(REGEXP_STRIP_CHAR, "");
			
			if(pattern.equals(url_template_url_pattern)){ 
				if(!stiki_utils.has_match_within(prots_pattern, url)) // ext
					url = "http://" + url;
			} // This block is the only non-standard among parent patterns
			
			url = uri_sanity_check(url);
			if(url != null)
				urls.add(url);
		} // iterate over all plain pattern matches
		return(urls);
	}
	
	/**
	 * Given a probable URL, perform a "sanity check" by converting to URI
	 * @param url Potentially well-formed URL
	 * @return TRUE if 'url' is a valid URI (per Java rules); FALSE, otherwise
	 */
	private static String uri_sanity_check(String url){
		URI uri;
		try {uri = new URI(url);}
		catch (Exception e){return null;}
		if(!uri.isOpaque()){
			try {uri = uri.parseServerAuthority();} 
			catch (Exception e) {return null;}
			url = uri.getHost();
			if(url == null)
				return null;
			if(!stiki_utils.has_match_within(hostname_pattern, url))
				return null;
		} // If opaque, we are good to go.
		return(uri.toASCIIString());
	}

	/**
	 * Find the substrings that match some group of capturing 
	 * parenthesis (by index), in a larger string.
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return All substrings of 'corpus' matching the n-th 
	 * (n=capture-group) group of parens in 'regex'
	 */
	private static List<String> all_matches_within(
			Pattern regex, String corpus, int capture_group){
		List<String> matches = new ArrayList<String>();
		Matcher match = regex.matcher(corpus);
		while(match.find())
			matches.add(match.group(capture_group));
		return(matches);
	}
	
}
