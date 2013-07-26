package gui_menus;

import executables.offline_review_driver;
import executables.stiki_frontend_driver;
import gui_support.gui_globals;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Andrew G. West - gui_menu_file.java - This class creates the menu 
 * "File", its menu items, and the action handlers on top of them.
 */
@SuppressWarnings("serial")
public class gui_menu_file extends JMenu implements ActionListener{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Frame which contains this menu.
	 */
	private stiki_frontend_driver parent;
	
	/**
	 * Menu item "Launch ORT" -- launches the offline review tool (ORT).
	 */
	private JMenuItem item_ort;
	
	/**
	 * Menu item "Close STiki" -- closes the STiki GUI.
	 */
	private JMenuItem item_close;

	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_menu_file] -- creating the menu button, 
	 * its menu items, and registering all necessary listeners.
	 * @param Parent class which contains this menu
	 */
	public gui_menu_file(stiki_frontend_driver parent){
		
		this.parent = parent;
		
			// First set properties of top-level menu item
		this.setText("File");
		this.setFont(gui_globals.PLAIN_NORMAL_FONT);
		this.setMnemonic(KeyEvent.VK_F);
		
			// Initialize the menu-items
		item_ort = gui_menu_bar.create_menu_item(
				"Launch ORT", KeyEvent.VK_L);
		item_ort.addActionListener(this);
		item_close = gui_menu_bar.create_menu_item(
				"Close STiki", KeyEvent.VK_C);
		item_close.addActionListener(this);
		
			// Then add the menu-items to this menu
		this.add(item_ort);
		this.add(item_close);
	}

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Map menu actions to the appropriate handlers.
	 */
	public void actionPerformed(ActionEvent event){
		if(event.getSource().equals(item_ort)){
			parent.exit_handler(false);
			try{new offline_review_driver();
			} catch(Exception e){
				System.out.println("Error in launching STiki-ORT:");
				e.printStackTrace();
			} // Close STiki and launch ORT
		} else if(event.getSource().equals(item_close)){
			parent.exit_handler(true);
		} // Simple action-event handlers
	}
	
}
