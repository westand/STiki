package gui_panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import core_objects.stiki_utils;
import executables.stiki_frontend_driver;

import gui_support.gui_globals;
import gui_support.gui_settings;
import gui_support.url_browse;

import mediawiki_api.api_post;
import mediawiki_api.api_retrieve;
import mediawiki_api.api_xml_user_perm;

/**
 * Andrew G. West - gui_login_panel.java - This class implements the
 * "login panel" -- where a user can login to his/her Wikipedia account,
 * so the reversions they make are associated with them -- or they can
 * choose to remain anonymous in doing so. This class handles both the
 * visual elements and the actions on top of them.
 * 
 * Perhaps most crucially, this module also stores the "session_cookie"
 * returned by Wikipedia at login. A user must attach this to HTTP POST/GET 
 * requests -- so that their actions map back to their account.
 */
@SuppressWarnings("serial")
public class gui_login_panel extends JPanel implements ActionListener{

	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Wikipedia administration take issues with the fact we enabled
	 * IP users to make edits (w/o CAPTCHA or other slowdowns). Thus, we 
	 * are going to compile w/o the anonymous user option until the issue
	 * has been discussed and approved by Wikipedia administration.
	 */
	public static final boolean ANON_ENABLED = false;
	
	/**
	 * Whether or not the anonymous login functionality should be visible,
	 * assuming it is disabled.
	 */
	public static final boolean ANON_VISIBLE = false;
	
	/**
	 * If TRUE, then editors have the decision whether or not the rollback
	 * action will be used. If TRUE, rollback will be assumed and the
	 * option will not be user-facing.
	 */
	public static final boolean ROLLBACK_OPTION = false;
	
	/**
	 * Whether rollback permission is required to use the STIki tool.
	 */
	public static final boolean ROLLBACK_REQUIRED = false;
	
	/**
	 * Options for watchlist behavior. This should have a 1-to-1 mapping
	 * onto the more user friendly presentations of [WATCHLIST_OPTIONS_STR].
	 */
	public static enum STIKI_WATCHLIST_OPTS{
		NEVER, 
		ONLY_ARTICLES, 
		ONLY_USERTALK, 
		WATCH_BOTH, 
		USER_PREFS};
	
	/**
	 * Options for watchlist behavior, as presented to the user.
	 */
	public static final String[] STIKI_WATCHLIST_OPTS_STR = 
		{"Never watchlist", 
		"Watch reverted articles",
		"Watch user talk if warned",
		"Watch article and user talk",
		"Use account preferences"};
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * The user-name or IP address of the editor using the STiki-frontend.
	 * May be null if log-in choice is yet to be made.
	 */
	private String editing_user;
	
	/**
	 * Whether or not the 'editing_user' has the 'rollback' permission.
	 */
	private boolean editor_has_native_rb = false;
	
	/**
	 * Speaks to the `stability' of the current log-in state. This variable
	 * is TRUE if a user is logged-in or editing as an anonymous user. If
	 * a user has stated their intention to log-in, but not yet successfully
	 * done so, then this variable will be FALSE.
	 */
	private boolean is_state_stable;
	
	/**
	 * Variable speakin to whether the login state (i.e., the user editing) has 
	 * changed since last access to this variable. A state change internal to 
	 * a RID view signals the need to renew the edit/rollback token.
	 */
	private boolean state_changed;
	
	/**
	 * Checkbox so a user may indicate if they wish to edit anonymously.
	 */
	private JCheckBox anon_checkbox;
	
	/**
	 * Link adjacent to anonymous CB, which will pop-up a dialog containing
	 * more information about anonymous user editing (if enabled).
	 */
	private JButton anon_link;
	
	/**
	 * Text field where the user-name should be entered (if logging in).
	 */
	private JTextField field_user;
	
	/**
	 * Text field where the password should be entered (if logging in). 
	 */
	private JPasswordField field_pass;
	
	/**
	 * "Login" button -- initiating login per the 'user' and 'password' fields.
	 */
	private JButton button_login;
	
	/**
	 * "Logout" button -- ending a login session for logged-in users. 
	 */
	private JButton button_logout;
	
	/**
	 * Status label announcing the success/failure of the login process, 
	 * as well as the current editing mode ("logged in as... {IP, user}").
	 */
	private JLabel label_status;

