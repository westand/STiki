package gui_support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Properties;

import core_objects.stiki_utils;

import executables.stiki_frontend_driver;
import gui_panels.gui_comment_panel.COMMENT_TAB;

/**
 * Andrew G. West - gui_settings.java - This class handles the persistent
 * storage of user settings (via a local file). This design was heavily
 * influenced and borrowed from the efforts of en.wp [[User:Meiskam]].
 * 
 * Note that [load_properties()] method MUST BE CALLED over the static
 * instance of this class before it can be used as expected. This should
 * be the first thing the STiki executable loads in the GUI.
 */
@SuppressWarnings("serial")
public class gui_settings extends Properties{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * A static instance of this class.
	 */
	public static gui_settings props = new gui_settings();
	
	/**
	 * Those perisistent user settings with are STRING based.
	 */
	public enum SETTINGS_STR
		{comment_vand2,			// Comment left with vandalism class.
		comment_spam2,			// Comment left with spam class.
		comment_agf3,			// Comment left with AGF class.
		login_user,				// User who last logged in to program.
		agf_custom1,			// Custom AGF notification #1
		agf_custom2,			// Custom AGF notification #2
		agf_custom3,			// Custom AGF notification #3
		agf_custom4,			// Custom AGF notification #4
		options_fontfam,		// Font used in the diff-browser
		color_diff_bg, 			// Color of diff's "background"
		color_diff_words,		// Color of diff's "changed words"
		color_diff_cont,		// Color of diff's "context blocks"
		color_diff_add,			// Color of diff's "added blocks"
		color_diff_del,			// Color of diff's "deleted blocks"
		color_diff_note,		// Color of diff's "note to users" (RB)
		color_diff_text};		// Color of diff's base text
		
	/**
	 * Those persistent user settings which are INTEGER based.
	 */
	public enum SETTINGS_INT 
		{win_width,				// Width of the STiki window
		win_height,				// Height of the STiki window
		win_locx,				// Horizontal position of STiki window
		win_loxy,				// Vertical position of STiki window
		options_fontsize,		// Size of the diff-browser font
		watchlist_set,			// Watchlisting semantic being applied
		settings_version,		// Version of the settings XML
		passes_used};			// Number of times "pass" used in career
		
	/**
	 * Those persistent user settings which are BOOLEAN based
	 */
	public enum SETTINGS_BOOL
		{warn_vand,				// Whether vandalism warnings places.
		warn_spam,				// Whether spam warnings placed.
		warn_agf,				// Whether AGF warnings placed.
		options_hyperlinks,		// Whether URLs are hyperlinked in diffs
		options_https,			// Whether the HTTPS protocol should be used
		options_dttr,			// Whether to warn if "templating a regular"
		options_agf_comment,    // Whether to message AGF-reverted users
		options_aiv_popup,		// Whether to pop-up notify on AIV post
		filter_privileged,      // Show edits by privileged editors?
		filter_numerical};      // Show small numerical edits?
		
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Link to main GUI class, which will provide access to many of the
	 * options we'd like to store persistently at shutdown.
	 */
	private static stiki_frontend_driver parent;
	
	
	/** 
	 * Here we MANUALLY note which version of the "settings"
	 * document this is. Not a concern as long as the settings
	 * are monotonically increasing. However, if we remove or
	 * modify setting names, they will remain in the doc forever,
	 * slowing things down and inhibiting manual editing. Thus by
	 * checking this parameter, we can create new file versions.
	 */
	private static final int SETTINGS_VERSION = 2;
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Statically set the "parent", providing access to all settings 
	 * @param parent the top-level GUI class, doing all coordination
	 */
	public static void set_parent(stiki_frontend_driver parent){
		gui_settings.parent = parent;
	}

	/**
	 * Load persistent settings from XML file on disk into Java struct. 
	 */
	public static void load_properties(){
		try{FileInputStream properties_input;
			properties_input = new FileInputStream(get_properties_file());
			props.loadFromXML(properties_input);
			properties_input.close();			
		} catch(FileNotFoundException e1){
		} catch(Exception e2){
			System.out.println("Error when loading persistent settings:");
			e2.printStackTrace();
		} // Ignore file-not-found Exceptions, otherwise report		
	}

