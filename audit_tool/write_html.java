package audit_tool;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core_objects.metadata;
import core_objects.pair;
import core_objects.stiki_utils;

import audit_tool.user_history.UPAGE_PROP;

/**
 * Andrew G. West - write_html.java - This class is used to take provided
 * data, and produce the audit reports.
 */
public class write_html{
	
	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * HTML header for output report
	 */
	public static final String HTML_HEADER = "" +
			"<HTML>\n" +
			"<HEAD>\n" +
			"<TITLE>WikiAudit report</TITLE>" +
			"<STYLE TYPE=\"text/css\">" +
			"body{background: #F0F0F0; width: 750px; " +
				"margin: 15px auto 15px auto; font-family: " +
				"\"Times New Roman\", \"Liberation Serif\", times, serif; " +
				"font-size: 13px;}" +
			"#main{background: #ffffff; width: 750px; border-right: " +
				"1px solid #cccccc; border-left: 1px solid #cccccc; " +
				"border-top: 1px solid #cccccc; border-bottom: 1px solid #cccccc;}" + 
			"#content{width: 90%; margin: auto; padding: 5px 10px;}" + 
			"</STYLE>" +
			"</HEAD>\n" +
			"<BODY><div id=\"main\"><div id=\"content\">\n";

	/**
	 * Documentation about how the report should be interpreted.
	 */
	public static final String DOCUMENTATION = 
			"This section provides brief information about how to read, " +
			"interpret, and respond to this report. Immediately below are " +
			"\"aggregate statistics\" that give a high-level overview of the " +
			"contributions/behavior of those IP addresses provided as input. " +
			"Below that are detailed/raw histories that detail contributions " +
			"on a per-IP basis. " +
			"\n<BR><BR>\n" +
			"Bear in mind that this tool only considers edits made by " +
			"<A HREF=\"https://en.wikipedia.org/wiki/Wikipedia:User_access_" +
			"levels#Unregistered_users\">unregistered</A> users. If someone " +
			"in the IP space has a registered username and participates " +
			"using that, it will not appear in these results. While useful " +
			"for conducting broad security investigations of any IP range, " +
			"we presume that whomever is reading this report somehow " +
			"*administrates* the addresses being reported on. " +
			"\n<BR><BR>\n" +
			"Foremost, this report is helpful in helping one to understand " +
			"the magnitude of wiki participation and the extent of any poor " +
			"behaviors. It is bad behavior which is of paramount concern. " +
			"Thus, some simple heuristics of the report help to identify " +
			"malicious users/contributions, which are <FONT COLOR=\"red\">" +
			"<B>highlighted red</B></FONT> in the raw report data:" +
			"\n<BR><BR>\n" +
			"<LI> Users that have a <A HREF=\"https://en.wikipedia.org/wiki/" +
			"Wikipedia:Block\">block history</A> are likely the most " +
			"problematic. However, the details of the block should be " +
			"investigated (use the provided link), as it could be the result " +
			"of collateral damage from a broad <A HREF=\"https://" +
			"en.wikipedia.org/wiki/Wikipedia:Range_block#Range_blocks\">" +
			"IP-range block</A>." +
			"\n<BR><BR>\n" +
			"<LI> Some users have a <A HREF=\"https://en.wikipedia.org/wiki/" +
			"Help:Using_talk_pages\">talk page</A>. Good (often, registered) " +
			"editors use these for discussion and coordination. However, in " +
			"the vast majority of cases, when an IP address has a talk page " +
			"it is because someone has tried to warn them of their " +
			"transgressions." +
			"\n<BR><BR>\n" +
			"<LI> If a talk page exists for an IP, WikiAudit scans it for " +
			"commonly-used <A HREF=\"https://en.wikipedia.org/wiki/" +
			"Wikipedia:Warning\">warning templates</A>. If vandalism/spam " +
			"warnings are found, this is included in the WikiAudit report. " +
			"Realize that when multiple such warnings accumulate in some " +
			"time period, this is the basis for enacting a block." +
			"\n<BR><BR>\n" +
			"<LI> Talk pages (when they exist) can also be used to provide " +
			"the wiki administration information about the nature of the IP " +
			"address. For example, some IPs that map to <A HREF=\"" +
			"https://en.wikipedia.org/wiki/Template:Shared_IP_edu\">" +
			"educational institutions</A>, <A HREF=\"https://en.wikipedia.org/" +
			"wiki/Template:Shared_IP_corp\">corporations</A>, and " +
			"<A HREF=\"https://en.wikipedia.org/wiki/Template:Shared_IP\">" +
			"generic shared/DHCP space</A> all have templates identifying " +
			"them as such. If you administrate the IP addresses, it may be " +
			"wise to label the talk pages appropriately and provide an " +
			"\"abuse contact point.\" The WikiAudit report should indicate " +
			"which user-talk pages already have such a template." +
			"\n<BR><BR>\n" +
			"<LI> Individual contributions that may be problematic are " +
			"also <FONT COLOR=\"red\"><B>highlighted red</B></FONT>. " +
			"This determination is made based on the *next edit*. If the " +
			"comment left by the next edit indicates vandalism removal, " +
			"the previous edit is flagged (note that this a cheap and " +
			"sometimes inaccurate heuristic). " +
			"\n<BR><BR>\n" +
			"<LI> Just because an edit was not reverted or did not generate " +
			"a talk-page warning does not mean it was constructive. We " +
			"especially encourage administrators to review the edit diff's " +
			"when the article topic is one related to the host " +
			"organization/institution. Individuals advancing their own " +
			"agenda to the detriment of <A HREF=\"https://en.wikipedia.org/" +
			"wiki/Wikipedia:NPOV\">neutral-point-of-view (NPOV)</A> policies " +
			"is not well received. Similarly, there may be policies against " +
			"<A HREF=\"https://en.wikipedia.org/wiki/Wikipedia:OR\">" +
			"original-research (OR)</A>" +
			"\n<BR><BR>\n" +
			"<I>So how should all of this be used?</I> We assume network/IT " +
			"administrators can use logs to associate IP addresses with " +
			"actual members of their institution/organization. Thus, " +
			"administrators can take action against perpetrators, educating " +
			"them that damage to collaborative environments is not tolerated, " +
			"and possibly taking punitive action in egregious cases. " +
			"Moreover, the organization/institution has a vested interest " +
			"in minimizing poor behavior, as it could reflect poorly on " +
			"their institution and create collateral damage for benign " +
			"users (i.e., with range-blocks). Finally, administrative and " +
			"casual-users alike can use this data to hunt for biased " +
			"editing (e.g., an institution/organization aggressively " +
			"promoting their own agenda)." +
			"\n<BR><BR>\n" +
			"<I>Disclaimer: </I> All heuristics were designed for speed " +
			"and will exhibit both false positives and true negatives. " +
			"Template-based heuristics may be specific to English Wikipedia. " +
			"WikiAudit should function reasonably for any MediaWiki wiki " +
			"using an English installation. Support for foreign MediaWiki " +
			"wikis is untested. Want to improve this? Visit " +
			"<A HREF=\"https://en.wikipedia.org/wiki/WP:WikiAudit\">" +
			"WikiAudit</A> online and learn how to contribute to coding " +
			"and localization efforts.<BR>\n";
	
