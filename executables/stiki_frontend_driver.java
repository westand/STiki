package executables;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import core_objects.metadata;
import core_objects.pair;
import core_objects.stiki_utils;
import core_objects.stiki_utils.QUEUE_TYPE;
import core_objects.stiki_utils.SCORE_SYS;
import db_client.client_interface;

import gui_edit_queue.edit_queue;
import gui_edit_queue.gui_display_pkg;
import gui_menus.gui_menu_bar;
import gui_panels.gui_button_panel;
import gui_panels.gui_comment_panel;
import gui_panels.gui_diff_panel;
import gui_panels.gui_login_panel;
import gui_panels.gui_metadata_panel;
import gui_panels.gui_revert_panel;
import gui_panels.gui_comment_panel.COMMENT_TAB;
import gui_support.gui_agf_dialogue;
import gui_support.gui_fb_handler;
import gui_support.gui_filesys_images;
import gui_support.gui_globals;
import gui_support.gui_ping_server;
import gui_support.gui_settings;

/**
 * Andrew G. West - stiki_frontend_driver.java - This class is a driver
 * for the STiki vandalism detection GUI (i.e., the front-end). This class
 * launches the GUI and all the backend support it requires. 
 */
@SuppressWarnings("serial")
public class stiki_frontend_driver extends JFrame{
	
	// **************************** PUBLIC FIELDS ****************************

	// *********** ACTIONS ************
	
	/**
	 * Classification types available to end-users.
	 */
	public enum FB_TYPE{INNOCENT, PASS, AGF, GUILTY};
	
	
	// ******* VISUAL ELEMENTS ********
	
	/**
	 * Version ID of the STiki program. A date concatenated in Integer format.
	 * While this should be updated at every distribution, it is only checked
	 * against to see if a forced update is required.
	 */
	public static final int CUR_VERSION = 20141125;
	
	
	// ******* VISUAL ELEMENTS ********
	//
	// Visual components made public, so sub-classes are able to alter
	// the visual GUI appearence without extensive passing
	
	/**
	 * Panel showing the visually-colored edit diffs.
	 */
	public gui_diff_panel diff_browser;
	
	/**
	 * Panel enabling users to log-in to their Wikipedia accounts.
	 */
	public gui_login_panel login_panel;
	
	/**
	 * Panel where edit properties are displayed (article, user, time-stamp).
	 */
	public gui_metadata_panel metadata_panel;
	
	/**
	 * Panel containing the classification buttons (guilty/innocent/pass).
	 */
	public gui_button_panel button_panel;
	
	/**
	 * Panel allowing users to customize revert-summary/warn-policy.
	 */
	public gui_comment_panel comment_panel;
	
	/**
	 * Top-level menu organization (allowing sub-menu access where req'd).
	 */
	public gui_menu_bar menu_bar;
	
	/**
	 * Panel showing status information about the last reversion.
	 */
	public gui_revert_panel revert_panel;
	
	
	// ****** BACKEND SUPPORT ******
	
	/**
	 * Interface handling all client stored-procedure calls to backend.
	 */
	public client_interface client_interface;
	
	/**
	 * Object managing which edits get shown to end users (via [queue]).
	 */
	public edit_queue edit_queue;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Number of threads to use in back-end support. (i.e., non-GUI items such
	 * as writing to database, fetching data from MediaWiki). INCLUDED IN THIS
	 * NUMBER SHOULD ALSO BE THE STATIC ALLOCATION THREADS (i.e., the thread
	 * that pings the DB, and that which keeps RID cache full/maintained).
	 */
	private static final int NUM_NON_GUI_THREADS = 10;
	
	/**
	 * Service dispatching work to available threads.
	 */
	private static ExecutorService WORKER_THREADS; 
	
	/**
	 * Queue "type" used to make the *last* classification. By comparing
	 * this to the *current* classification values, it is possible to
	 * determine if the GUI needs updated to reflect queue/type change.
	 */
	private static QUEUE_TYPE last_q_type;
	
	/**
	 * Structure storing last RID classified, and how it was classified.
	 * Useful if "back" button is used to detect the troublesome 
	 * "INNOCENT -> PASS" case, (the edit was deleted).
	 */
	private static pair<FB_TYPE, Long> last_classification = 
		new pair<FB_TYPE, Long>(FB_TYPE.PASS, 0L);
	
	/**
	 * If we want to warn the user about their classification (e.g. DTTR) and
	 * give them a second chance to review, we use this parameter, which 
	 * should also be used to prohibit tertiary review.
	 */
	private static boolean secondary_review = false;
	
