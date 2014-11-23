package utilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import mediawiki_api.api_post;
import mediawiki_api.api_retrieve;

import core_objects.pair;
import core_objects.stiki_utils;

import db_server.stiki_con_server;

/**
 * Andrew G. West - mailer.java - This is a class used for the mass-posting
 * of a text to Wikipedia pages. Presumably, it will be used to append a
 * message to the talk page of multiple Wikipedia users. This has the 
 * potential to be spammy, and the number of recipients must be well-chosen
 * and minimized to avoid issues. 
 */
public class mailer{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * The user account from which messages should be sent.
	 */
	private static final String SEND_USER = "blanked";
	
	/**
	 * Password associated with the 'SEND_USER" account.
	 */
	private static final String SEND_PASS = "blanked";
	
	/**
	 * Message to be appended to pages. Note that this "raw" form may
	 * contain some placeholders for personalization.
	 */
	private static final String MESSAGE = 
			"\n== STiki: A new version and a thank you! ==\n" +
			"Greetings #u#. As the developer of the [[WP:STiki|STiki]] " +
			"anti-vandal tool, I would like to thank you for recent " +
			"and non-trivial use of my software. Whether you just " +
			"tried out the tool briefly or have been a long-term " +
			"participant, I appreciate your efforts (as I am sure " +
			"does the entire Wikipedia community)! " +
			"\n\n" +
			"I write to inform you of a [[Wikipedia_talk:" +
			"STiki#CHANGELOG_for_2012-04-11_STiki_Release.21.21|" +
			"new version of the software]] (link goes to list of " +
			"new features). This version addresses multiple long-term " +
			"issues that I am happy to put behind us. " +
			"Try it out! Provide some feedback!" +
			"\n\n" +
			"The STiki project is also always seeking collaborators. " +
			"In particular, we are seeking non-technical colleagues. " +
			"Tasks like publicity, talk-page maintenance, " +
			"[[Wikipedia:BANNER|advertisement]], and barn-star " +
			"distribution are a burden to technical development. " +
			"If you are interested, write me at " +
			"[[User_talk:West.andrew.g|my talk page]] or " +
			"[[Wikipedia_talk:STiki|STiki's talk page]]." +
			"\n\n" +
			"As STiki approaches two significant thresholds: " +
			"(1) 100,000 revert actions and (2) 400 unique users -- " +
			"I hope to have your support in continuing the efficient" +
			" fight against unconstructive editing. Thanks, ~~~~";
	
	/**
	 * Edit comment to be associated with message post.
	 */
	private static final String COMMENT = "Notification of new [[WP:STiki]] " +
			"version for a frequent/recent user";
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Run the mailer script; send messages
	 * @param args No arguments are taken by this method
	 */
	public static void main(String[] args) throws Exception{
	
		pair<String,String> edit_token;
		String cookie = api_post.process_login(SEND_USER, SEND_PASS);
		List<String> pages = pages_to_target();
		for(int i=0; i < pages.size(); i++){
			edit_token = api_retrieve.process_edit_token(pages.get(i), cookie);
			api_post.edit_append_text(pages.get(i), COMMENT, MESSAGE.replace(
					"#u#", pages.get(i).replace("User_talk:", "")), 
					false, edit_token, cookie, false, true, true);
			System.out.println("Message posted to: " + pages.get(i));
			Thread.sleep(1000 * 45);
		}  // iterate over all users to be targeted
		api_post.process_logout();
	}

	
	// **************************** PRIVATE METHODS **************************
	
	/**
	 * Return a list of pages to target. This version uses the STiki 
	 * database to locate users which may quality for STiki 'notices'.
	 * @return list containing those pages to which 'MESSAGE' should be posted.
	 */
	private static List<String> pages_to_target() throws Exception{

			// Setup database query
		stiki_con_server db = new stiki_con_server();
		PreparedStatement pstmt = db.con.prepareStatement(
				"SELECT USER_FB,COUNT(*) FROM feedback " +
				"WHERE TS_FB>? AND USER_FB!=\"west.andrew.g\"" +
				"GROUP BY USER_FB " +
				"HAVING COUNT(*) > ? " +
				"ORDER BY COUNT(*) DESC"); 
		
			// Parameters for user selection:
			// 1: Historical window for evaluation (1 month in this case)
			// 2: Minimum number of edits made in window (100+ here)
		pstmt.setLong(1, (stiki_utils.cur_unix_time() - (60*60*24*30)));
		pstmt.setInt(2, 100);
		
		String username;
		List<String> pages = new ArrayList<String>();
		ResultSet rs = pstmt.executeQuery();
		while(rs.next()){
			username = rs.getString(1);
			pages.add("User_talk:" + username);
		} // Simply iterate over all qualifying users
		
			// Clean-up
		rs.close();
		pstmt.close();
		db.shutdown();
		return(pages);
	}
	
}