	/**
	 * HTML footer for the output report.
	 */
	public static final String HTML_FOOTER = "" +
			"\n</BODY>\n" +
			"</HTML>";
	
	
	// ***************************** PRIVATE FIELDS **************************
	
	/**
	 * Parent class. Contains some global audit variables.
	 */
	audit PARENT_AUDIT;
	
	/**
	 * User-centric wiki history.
	 */
	List<user_history> HIST;
	
	/**
	 * Buffer to which HTML report/output should be published.
	 */
	BufferedWriter OUT;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [write_html] object by providing all fields. Field
	 * documentation can be found as the private fields of this class.
	 */
	public write_html(audit parent, List<user_history> hist,
			BufferedWriter out){
		this.PARENT_AUDIT = parent;
		this.HIST = hist;
		this.OUT = out;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Write an HTML document from data contained in the 'HIST' (history)
	 */
	public void write() throws Exception{
		
		OUT.write(HTML_HEADER);
		OUT.write(header_stats());
		for(int i=0; i < HIST.size(); i++){
			OUT.write(user_stats(i));
			OUT.write(contribs_stats(i));
		} // Print out history in user centric fashion.
		OUT.write(HTML_FOOTER);
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Produce some summary statistics about the report
	 * @return String containing summary statistics about the report
	 */
	private String header_stats(){
		
			// Reporting the input arguments
		StringBuilder header = new StringBuilder();
		header.append("<H2>INTRODUCTION</H2>\n");
		header.append("This is a <A HREF=\"https://en.wikipedia.org/wiki/" +
				"WP:WikiAudit\">WikiAudit</A> report, generated with " +
				"the following parameters:<BR><BR>\n");
		header.append("  Conn. string:   " + PARENT_AUDIT.API_PATH + "<BR>\n");
		header.append("  Time bound:     " + PARENT_AUDIT.LIMIT_TIME + "<BR>\n");
		header.append("  IP ranges (one per line):<BR>\n");
		for(int i=0; i < PARENT_AUDIT.IP_RANGES.size(); i++)
			header.append("<LI>" + PARENT_AUDIT.IP_RANGES.get(i).toString() + "\n");
		
			//
		header.append("<BR><BR><HR>\n");
		header.append("<H2>INTERPRETING THIS REPORT</H2>\n");
		header.append(DOCUMENTATION);
		
			// Spin once over the history to produce some stats
		List<String> trouble_users = new ArrayList<String>();
		int total_editors = HIST.size();
		int total_edits = 0;
		int reverted_edits = 0;
		int users_with_revert = 0;
		int users_with_talk = 0;
		int users_with_block = 0;
		long num_ips = PARENT_AUDIT.NUM_IPS;
		for(int i=0; i < HIST.size(); i++){
			total_edits += HIST.get(i).EDITS.size();
			reverted_edits += HIST.get(i).REVERTED_EDITS;
			if(HIST.get(i).REVERTED_EDITS > 0)
				users_with_revert++;
			if(HIST.get(i).USER_TALK_PAGE != null){
				users_with_talk++;
				if(HIST.get(i).BLOCK_LAST != null)
					users_with_block++;
				trouble_users.add(HIST.get(i).USER);
			} // Larger condition	
		} // Iterate over user-centric edit history
		
			// Then output aggregate stats
		header.append("<BR><HR>\n");
		header.append("<H2>AGGREGATE STATS</H2>\n");
		header.append("Here we provide some aggregate statistics about " +
				"the report.<BR><BR>\n");		
		header.append("Total IPs in range(s):        " + num_ips + "<BR>\n");
		header.append("  IPs w/1+ contributions:     " + total_editors + "<BR>\n");
		header.append("  IPs with 1+ reverted edits: " + users_with_revert + "<BR>\n");
		header.append("  IPs with a discussion page: " + users_with_talk + "<BR>\n");
		header.append("  IPs with block history:     " + users_with_block + "<BR><BR>\n");
		header.append("Total contributions:          " + total_edits + "<BR>\n");
		header.append("  Contributions reverted:     " + reverted_edits + "<BR>\n");
		header.append("Most problematic users (link goes to raw data):<BR>\n");
		for(int i=0; i < trouble_users.size(); i++){
			header.append("<A HREF=\"#" + trouble_users.get(i) + "\">" + 
					trouble_users.get(i) + "</A>, \n");
		} // Just print a hyperlinked list of problematic users
		
		header.append("<BR><BR><HR>\n");
		header.append("<H2>RAW REPORT DATA</H2>\n");
		return(header.toString());
	}
	
	/**
	 * Producing a text-line for a user (contribution total, talk page
	 * status, current block status).
	 * @param i Index of user in history structure
	 * @return String describing user behavior/activity
	 */
	private String user_stats(int i){
		
		StringBuilder ln = new StringBuilder();
		ln.append("<A NAME=\"" + HIST.get(i).USER + "\"></A>");
		ln.append("<B>" + HIST.get(i).USER + "</B> ");
		ln.append("(<A HREF=\"" + url_contribs(HIST.get(i).USER) + "\">" + 
				HIST.get(i).EDITS.size() + " edits</A>) ");
		
		if(HIST.get(i).USER_TALK_PAGE != null){
			ln.append(" has a <A HREF=\"" + 
					url_page("User_talk:" + HIST.get(i).USER) + "\">talk page</A>");
			if(HIST.get(i).UPAGE_FEATS.size() > 0)
				ln.append(" exhibiting: ");
			if(HIST.get(i).UPAGE_FEATS.contains(UPAGE_PROP.VANDALISM))
				ln.append("<FONT COLOR=\"red\"><B>[VANDALISM WARNING(S)] </B></FONT> ");
			if(HIST.get(i).UPAGE_FEATS.contains(UPAGE_PROP.SPAM))
				ln.append("<FONT COLOR=\"red\"><B>[SPAM WARNINGS] </B></FONT> ");
			if(HIST.get(i).UPAGE_FEATS.contains(UPAGE_PROP.SHARED_IP))
				ln.append("[Shared IP template]  ");	
		} // Some properties derived from talk page
		
		if(HIST.get(i).BLOCK_LAST == null)
			ln.append(" and was not blocked in interval");
		else ln.append(" and was <A HREF=\"" + url_block(HIST.get(i).USER) + "\">" +
					"<FONT COLOR=\"red\"><B>BLOCKED IN INTERVAL</B></FONT></A>");
		ln.append("\n");
		return(ln.toString());
	}
	
	/**
	 * Produce text-lines summarizing all user contributions
	 * @param i Index of user in history structure
	 * @return String describing contributions (one per line)
	 */
	private String contribs_stats(int i){
		
		StringBuilder contribs_ln = new StringBuilder();
		List<pair<metadata,Boolean>> EDITS = HIST.get(i).EDITS;
		Collections.reverse(EDITS); // Should now be most recent first?
		for(int j=0; j < EDITS.size(); j++){	
			if(EDITS.get(j).snd == true)
				contribs_ln.append("  <LI> <FONT COLOR=\"red\"><B> Edited ");
			else contribs_ln.append("  <LI> Edited ");
			
			contribs_ln.append("<A HREF=\"" + url_page(EDITS.get(j).fst.title) + 
					"\">" + EDITS.get(j).fst.title + "</A> ");
			contribs_ln.append(" with <A HREF=\"" + url_diff(EDITS.get(j).fst.rid) + 
					"\">changes (diff)</A> ");
			contribs_ln.append("at time " + 
					stiki_utils.unix_ts_to_wiki(EDITS.get(j).fst.timestamp));
			
			
			if(EDITS.get(j).snd == true)
				contribs_ln.append(" (reverted) </B></FONT>");
			contribs_ln.append("<BR>\n");
		} // Iterate over all user edits
		return(contribs_ln.append("<BR>").toString());
	}
	
	/**
	 * Produce a wiki-link to display a users contributions
	 * @param user Username (or IP) of interest
	 * @return URL displaying user's contributions.
	 */
	private String url_contribs(String user){
		String url = this.PARENT_AUDIT.LINK_PREFIX;
		url += "Special:Contributions/" + user;
		return(url);
	}
	
	/**
	 * Produce a wiki-link to display a particular edit diff.
	 * @param rid Revision-ID of revision of interest
	 * @return URL displaying diff generated by 'rid'
	 */
	private String url_diff(long rid){
		String url = this.PARENT_AUDIT.API_PATH;
		url += "index.php?oldid=" + rid + "&diff=prev";
		return(url);
	}
	
	/**
	 * Produce a wiki-link to the current version of some page
	 * @param page Title of the page of interest
	 * @return URL linking to current version of 'page'
	 */
	private String url_page(String page){
		String url = this.PARENT_AUDIT.LINK_PREFIX;
		url += page;
		return(url);
	}
	
	/**
	 * Produce a wiki-link to examine a user's block history
	 * @param user Username/IP of the individual of interest
	 * @return URL linking to the block history of 'user'
	 */
	private String url_block(String user){
		String url = this.PARENT_AUDIT.API_PATH;
		url += "index.php?title=Special:Log/block&page=User:" + user;
		return(url);
	}
		
}
