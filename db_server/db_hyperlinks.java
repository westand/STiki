package db_server;

import irc_work.irc_output;

import java.sql.PreparedStatement;
import java.util.List;

import core_objects.pair;
import core_objects.stiki_utils;
import edit_processing.rollback_handler.RB_TYPE;

/**
 * Andrew G. West - db_hyperlinks.java -  This class is the database handler
 * for the [hyperlinks] table. If a revision adds a link to some article,
 * then revision-ID and URL added are written to the table. Edits found to
 * be offending (OEs) are flagged -- and we plan to do analysis over this
 * data to see if URLs are a basis on which vandalism can be detected.
 */
public class db_hyperlinks{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL inserting a row into the [hyperlinks] table. 
	 */
	private PreparedStatement pstmt_insert;
	
	/**
	 * SQL flagging a guilty R_ID as such (triggered by OE handlers).
	 */
	private PreparedStatement pstmt_flag_oe;

	/**
	 * Connection to the PreSTA-STiki database (fully privileged).
	 */
	private stiki_con_server con_server;
	
	/**
	 * IRC handler. Link additions are written to a public feed.
	 */
	private irc_output irc_out;
	
	/**
	 * Maximum permitted length of any [URL] or [DESC] in the database. 
	 * Should be checked so as to avoid SQLExceptions.
	 */
	private static final int MAX_STR_LENGTH = 512;
	
	/**
	 * Maximum permitted length of any [USER] in the database.
	 */
	private static final int MAX_USR_LENGTH = 256;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_hyperlinks] object -- connect to DB and prepare SQL.
	 * @param con_server Connection to the PreSTA-STiki database (full privs.)
	 * @pararm irc_out IRC handler to which link additions are written.
	 */
	public db_hyperlinks(stiki_con_server con_server, irc_output irc_out) 
			throws Exception{
		this.con_server = con_server;
		this.irc_out = irc_out;
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Insert hyperlink addition(s) into the database.
	 * @param links List of links added in revision 'rid'. A link is 
	 * represented as a pair of Strings, whose first element is the
	 * URL and the second element is the hypertext description
	 * @param rid Revision-ID which added 'url' to some article
	 * @param pid Page-ID to which 'rid' maps
	 * @param ts Timestamp (UNIX) at which 'rid' was committed
	 * @param user Username of individual who committed 'rid'
	 * @param rid_link_quantity Total number of links added by 'rid'
	 */
	public synchronized void insert_links(List<pair<String,String>> links, 
			long rid, long pid, long ts, String user) throws Exception{
		
			// Note that we do links for an RID in bulk, rather than
			// individually, because the synchronized nature of this class
			// means that shared-RID links will go to IRC *together*.
		
		if(user.length() > MAX_USR_LENGTH) // Watch for column over-runs
			user = user.substring(0, MAX_USR_LENGTH);
		String url, desc;
		for(int i=0; i < links.size(); i++){
	
				// First record the addition to the public IRC feed, so it
				// is immune to any trimming done for DB purposes.
			url = links.get(i).fst;
			desc = links.get(i).snd;
			irc_out.msg(irc_output.CHANNELS.STIKI_LINKS, 
					rid + " " + pid + " " + links.size() + " " + 
					url + " " + desc);
			
				// Trim if needed and insert to DB
			if(url.length() > MAX_STR_LENGTH)
				url = url.substring(0, MAX_STR_LENGTH);
			if(desc.length() > MAX_STR_LENGTH)
				desc = desc.substring(0, MAX_STR_LENGTH);
			pstmt_insert.setLong(1, rid);
			pstmt_insert.setInt(2, 0); // Innocence at insertion
			pstmt_insert.setString(3, url);
			pstmt_insert.setString(4, desc);
			pstmt_insert.setLong(5, ts);
			pstmt_insert.setString(6, user);
			pstmt_insert.executeUpdate();
		} // Make an insertion for all links added
	}
	
	/**
	 * Notify that an RID produced no link additions.
	 * @param rid Revision-ID which added 'url' to some article
	 * @param pid Page-ID to which 'rid' maps
	 */
	public synchronized void notify_no_links(long rid, long pid) 
			throws Exception{
	
			// No need to waste database space with this stuff
			// However, outputting to IRC can help with queuing considerations
		irc_out.msg(irc_output.CHANNELS.STIKI_LINKS, 
				rid + " " + pid + " " + 0);
	}
	
	/**
	 * Mark an edit as offending-one, if it added a hyperlink.
	 * @param rid Revision-ID of edit known to be an offending one
	 * @param rb_type Was the offending edit located by a bot or human?
	 */
	public synchronized void flag_as_oe(long rid, RB_TYPE rb_type) 
			throws Exception{
		
			// For research purposes, we distinguish whether the offending
			// edit was autonomously found (bot) or human-verified.
		if(rb_type.equals(RB_TYPE.BOT))
			pstmt_flag_oe.setInt(1, 2);
		else // if rb_type.equals(RB_TYPE.HUMAN)
			pstmt_flag_oe.setInt(1, 1);
		pstmt_flag_oe.setLong(2, rid);
		pstmt_flag_oe.executeUpdate();
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_insert.close();
		pstmt_flag_oe.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		String insert = "INSERT INTO " + stiki_utils.tbl_links + " ";
		insert += "VALUES (?,?,?,?,?,?)"; // 6 params
		pstmt_insert = con_server.con.prepareStatement(insert);
		
		String flag_oe = "UPDATE " + stiki_utils.tbl_links + " SET RBED=? ";
		flag_oe += "WHERE R_ID=?";
		pstmt_flag_oe = con_server.con.prepareStatement(flag_oe);
	}	
	
}