	/**
	 * Tracking use of the "PASS" button to monitor over-use. Note that 
	 * this is a value that will be pulled from persistent settings at
	 * construction, its static nature here prevents calls to settings 
	 * before that file has been loaded. This is not an authoritative 
	 * count! Use the database for that!
	 */
	private static int passes_in_career;

	
	// ***************************** MAIN METHOD *****************************
	
	/**
	 * Driver method. Check network connectivity. If connected, 
	 * launch the STiki GUI. Else shutdown and print error message
	 * @param args No arguments are required by this method
	 */
	public static void main(String[] args) throws Exception{
		
		client_interface ci = new client_interface();
		if(ci.con_client.con != null){ 
			if(req_version(ci))
				new stiki_frontend_driver(ci);
			else System.exit(1);
		} else{ // If connectivity, launch the GUI 
			JFrame frame = new JFrame();
			frame.setIconImage(gui_filesys_images.ICON_64);
			JOptionPane.showMessageDialog(frame,
					"Unable to connect to the STiki back-end:\n" +
					"This is likely the result of one of four things:\n\n" +
					"(1) You are not connected to the Internet.\n\n" +
					"(2) Port 3306 is not open (MySQL), due to a\n" +
					"    firewall or your network's admin. settings\n\n" +
					"(3) The STiki server is down. Check [[WP:STiki]]\n\n" +
					"(4) A required software upgrade has been issued,\n" +
					"    breaking this version. See [[WP:STiki]]\n\n",
	       		    "Error: Backend connection is required",
	       		    JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} // Else open invisible frame, pop error message, and exit
	}
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [stiki_frontend_driver] -- Intializing all visual 
	 * components, and making them visible to the end user.
	 * @param ci Connection handling all client requests to STiki server
	 */
	public stiki_frontend_driver(client_interface ci) throws Exception{
		
			// Import persistent settings from file	
			// This MUST be the first thing to happen
			// Then we can use those settings freely
		gui_settings.set_parent(this);
		gui_settings.load_properties();
		passes_in_career = gui_settings.get_int_def(
				gui_settings.SETTINGS_INT.passes_used, 0);
		
			// Initialize communication to back-end DB (client version)
		this.client_interface = ci;
		
			// Prepare work threads, go ahead and put static ones to work
		SCORE_SYS default_queue = client_interface.default_queue();
		QUEUE_TYPE default_type = stiki_utils.queue_to_type(default_queue);
		last_q_type = default_type;
		WORKER_THREADS = Executors.newFixedThreadPool(NUM_NON_GUI_THREADS);
		WORKER_THREADS.submit(new gui_ping_server(this));
		edit_queue = new edit_queue(this, WORKER_THREADS, 
				client_interface, default_queue);
		
			// NOW TO FOCUS ON THE VISUAL COMPONENTS:
			//
			// Initialize all main-window components 
		button_panel = new gui_button_panel(this, default_type);
	    metadata_panel = new gui_metadata_panel();
	    revert_panel = new gui_revert_panel(this);
	    login_panel = new gui_login_panel(this);
	    comment_panel = new gui_comment_panel(default_type);
		diff_browser = new gui_diff_panel();
		menu_bar = new gui_menu_bar(this, default_queue);
		this.setJMenuBar(menu_bar); // Menu must be last
		
			// Then populate the center-panel
		JPanel center_panel = new JPanel(new BorderLayout(0,0));
		diff_browser.setBorder(gui_globals.
				produce_titled_border("DIFF-Browser"));
		center_panel.add(diff_browser, BorderLayout.CENTER);
		center_panel.add(initialize_bottom_panel(), BorderLayout.SOUTH);
		
			// Layout the components in the larger frame
		this.getContentPane().add(create_left_panel(), BorderLayout.WEST);
	    this.getContentPane().add(center_panel, BorderLayout.CENTER);
	    this.window_size_position();
	    
	    	// Set frame properties and make visible
	    this.setTitle("STiki: A Vandalism Detection Tool for Wikipedia");
	    this.setIconImage(gui_filesys_images.ICON_64);
	    this.setVisible(true);
	    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	    this.addWindowListener(get_exit_handler());
	    this.advance_revision(false); // Initialize the window with an edit
	}
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Exit the STiki GUI, gracefully closing all components/connections.
	 * @param system_shutdown If TRUE, then 'System.exit()' will be fired,
	 * effectively terminating any code. If FALSE, the GUI will just be
	 * made invisible, permitting other code to run.
	 */
	public void exit_handler(boolean system_shutdown){
		
		gui_settings.save_properties(); // Persistent user settings
		
		try{ // Shutdown cleanly terminates all DB cons/structs
			WORKER_THREADS.shutdown();
			edit_queue.shutdown();
			client_interface.shutdown();
			WORKER_THREADS.shutdownNow(); // Kill infinite loops
		} catch(Exception e){
			System.out.println("Error during STiki shutdown:");
			e.printStackTrace();
		} // Try-catch required for interface compliance
		 
		if(system_shutdown)
			System.exit(0); // Hard shut-down
		else this.setVisible(false); // Soft shut-down
	}
	
	/**
	 * In the event that a DB call fails due to a broken pipe or unforeseen
	 * CommunicationsException, this method can be called in order to 
	 * obtain a new connection, notify the user, and avoid a hard-crash.
	 * @param show_message Whether a notification dialog should be popped,
	 * asumming the reset is succesful. If reset fails; a dialog will be shown
	 * regardless, and the STiki GUI will be closed.
	 */
	public void reset_connection(boolean show_message) 
			throws Exception{
		this.client_interface = new client_interface();
		if(this.client_interface.con_client.con == null){
			JOptionPane.showMessageDialog(this,
					"Unable to connect to the STiki back-end:\n" +
	       		 	"The program will now exit. Try to restart STiki.\n" +
	       		 	"More information will be given if that fails.",
	       		    "Error: Back-end connectivity is required",
	       		    JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} else if(show_message) 
			JOptionPane.showMessageDialog(this,
					"Your connection to the STiki database was\n" +
					"lost. A new connection has been obtained.",
	       		    "Warning: Connection was reset",
	       		    JOptionPane.WARNING_MESSAGE);
	}
	
	/**
	 * Map a user-classification to DB and on-Wikipedia events.
	 * @param fb_type Type of feedback being left
	 */
	public void class_action(FB_TYPE fb_type) throws Exception{

		if(!login_panel.is_state_stable()){ 
			JOptionPane.showMessageDialog(this,
					"To classify using STiki, you must log-in.",
	       		    "Warning: Must login",
	       		    JOptionPane.WARNING_MESSAGE);
			return;
		} // Confirm that the login-panel has an established editing user, if
		  // not, ask user to do so before proceeding.
		
		if(fb_type.equals(FB_TYPE.PASS)){
			passes_in_career++;
			if(gui_globals.set_pass_warn_points().contains(passes_in_career))
				gui_globals.pop_overused_pass_warning(
						this.diff_browser, passes_in_career);
		} // Monitor over-use of the "PASS" button. This is done at several
		  // points in user history; less frequently for long-term users
		
		gui_display_pkg edit_pkg = edit_queue.get_cur_edit();
		metadata md = edit_pkg.page_hist.get(0); // convenience
		if((fb_type.equals(FB_TYPE.AGF) || fb_type.equals(FB_TYPE.GUILTY)) && 
				menu_bar.get_options_menu().get_dttr_policy() && 
				!secondary_review && edit_pkg.get_user_edit_count() >= 50){
			gui_globals.pop_dttr_warning(this.diff_browser);
			secondary_review = true;
			return; // give a second chance at review
		} // Here is the logic to ensure warning and retry is possible to
		  // avoid "templating regulars"; assuming that option is enabled.
		secondary_review = false;
		
		boolean login_change = login_panel.check_and_reset_state_change();
		if((fb_type.equals(FB_TYPE.GUILTY) || fb_type.equals(FB_TYPE.AGF))  
				&& login_change){
			edit_queue.refresh_edit_token(login_panel.get_session_cookie());
			if(login_panel.editor_using_native_rb())
				edit_queue.refresh_rb_token(login_panel.get_session_cookie());
		}	// If editing user has changed internal to this edit, and
			// we are going to revert/rollback, then tokens need renewed.
			// An edit token is ALWAYS needed (for placing user-talk warning)
			
		if(last_classification.snd == md.rid && fb_type.equals(FB_TYPE.PASS) &&
				last_classification.fst.equals(FB_TYPE.INNOCENT)){
			client_interface.queues.queue_resurrect(md.rid, md.pid);
		} // If above criteria are met, we have an INNOCENT->PASS class
		  // change via "back", the lone problematic case.
		
			// This sanity check should be unnecessary -- but somewhere (cache
			// clearing?) -- missing tokens aren't getting populated
		if(edit_pkg.get_token() == null)
			edit_pkg.refresh_edit_token(login_panel.get_session_cookie());
		if(login_panel.editor_using_native_rb() && md.rb_token == null)
			edit_pkg.refresh_rb_token(login_panel.get_session_cookie());
		
			// Provide data necessary to provide feedback, wrap as a threaded
			// object; certain parts dependent on feedback nature
		COMMENT_TAB ct;
		String usr_talk_msg = null;
		gui_fb_handler fb_task;
		if(fb_type.equals(FB_TYPE.GUILTY) && stiki_utils.queue_to_type(
				edit_pkg.source_queue).equals(QUEUE_TYPE.VANDALISM))
			ct = COMMENT_TAB.VAND;
		else if(fb_type.equals(FB_TYPE.GUILTY) && stiki_utils.queue_to_type(
				edit_pkg.source_queue).equals(QUEUE_TYPE.LINK_SPAM))
			ct = COMMENT_TAB.SPAM;
		else if(fb_type.equals(FB_TYPE.AGF)){
			ct = COMMENT_TAB.AGF;
			if(menu_bar.get_options_menu().get_agf_comment_policy()){
				usr_talk_msg = new gui_agf_dialogue(this).get_result(edit_pkg);
				if(usr_talk_msg.equals(gui_agf_dialogue.ABANDON_MSG))
					return; // If user abandons the revert in-dialogue
			} // Users can choose whether or not to customize AGF messages
		} else ct = COMMENT_TAB.VOID;		
		fb_task = new gui_fb_handler(this, fb_type, edit_pkg,
				login_panel.get_editing_user(),
				comment_panel.get_comment(edit_pkg, ct), 
				login_panel.get_session_cookie(),
				login_panel.editor_has_native_rb(),
				login_panel.rb_checkbox_selected(),
				login_panel.watchlist_combobox(),
				comment_panel.get_warn_status(ct),
				usr_talk_msg,
				revert_panel, WORKER_THREADS);
		WORKER_THREADS.submit(fb_task); // GIVE THE FB-TASK TO A THREAD
		
			// Configure functionality of the "back" button
		if(fb_type.equals(FB_TYPE.GUILTY) || fb_type.equals(FB_TYPE.AGF)) 
			this.button_panel.back_button_enabled(false); // "back" button
		else this.button_panel.back_button_enabled(true);
		last_classification = new pair<FB_TYPE, Long>( // Store result
				fb_type, edit_queue.get_cur_edit().metadata.rid);

		this.advance_revision(false); // ALL CLASSIFICATIONS; advance RID
	}
	
	/**
	 * Take necessary action when the back button is pressed (change 
	 * displayed revision, dis-able some buttons, etc.)
	 */
	public void back_button_pressed() throws Exception{
		this.advance_revision(true);
		this.button_panel.back_button_enabled(false);
	}
	
	/**
	 * Accessor method to the "passes_in_career" variable.
	 * @return The number of passes in the career of the STiki user. Note 
	 * this value is initialized by a call to persistent settings and then
	 * incremented over the course of each user session.
	 */
	public int get_passes_in_career(){
		return(passes_in_career);
	}

	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Determine if the current version of STiki meets minimum version reqs. 
	 * @param ci Client connection to the central STiki database, where
	 * the minimum required version is notated.
	 * @return TRUE if the current version meets minimum requirements. 
	 * Otherwise return FALSE.
	 */
	private static boolean req_version(client_interface ci) throws Exception{
		
		int min_version = ci.req_version();
		if(min_version <= CUR_VERSION)
			return(true);
		else{
			JFrame frame = new JFrame();
			frame.setIconImage(gui_filesys_images.ICON_64);
			JOptionPane.showMessageDialog(frame,
					"An updated version of STiki is required!\n\n" +
					"Your current version is insufficient. Most likely\n" +
					"this is because the WMF has made changes that\n" +
					"fundamentally broke your current version. A new  \n" +
					"version is available at [[WP:STiki]], and more\n" +
					"information should be on the talk page.\n\n" +
					"The software will now exit.\n\n",
	       		    "Error: Software update required",
	       		    JOptionPane.ERROR_MESSAGE);
			return(false);
		} // error message if version is insufficient
	
	}
	
	/**
	 * Move visual components forward to the next revision.
	 * @param previous If TRUE, instruct the queue to re-show the previous
	 * edit, rather than advancing onward (the FALSE case).
	 */
	private void advance_revision(boolean previous) throws Exception{
		
			// Possible transition to new GUI setup (vandalism, spam, etc.)
		switch_mode_if_needed();
		
			// Fetch new edit, and display it in GUI		
		edit_queue.next_rid(login_panel.get_editing_user(), 
				login_panel.get_session_cookie(), 
				login_panel.editor_using_native_rb(), 
				menu_bar.selected_queue(), previous, false);
		diff_browser.display_content(edit_queue.get_cur_edit());
		metadata_panel.set_displayed_rid(edit_queue.get_cur_edit());
	}
	
	/**
	 * If the GUI "mode/type" (spam, vandalism, etc.) needs changed, 
	 * have the visual elements of the GUI make that change
	 */
	private void switch_mode_if_needed(){
		QUEUE_TYPE cur_type = menu_bar.selected_type();
		if(cur_type != last_q_type){
			button_panel.change_type_setup(cur_type);
			comment_panel.change_queue_type(cur_type);
			last_q_type = cur_type;		
			if(cur_type == QUEUE_TYPE.LINK_SPAM)
				menu_bar.get_options_menu().set_hyperlink_policy(true);
		} // Handle broad, then queue-specific change elements
	}
	
	/**
	 * Initialize the left-sidebar of the GUI
	 */
	private JPanel create_left_panel(){
		JPanel left_sidepanel = new JPanel();
		left_sidepanel.setLayout(new BoxLayout(left_sidepanel, 
				BoxLayout.Y_AXIS));
		
			// Add borders to beautify the major panels
		login_panel.setBorder(gui_globals.
				produce_titled_border("Login Panel"));
		button_panel.setBorder(gui_globals.
				produce_titled_border("Classification"));
		comment_panel.setBorder(gui_globals.
				produce_titled_border("Comments"));
		
			// Add the components to the main panel
		left_sidepanel.add(gui_globals.center_comp_with_glue(login_panel));
		left_sidepanel.add(Box.createVerticalGlue());
		left_sidepanel.add(gui_globals.center_comp_with_glue(button_panel));
		left_sidepanel.add(Box.createVerticalGlue());
		left_sidepanel.add(gui_globals.center_comp_with_glue(comment_panel));
		
			// CRITICAL: Set properties about the layout
			// Recall, BorderLayout ignores max/min size settings,
			// instead respects only preferred ones
		left_sidepanel.setPreferredSize(new Dimension(
				gui_globals.LEFT_SIDEBAR_WIDTH, Integer.MAX_VALUE));
		return(left_sidepanel);
	}
	
	/**
	 * Intialize the GUI bottom panel (metadata and revert-data sections).
	 * @return JPanel object which composes the "bottom panel"
	 */
	private JPanel initialize_bottom_panel(){
		
			// Beautify components going in bottom-panel
		metadata_panel.setBorder(gui_globals.
				produce_titled_border("Edit Properties"));
		revert_panel.setBorder(gui_globals.
				produce_titled_border("Last Revert"));
		
			// Straightforward layout
		JPanel bottom_panel = new JPanel(new BorderLayout(0,0));
		bottom_panel.add(revert_panel, BorderLayout.WEST);
		bottom_panel.add(metadata_panel, BorderLayout.CENTER);
		return(bottom_panel);
	}
	
	/**
	 * Set the size and position of the STiki window. We attempt to do this
	 * using persistent settings from file, but also have a default option.
	 */
	private void window_size_position(){
	    int win_width = gui_settings.get_int_def(
	    		gui_settings.SETTINGS_INT.win_width, Integer.MIN_VALUE);
	    int win_height = gui_settings.get_int_def(
	    		gui_settings.SETTINGS_INT.win_height, Integer.MIN_VALUE);
	    int win_locx = gui_settings.get_int_def(
	    		gui_settings.SETTINGS_INT.win_locx, Integer.MIN_VALUE);
	    int win_locy = gui_settings.get_int_def(
	    		gui_settings.SETTINGS_INT.win_loxy, Integer.MIN_VALUE);

	    if(win_locy == Integer.MIN_VALUE || win_locx == Integer.MIN_VALUE || 
	    		win_height == Integer.MIN_VALUE || 
	    		win_width == Integer.MIN_VALUE){
			Dimension screen_size = this.getToolkit().getScreenSize();
		    win_width = (screen_size.width * 8 / 10);
		    win_height = (screen_size.height * 8 / 10);
		    win_locx = ((screen_size.width - win_width) / 2);
		    win_locy = ((screen_size.height - win_height) / 2);
	    } // If that fails, use some default settings
	    this.setBounds(win_locx, win_locy, win_width, win_height); // Do it!
	}
	
	/**
	 * Return the event-handler for when the main frame is exited 
	 * @return An event-handler (to be added as listener) for the exit process.
	 */
	private WindowAdapter get_exit_handler(){
		 WindowAdapter win_close = new WindowAdapter(){
			 public void windowClosing(WindowEvent w){
				 exit_handler(true);
			 }  // Smoothly shut-down all DB structs/connections
		}; // An anonymous class implements the only WindowEvent we care about
		return(win_close);
	}
	
}
