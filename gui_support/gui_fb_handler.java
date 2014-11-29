package gui_support;

import java.util.concurrent.ExecutorService;

import com.mysql.jdbc.CommunicationsException;

import gui_edit_queue.gui_display_pkg;
import gui_panels.gui_login_panel.STIKI_WATCHLIST_OPTS;
import gui_panels.gui_revert_panel;
import core_objects.metadata;
import core_objects.stiki_utils;
import core_objects.stiki_utils.SCORE_SYS;
import db_server.db_off_edits;
import edit_processing.rollback_handler.RB_TYPE;
import executables.stiki_frontend_driver;
import executables.stiki_frontend_driver.FB_TYPE;

/**
 * Andrew G. West - gui_fb_handler.java - When a user selects an edit to be
 * either "vandalism" or "not-vandalism" -- it is passed to this class
 * which: (1) If vandalism, reverts the edit on Wikipedia. (2) Writes the
 * feedback to the [feedback] table (which in turn closes the feedback loop,
 * via a trigger). (3) deletes the RID on the server-side.
 * 
 * CRITICALLY, this class executes all these operations in their own thread
 * (thus the large number of private variables).
 * 
 * Note that all DB-actions are wrapped in try-catch blocks such that the
 * parent class can be notified, and in the connection reset. Then, the
 * class will fetch its freshly-connected counterpart, and re-attempt. The
 * error caught is always indicative of a Connection failure.
 */
public class gui_fb_handler implements Runnable{

	// **************************** PRIVATE FIELDS ***************************
	
	// ******** ALL FB RELEVANT *******
	
	/**
	 * Top GUI class. Must be notified if any DB actions fail. Also provides
	 * accessibility to several shared DB-handlers.
	 */
	private stiki_frontend_driver parent;
	
	/**
	 * Nature of feedback being left.
	 */
	private FB_TYPE fb;
	
	/**
	 * Metadata object associated w/edit for which feedback is being left.
	 */
	private metadata md;
	
	/**
	 * Username of the STiki user who is leaving the feedback.
	 */
	private String user;

	/**
	 * Wrapper around edit to-be-reverted. Much metadata.
	 */
	private gui_display_pkg edit_pkg;
	
	/**
	 * Edit summary which should be left with on-Wikipedia reversions.
	 */
	private String summary;
	
	/**
	 * Session cookie so revert action can be mapped to STiki user.
	 */
	private String session_cookie;
	
	/**
	 * Whether or not the editing user has native rollback permissions.
	 */
	private boolean user_has_native_rb;
	
	/**
	 * Whether or not the rollback option is being used to revert.
	 */
	private boolean rollback;
	
	/**
	 * How to handle watchlisting w.r.t. to articles and warned user.
	 */
	private STIKI_WATCHLIST_OPTS watchlist_opt;
	
	/**
	 * Whether or not a warning should be left on guilty UserTalk page. 
	 */
	private boolean warn;
	
	/**
	 * Customized message to be placed on guilty userTalk page, presumably
	 * in lieu of a warning (designed for AGF cases). 
	 */
	private String usr_talk_msg;
	
	/**
	 * GUI panel which will update revert/warn status once complete.
	 */
	private gui_revert_panel gui_revert_panel;
	
