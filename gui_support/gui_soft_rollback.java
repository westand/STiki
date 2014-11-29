package gui_support;

import gui_edit_queue.gui_display_pkg;
import gui_panels.gui_login_panel.STIKI_WATCHLIST_OPTS;

import java.io.InputStream;

import mediawiki_api.api_post;
import mediawiki_api.api_post.EDIT_OUTCOME;
import mediawiki_api.api_retrieve;

import core_objects.metadata;

/**
 * Andrew G. West - gui_soft_rollback.java - As opposed to "native" rollback
 * which has straightforward API support, here we encode "software rollback"
 * which mimics native functionality for users without the rollback permission.
 * 
 * Summarily, we are given a particular edit to "undo." That edit will be
 * undone along with all consecutively previous revisions by the same author.
 * If the page has only one author, this proves problematic, and only
 * the most recent contribution will be undone. Further, we install a default
 * search depth. If a single author has made all revisions within that depth,
 * only the most recent contribution will be undone.
 */
public class gui_soft_rollback{
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Perform a 'software rollback' (see class header for definition).
	 * @param edit_pkg Wrapper for the edit/edit-chain being reverted
	 * @param revert_comment Edit summary to associate with any reversion(s)
	 * @param minor Whether or not the edit should be marked as minor
	 * @param edit_token Edit token held on the article which 'meta' wraps
	 * @param session_cookie Cookie identifying editing user
	 * @param watchlist_opt How edits should be watchlisted
 	 * @param assert_user Whether edit should fail if user not logged in
	 * @return Number of edits reverted as part of rollback action. Zero (0)
	 * will be returned if "beaten" to edit. Negative values (< 0) will be
	 * returned in case of error, special value "-2" is reserved for the
	 * "assertuserfailed" error.
	 */
	public static int software_rollback(gui_display_pkg edit_pkg, 
			String revert_comment, boolean minor, String session_cookie, 
			STIKI_WATCHLIST_OPTS watchlist_opt, boolean assert_user) 
			throws Exception{
		
		metadata meta = edit_pkg.page_hist.get(0);
		if(edit_pkg.rb_depth == 1 || 
				edit_pkg.rb_depth == edit_pkg.page_hist.size()){
			
			InputStream in = api_post.edit_revert(meta.rid, meta.title, 
					revert_comment, minor, edit_pkg.get_token(), 
					session_cookie, api_post.convert_wl(watchlist_opt, false), 
					assert_user);
			
			EDIT_OUTCOME undo_success = api_post.edit_was_made(in);
			in.close();
			
			if(undo_success.equals(EDIT_OUTCOME.ASSERT_FAIL)) return -2;
			else if(undo_success.equals(EDIT_OUTCOME.ERROR)) return -1;
			else if(undo_success.equals(EDIT_OUTCOME.BEATEN)) return 0;
			else /*if(undo_success.equals(EDIT_OUTCOME.SUCCESS))*/ return 1;
	
		} else{ // Do the software rollback (fetch and replace content)

			
				// Of all the possible revert/AGF and native vs. software
				// rollback/revert paths, this one is unique becuase it uses
				// "edit full text". All other methods use API functionality 
				// that has some type of constraint checking to ensure an 
				// intermediate edit has not entered the chain. "edit full 
				// text" does not, so we much explicitly do this check here.
			
			long last_rid_on_page = api_retrieve.process_latest_page(meta.pid);
			if(last_rid_on_page != meta.rid)
				return -1;			
			
			InputStream in = api_post.edit_full_text(meta.title,
					revert_comment, api_retrieve.process_page_content(
					edit_pkg.page_hist.get(edit_pkg.rb_depth).rid), minor, 
					edit_pkg.get_token(), session_cookie, 
					false, api_post.convert_wl(watchlist_opt, false), 
					assert_user);
			
			EDIT_OUTCOME undo_success = api_post.edit_was_made(in);
			in.close();
			
			if(undo_success.equals(EDIT_OUTCOME.ASSERT_FAIL)) return -2;
			else if(undo_success.equals(EDIT_OUTCOME.ERROR)) return -1;
			else if(undo_success.equals(EDIT_OUTCOME.BEATEN)) return 0;
			else /*if(undo_success.equals(EDIT_OUTCOME.SUCCESS))*/ 
				return(edit_pkg.rb_depth);
		}
	}
}
