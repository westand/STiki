package utilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import mediawiki_api.api_post;
import mediawiki_api.api_retrieve;
import core_objects.metadata;
import core_objects.pair;
import db_server.stiki_con_server;

/**
 * Andrew G. West - tagger.java - Script for the ex post facto tagging
 * of edits made by STiki. As of 2018-APR, new edits will be tagged
 * automatically by STiki code. Back-tagging proves trickier.
 * 
 * First, STiki is primarily concerned with the RIDs under review. 
 * We have those IDs and how a user labeled them (innocent/vandalism/AGF).
 * STiki does not record the RIDs produced by the subsequent reverts/warnings.
 * 
 * Instead, we will look at the metadata of the several edits that follow
 * any RID a STiki user labeled as "vandalism/spam/agf". If any of the
 * comments mention "STiki", we will tag it. This is an imperfect and 
 * brittle processs, but should get us 99.9% of the way there. No 
 * attempt will be made to tag edits corresponding to AIV/warning posts.
 * 
 * As old versions of the STiki executable will continue not to apply the
 * tag automatically, we may need to re-run this code periodically to 
 * include those contributions in the tagging process.
 */
public class tagger{

	// **************************** PRIVATE FIELDS ***************************
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Driver method; tag some edits
	 * @param args No arguments are taken by this method
	 */
	public static void main(String[] args) throws Exception{
	
			// GET RIDS WHICH MAY HAVE TRIGGERED A REVERT/ROLLBACK
		
		String sql = "SELECT R_ID, USER_FB FROM feedback "
				+ "WHERE LABEL > 0 " // innocent are negative
				+ "AND TS_FB > 0 " // helpful with incremental updates
				+ "ORDER BY TS_FB ASC";
		stiki_con_server DB = new stiki_con_server();
		PreparedStatement pstmt_revert_rids = DB.con.prepareStatement(sql);
		
		ResultSet rs = pstmt_revert_rids.executeQuery();
		List<pair<Long,String>> revert_rids = new ArrayList<pair<Long,String>>();
		while(rs.next())
			revert_rids.add(new pair<Long,String>(rs.getLong(1), rs.getString(2)));
		System.out.println(revert_rids.size() + " RIDs to search");
		
		rs.close();
		pstmt_revert_rids.close();
		DB.shutdown();
		
		
			// LOOK AHEAD TO FIND/TAG THE (POSSIBLY) STIKI EDIT
		
		String user = "BLANK"; // ** DON'T FORGET TO BLANK **
		String pass = "BLANK";
		pair<String,String> edit_token;
		api_post.process_login(user, pass);
		
		int errors = 0;
		int matches = 0; 
		
		metadata guilty_meta; 
		List<metadata> following_meta;
		for(int i=0; i < revert_rids.size(); i++){
	
			try{guilty_meta = api_retrieve.process_basic_rid(
						revert_rids.get(i).fst);
				following_meta = api_retrieve.process_page_next_meta(
						guilty_meta.pid, guilty_meta.rid, 5, null);
				for(int j=0; j < following_meta.size(); j++){
					if(following_meta.get(j).comment.toLowerCase().contains("stiki")){
						edit_token = api_retrieve.process_edit_token(
								following_meta.get(j).pid);
						api_post.tag_rid(following_meta.get(j).rid, 
								api_post.TAGS.STiki.toString(), 
								edit_token);
						matches++; 
						break;
					} // is comment evidence enough of STiki use?
				} // consider 5 edits after a flagged RID
			} catch(Exception e){errors++;};
			
			if(i % 100 == 0){
				System.out.println(i + " processeed "
						+ "with " + matches + " matches "
						+ "and " + errors + " errors; "
						+ "last RID was " + revert_rids.get(i).fst);
			} // progress reporting
				
		} // consider all RIDs ever flagged in STiki 
		
		api_post.process_logout();	
	}
	
}
