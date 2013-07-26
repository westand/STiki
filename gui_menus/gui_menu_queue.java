package gui_menus;

import executables.stiki_frontend_driver;
import gui_support.gui_filesys_images;
import gui_support.gui_globals;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import core_objects.stiki_utils;
import core_objects.stiki_utils.QUEUE_TYPE;
import core_objects.stiki_utils.SCORE_SYS;

/**
 * Andrew G. West - gui_menu_queue.java - This class creates the menu 
 * "Queue", its menu items, and the action handlers on top of them. The 
 * menu allows users to select which queue they are pulling edits from
 */
@SuppressWarnings("serial")
public class gui_menu_queue extends JMenu implements ActionListener{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Radio button for the "Cluebot-NG" queue.
	 */
	private JRadioButtonMenuItem item_cluebotng;
	
	/**
	 * Radio button for the "STiki (metadata)" queue.
	 */
	private JRadioButtonMenuItem item_stiki;
	
	/**
	 * Radio button for the "WikiTrust" queue.
	 */
	private JRadioButtonMenuItem item_wikitrust;
	
	/**
	 * Radio button for the "WikiTrust" queue.
	 */
	private JRadioButtonMenuItem item_spam;
	
	/**
	 * Radio button for the "Meta (combination)" queue.
	 */
	private JRadioButtonMenuItem item_meta;
	
	/**
	 * Menu item popping a dialogue describing recent usage rates.
	 */
	private JMenuItem item_recent_use;
	
	/**
	 * Menu item popping a simplified leaderboard.
	 */
	private JMenuItem item_leaderboard;
	
	
	/////
	
	/**
	 * Parent driver. This provides (1) a GUI frame from which to pop
	 * any needed dialogs, and (2) access to the client DB connection,
	 * which is needed to obtain "recent usage" statistics.
	 */
	private stiki_frontend_driver parent;
	
	/**
	 * Indicator of the currently selected queue (radio button). 
	 */
	private SCORE_SYS selected_queue;
	