	/**
	 * Save persistent settings to an XML file on disk
	 */
	public static void save_properties(){
		
			// Check if expired properties need deleting, which is actually
			// realized by a full deletion, and then the save that follows
		if(get_int_def(SETTINGS_INT.settings_version, 0) < SETTINGS_VERSION)
			props.clear(); 
		update_properties(); // update properties prior to saving
		try{FileOutputStream properties_output;
			File props_file = get_properties_file();
			
				// Set perms so only "owner" can read/write
			props_file.setReadable(false, false);
			props_file.setWritable(false, false);
			props_file.setExecutable(false, false);
			props_file.setReadable(true, true);
			props_file.setWritable(true, true);
			props_file.setExecutable(false, true);
			
				// Then write out properties
			properties_output = new FileOutputStream(props_file);
			props.storeToXML(properties_output, "STiki Settings", "UTF-8");
			properties_output.close();
		} catch(Exception e){
			System.out.println("Error when saving persistent settings:");
			e.printStackTrace();
		} 
	}
	
	// ****** PROPERTY ACCESSORS ******

	/**
	 * Attempt to lookup persistent setting (int); return default if it DNE.
	 * @param key Property key known to be of type "int"
	 * @param def Default value to return if 'key' does not exist
	 * @return The value associated with 'key' if it exists; 'def' otherwise
	 */
	public static int get_int_def(SETTINGS_INT key, int def){
		try{if(props.containsKey(key.toString()))
				return(get_int(key));
		} catch(Exception e){
			System.out.println("Error in getting persistent setting (int):");
			e.printStackTrace();
		} return(def);
	}
	
	/**
	 * Attempt to lookup persistent setting (String); return default if it DNE.
	 * @param key Property key know to be of type "String"
	 * @param def Default value to return if 'key' does not exist
	 * @return The value associated with 'key' if it exists; 'def' otherwise
	 */
	public static String get_str_def(SETTINGS_STR key, String def){
		try{if(props.containsKey(key.toString()))
			return(props.getProperty(key.toString()));
		} catch(Exception e){
			System.out.println("Error in getting persistent setting (String):");
			e.printStackTrace();
		} return(def);
	}
	