	/**
	 * Executor service, should additional threads be needed to hasten tasks.
	 */
	private ExecutorService threads;

		
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a FB-handler instance. This long form of constructor works
	 * for all FB_TYPE passed. Argument descriptions are same as 
	 * those for the private variables of this class..
	 */
	public gui_fb_handler(stiki_frontend_driver parent, FB_TYPE fb, 
			gui_display_pkg edit_pkg, String user, String summary, 
			String session_cookie, boolean user_has_native_rb, 
			boolean rollback, STIKI_WATCHLIST_OPTS watchlist_opt, boolean warn, 
			String usr_talk_msg, gui_revert_panel gui_revert_panel, 
			ExecutorService threads){
		
		this.parent = parent;
		this.fb = fb;
		this.edit_pkg = edit_pkg;
		this.md = edit_pkg.page_hist.get(0);
		this.user = user;
		this.summary = summary;
		this.session_cookie = session_cookie;
		this.user_has_native_rb = user_has_native_rb;
		this.rollback = rollback;
		this.watchlist_opt = watchlist_opt;
		this.warn = warn;
		this.usr_talk_msg = usr_talk_msg;
		this.gui_revert_panel = gui_revert_panel;
		this.threads = threads;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Execute DB-feedback (and on-Wiki reversion/warn, if needed)
	 */
	public void run(){
		try{if(this.fb.equals(FB_TYPE.INNOCENT))
				this.submit_innocent();
			else if(this.fb.equals(FB_TYPE.PASS))
				this.submit_pass();
			else if(this.fb.equals(FB_TYPE.AGF))
				this.submit_agf();
			else if(this.fb.equals(FB_TYPE.GUILTY))
				this.submit_vandalism();
		} catch(Exception e){}
	}
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Classify an edit as 'innocent' (i.e., not vandalism)
	 */
	private void submit_innocent() throws Exception{
		try{parent.client_interface.feedback_insert( // RID delete internal
					md.rid, fb_constant(fb, edit_pkg.source_queue), user);
		} catch(CommunicationsException e1){
			parent.reset_connection(false);
			submit_innocent(); // If con failure - reconnect - reattempt
		} catch(Exception e2){
			System.out.println("Error internal to \"innocent\" handler");
			e2.printStackTrace();
		} // Catch and/or report errors as appropriate
	}
	
	/**
	 * Do not classify an edit, but instead 'pass' on it -- and ensure that
	 * the passing user will never see the edit in question again. 
	 */
	private void submit_pass() throws Exception{
		try{parent.client_interface.queues.queue_ignore(md.rid, user);
		} catch(CommunicationsException e){
			parent.reset_connection(false);
			submit_pass(); // If con failure - reconnect - reattempt
		} catch(Exception e2){
			System.out.println("Error internal to \"pass\" handler");
			e2.printStackTrace();
		} // Catch and/or report errors as appropriate
	}
	
	/**
	 * Classify an edit as AGF = "good faith revert". 
	 */
	private void submit_agf() throws Exception{
		
			// Being by reverting the edit on Wikipedia (threaded)
			// Should be a straightforward rollback w/o warning
		threads.submit(new gui_revert_and_warn(FB_TYPE.AGF, edit_pkg, summary, 
				session_cookie, user_has_native_rb, rollback, 
				watchlist_opt, warn, usr_talk_msg, gui_revert_panel));
		
		try{parent.client_interface.feedback_insert( // RID delete internal
					md.rid, fb_constant(fb, edit_pkg.source_queue), user);
		} catch(CommunicationsException e){
			parent.reset_connection(false);
			submit_agf(); // If con failure - reconnect - reattempt
		} catch(Exception e2){
			System.out.println("Error internal to \"AGF\" handler");
			e2.printStackTrace();
		} // Catch and/or report errors as appropriate
	}
	
	/**
	 * Classify an edit as vandalism. 
	 */
	private void submit_vandalism() throws Exception{
		
			// Being by reverting the edit on Wikipedia (threaded)
			// If the reversion succeeds, user may also be warned.
			// A panel displays the result/warning to end users
		threads.submit(new gui_revert_and_warn(FB_TYPE.GUILTY, edit_pkg, 
				summary, session_cookie, user_has_native_rb, rollback, 
				watchlist_opt, warn, usr_talk_msg, gui_revert_panel));
		
		try{parent.client_interface.feedback_insert( // RID delete internal
					md.rid, fb_constant(fb, edit_pkg.source_queue), user);
			parent.client_interface.oe_insert(
					md, db_off_edits.FLAG_RID_CLIENT, RB_TYPE.HUMAN);
		} catch(CommunicationsException e){
			parent.reset_connection(false);
			submit_vandalism(); // If con failure - reconnect - reattempt
		} catch(Exception e2){
			System.out.println("Error internal to \"vandalism\" handler");
			e2.printStackTrace();
		} // Catch and/or report errors as appropriate
	}
	
	/**
	 * Produce the 'feedback constant' for a feedback event (to an RID)
	 * @param fb Feedback type (guilty/innocent/pass)
	 * @param source_queue Queue from which the RID being class'ed was pulled
	 * @return Integer code capturing both the scoring queue used (another
	 * code), and the feedback type (the sign of the returned int).
	 */
	private int fb_constant(FB_TYPE fb, SCORE_SYS source_queue){
		
			// The "multiply by 5" AGF hack is not the most elegant
			// but seems reasonable for the time being
			//
			// QUEUE 	INNOCENT	GUILTY		AGF
			// STIKI	-1			1			5
			// CBNG		-2			2			10
			// WT		-3			3			15
			// SPAM		-4			4			20
				
		if(fb.equals(FB_TYPE.INNOCENT)) // innocents made negative
			return(-1 * stiki_utils.queue_to_constant(source_queue));
		else if(fb.equals(FB_TYPE.GUILTY))
			return(stiki_utils.queue_to_constant(source_queue));
		else if(fb.equals(FB_TYPE.AGF))
			return(5 * stiki_utils.queue_to_constant(source_queue));
		else return(0); // generally not used at this tiem
	}

}
