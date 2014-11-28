package gui_support;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import mediawiki_api.api_retrieve;

import core_objects.stiki_utils;
import core_objects.xlink_parser;

/**
 * Andrew G. West - diff_markup.java - Provided the raw HTML of a diff
 * between two edits (per the MediaWiki API), including references to 
 * style-sheets that do not exist locally -- this class is in charge of
 * beautifying the HTML such that it is aesthetically pleasing, yet also
 * capable of being displayed in a JEditorPane.
 * 
 * This includes the activation of URLs/hyperlinks in the diff -- a 
 * rather non-trivial task given the presence of possible token changes
 * internal to a single URL.
 */
public class diff_markup{
	
	// ***************************** PUBLIC FIELDS ***************************
	
		// The default color scheme employed by WMF for diffs
	public static String DEF_COL_DIFF_BG = "#ffffff";
	public static String DEF_COL_DIFF_WORDS = "#ff0000";
	public static String DEF_COL_DIFF_CONT = "#eeeeee";
	public static String DEF_COL_DIFF_DEL = "#ffffaa";
	public static String DEF_COL_DIFF_ADD = "#ccffcc";
	public static String DEF_COL_DIFF_NOTE = "#800080";
	public static String DEF_COL_DIFF_TEXT = "#000000";
	
		// Some color constants w.r.t. diff rendering
	public static String COLOR_DIFF_BG = gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.color_diff_bg, DEF_COL_DIFF_BG);
	public static String COLOR_DIFF_WORDS = gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.color_diff_words, DEF_COL_DIFF_WORDS);
	public static String COLOR_DIFF_CONT = gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.color_diff_cont, DEF_COL_DIFF_CONT);
	public static String COLOR_DIFF_DEL = gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.color_diff_del, DEF_COL_DIFF_DEL);
	public static String COLOR_DIFF_ADD = gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.color_diff_add, DEF_COL_DIFF_ADD);
	public static String COLOR_DIFF_NOTE =  gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.color_diff_note, DEF_COL_DIFF_NOTE);
	public static String COLOR_DIFF_TEXT =  gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.color_diff_text, DEF_COL_DIFF_TEXT);
	
	
	// ***************************** TEST HARNESS ****************************
	
	/**
	 * Test harness for this class
	 * @param args No arguments are taken
	 */
	public static void main(String[] args) throws Exception{
		
			// A simple test of link parsing.
		/*String html = "http://www.example.com for one. " +
				"Here is a http://www.example.com link. However " +
				"things can get dirty, and a link might look like " +
				"http://www.<font color=#ff0000><b>example2</b></font>.com " +
				"but multiple http://www.example.com links are also problems";
		System.out.println(add_hyperlinks(html)); */
		
		String html = api_retrieve.process_diff_prev(495022990);
		System.out.println(beautify_markup(html, "Title", "", true));
	}
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Given the HTML diff per the MediaWiki API, beautify that HTML to a 
	 * version which is more appropriate for local, JEditorPane use.
	 * @param raw_html Raw-diff HTML per the MediaWiki API
	 * @param page_title Title of the Wiki-article whose diff is in 'raw_html'
	 * @param note Note to appear in diff, just below the title. This is
	 * optional, pass in an empty String if no "note" is desired.
	 * @param links TRUE if Xlinks should be click-able; false, otherwise
	 * @return Beautified version of the input text
	 */
	public static String beautify_markup(String raw_html, String page_title, 
			String note, boolean links){
		
			// Write header/title/footer around diff-table to complete HTML
		String html = "<html><body bgcolor=" + COLOR_DIFF_BG + ">";
		html += "<font color=" + COLOR_DIFF_TEXT + ">";
		html += "<center><br><b><u><font color=" + COLOR_DIFF_TEXT + ">";
		html += page_title + "</font></u></b>";
		if(note.length() > 0)
			html += "<br><font color=" + COLOR_DIFF_NOTE + ">" + note + "</font>";
		html += "<br><br>";
		html += "<table border=\"0\" cellspacing=\"5\">";
		html += raw_html + "</center></table></font></body></html>";
	
			// Beautify and clean-up the Wiki-provided HTML
		html = markup_table_style(html);
		html = markup_delete_unneccesary(html);
		html = markup_cell_widths(html);
		
		if(links) // If requested, activate any hyerlinks
			html = add_hyperlinks(html);
		return (html);
	}
	
	/**
	 * Reset the diff coloring to the default used by the WMF
	 */
	public static void reset_default_colors(){
		COLOR_DIFF_BG = DEF_COL_DIFF_BG;
		COLOR_DIFF_WORDS = DEF_COL_DIFF_WORDS;
		COLOR_DIFF_CONT = DEF_COL_DIFF_CONT;
		COLOR_DIFF_DEL = DEF_COL_DIFF_DEL;
		COLOR_DIFF_ADD  = DEF_COL_DIFF_ADD;
		COLOR_DIFF_NOTE = DEF_COL_DIFF_NOTE;
		COLOR_DIFF_TEXT = DEF_COL_DIFF_TEXT;
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Remove HTML references to Wikipedia style-sheets for table style, and
	 * replace with more conventional, pure HTML equivalent.
	 */
	private static String markup_table_style(String html){
		
			// Word-level changes should be bolded-red
		html = html.replace("<span class=\"diffchange diffchange-inline\">", 
				"<font color=" + COLOR_DIFF_WORDS + "><b>");
		html = html.replace("<del class=\"diffchange diffchange-inline\">", 
				"<font color=" + COLOR_DIFF_WORDS + "><b>");
		html = html.replace("<ins class=\"diffchange diffchange-inline\">", 
				"<font color=" + COLOR_DIFF_WORDS + "><b>");
		html = html.replace("</span>", "</b></font>");
		html = html.replace("</del>", "</b></font>"); // <del> new 2014-01
		html = html.replace("</ins>", "</b></font>"); // <ins> new 2014-01
			
			// No style associated with the printing of context-line
		html = html.replace("class=\"diff-lineno\"", "");
		
			// Shading (grey, green, yellow, respectively) of diff-table
		html = html.replace("class=\"diff-context\"", 
				"bgcolor=" + COLOR_DIFF_CONT);
		html = html.replace("class=\"diff-deletedline\"", 
				"bgcolor=" + COLOR_DIFF_DEL);
		html = html.replace("class=\"diff-addedline\"", 
				"bgcolor=" + COLOR_DIFF_ADD);
		return (html);
	}
	
	/**
	 * Delete unnecessary (empty) HTML tags.
	 */
	private static String markup_delete_unneccesary(String html){
		
			// We remove the 'diff-marker' cells (i.e., plus/minus), and
			// then must remove the column-span to compensate
		html = html.replaceAll("<td class=\"diff-marker\">.*</td>", "");
		html = html.replace(" colspan=\"2\"", "");

			// Delete <div> blocks, empty rows, and lines. Note that empty
			// cells are not removed, as they prevent the need to use 
			// "colspan" in order to ensure diff alignment.
		html = html.replaceAll("(<div>|</div>)", "");
		//html = html.replaceAll("<td.*></td>", "");	
		html = html.replaceAll("<tr>\\s*</tr>", "");
		html = html.replaceAll("\\n\\s*\\n", "\n");
		return (html);
	}
	
	/**
	 * Adjust all cells so they occupy 50% of the width available.
	 */
	private static String markup_cell_widths(String html){
		html = html.replace("<td", "<td width=\"50%\"");
		return(html);
	}
	
	/**
	 * Turn external hyperlinks appearing in a Wikipedia diff
	 * into actual clickable-links (utilizing simple HTML format).
	 */
	private static String add_hyperlinks(String html){
		
			// Two big ideas here: (1) We have to be careful about span
			// tags appearing intra-url. (2) Naive replacement of URLs
			// wrapped as HTML links will result in duplicate captures

			// Parse links from an HTML-free version
		html = " " + html; // Don't have to worry about start boundary
		String no_html = html.replaceAll("<[^<]*>", "");
		List<String> urls = xlink_parser.parse_xlinks(no_html);
		
			// Spans (token level changes) internal to text blocks are what 
			// really screw up link parsing. Thus our exhaustive searches
			// for their splitting of links is made a little more efficient
			// by tightening their pattern.
		html = html.replace("<font color=" + COLOR_DIFF_WORDS + "><b>", "<@>");
		html = html.replace("</b></font>", "</@>");

		List<String> sorted_urls = 
				new LinkedList<String>(new TreeSet<String>(urls));
		Collections.sort(sorted_urls, new Comparator<String>(){
			public int compare(String str1, String str2){
				return(str2.length() - str1.length());
			} // We make link-set a sorted list from longest to shortest URL
		}); // This prevents greediness, (e.g. www.google.com potentially 
			// stealing and shortening the hyperlink for www.google.com/bla)

			// We use an obscure unicode character to mark URLs that have 
			// already been HTML-fied.
		char PROC = 0xFFFD;
		String RX_PROC = "\\uFFFD";
	
		String url, regex, match;
		Character strip_char;
		Iterator<String> url_iter = sorted_urls.iterator(); // Is a set
		while(url_iter.hasNext()){
			url = url_iter.next();	
			regex = produce_url_search_regex(url); // Find unproc instances
			while(true){
				match = stiki_utils.first_match_within(regex, html);
				if(match == null)
					break; // If we are done with URL
				
					// Find one instance of a URL at a time. Mark as
					// processed. Loop around to see if more
					// (and once done, remove "processed" notation)
				strip_char = match.charAt(0);
				match = match.substring(1);	
				html = html.replaceFirst("[^" + RX_PROC + "]" + 
						pattern_quote(match), strip_char + 
						"<A HREF=\"" + PROC + url + "\">" + 
						PROC + match + "</A>"); 
			} // Keep finding instances of URL until all are marked processed
		} // Iterate over all URLs found (although a list; also a set)
		
			// Undo our concise notation for internal spans
			// Clear all other notation
		html = html.replace("<@>", "<font color=" + COLOR_DIFF_WORDS + "><b>");
		html = html.replace("</@>", "</b></font>");
		html = html.replaceAll(RX_PROC, ""); // Cl
		html = html.substring(1);
		return(html);
	}
	
	/**
	 * Provided a URL, produce a regex that allows us to find that URL in
	 * the presence of possible span tags (i.e., intra-URL change formatting). 
	 * Note that this method is only a helper for add_hyperlinks(). It is
	 * not intended to be for general purpose use.
	 * @param url Plain-text url
	 * @return Regex that will capture URL in specially formatted diff text.
	 */
	private static String produce_url_search_regex(String url){
		
			// Here we build a regex that (a) does not capture links that
			// have already been processed, and (b) considers the possibility
			// that span tags could appear *anywhere*.
		
		String SPAN_DELIMITS = "[<@>|</@>]*";
		String NO_START = "[^\\uFFFD]"; // Marker for processed URLs
		StringBuilder regex = new StringBuilder(NO_START);
		for(int i=0; i < url.length(); i++){
			if(i == url.length() -1)
				regex.append(pattern_quote(url.charAt(i)+""));
			else regex.append(pattern_quote(url.charAt(i)+"") + SPAN_DELIMITS);
		} // Iterate over URL character by character; painful!
		return(regex.toString());
	}
	
	/**
	 * Later Java versions contain a Pattern.quote() method. It is not
	 * included in our version of the libraries, but this is the actual
	 * source code from the later versions which is compliant. 
	 * @param s String to be quoted in Pattern form
	 * @return Quoted version of 's' for Pattern input
	 */
	private static String pattern_quote(String s){
	    int slashEIndex = s.indexOf("\\E");
	    if (slashEIndex == -1)
	        return "\\Q" + s + "\\E";

	    StringBuilder sb = new StringBuilder(s.length() * 2);
	    sb.append("\\Q");
	    slashEIndex = 0;
	    int current = 0;
	    while ((slashEIndex = s.indexOf("\\E", current)) != -1) {
	        sb.append(s.substring(current, slashEIndex));
	        current = slashEIndex + 2;
	        sb.append("\\E\\\\E\\Q");
	    }
	    sb.append(s.substring(current, s.length()));
	    sb.append("\\E");
	    return sb.toString();
	}

}