	/**
	 * Combo box which contains various watchlist options.
	 */
	private JComboBox<String> watchlist_combobox;
	
	/**
	 * Checkbox to indicate if rollback should be used for reverts. This is
	 * an optional part of the GUI setup.
	 */
	private JCheckBox rollback_checkbox;
	
	/**
	 * Parent class; For using client-interface to DB for permissions check
	 */
	private stiki_frontend_driver parent;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Create a [gui_login_panel] object, positioning the visual components,
	 * and initializing the editing user to be "anonymous".
	 * @param parent Parent class (the frontend executable) 
	 */
	public gui_login_panel(stiki_frontend_driver parent) throws Exception{
		
			// Basic initaliztion
		this.parent = parent;
		
			// Set the component alignment (vertical boxes)
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
			// Add the components to the panel layout
		if(ANON_VISIBLE){
			this.add(Box.createVerticalGlue());
			this.add(get_checkbox_subpanel());
		} // If wanting to make anonymous login invisible
		this.add(Box.createVerticalGlue());
		this.add(get_field_subpanel());
		this.add(Box.createVerticalGlue());
		this.add(get_button_subpanel());
		this.add(Box.createVerticalGlue());
		this.add(get_status_subpanel());	
		this.add(Box.createVerticalGlue());
		if(ROLLBACK_OPTION)
			this.add(get_rb_subpanel());
		else get_rb_subpanel(); // Initialize but don't show; variables within
		this.add(get_watchlist_subpanel());
		this.add(Box.createVerticalGlue());
		
			// Set initial state of the login page (and potentially 
			// disable the use of anonymous (IP) editing)
		if(ANON_ENABLED){
			this.anon_checkbox.setSelected(true);
			this.anonymous_checked();
		} else if(ANON_VISIBLE){
			this.anon_checkbox.setEnabled(false);
			this.anonymous_unchecked();
		} else{
			this.anonymous_unchecked();
		}
	}
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Interface compliance: Take an action completed on a visual GUI
	 * element, and perform the expected model/view behavior.
	 */
	public void actionPerformed(ActionEvent event){
		try{
			if(event.getSource().equals(this.anon_checkbox)){
				if(this.anon_checkbox.isSelected())
					this.anonymous_checked();
				else // if(this.anon_checkbox.isSelected())
					this.anonymous_unchecked();
			} else if(event.getSource().equals(this.button_login))
				this.login_clicked();
			else if(event.getSource().equals(this.button_logout))
				this.logout_clicked();
			else if(event.getSource().equals(this.anon_link))
				this.anon_dialog();
			else if(event.getSource().equals(this.rollback_checkbox))
				this.state_changed = true;
		} catch(Exception e){
		
				// Generic error message should anything go wrong
			JOptionPane.showMessageDialog(this,
	       		      "Error in the user login interface,\n" +
	       		      "likely caused by network error \n" + e.getMessage(),
	       		      "Error: Problem in log-in pane",
	       		      JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			
		} // Map action events to their handlers
	}
	
	/**
	 * Determine if the login state (i.e., the user editing) has changed 
	 * since this method was last called (the method also resets the var).
	 * @return TRUE if the editing user has changed since the last time
	 * this method was called, and FALSE otherwise.
	 */
	public boolean check_and_reset_state_change(){
		boolean changed = this.state_changed;
		this.state_changed = false;
		return(changed);
	}

	/**
	 * Return the 'stability' of the login.
	 * @return TRUE if the login-status is such that the editor is prepared
	 * to edit as an anonymous or logged-in user. FALSE if the user has 
	 * indicated they wish to edit as a logged-in user, but is yet to provide
	 * the necessary credentials in order to do so.
	 */
	public boolean is_state_stable(){
		return (this.is_state_stable);
	}
	
	/**
	 * Return the user-name or IP of edit currently using STiki.
	 * @return User-name or IP of edit currently using STiki
	 */
	public String get_editing_user(){
		
			// Fix 1/8/2013. People adding leading whitespace at login 
			// has screwed up leaderboard compilation; thus trim.
		if(this.editing_user == null)
			return(null);
		else return (this.editing_user.trim());
	}
	
	/**
	 * Return the contents of the "username" field. This is unique from 
	 * "get_editing_user()" which will return NULL if the field contains
	 * data but is not legitimately logged in. This method returns the
	 * field contents regardless of login status
	 * @return Contents of the "username" field of this panel
	 */
	public String get_login_user_field(){
		return (this.field_user.getText());
	}
	
	/**
	 * Return whether or not the editing user has rollback permissions.
	 * @return TRUE if the editing user has rollback rights; FALSE, otherwise
	 */
	public boolean editor_has_native_rb(){
		return (this.editor_has_native_rb);
	}
	
	/**
	 * Examine whether the "rollback" checkbox is selected or not.
	 * @return TRUE if the "rollback" checkbox is selected. FALSE, otherwise.
	 */
	public boolean rb_checkbox_selected(){
		return (this.rollback_checkbox.isSelected());
	}
	
	/**
	 * Whether or not the editing user is USING the native RB permission.
	 * @return TRUE if the user is USING native RB rights; FALSE, otherwise
	 */
	public boolean editor_using_native_rb(){
		return (editor_has_native_rb && rb_checkbox_selected());
	}
	
	/**
	 * Return the currently selected option in the "watchlist combo-box"
	 * @return the currently selected option in the "watchlist combo-box".
	 */
	public STIKI_WATCHLIST_OPTS watchlist_combobox(){
		
		 /* WATCHLIST_OPTIONS_STR = {"Never watchlist",	0
			"Watch reverted articles",					1
			"Watch user talk if warned",				2
			"Watch article and user talk",				3
			"Use account preferences"}; 				4 */
		
		if(watchlist_combobox.getSelectedIndex() == 0)
			return(STIKI_WATCHLIST_OPTS.NEVER);
		else if(watchlist_combobox.getSelectedIndex() == 1)
			return(STIKI_WATCHLIST_OPTS.ONLY_ARTICLES);
		else if(watchlist_combobox.getSelectedIndex() == 2)
			return(STIKI_WATCHLIST_OPTS.ONLY_USERTALK);
		else if(watchlist_combobox.getSelectedIndex() == 3)
			return(STIKI_WATCHLIST_OPTS.WATCH_BOTH);
		else // if(watchlist_combobox.getSelectedIndex() == 4)
			return(STIKI_WATCHLIST_OPTS.USER_PREFS);
	}
	
	/**
	 * Return the currently selected index in the "watchlist combo-box"
	 * @return the currently selected index in the "watchlist combo-box".
	 */
	public int watchlist_combobox_index(){
		return(watchlist_combobox.getSelectedIndex());
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Create the subpanel containing the user/password labels and fields.
	 * @return JPanel containing initialized components described above
	 */
	private JPanel get_field_subpanel(){
		
			// Initialize components, constraint greedy height of the fields
		field_user = new JTextField("anonymous");
		field_pass = new JPasswordField("");
		Dimension field_dim = new Dimension(Integer.MAX_VALUE, 
				field_user.getPreferredSize().height);
		field_user.setMaximumSize(field_dim);
		field_pass.setMaximumSize(field_dim);
		
			// Intialize user/pass labels so font can be set
		JLabel label_user = new JLabel("Username:");
		label_user.setFont(gui_globals.BOLD_NORMAL_FONT);
		JLabel label_pass = new JLabel("Password:");
		label_pass.setFont(gui_globals.BOLD_NORMAL_FONT);
		
			// Intialize panel
		JPanel field_subpanel = new JPanel();
		field_subpanel.setLayout(new BoxLayout(field_subpanel,
				BoxLayout.Y_AXIS));
		field_subpanel.add(label_user);
		field_subpanel.add(field_user);
		field_subpanel.add(Box.createVerticalGlue());
		field_subpanel.add(label_pass);
		field_subpanel.add(field_pass);
		field_subpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return(field_subpanel);
	}
	
	/**
	 * Create the subpanel containing the anonymous-editing checkbox.
	 * @return JPanel containing initialized components described above
	 */
	private JPanel get_checkbox_subpanel(){
		
			// Initialize the checkbox and link
		anon_checkbox = new JCheckBox("Edit Anonymously");
		anon_checkbox.setMnemonic(KeyEvent.VK_A);
		anon_checkbox.setFont(gui_globals.PLAIN_NORMAL_FONT);
		anon_checkbox.addActionListener(this);		
		anon_link = gui_globals.create_link("[?]", false, this);

			// Simple horizontal arrangement of CB and link
		JPanel subpanel = new JPanel();
		subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.X_AXIS));
		subpanel.add(Box.createHorizontalGlue());
		subpanel.add(anon_checkbox);
		subpanel.add(anon_link);
		subpanel.add(Box.createHorizontalGlue());
		subpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return(subpanel);	
	}
	
