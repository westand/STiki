package gui_menus;

import gui_support.gui_filesys_images;
import gui_support.gui_globals;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

/**
 * Andrew G. West - gui_menu_about.java - This class creates the menu 
 * "About STiki", its menu items, and the action handlers on top of them.
 */
@SuppressWarnings("serial")
public class gui_menu_about extends JMenu implements ActionListener{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Height, in pixels, of the displayed "About" frame.
	 */
	private static final int ABOUT_HEIGHT = 350;
	
	/**
	 * Width, in pixels, of the displayed "About" frame.
	 */
	private static final int ABOUT_WIDTH  = 200;
	
	/**
	 * Menu item "About STiki" -- launches associated info-window.
	 */
	private JMenuItem item_about;
	
	/**
	 * Menu item "Visit Website" -- launches browser with STiki website.  
	 */
	private JMenuItem item_website;

	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_menu_about] -- creating the menu button, 
	 * its menu items, and registering all necessary listeners.
	 */
	public gui_menu_about(){
		
			// First set properties of top-level menu item
		this.setText("About STiki");
		this.setFont(gui_globals.PLAIN_NORMAL_FONT);
		this.setMnemonic(KeyEvent.VK_S);
		
			// Initialize the menu-items
		item_about = gui_menu_bar.create_menu_item(
				"About STiki", KeyEvent.VK_A);
		item_about.addActionListener(this);
		item_website = gui_menu_bar.create_menu_item(
				"Visit Website", KeyEvent.VK_W);
		item_website.addActionListener(this);
		
			// Then add the menu-items to this menu
		this.add(item_about);
		this.add(item_website);
	}

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Map menu actions to the appropriate handlers.
	 */
	public void actionPerformed(ActionEvent event){
		if(event.getSource().equals(item_about)){
			try{open_about_panel();}
			catch(Exception e){}
		}else if(event.getSource().equals(item_website)){
			String url = "http://en.wikipedia.org/wiki/Wikipedia:STiki";
			gui_globals.open_url(this, url);
		} // Simple action-event handlers
	}
	

	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Open the Frame containing "About STiki" content.
	 */
	private static void open_about_panel() throws Exception{
				
			// First create the pane for content display
		JFrame frame = new JFrame();
		Dimension screen_size = frame.getToolkit().getScreenSize();
		int win_locx = ((screen_size.width - ABOUT_WIDTH) / 2);
	    int win_locy = ((screen_size.height - ABOUT_HEIGHT) / 2);
		frame.setBounds(win_locx, win_locy, ABOUT_WIDTH, ABOUT_HEIGHT);
		frame.setTitle("About STiki");
		frame.setIconImage(gui_filesys_images.ICON_64);
		
			// Prepare the STiki logo for display
		Image img = gui_filesys_images.ICON_128;
		JLabel label = new JLabel(new ImageIcon(img));
		label.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
			// Text content of the "about" frame
		JLabel version = gui_globals.plain_centered_multiline_label(
				"Version 2.1");
		JLabel author = gui_globals.plain_centered_multiline_label(
				"by Andrew G. West<BR>westand@cis.upenn.edu");
		JLabel copyright = gui_globals.plain_centered_multiline_label(
				"Copyright&#169 2010-13");
		JLabel support = gui_globals.plain_centered_multiline_label(
				"Development of STiki was supported in part by " +
				"ONR MURI N00014-07-1-0907");
			
			// Arrange all components in a single vertical column, all
			// centered, with equi-distant vertical spacing between them.
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(Box.createVerticalGlue());
		panel.add(gui_globals.center_comp_with_glue(label));
		panel.add(Box.createVerticalGlue());
		panel.add(gui_globals.center_comp_with_glue(version));
		panel.add(Box.createVerticalGlue());
		panel.add(gui_globals.center_comp_with_glue(author));
		panel.add(Box.createVerticalGlue());
		panel.add(gui_globals.center_comp_with_glue(copyright));
		panel.add(Box.createVerticalGlue());
		panel.add(gui_globals.center_comp_with_glue(support));
		panel.add(Box.createVerticalGlue());
		panel.setBorder(BorderFactory.createEmptyBorder(
				gui_globals.OUT_BORDER_WIDTH, gui_globals.OUT_BORDER_WIDTH,
				gui_globals.OUT_BORDER_WIDTH, gui_globals.OUT_BORDER_WIDTH));
		
		frame.add(panel);
		frame.setVisible(true);
	}

}
