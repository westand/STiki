package gui_menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

import executables.stiki_frontend_driver;

import gui_support.gui_globals;
import gui_support.gui_settings;

/**
 * Andrew G. West - gui_menu_filter.java - This class implements the 
 * "filter" menu, providing user notification and control over what
 * type of edits are exiting the queues and available for classification.
 */
@SuppressWarnings("serial")
public class gui_menu_filter extends JMenu implements ActionListener{
	 
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Frame which contains this menu, so that the visual aspects
	 * can be altered via menu selections.
	 */
	private stiki_frontend_driver parent;
	
		// A hierarchical view of this menu's components
	private JCheckBoxMenuItem cb_privileged;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_menu_filter] -- creating the menu, its mnemonic,
	 * its submenus, the items, and adding all necessary listeners.
	 * @param Parent class which contains this menu, so that the visual aspects
	 * can be altered via menu selections.
	 */
	public gui_menu_filter(stiki_frontend_driver parent){
		
		this.parent = parent; // Argument assignment
		
			// First set properties of top-level menu item
		this.setText("Revision Filters");
		this.setFont(gui_globals.PLAIN_NORMAL_FONT);
		this.setMnemonic(KeyEvent.VK_R);
		
			// Then initialize interesting items, using settings file
		cb_privileged = gui_globals.checkbox_item(
				"Edits by Privileged Users", KeyEvent.VK_X, true, 
				gui_settings.get_bool_def(
				gui_settings.SETTINGS_BOOL.filter_privileged, true));
		
			// Add items to the menu
		this.add(cb_privileged);
		this.add(gui_globals.HORIZ_MENU_SEP);
		this.add(gui_globals.checkbox_item("Namespace-Zero (NS0)", 
				KeyEvent.VK_Z, false, true));
		this.add(gui_globals.checkbox_item("Anonymous User Edits", 
				KeyEvent.VK_A, false, true));
		this.add(gui_globals.checkbox_item("Registered User Edits", 
				KeyEvent.VK_R, false, true));
		this.add(gui_globals.checkbox_item("Only Most Recent on Page",
				KeyEvent.VK_M, false, true));	
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Map menu-item selections to opening of help pane/dialog.
	 */
	public void actionPerformed(ActionEvent event){
		
		if(event.getActionCommand().equals(this.cb_privileged)){
			pop_filter_cache_dialog();
		}
	}
	
	/**
	 * Accessor into the status of the "Edits by Privileged Users" option.
	 * @return Whether the "Edits by Privileged Users" option is checked
	 */
	public boolean get_privileged_status(){
		return(cb_privileged.isSelected());
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Pop the informational dialogue warning that users that changes to
	 * edit filters may take several edits to take effect due to the
	 * way client-side caching of revisions operates.
	 */
	private void pop_filter_cache_dialog(){
		
		JOptionPane.showMessageDialog(parent,
				"The new revision filter setting is now in place!\n\n" +
				"However, realize that several subsequent edits may\n" +
				"already be locally cached/enqueued under the old\n" +
				"settings. Once those are flushed (usually less than 10\n" +
				"edits), the new setting will take hold and remain\n" +
				"persistent across STiki sessions.\n\n",		
       		    "Warning: Filter changes must flush from queue",
       		    JOptionPane.INFORMATION_MESSAGE);
	}

}
	