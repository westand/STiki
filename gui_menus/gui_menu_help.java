package gui_menus;

import gui_support.gui_globals;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * Andrew G. West - gui_menu_help.java - This class builds the "help" menu,
 * which consists of a menu-item to the full help-text, as well as 
 * intra-section menu-items (mainly to fill aesthetic space).
 */
@SuppressWarnings("serial")
public class gui_menu_help extends JMenu implements ActionListener{
	
	// **************************** PRIVATE FIELDS ***************************
	
		// These are the menu-items for the "help menu". A javadoc style
		// description is unnecessary. Basically, the help-page can be read
		// starting at the top, or we can-anchor jump to specific sections.
	private JMenuItem item_full;
	private JMenuItem item_queue;
	private JMenuItem item_stiki_s;
	private JMenuItem item_filters;
	private JMenuItem item_browser;
	private JMenuItem item_metadata;
	private JMenuItem item_class;
	private JMenuItem item_login;
	private JMenuItem item_comment;
	private JMenuItem item_lastrv;
	private JMenuItem item_pform;
	private JMenuItem item_ort;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_menu_help] -- creating the menu button, its mnemonic,
	 * its menu items, and registering all necessary listeners.
	 */
	public gui_menu_help(){
		
			// First set properties of top-level menu item
		this.setText("Help");
		this.setFont(gui_globals.PLAIN_NORMAL_FONT);
		this.setMnemonic(KeyEvent.VK_H);
		
			// Intialize menu items
		initialize_menu_items();
		
			// Then add the menu-items to this menu
		this.add(item_full);
		this.addSeparator();
		this.add(item_queue);
		this.add(item_stiki_s);
		this.add(item_filters);
		this.add(item_browser);
		this.add(item_metadata);
		this.add(item_class);
		this.add(item_login);
		this.add(item_comment);
		this.add(item_lastrv);
		this.add(item_pform);
		this.add(item_ort);
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Map menu-item selections to opening of help pane/dialog.
	 */
	public void actionPerformed(ActionEvent event){
		
			try{ // No matter action, open the same help file, just
				 // scroll to a different anchor location therein.
				
				if(event.getSource().equals(item_full))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_FULL);
				else if(event.getSource().equals(item_queue))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_QUEUE);
				else if(event.getSource().equals(item_stiki_s))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_STIKI_S);
				else if(event.getSource().equals(item_filters))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_FILTERS);
				else if(event.getSource().equals(item_browser))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_BROWSER);
				else if(event.getSource().equals(item_metadata))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_METADATA);
				else if(event.getSource().equals(item_class))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_CLASS);
				else if(event.getSource().equals(item_login))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_LOGIN);
				else if(event.getSource().equals(item_comment))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_COMMENT);
				else if(event.getSource().equals(item_lastrv))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_LASTRV);
				else if(event.getSource().equals(item_pform))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_PFORM);
				else if(event.getSource().equals(item_ort))
					gui_help_doc.show_help(this, gui_help_doc.ANCHOR_ORT);
				
			} catch(Exception e){
				
				JOptionPane.showMessageDialog(this,
		       		      "Help file cannot be opened\n" +
		       		      "Please consult on-line documentation",
		       		      "Error: Help file inaccessible",
		       		      JOptionPane.ERROR_MESSAGE);	
			}
	}
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Initialize the menu items beneath the "help" menu header.
	 */
	private void initialize_menu_items(){
		
		item_full = gui_menu_bar.create_menu_item(
				"Show All Help ...", KeyEvent.VK_H);
		item_queue = gui_menu_bar.create_menu_item(
				"Help: Revision Queues", KeyEvent.VK_Q);
		item_stiki_s = gui_menu_bar.create_menu_item(
				"Help: Metadata Scoring", KeyEvent.VK_S);
		item_filters = gui_menu_bar.create_menu_item(
				"Help: Revision Filters", KeyEvent.VK_F);
		item_browser = gui_menu_bar.create_menu_item(
				"Help: Diff Browser", KeyEvent.VK_D);
		item_metadata = gui_menu_bar.create_menu_item(
				"Help: Edit Properties", KeyEvent.VK_E);
		item_class = gui_menu_bar.create_menu_item(
				"Help: Classification", KeyEvent.VK_C);
		item_login = gui_menu_bar.create_menu_item(
				"Help: Login Panel", KeyEvent.VK_L);
		item_comment = gui_menu_bar.create_menu_item(
				"Help: Reversion Comment", KeyEvent.VK_R);
		item_lastrv = gui_menu_bar.create_menu_item(
				"Help: Last Revert Panel", KeyEvent.VK_L);
		item_pform = gui_menu_bar.create_menu_item(
				"Help: STiki Performance", KeyEvent.VK_P);
		item_ort = gui_menu_bar.create_menu_item(
				"Help: Offline Review Tool", KeyEvent.VK_O);
		
		item_full.addActionListener(this);
		item_queue.addActionListener(this);
		item_stiki_s.addActionListener(this);
		item_filters.addActionListener(this);
		item_browser.addActionListener(this);
		item_metadata.addActionListener(this);
		item_class.addActionListener(this);
		item_login.addActionListener(this);
		item_comment.addActionListener(this);
		item_lastrv.addActionListener(this);
		item_pform.addActionListener(this);
		item_ort.addActionListener(this);
	}
	
}