	/**
	 * Type of currently selected queue (vandalism, spam, etc).
	 */
	private QUEUE_TYPE selected_type;
	

	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_menu_queue] -- creating its items, and listeners.
	 * @param default_queue The queue which should be selected by default
	 */
	public gui_menu_queue(stiki_frontend_driver parent, 
			SCORE_SYS default_queue){
		
			// Assign passed parameters
		this.parent = parent;
		
			// First set properties of top-level menu item
		this.setText("Rev. Queue");
		this.setFont(gui_globals.PLAIN_NORMAL_FONT);
		this.setMnemonic(KeyEvent.VK_Q);
		
			// Initialize the menu-items
		item_cluebotng = gui_globals.radiobutton_item("Cluebot-NG", 
				KeyEvent.VK_C, true, false); 
		item_stiki = gui_globals.radiobutton_item("STiki (metadata)", 
				KeyEvent.VK_S, true, false);
		item_wikitrust = gui_globals.radiobutton_item("WikiTrust", 
				KeyEvent.VK_W, false, false);
		item_spam = gui_globals.radiobutton_item("Link Spam", 
				KeyEvent.VK_W, false, false);
		item_meta = gui_globals.radiobutton_item("Meta (combination)", 
				KeyEvent.VK_META, false, false);
			/////
		item_recent_use = gui_menu_bar.create_menu_item(
				"Recent usage stats.", KeyEvent.VK_R);
		item_leaderboard = gui_menu_bar.create_menu_item(
				"Generate leaderboard", KeyEvent.VK_L);
			
			// Add the action-listener to all items
		item_cluebotng.addActionListener(this);
		item_stiki.addActionListener(this);
		item_wikitrust.addActionListener(this);
		item_spam.addActionListener(this);
		item_meta.addActionListener(this);
		item_recent_use.addActionListener(this);
		item_leaderboard.addActionListener(this);
		
			// Add to ButtonGroup to enforce "one selected" semantic
	    ButtonGroup bgroup = new ButtonGroup();
	    bgroup.add(item_cluebotng);
	    bgroup.add(item_stiki);
	    bgroup.add(item_wikitrust);
	    bgroup.add(item_spam);
	    bgroup.add(item_meta);
		
			// Then add the menu-items to this menu
		this.add(item_cluebotng);
		this.add(item_stiki);
		this.add(item_wikitrust);
		this.add(item_spam);
		this.add(item_meta);
		this.addSeparator();
		this.add(item_recent_use);
		this.add(item_leaderboard);
		
			// Finally, set the initial state
		selected_queue = default_queue;
		selected_type = stiki_utils.queue_to_type(selected_queue);
		set_initial_state(selected_queue);
	}

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Map menu actions to the appropriate handlers.
	 */
	public void actionPerformed(ActionEvent event){
		
		if(event.getSource().equals(item_recent_use)){
			try{pop_recent_use_dialog();}
			catch(Exception e){
				System.out.println("Error in recent usage stats. dialog pop:");
				e.printStackTrace();
			} // Must try-catch for interface compliance (query within)
		} else if(event.getSource().equals(item_leaderboard)){
			try{pop_leaderboard_dialog();}
			catch(Exception e){
				System.out.println("Error in leaderboard dialog pop:");
				e.printStackTrace();
			} // Must try-catch for interface compliance (query within)
		} else{
			if(item_cluebotng.isSelected())
				selected_queue = SCORE_SYS.CBNG;
			else if(item_stiki.isSelected())
				selected_queue = SCORE_SYS.STIKI;
			else if(item_wikitrust.isSelected())
				selected_queue = SCORE_SYS.WT;
			else if(item_spam.isSelected())
				selected_queue = SCORE_SYS.SPAM;
			selected_type = stiki_utils.queue_to_type(selected_queue);
		} // All class items pertain to a choose-one radio selection; 
		  // except one which is handled before all others
	}
	
	/**
	 * Return the queue currently selected in the radio button group.
	 * @return the queue currently selected in the radio button group
	 */
	public SCORE_SYS selected_queue(){
		return(selected_queue);
	}
	
	/**
	 * Return the type (vandalism, spam, etc.) of the selected queue
	 * @return the type of the currently selected queue.
	 */
	public QUEUE_TYPE selected_type(){
		return(selected_type);
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Set the initially selected queue in a programmatic fashion
	 * @param default_sys Queue which should be selected
	 */
	private void set_initial_state(SCORE_SYS default_sys){
		if(default_sys.equals(SCORE_SYS.STIKI)) 
			item_stiki.setSelected(true);
		else if(default_sys.equals(SCORE_SYS.CBNG))
			item_cluebotng.setSelected(true);
		else if(default_sys.equals(SCORE_SYS.WT))
			item_stiki.setSelected(true);
		else if(default_sys.equals(SCORE_SYS.SPAM))
			item_spam.setSelected(true);
	}

	/**
	 * Pop a dialog providing recent STiki/queue use statistics. Designed
	 * to inform users about recent traffic, possibly telling them to use
	 * STiki at a later time or select a low-traffic queue to positively
	 * influence tool hit-rate.
	 */
	private void pop_recent_use_dialog() throws Exception{
		
		DecimalFormat df = new DecimalFormat("#.##");
		StringBuilder sb_message = new StringBuilder();
		sb_message.append(
			"The following are STiki/queue usage statistics.\n" +
			"Large quantities of recent use are likely to\n" +
			"decrease vandalism hit-rates. These statistics\n" +
			"are presented so users can best allocate their\n" +
			"efforts as they see fit, either by varying\n" + 
			"queues or usage times:\n\n");
		
			// Get recent usage statistics via DB (one hour ago)
		long time_ago = stiki_utils.cur_unix_time() - 3600;
		int[] recent_use = parent.client_interface.recent_use(time_ago);
		int stiki_use = recent_use[0];
		double stiki_per = (stiki_use > 0) ? 100.0*recent_use[1]/stiki_use : 0.0;
		int cbng_use = recent_use[2];
		double cbng_per = (cbng_use > 0) ? 100.0*recent_use[3]/cbng_use : 0.0;
		int wt_use = recent_use[4];
		double wt_per = (wt_use > 0) ? 100.0*recent_use[5]/wt_use : 0.0;
		int spam_use = recent_use[6];
		double spam_per = (spam_use > 0) ? 100.0*recent_use[7]/spam_use : 0.0;
		
			// Append one-hour statistics into message
		sb_message.append(
			"QUEUE - CLASSIFICATIONS (REVERT-%)\n\n" + 
			"In the last one hour:\n" +
			"---------------------------------------\n" + 
			"ClueBot NG - " + cbng_use + " (" + df.format(cbng_per) + "%)\n" + 
			"Metadata - " + stiki_use + " (" + df.format(stiki_per) + "%)\n" + 
			"Wikitrust - " + wt_use + " (" + df.format(wt_per) + "%)\n" + 
			"Link Spam - " + spam_use + " ("  + df.format(spam_per) + "%)\n\n");
				
			// Get recent usage statistics via DB (six hours ago)
		time_ago = stiki_utils.cur_unix_time() - 3600*6;
		recent_use = parent.client_interface.recent_use(time_ago);
		stiki_use = recent_use[0];
		stiki_per = (stiki_use > 0) ? 100.0*recent_use[1]/stiki_use : 0.0;
		cbng_use = recent_use[2];
		cbng_per = (cbng_use > 0) ? 100.0*recent_use[3]/cbng_use : 0.0;
		wt_use = recent_use[4];
		wt_per = (wt_use > 0) ? 100.0*recent_use[5]/wt_use : 0.0;
		spam_use = recent_use[6];
		spam_per = (spam_use > 0) ? 100.0*recent_use[7]/spam_use : 0.0;		

			// Append six-hour statistics into message
		sb_message.append(
			"In the last six hours:\n" + 
			"---------------------------------------\n" + 
			"ClueBot NG - " + cbng_use + " (" + df.format(cbng_per) + "%)\n" + 
			"Metadata - " + stiki_use + " (" + df.format(stiki_per) + "%)\n" + 
			"Wikitrust - " + wt_use + " (" + df.format(wt_per) + "%)\n" + 
			"Link Spam - " + spam_use + " ("  + df.format(spam_per) + "%)\n\n");
			
		JOptionPane.showMessageDialog(parent, sb_message.toString(),
				"Recent STiki/queue usage statistics", 
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Pop the dialogue/window showign the simplified leaderboard. 
	 */
	private void pop_leaderboard_dialog() throws Exception{
		
		String html = leaderboard_str(parent.login_panel.get_login_user_field());
		
			// All of what follows is based heavily from the help-doc
		
		JFrame frame = new JFrame();
		Dimension screen_size = frame.getToolkit().getScreenSize();
		int win_locx = ((screen_size.width - 595) / 2);
	    int win_locy = ((screen_size.height - 700) / 2);
		frame.setBounds(win_locx, win_locy, 700, 595);
		frame.setTitle("Simplified STiki Leaderboard");
		frame.setIconImage(gui_filesys_images.ICON_64);
	
			// Add help-doc HTML pane, make scrollable, add to frame
		JEditorPane content = gui_globals.create_stiki_html_pane(null, false);
		content.setText(html);
		JScrollPane content_scrollable = new JScrollPane(content);
		content.setCaretPosition(0); // reset to leaderboard top
		frame.add(content_scrollable);
		
			// Beautify the frame with borders
		Border empty_border = BorderFactory.createEmptyBorder(
				gui_globals.BROWSER_BORDER, gui_globals.BROWSER_BORDER, 
				gui_globals.BROWSER_BORDER, gui_globals.BROWSER_BORDER);
		Border outline_border = BorderFactory.createLineBorder(Color.BLACK, 1);
		content.setBorder(empty_border);
		content_scrollable.setBorder(BorderFactory.
				createCompoundBorder(empty_border, outline_border));
			
			// Make visible; already scrolled at creation
		frame.setVisible(true);
	}
	
	/**
	 * Produce an HTML-table format of a simplified STiki leaderboard.
	 * @param user_in User making this request
	 * @return Simplified STiki leaderboard (as an HTML string)
	 */
	private String leaderboard_str(String user_in) throws Exception{
		
		String csv_leaderboard = this.parent.client_interface.leaderboard();
		StringBuilder sb_leaderboard = new StringBuilder();
		sb_leaderboard.append("" +
				"<DIV ALIGN=\"CENTER\"><TABLE BORDER=\"1\">\n<TR>" +
				"<TD BGCOLOR=\"#C0C0C0\"><B>RANK</B></TD>" +
				"<TD BGCOLOR=\"#C0C0C0\"><B>USER</B></TD>" +
				"<TD BGCOLOR=\"#C0C0C0\"><B>CLASS-#</B></TD>" +
				"<TD BGCOLOR=\"#C0C0C0\"><B>VAND-%</B></TD>" +
				"<TD BGCOLOR=\"#C0C0C0\"><B>AGF-%</B></TD></TR>\n");
		DecimalFormat df = new DecimalFormat("#.##");
		
		String user;
		int quant, vand, agf, rank=0, user_rank = 0;;
		String[] parts = csv_leaderboard.split(",");
		for(int i=0; i < parts.length; i+=4){
			rank++;
			user = parts[i];
			quant = Integer.parseInt(parts[i+1]);
			vand = Integer.parseInt(parts[i+2]);
			agf = Integer.parseInt(parts[i+3]);
			if(user.equalsIgnoreCase(user_in))
				user_rank = rank;
			sb_leaderboard.append("<TR>" +
					"<TD>" + rank + "</TD>" +
					"<TD>" + user + "</TD>" +
					"<TD>" + quant + "</TD>" +
					"<TD>" + df.format(100.0 * vand / quant) + "%</TD>" +
					"<TD>" + df.format(100.0 * agf / quant)  + "%</TD></TR>\n");
		} // Convert CSV into HTML table format
		sb_leaderboard.append("</TABLE></DIV>\n");
		
		sb_leaderboard.insert(0,
				"You are in position " + user_rank + 
				" on the leaderboard:<BR><BR>\n");
		return(sb_leaderboard.toString());		
	}
	
}