	/**
	 * Create the subpanel containing the login/logout buttons.
	 * @return JPanel containing initialized components described above
	 */
	private JPanel get_button_subpanel(){
		
			// Intialize the buttons and their action-listener
		button_login = new JButton("Log-in");
		button_login.setMnemonic(KeyEvent.VK_L);
		button_login.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_login.addActionListener(this);
		button_logout = new JButton("Log-out");
		button_logout.setMnemonic(KeyEvent.VK_O);
		button_logout.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_logout.addActionListener(this);
		
			// Arrange butons horizontally into panel, with equi-distant
			// button_subpanel on either side, as well as between.
		JPanel button_subpanel = new JPanel();
		button_subpanel.add(Box.createHorizontalGlue());
		button_subpanel.setLayout(new BoxLayout(button_subpanel, 
				BoxLayout.X_AXIS));
		button_subpanel.add(button_login);
		button_subpanel.add(Box.createHorizontalGlue());
		button_subpanel.add(button_logout);
		button_subpanel.add(Box.createHorizontalGlue());
		button_subpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return(button_subpanel);
	}
	
	/**
	 * Create the subpanel containing the login status message.
	 * @return JPanel containing initialized components described above
	 */
	private JPanel get_status_subpanel(){
		
			// Initialize status label -- which will show HTML code
			// Both centerings are REQUIRED for the expected effect
		label_status = new JLabel("");
		label_status.setFont(gui_globals.PLAIN_NORMAL_FONT);
		label_status.setHorizontalAlignment(SwingConstants.CENTER);
		label_status.setHorizontalTextPosition(SwingConstants.CENTER);

			// The message should be centered relative to its multiline 
			// self (above), and the whole label block should also be
			// centered within the larger pane (below)
		return(gui_globals.center_comp_with_glue(label_status));	
	}
	