	/**
	 * Attempt to lookup persistent setting (bool); return default if it DNE.
	 * @param key Property key known to be of type "boolean"
	 * @param def Default value to return if 'key' does not exist
	 * @return The value associated with 'key' if it exists; 'def' otherwise
	 */
	public static boolean get_bool_def(SETTINGS_BOOL key, boolean def){
		try{if(props.containsKey(key.toString()))
			return(get_bool(key));
		} catch(Exception e){
			System.out.println("Error in getting persistent setting (bool):");
			e.printStackTrace();
		} return(def);
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Return the file where persistent properties are stored.
	 * @return File where persistent properties are stored
	 */
	private static File get_properties_file() throws Exception {

			// This code attempts to store the hidden properties file first
			// in the "user.home" directory; if that fails, then it should
			// be stored alongside the executable
		
		String user_home = System.getProperty("user.home");
		File home_dir = null;
		if(user_home != null)
			home_dir = new File(user_home);
		return(new File(home_dir, ".STiki.props.xml"));
	}
	
	/**
	 * Do a wholesale update of all persistent properties.
	 */
	private static void update_properties(){
		
			// Relevant to window placement and sizing
		props.setProperty(SETTINGS_INT.win_width.toString(),
				String.valueOf(parent.getWidth()));
		props.setProperty(SETTINGS_INT.win_height.toString(), 
				String.valueOf(parent.getHeight()));
		props.setProperty(SETTINGS_INT.win_locx.toString(), 
				String.valueOf(parent.getX()));
		props.setProperty(SETTINGS_INT.win_loxy.toString(), 
				String.valueOf(parent.getY()));
		
			// Relevent to revert comments and warning
		props.setProperty(SETTINGS_STR.comment_vand2.toString(), 
				parent.comment_panel.get_comment(COMMENT_TAB.VAND));
		props.setProperty(SETTINGS_STR.comment_spam2.toString(), 
				parent.comment_panel.get_comment(COMMENT_TAB.SPAM));
		props.setProperty(SETTINGS_STR.comment_agf3.toString(), 
				parent.comment_panel.get_comment(COMMENT_TAB.AGF));
		props.setProperty(SETTINGS_BOOL.warn_vand.toString(), String.valueOf(
				parent.comment_panel.get_warn_status(COMMENT_TAB.VAND)));
		props.setProperty(SETTINGS_BOOL.warn_spam.toString(), String.valueOf(
				parent.comment_panel.get_warn_status(COMMENT_TAB.SPAM)));
		props.setProperty(SETTINGS_BOOL.warn_agf.toString(), String.valueOf(
				parent.comment_panel.get_warn_status(COMMENT_TAB.AGF)));
		
			// Properties in the login panel
		props.setProperty(SETTINGS_STR.login_user.toString(), 
				parent.login_panel.get_login_user_field());
		props.setProperty(SETTINGS_INT.watchlist_set.toString(), 
				String.valueOf(parent.login_panel.watchlist_combobox_index()));
		
			// Queue options and filters over those queues
		props.setProperty(SETTINGS_BOOL.filter_privileged.toString(),
				String.valueOf(parent.menu_bar.get_filter_menu().
				get_privileged_status()));
		props.setProperty(SETTINGS_BOOL.filter_numerical.toString(),
				String.valueOf(parent.menu_bar.get_filter_menu().
				get_numerical_status()));
		
			// Menu properties found in "options"
		props.setProperty(SETTINGS_INT.options_fontsize.toString(), 
				String.valueOf(parent.menu_bar.
				get_options_menu().get_browser_fontsize()));
		props.setProperty(SETTINGS_STR.options_fontfam.toString(), 
				parent.menu_bar.get_options_menu().get_browser_fontfam());
				
		props.setProperty(SETTINGS_BOOL.options_hyperlinks.toString(), 
				String.valueOf(parent.menu_bar.
				get_options_menu().get_hyperlink_policy()));
		props.setProperty(SETTINGS_BOOL.options_https.toString(), 
				String.valueOf(parent.menu_bar.
				get_options_menu().get_https_policy()));
		props.setProperty(SETTINGS_BOOL.options_dttr.toString(),
				String.valueOf(parent.menu_bar.
				get_options_menu().get_dttr_policy()));
		props.setProperty(SETTINGS_BOOL.options_agf_comment.toString(),
				String.valueOf(parent.menu_bar.
				get_options_menu().get_agf_comment_policy()));
		props.setProperty(SETTINGS_BOOL.options_aiv_popup.toString(),
				String.valueOf(parent.menu_bar.
				get_options_menu().get_aiv_popup_policy()));
		
			// Custom AGF messages internal to that dialog
		props.setProperty(SETTINGS_STR.agf_custom1.toString(), 
				gui_agf_dialogue.get_custom_agf(1));
		props.setProperty(SETTINGS_STR.agf_custom2.toString(), 
				gui_agf_dialogue.get_custom_agf(2));
		props.setProperty(SETTINGS_STR.agf_custom3.toString(), 
				gui_agf_dialogue.get_custom_agf(3));
		props.setProperty(SETTINGS_STR.agf_custom4.toString(), 
				gui_agf_dialogue.get_custom_agf(4));
		
			// Coloring inside the diff window
		props.setProperty(SETTINGS_STR.color_diff_bg.toString(), 
				diff_markup.COLOR_DIFF_BG);
		props.setProperty(SETTINGS_STR.color_diff_words.toString(), 
				diff_markup.COLOR_DIFF_WORDS);
		props.setProperty(SETTINGS_STR.color_diff_cont.toString(), 
				diff_markup.COLOR_DIFF_CONT);
		props.setProperty(SETTINGS_STR.color_diff_add.toString(), 
				diff_markup.COLOR_DIFF_ADD);
		props.setProperty(SETTINGS_STR.color_diff_del.toString(), 
				diff_markup.COLOR_DIFF_DEL);
		props.setProperty(SETTINGS_STR.color_diff_note.toString(), 
				diff_markup.COLOR_DIFF_NOTE);
		props.setProperty(SETTINGS_STR.color_diff_text.toString(), 
				diff_markup.COLOR_DIFF_TEXT);
		
			// Miscellania, statistics, etc.
		props.setProperty(SETTINGS_INT.passes_used.toString(), 
				String.valueOf(parent.get_passes_in_career()));
		
			// Meta settings
		props.setProperty(SETTINGS_INT.settings_version.toString(), 
				String.valueOf(SETTINGS_VERSION));
	}
	
	
	// ****** PROPERTY ACCESSORS ******
	
	/**
	 * Wrap the getProperty() method, so it returns an Integer
	 * @param key Property key known to be of type "int"
	 * @return The value associated with the 'key', as an Integer
	 */
	private static int get_int(SETTINGS_INT key) throws Exception{
		return(Integer.parseInt(props.getProperty(key.toString())));
	}
	
	/**
	 * Wrap the getProperty() method, so it returns a Boolean
	 * @param key Property key known to be of type "boolean"
	 * @return The value associated with the 'key', as a boolean
	 */
	private static boolean get_bool(SETTINGS_BOOL key) throws Exception{
		return(stiki_utils.str_to_bool(props.getProperty(key.toString())));
	}

}
