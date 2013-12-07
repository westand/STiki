package gui_menus;

import executables.stiki_frontend_driver;
import gui_support.gui_globals;
import gui_support.gui_settings;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * Andrew G. West - gui_menu_options.java - This class builds the "options"
 * menu, which allows users to alter the appearance of the STiki GUI and
 * other, similar, non-mission critical selections
 */
@SuppressWarnings("serial")
public class gui_menu_options extends JMenu implements ActionListener{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Frame which contains this menu, so that the visual aspects
	 * can be altered via menu selections.
	 */
	private stiki_frontend_driver parent;
	
		// A hierarchical view of the component layout of this menu
	private JMenu submenu_browser_font;
		private JRadioButtonMenuItem browser_font_10;
		private JRadioButtonMenuItem browser_font_12;
		private JRadioButtonMenuItem browser_font_14;
		private JRadioButtonMenuItem browser_font_16;
		private JRadioButtonMenuItem browser_font_18;
		private JRadioButtonMenuItem browser_font_20;
		private JRadioButtonMenuItem browser_font_22;
		private JRadioButtonMenuItem browser_font_24;
	private JCheckBoxMenuItem xlink_cb;
	private JCheckBoxMenuItem https_cb;
	private JCheckBoxMenuItem dttr_cb;
	private JCheckBoxMenuItem agf_comment_cb;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_menu_options] -- creating the button, its mnemonic,
	 * its submenus, the sub-menu items, and adding all necessary listeners.
	 * @param Parent class which contains this menu, so that the visual aspects
	 * can be altered via menu selections.
	 */
	public gui_menu_options(stiki_frontend_driver parent){
		
		this.parent = parent; // Argument assignment
		
			// First set properties of top-level menu item
		this.setText("Options");
		this.setFont(gui_globals.PLAIN_NORMAL_FONT);
		this.setMnemonic(KeyEvent.VK_T);
		
			// Intialize sub-menus and items, add them to top-level
		initialize_subitems();
		this.add(submenu_browser_font);
		this.add(xlink_cb);
		this.add(https_cb);
		this.add(dttr_cb);
		this.add(agf_comment_cb);
		
			// Set default menu selections (per persistent settings)
		this.selected_browser_font(gui_settings.get_int_def(
				gui_settings.SETTINGS_INT.options_fontsize, 
				gui_globals.DEFAULT_BROWSER_FONT.getSize()));
		this.set_hyperlink_policy(gui_settings.get_bool_def(
				gui_settings.SETTINGS_BOOL.options_hyperlinks, 
				parent.diff_browser.get_hyperlink_policy()));
		this.set_https_policy(gui_settings.get_bool_def(
				gui_settings.SETTINGS_BOOL.options_https, false));
		this.set_dttr_policy(gui_settings.get_bool_def(
				gui_settings.SETTINGS_BOOL.options_dttr, true));
		this.set_agf_comment_policy(gui_settings.get_bool_def(
				gui_settings.SETTINGS_BOOL.options_agf_comment, true));
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Map menu-item selections to opening of help pane/dialog.
	 */
	public void actionPerformed(ActionEvent event){
		
		if(event.getSource().equals(xlink_cb)){
			set_hyperlink_policy(xlink_cb.isSelected());
		} else if(event.getSource().equals(https_cb)){
			
			// Do nothing. As label notes, a restart is required. Thus,
			// at shutdown the CB status will be written to settings,
			// and read from that file at next startup.
		
		} else if(event.getSource().equals(dttr_cb)){
		
			// Again, nothing. Other classes will check-in with this class
			// as needed to determine option status
			
		} else if(event.getSource().equals(agf_comment_cb)){
		
			// Do nothing
			
		} else{
			
				// If ActionEvent was not sourced per above, we assume it
				// came from one of the many font-size submenu items
			int browser_font_size = -1;
			if(event.getSource().equals(browser_font_10)) 
				browser_font_size = 10;
			else if(event.getSource().equals(browser_font_12))
				browser_font_size = 12;
			else if(event.getSource().equals(browser_font_14))
				browser_font_size = 14;
			else if(event.getSource().equals(browser_font_16))
				browser_font_size = 16;
			else if(event.getSource().equals(browser_font_18))
				browser_font_size = 18;
			else if(event.getSource().equals(browser_font_20))
				browser_font_size = 20;
			else if(event.getSource().equals(browser_font_22))
				browser_font_size = 22;
			else if(event.getSource().equals(browser_font_24))
				browser_font_size = 24;
			
				// Pass off size-change to handler
			this.selected_browser_font(browser_font_size);
		}
	}

	
	// ***** ACESSOR METHODS *****
	
	/**
	 * Return the point-size of the currently selected browser font.
	 * @return the point-size of the currently selected browser font
	 */
	public int get_browser_fontsize(){
		if(browser_font_10.isSelected()) return 10;
		else if(browser_font_12.isSelected()) return 12;
		else if(browser_font_14.isSelected()) return 14;
		else if(browser_font_16.isSelected()) return 16;
		else if(browser_font_18.isSelected()) return 18;
		else if(browser_font_20.isSelected()) return 20;
		else if(browser_font_22.isSelected()) return 22;
		else if(browser_font_24.isSelected()) return 24;
		else return(gui_globals.DEFAULT_BROWSER_FONT.getSize()); // unneeded 
	}
	
	/**
	 * Set the hyperlink policy in the menu (and the entire GUI).
	 * @param enable TRUE if hyperlinks in diff-display should be enabled
	 * (click-able). FASLE, otherwise.
	 */
	public void set_hyperlink_policy(boolean enable){
		xlink_cb.setSelected(enable);
		parent.diff_browser.set_hyperlink_policy(enable);
	}
	
	/**
	 * Return whether the 'activate hyperlinks' checkbox is selected.
	 * @return whether the 'activate hyperlinks' checkbox is selected
	 */
	public boolean get_hyperlink_policy(){
		return(xlink_cb.isSelected());
	}
	
	/**
	 * Set the HTTPS policy in the menu
	 * @param enable TRUE if HTTPS should be used for all Mediawiki
	 * interface/API links. FALSE, otherwise
	 */
	public void set_https_policy(boolean enable){
		https_cb.setSelected(enable);
	}
	
	/**
	 * Return whether the 'use HTTPS' checkbox is selected.
	 * @return whether the 'use HTTPS' checbox is selected
	 */
	public boolean get_https_policy(){
		return(https_cb.isSelected());
	}
	
	/**
	 * Set the DTTR policy in the menu (i.e., 'warn if templating regular')
	 * @param enable TRUE if editors should be warned if trying to revert/
	 * template/warn a regular user (whose definition is elsewehere).
	 */
	public void set_dttr_policy(boolean enable){
		dttr_cb.setSelected(enable);
	}
	
	/**
	 * Return whether the 'warn if templating regular' checkbox is selected.
	 * @return whether the 'warn if templating regular' checkbox is selected
	 */
	public boolean get_dttr_policy(){
		return(dttr_cb.isSelected());
	}
	
	/**
	 * Set the AGF comment policy in the menu
	 * @param enable TRUE if STiki users will be given the opportunity to
	 * send a message to editors reverted in an AGF fashion. Else, false.
	 */
	public void set_agf_comment_policy(boolean enable){
		agf_comment_cb.setSelected(enable);
	}
	
	/**
	 * Return whether the 'AGF comment' checkbox is selected.
	 * @return whether the 'AGF comment' checkbox is selected
	 */
	public boolean get_agf_comment_policy(){
		return(agf_comment_cb.isSelected());
	}
	

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Initialize the submenus/items beneath the "appearance" menu header.
	 */
	private void initialize_subitems(){
	
			// Intialize the sub-menu header for "browser font size"
		submenu_browser_font = new JMenu("Browser Font Size");
		submenu_browser_font.setFont(gui_globals.PLAIN_NORMAL_FONT);
		submenu_browser_font.setMnemonic(KeyEvent.VK_F);
	
			// Then add items to the "browser font size" sub-menu
		submenu_browser_font.add(browser_font_10 = create_rb_item(
				"10 point", KeyEvent.VK_1));
		submenu_browser_font.add(browser_font_12 = create_rb_item(
				"12 point", KeyEvent.VK_2));
		submenu_browser_font.add(browser_font_14 = create_rb_item(
				"14 point", KeyEvent.VK_4));
		submenu_browser_font.add(browser_font_16 = create_rb_item(
				"16 point", KeyEvent.VK_6));
		submenu_browser_font.add(browser_font_18 = create_rb_item(
				"18 point", KeyEvent.VK_8));
		submenu_browser_font.add(browser_font_20 = create_rb_item(
				"20 point", KeyEvent.VK_0));
		submenu_browser_font.add(browser_font_22 = create_rb_item(
				"22 point", KeyEvent.VK_P));
		submenu_browser_font.add(browser_font_24 = create_rb_item(
				"24 point", KeyEvent.VK_O));
		
		xlink_cb = create_cb_item("Activate Ext-Links", KeyEvent.VK_X);
		https_cb = create_cb_item("Use HTTPS (restart reqd.)", KeyEvent.VK_H);
		dttr_cb = create_cb_item("Warn if templating regular", KeyEvent.VK_W);
		agf_comment_cb = create_cb_item("Message AGF reverted users", KeyEvent.VK_A);
	}
	
	/**
	 * Alter which of the 'browser-font' radio buttons is currently selected.
	 * @param font_size Integer point size of the button to be selected
	 */
	private void selected_browser_font(int font_size){

			// Begin by un-setting all radio buttons
		browser_font_10.setSelected(false);
		browser_font_12.setSelected(false);
		browser_font_14.setSelected(false);
		browser_font_16.setSelected(false);
		browser_font_18.setSelected(false);
		browser_font_20.setSelected(false);
		browser_font_22.setSelected(false);
		browser_font_24.setSelected(false);
		
		switch(font_size){
	        case 10: browser_font_10.setSelected(true); break;
	        case 12: browser_font_12.setSelected(true); break;
	        case 14: browser_font_14.setSelected(true); break;
	        case 16: browser_font_16.setSelected(true); break;
	        case 18: browser_font_18.setSelected(true); break;
	        case 20: browser_font_20.setSelected(true); break;
	        case 22: browser_font_22.setSelected(true); break;
	        case 24: browser_font_24.setSelected(true); break;
		} // Then "select" the appropriate one		
		
			// Pass the actual change off to the browser
		Font new_font = new Font(gui_globals.DEFAULT_BROWSER_FONT.getName(), 
				gui_globals.DEFAULT_BROWSER_FONT.getStyle(), font_size);
		parent.diff_browser.change_browser_font(new_font);	
	}
	
	
	// ***** SIMPLIFICATIONS OF GUI_GLOBALS() FOR THIS CLASS
	
	/**
	 * Create a radio-button menu-item of the style used by STiki
	 * @param text Text which should be displayed on the menu-item
	 * @param keyevent Key mnemonic to fire this item
	 * @return A radio-button menu-item, labeled as 'text', fired by 'keyevent'
	 */
	private JRadioButtonMenuItem create_rb_item(String text, int keyevent){
		JRadioButtonMenuItem rb_item = new JRadioButtonMenuItem(text);
		rb_item.setMnemonic(keyevent);
		rb_item.setFont(gui_globals.PLAIN_NORMAL_FONT);
		rb_item.addActionListener(this);
		return (rb_item);
	}
	
	/**
	 * Create a checkbox menu-item of the style used by STiki
	 * @param text Text which should be displayed on the menu-item
	 * @param keyevent Key mnemonic to fire this item
	 * @return A checkbox menu-item, labeled as 'text', fired by 'keyevent'
	 */
	private JCheckBoxMenuItem create_cb_item(String text, int keyevent){
		JCheckBoxMenuItem cb_item = new JCheckBoxMenuItem(text);
		cb_item.setMnemonic(keyevent);
		cb_item.setFont(gui_globals.PLAIN_NORMAL_FONT);
		cb_item.addActionListener(this);
		return (cb_item);
	}
	
	
}