	/**
	 * Create the subpanel containing the "Use rollback?" checkbox
	 * @return JPanel containing initialized compontents described above
	 */
	private JPanel get_rb_subpanel(){
		rollback_checkbox = new JCheckBox("Use Rollback Action", true);
		rollback_checkbox.setMnemonic(KeyEvent.VK_U);
		rollback_checkbox.setFont(gui_globals.PLAIN_NORMAL_FONT);
		rollback_checkbox.addActionListener(this);
		return(gui_globals.center_comp_with_glue(rollback_checkbox)); 
	}
	
	/**
	 * Create the subpanel containing the no-watchlist checkbox.
	 * @return JPanel containing initialized components described above
	 */
	private JPanel get_watchlist_subpanel(){
		
		/* OLD SETUP WITH SINGLE CHECKBOX AND INFORMATIONAL DIALOG:
		watchlist_checkbox = new JCheckBox("Never Watchlist", 
				gui_settings.get_bool_def(
						gui_settings.SETTINGS_BOOL.login_watch, true));
		watchlist_checkbox.setMnemonic(KeyEvent.VK_N);
		watchlist_checkbox.setFont(gui_globals.PLAIN_NORMAL_FONT);
		watchlist_checkbox.addActionListener(this);		
		watchlist_link = gui_globals.create_link("[?]", false, this); */
		
		watchlist_combobox = new JComboBox<String>(STIKI_WATCHLIST_OPTS_STR);
		watchlist_combobox.setSelectedIndex(gui_settings.get_int_def(
				gui_settings.SETTINGS_INT.watchlist_set, 0));
		watchlist_combobox.setBackground(Color.WHITE);
		watchlist_combobox.setFont(gui_globals.SMALL_NORMAL_FONT);
		
			// JComboBox's have well documented sizing difficulties;
			// following is a best effort to keep things sane
		watchlist_combobox.setPreferredSize(
				new Dimension(watchlist_combobox.getPreferredSize().width, 
				watchlist_combobox.getFont().getSize()));	
		watchlist_combobox.setMinimumSize(
				new Dimension(watchlist_combobox.getPreferredSize().width, 
				watchlist_combobox.getFont().getSize()));	
		
			// Simple vertical arrangement of label and combo-box
		JPanel subpanel = new JPanel();
		subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.Y_AXIS));
		subpanel.add(gui_globals.center_comp_with_glue(
				gui_globals.create_intro_label("Watchlist options:")));
		subpanel.add(gui_globals.center_comp_with_glue(watchlist_combobox));
		return(subpanel);
	}
	
	/**
	 * Assuming the editor has just chosen to edit anonymously (i.e., checking
	 * the checkbox), bring the class state to reflect this selection.
	 */
	private void anonymous_checked(){
		
		try{ api_post.process_logout(); } 
		catch(Exception e){
			JOptionPane.showMessageDialog(this,
					"Wikipedia logout failed,\n" +
		   		 	"please ensure network connectivity.",
		   		    "Warning: Logout attempt failed",
		   		    JOptionPane.ERROR_MESSAGE);
		} // Logout any user logged-in at Wiki, 
		
			// Reset session-variables.
		this.is_state_stable = true;
		this.state_changed = true;
		this.editing_user = this.get_machine_ip();
		this.editor_has_native_rb = false;
		
			// Blank and disable the user-login mechanisms.
		field_user.setText("anonymous");
		field_user.setEditable(false);
		field_pass.setText("");
		field_pass.setEditable(false);
		button_login.setEnabled(false);
		button_logout.setEnabled(false);
		
			// Visually report the IP editor
		this.set_status_to_current_editor(this.editing_user);
	}

	/**
	 * If the user has UN-checked the option to edit anonymopusly, bring
	 * the class state to reflect this selection.
	 */
	private void anonymous_unchecked() throws Exception{
		
			// Prevent edits from being made until the user logs-in,
			// or re-selects the anonymous option.
		this.is_state_stable = false;
		
			// Enable the login fields, try to rely on persistent settings
		field_user.setText(gui_settings.get_str_def(
				gui_settings.SETTINGS_STR.login_user, ""));
		field_user.setEditable(true);
		field_pass.setEditable(true);
		button_login.setEnabled(true);
		button_logout.setEnabled(false);
		label_status.setText("<HTML><CENTER>Please Log-in</CENTER></HTML>");	
	}
	
	/**
	 * If the user has clicked the "login" button -- pull content from user/
	 * pass field and attempt to log-in user. Setting the class state and 
	 * reporting result to user no matter what the outcome.
	 */
	private void login_clicked() throws Exception{
		
			// Attempt the login via MediaWiki API
		String user = field_user.getText();
		String pass = String.valueOf(field_pass.getPassword());
		
			// Check qualification conditions:
		Set<String> user_perms = api_retrieve.process_user_perm(user);
		boolean native_rb = api_xml_user_perm.has_rollback(user_perms);
		if(!login_qualified(user, native_rb))
			return;
		
			// Having passed reqs; proceed with login
		boolean login_success = api_post.process_login(user, pass);
		if(!login_success) // if login fails
			label_status.setText("Log-in Failed");
		else{
				// We have a new editing user
			this.is_state_stable = true;
			this.state_changed = true;
			this.editing_user = user;
			this.editor_has_native_rb = native_rb;
					
				// Disable login, enable logout
			field_user.setEditable(false);
			field_pass.setEditable(false);
			button_login.setEnabled(false);
			button_logout.setEnabled(true);
			
				// Visually report success
			this.set_status_to_current_editor(this.editing_user);
		} // If login succeeds
	}
	
	/**
	 * Determine if some user qualifies to use the STiki tool.
	 * @param uname Username of whomever is trying to login
	 * @param has_rb Whether or not 'uname' has the rollback permission
	 * @return TRUE if 'uname" is allowed to use STiki. FALSE, otherwise.
	 */
	private boolean login_qualified(String uname, boolean has_rb) 
			throws Exception{
		
			// Current qualifications include (user needs just one):
			// 1. The rollback permission
			// 2. Edit count > 1000 in article namespace
			// 3. Explicit permission per DB table (for grandfathering, also)
			//
			// Here we try to call those by order of expense
		if(has_rb || (api_retrieve.process_user_edits(uname, 0, 
				stiki_utils.cur_unix_time(), 0, 1000, null, 500) >= 1000) 
				||  parent.client_interface.user_explicit(uname)){
			return true;
		} else{
			label_status.setText("Insufficient Permissions");
			not_qualified_dialog();
			return(false);
		} // Notify user if qualifications are not met
		
	}
	
	/**
	 * If the user has clicked the "logout" button -- terminate the
	 * Wikipedia session and adjust class state appropriately. 
	 */
	private void logout_clicked() throws Exception{
		
			// Terminate the Wikipedia session, blank local session variables
		api_post.process_logout();
		this.is_state_stable = false;
		
			// Enable log-in fields
		field_user.setText("");
		field_user.setEditable(true);
		editor_has_native_rb = false;
		field_pass.setText("");
		field_pass.setEditable(true);
		button_login.setEnabled(true);
		button_logout.setEnabled(false);
		label_status.setText("<HTML><CENTER>Log-out Successful" +
				"</CENTER></HTML>");
	}

	/**
	 * The external IP address of the machine on which this code is running.
	 * @return external IP address of the machine on which this code is running
	 */
	private String get_machine_ip(){
		
			// Previously this returned the actual client-IP. This can't be 
			// done trivially from Java, and an external server must be 
			// contacted. http://whatismyip.org/ was used, but is unreliable.
		return("-anonymous-");
	}
	
	/**
	 * Set the status-label to reflect the fact user "x" is editing.
	 * Crucially, this gives us the opportunity to abbreviate any user-name
	 * so long that it might stretch the horizontal nature of the panel.
	 * @param editor User-name or IP of the current editor
	 */
	private void set_status_to_current_editor(String editor){
		
			// Potentially abbreviate editor before setting text
		if(editor.length() > 20)
			editor = editor.substring(0, 17) + "...";
		label_status.setText("<HTML><CENTER>Currently editing as<BR>" + 
				editor + "</CENTER></HTML>");
	}
	
	/**
	 * The dialog popped when the 'anonymous link' is clicked.
	 */
	private void anon_dialog(){
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
				"Note that due to concerns of abuse -- anonymous editing\n" +
				"has been temporarily disabled. This functionality may\n" +
				"return pending discussions with the Wikipedia " + 
				"administration.\n\n" +
				"A Wikipedia log-in is now required in order to use STiki.\n\n",
				"Information: Anonymous editing disabled",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * The dialog popped when the 'watchlist link' is clicked.
	 */
	@SuppressWarnings("unused")
	private void watchlist_dialog(){
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
				"If this box is checked, STiki will *never* add an article\n" +
				"which is edited/reverted to your watchlist. If unchecked,\n" +
				"STiki will default to your account \"preferences\".\n\n",
				"Information: Watchlist checkbox",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * The dialog popped when user has insufficient permissions to use tool.
	 */
	private void not_qualified_dialog(){
		
			// We want a fairly rich message here with working hyperlinks,
			// so we do it as a pane, with HyperlinkListerner
		JEditorPane message_pane = 
				gui_globals.create_stiki_html_pane(null, true);
		message_pane.setBackground(UIManager.getDefaults().getColor(
				parent.getBackground()));
		message_pane.addHyperlinkListener(new HyperlinkListener(){
		public void hyperlinkUpdate(HyperlinkEvent e){
			if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
				url_browse.openURL(e.getURL().toString());
		}	}	});
		
			// If we want hyperlinks in a JDialog, then it seems we have
			// to do manual line breaks. Not at all pleased with the 
			// inelegance of this, but did much research on it.
		String text = 
			"The user you are attempting to login <B>does not have <BR>" +
			"sufficient privileges</B> to use the STiki tool. At the <BR>" +
			"current time a user meet one of the following criteria:" +
			"<BR><BR>" +
			"1. Have the <A HREF=\"http://en.wikipedia.org/wiki/" +
				"Wikipedia:Rollback\">rollback</A> permission.<BR>" +
			"2. Have > 1000 edits in Wikipedia's article namespace<BR>" +
			"3. Get special approval on <A HREF=\"http://en.wikipedia.org/" +
				"wiki/Wikipedia_talk:STiki\">STiki's talk page</A>" +
			"<BR><BR>" +
			"Well-intentioned novice editors should consider option<BR>" +
			"#3 above. You can be adopted by an experienced STiki<BR>" +
			"user and be fighting vandalism in no time!<BR><BR>";
		message_pane.setText(text);

			// Pop the dialog
		JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
				message_pane,
				"Error: User lacks STiki permissions",
				JOptionPane.ERROR_MESSAGE);
	}
}


