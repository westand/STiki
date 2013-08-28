package gui_menus;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import core_objects.stiki_utils.QUEUE_TYPE;
import core_objects.stiki_utils.SCORE_SYS;

import executables.stiki_frontend_driver;

import gui_support.gui_globals;

/**
 * Andrew G. West - gui_menu_bar.java - This class implements the STiki
 * GUI menu bar, its menu items, and the action-events over them.
 */
@SuppressWarnings("serial")
public class gui_menu_bar extends JMenuBar{
	 
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Menu to close STiki, or launch the offline-review-tool (ORT).
	 */
	private JMenu file_menu;
	
	/**
	 * Menu for chooshing which queue to view edits from.
	 */
	private gui_menu_queue queue_menu;
	
	/**
	 * Top-level menu where filters can be selected which affect which 
	 * edits are shown in the main window (i.e., filter the RID queue).
	 */
	private gui_menu_filter filter_menu;
	
	/**
	 * Top-level menu allowing users to alter small-level options.
	 */
	private gui_menu_options options_menu;
	
	/**
	 * Top-level menu for directing a user to data to provide "help" on
	 * how STiki works and should be used. 
	 */
	private JMenu help_menu;
	
	/**
	 * Top-level menu for learning more "about" STiki.
	 */
	private JMenu about_menu;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_menu_bar], visually establishing all menus, 
	 * and the action-items that occur over them.
	 * @param main_win The STiki frontend -- which has public access to all
	 * visual components, s.t. this menu can affect the display of main
	 * window content without over-zealous component passing.
	 * @param default_queue Queue selected by default at program start
	 */
	public gui_menu_bar(stiki_frontend_driver frame, SCORE_SYS default_queue){
		
			// More interesting menus are handled by separate classes
		file_menu = new gui_menu_file(frame);
		queue_menu = new gui_menu_queue(frame, default_queue);
		filter_menu = new gui_menu_filter(frame);
		options_menu = new gui_menu_options(frame);
		help_menu = new gui_menu_help();
		about_menu = new gui_menu_about();
		
			// Add the individual components to the MenuBar
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(file_menu);
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(gui_globals.create_vert_separator());
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(queue_menu);
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(gui_globals.create_vert_separator());
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(filter_menu);
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(gui_globals.create_vert_separator());
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(options_menu);
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(gui_globals.create_vert_separator());
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(help_menu);
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(gui_globals.create_vert_separator());
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(about_menu);
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(gui_globals.create_vert_separator());
		this.add(Box.createHorizontalGlue());
	}
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Create a top-level menu by providing a name and key-event.
	 * @param text Text used to label the menu being produced.
	 * @param keyevent Mnemonic KeyEvent character to associate with the menu
	 * @return A JMenu, labeled as 'text' with mnemonic 'keyevent'
	 */
	public static JMenu create_top_menu(String text, int keyevent){
		JMenu new_menu = new JMenu(text);
		new_menu.setFont(gui_globals.PLAIN_NORMAL_FONT);
		new_menu.setMnemonic(keyevent);
		return(new_menu);
	}
	
	/**
	 * Create a menu-item by providing a name and key-event.
	 * @param text Text used to label the item being produced.
	 * @param keyevent Mnemonic KeyEvent character to associate with item menu
	 * @return A JMenuItem, labeled as 'text' with mnemonic 'keyevent'
	 */
	public static JMenuItem create_menu_item(String text, int keyevent){
		JMenuItem new_item = new JMenuItem(text);
		new_item.setFont(gui_globals.PLAIN_NORMAL_FONT);
		new_item.setMnemonic(keyevent);
		return(new_item);
	}
	
	/**
	 * Return the current selection in the radio buttons of the "queue" menu
	 * @return the queue currently selected in the "queue" radio button group
	 */
	public SCORE_SYS selected_queue(){
		return(queue_menu.selected_queue());
	}
	
	/**
	 * Return the current queue type (spam, vandalism, etc.).
	 * @return the queue type currently selected
	 */
	public QUEUE_TYPE selected_type(){
		return(queue_menu.selected_type());
	}
	
	/**
	 * Return the "filter menu", wherein some queue settings lie.
	 * @return the "filter menu" of this menu bar
	 */
	public gui_menu_filter get_filter_menu(){
		return(filter_menu);
	}
	
	/**
	 * Return the "options menu", wherein many option settings are
	 * presented to the end-user (i.e., checkboxes, etc.). An accessor.
	 * @return the "options menu" of this menu bar
	 */
	public gui_menu_options get_options_menu(){
		return(options_menu);
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
}
