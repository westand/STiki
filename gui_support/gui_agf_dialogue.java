package gui_support;

import gui_edit_queue.gui_display_pkg;
import gui_panels.gui_comment_panel;
import gui_support.gui_settings.SETTINGS_STR;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.MouseInputListener;

/**
 * Andrew G. West - gui_agf_dialogue.java - This class displays the dialogue
 * associated with AGF ("assume good faith") reverts, that allows a custom
 * message to be placed on the reverted users talk page.
 */
@SuppressWarnings("serial")
public class gui_agf_dialogue extends JDialog implements 
		ActionListener, MouseInputListener, KeyListener{
	
	// ***************************** PUBLIC FIELDS ***************************	
	
	/**
	 * This is the value will be returned by the dialogue if the user
	 * chooses to "abandon" the revert.
	 */
	public static String ABANDON_MSG = "ABANDON";
	
	
	// **************************** PRIVATE FIELDS ***************************	
	
	/**
	 * A map between terse descriptions of pre-written AGF summaries, and 
	 * the more lengthy and wikitext-formatted summaries themselves. The 
	 * keys here are the drop-down box descriptions and the values are the 
	 * text-area content which are associated.
	 */
	private Map<String,String> message_map;
	
	/**
	 * Content of the text-area at the point the dialog is closed. Per the
	 * nature of this class, this field must be subsequently obtained.
	 */
	private String talk_output = null;
	
	/**
	 * Text area which contains the message for user's talk page.
	 */
	private JTextArea text_area;
	
	/**
	 * Drop down box wrapping pre-written text-area content.
	 */
	@SuppressWarnings("rawtypes")
	private JComboBox combo_opts;
	
	/**
	 * Button that submits the dialog (and closes it).
	 */
	private JButton button_submit;
	
	/**
	 * Button closing the dialog without setting 'talk_output' variable.
	 */
	private JButton button_nomsg;
	
	/**
	 * Button abandoning the dialog and the revert action. 
	 */
	private JButton button_abandon;
	
	
	// ***** STATIC FIELDS *****
	
	/**
	 * Define a number of "custom AGF options". These will be imported
	 * and exported to the settings/config file at program start and exit.
	 * These values should NEVER be NULL.
	 */
	private static String AGF_CUSTOM1 = 
			gui_settings.get_str_def(SETTINGS_STR.agf_custom1, "");
	private static String AGF_CUSTOM2 = 
			gui_settings.get_str_def(SETTINGS_STR.agf_custom2, "");
	private static String AGF_CUSTOM3 = 
			gui_settings.get_str_def(SETTINGS_STR.agf_custom3, "");
	private static String AGF_CUSTOM4 = 
			gui_settings.get_str_def(SETTINGS_STR.agf_custom4, "");
	
	/**
	 * Introductory/instructive message for the dialog.
	 */
	private static final String INTRO_MSG =
			"==Section title==\nThe content in this text area when " +
			"the \"submit\" button is pressed will be posted to the " +
			"offending user's talk page. You should include a section " +
			"header (i.e., \"== Section title ==\"). Occurences of " +
			"\"#u#\" and \"#a#\" will be substituted with the user " +
			"name and article title (wiki-linked), respectively. \n\n" +
			"The drop-down box above provides quick access to some " +
			"common and well formatted AGF-style messages.";
	
	
	// **************************** TEST HARNESS *****************************
	
	/**
	 * Test harness for this class. Output is to STDOUT.
	 * @param args No arguments are taken by this method
	 */
	public static void main(String[] args){
		/*JFrame frame = new JFrame();
        frame.setVisible(true);
		System.out.println(new gui_agf_dialogue(frame).get_result(
				"SomeUser", "Main_page"));
		frame.dispose();
		frame.setVisible(false);*/
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Pop the AGF dialogue.
	 * @param frame Parent frame from which the dialog is being popped.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public gui_agf_dialogue(JFrame frame){
		
		super(frame, true); // Be careful of this line
		this.setTitle("Customize the AGF message");
	
			// First handle the "submit" and "cancel" buttons
			// Wrap into independent panel
		button_submit = new JButton("Submit");
		button_submit.setMnemonic(KeyEvent.VK_S);
		button_submit.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_nomsg = new JButton("No MSG");
		button_nomsg.setMnemonic(KeyEvent.VK_N);
		button_nomsg.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_abandon = new JButton("Abandon");
		button_abandon.setMnemonic(KeyEvent.VK_A);
		button_abandon.setFont(gui_globals.PLAIN_NORMAL_FONT);
		
		JPanel panel_buttons = new JPanel();
		panel_buttons.setLayout(new BoxLayout(panel_buttons, BoxLayout.Y_AXIS));
		panel_buttons.add(Box.createVerticalGlue());
		panel_buttons.add(button_submit);
		panel_buttons.add(Box.createVerticalGlue());
		panel_buttons.add(button_nomsg);
		panel_buttons.add(Box.createVerticalGlue());
		panel_buttons.add(button_abandon);
		panel_buttons.add(Box.createVerticalGlue());
		panel_buttons.setBorder(BorderFactory.createEmptyBorder(
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER, 
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER));
		
			// Handle the text area; set initial grey text instructions 
			// (to dissappear on interaction). Make scrollable; panel.
		text_area = new JTextArea(10, 40);
		text_area.setLineWrap(true);
		text_area.setWrapStyleWord(true);
		text_area.setForeground(Color.GRAY);
		text_area.setFont(gui_globals.PLAIN_NORMAL_FONT);
		text_area.setText(INTRO_MSG);
		text_area.setBorder(BorderFactory.createEmptyBorder(
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER, 
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER));
	    JScrollPane text_scrollable = new JScrollPane(text_area);
	    text_scrollable.setVerticalScrollBarPolicy(
	    		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); 
		JPanel panel_text = new JPanel();
		panel_text.add(text_scrollable);
		panel_text.setBorder(BorderFactory.createEmptyBorder(
				gui_globals.PANEL_BORDER, gui_globals.PANEL_BORDER, 
				gui_globals.PANEL_BORDER, gui_globals.PANEL_BORDER));
	    
			// Build the drop-down, initialize to "none"
		message_map = init_message_map();
		combo_opts = new JComboBox();
		combo_opts.setFont(gui_globals.PLAIN_NORMAL_FONT);
		Iterator<String> keyset = message_map.keySet().iterator();
		while(keyset.hasNext())
			combo_opts.addItem(keyset.next());
		combo_opts.setMaximumRowCount(Integer.MAX_VALUE); // Show all options
		combo_opts.setSelectedItem("None");
		
			// Meta-panel for the bottom half of dialog
		JPanel panel_bottom = new JPanel();
		panel_bottom.setLayout(new BoxLayout(panel_bottom, BoxLayout.X_AXIS));
		panel_bottom.add(Box.createHorizontalGlue());
		panel_bottom.add(panel_text);
		panel_bottom.add(Box.createHorizontalGlue());
		panel_bottom.add(panel_buttons);
		panel_bottom.add(Box.createHorizontalGlue());
		
			// Top-half of dialog; containing options drop-down
		JPanel panel_opts = new JPanel();
		panel_opts.setLayout(new BoxLayout(panel_opts, BoxLayout.X_AXIS));
		panel_opts.add(Box.createHorizontalGlue());
		panel_opts.add(new JLabel("Pre-formatted messages:"));
		panel_opts.add(Box.createHorizontalGlue());
		panel_opts.add(combo_opts);
		panel_opts.add(Box.createHorizontalGlue());
		panel_opts.setBorder(BorderFactory.createEmptyBorder(
	    		gui_globals.PANEL_BORDER, gui_globals.PANEL_BORDER, 
	    		gui_globals.PANEL_BORDER, gui_globals.PANEL_BORDER));
		
			// Put all the pieces together
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(Box.createVerticalGlue());
		panel.add(panel_opts);
		panel.add(Box.createVerticalGlue());
		panel.add(panel_bottom);
		panel.add(Box.createVerticalGlue());
		panel.setBorder(BorderFactory.createEmptyBorder(
				gui_globals.PANEL_BORDER, gui_globals.PANEL_BORDER, 
				gui_globals.PANEL_BORDER, gui_globals.PANEL_BORDER));
		
			// Add action listeners
		button_submit.addActionListener(this);
		button_nomsg.addActionListener(this);
		button_abandon.addActionListener(this);
		combo_opts.addActionListener(this);
		text_area.addMouseListener(this);
		text_area.addKeyListener(this);
	
			// Pack in top-level container
		this.addWindowListener(new WindowAdapter(){
		    public void windowClosing(WindowEvent e){
		    	talk_output = ABANDON_MSG;
		}}); // A dialog close is equivalent to "abandon" action
		this.getContentPane().add(panel);
		this.pack();
		this.setLocationRelativeTo(frame); // Center on screen
		this.setVisible(true);
	}

	/**
	 * Assuming this class has been constructed, and that constructor has
	 * completely exited (the dialog has been closed), get the result.
	 * @param edit_pkg Wrapper for the edit chain being operated over. 
	 * @return The result here is the content of the text-area at the
	 * point when the dialog was closed, after substituting any edit
	 * dependent place-holder values. The return value may be NULL
	 * if the dialog was closed via the "cancel" button
	 */
	public String get_result(gui_display_pkg edit_pkg){
		if(talk_output == null)
			return("");
		else if(talk_output.equals(INTRO_MSG))
			return("");
		else if(talk_output.trim().equals(""))
			return("");
		else if(talk_output.equals(ABANDON_MSG))
			return(ABANDON_MSG);
		else return("\n\n" + gui_comment_panel.substitute_placeholders(
				talk_output, edit_pkg));	
	}
	
	/**
	 * Handle action events over dialog components
	 */
	public void actionPerformed(ActionEvent ae){
		if(ae.getSource().equals(combo_opts)){
			text_area.setForeground(Color.BLACK);
			text_area.setText(message_map.get(
					combo_opts.getSelectedItem().toString()));
		} else if(ae.getSource().equals(button_submit)){
			talk_output = text_area.getText();
			this.setVisible(false);
		} else if(ae.getSource().equals(button_nomsg)){
			this.setVisible(false);
		} else if(ae.getSource().equals(button_abandon)){
			talk_output = ABANDON_MSG;
			this.setVisible(false);
		} // Map actions to their source. Basically, we handle button
		  // clicks and drop-down menu selections
	}

	/**
	 * Here we override a bunch of mouse handlers, so we can extend the
	 * click action over the text area (to blank initial instruction msg)..
	 */
	public void mouseClicked(MouseEvent me){}
	public void mouseEntered(MouseEvent me){}
	public void mouseExited(MouseEvent me){}
	public void mouseReleased(MouseEvent me){}
	public void mouseDragged(MouseEvent me){}
	public void mouseMoved(MouseEvent me){}
	public void mousePressed(MouseEvent me){
		if(me.getSource().equals(text_area)){
			if(text_area.getText().equals(INTRO_MSG)){
				text_area.setForeground(Color.BLACK);
				text_area.setText("");
			} // Only convert if intro message is active
		} // Convert gray-intro message to blank form when textarea clicked	
	}

	/**
	 * Override keyboard events. The purpose is to know when text modified,
	 * so we can save changes to "custom" messages
	 */
	public void keyPressed(KeyEvent ke){}
	public void keyReleased(KeyEvent ke){}
	public void keyTyped(KeyEvent ke){
		if(ke.getSource().equals(text_area)){
			if(combo_opts.getSelectedItem().equals("User custom #1")){
				AGF_CUSTOM1 = text_area.getText();
				message_map.put("User custom #1", AGF_CUSTOM1);
			} else if(combo_opts.getSelectedItem().equals("User custom #2")){
				AGF_CUSTOM2 = text_area.getText();
				message_map.put("User custom #2", AGF_CUSTOM2);
			} else if(combo_opts.getSelectedItem().equals("User custom #3")){
				AGF_CUSTOM3 = text_area.getText();
				message_map.put("User custom #3", AGF_CUSTOM3);
			} else if(combo_opts.getSelectedItem().equals("User custom #4")){
				AGF_CUSTOM4 = text_area.getText();
				message_map.put("User custom #4", AGF_CUSTOM4);
			} // Save text if a custom option has been selected
		} // Only concerned with typing in text-area
	}
	
	
	// ***** STATIC ACESSOR METHODS *****
	
	/**
	 * Return the custom AGF messages authored inside/used by the dialog.
	 * @param num Integer code of message. This is just a trivial and 
	 * lightweight identifier that is defined within this class.
	 * @return The AGF message associated with 'num', or NULL if
	 * no such message exists.
	 */
	public static String get_custom_agf(int num){
		if(num == 1) return(AGF_CUSTOM1);
		if(num == 2) return(AGF_CUSTOM2);
		if(num == 3) return(AGF_CUSTOM3);
		if(num == 4) return(AGF_CUSTOM4);
		return(null);
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Write the labels and summaries which constitute the message map. 
	 */
	private Map<String,String> init_message_map(){
		TreeMap<String,String> map = new TreeMap<String, String>();
		
		map.put("None", 
				"");
		
		map.put("Non-notable list entry", 
				"==Recent edit#s# to [[#a#]]==\n" + 
				"[[Image:Information.svg|25px]] Hello, and welcome to " + 
				"Wikipedia. Your recent edit#s# to [[#a#]] added the name " +
				"of an apparently non-notable entity. In general, " +
				"a person or organization added to a list " +  
				"[[Wikipedia:NLIST|should have]] a pre-existing article " + 
				"to establish notability. If you wish to create such " + 
				"an article, please confirm that your subject is " + 
				"notable according to Wikipedia's " + 
				"[[Wikipedia:Notability|notability]] guideline. " +   
				"Thank you! ~~~~");
		
		map.put("Talk in article", 
				"==Recent edit#s# to [[#a#]]==\n" + 
				"[[Image:Information.svg|25px]] Hello, and thank you " +  
				"for your contribution#s# to Wikipedia. I noticed that " +  
				"you recently added commentary to the [[#a#]] article. While " +  
				"Wikipedia welcomes editors' opinions on an article and " +  
				"how it could be changed, these comments are more " +  
				"appropriate for the article's accompanying " +  
				"[[Help:Talk page|talk page]]. If you post your comments " +  
				"there, other [[Wikipedia:Wikipedians|editors]] working " +  
				"on the same article will notice and respond to them, " +  
				"and your comments will not disrupt the flow of the " +  
				"article. Thank you! ~~~~");
		
		map.put("Non-English contributions", 
				"==Recent edit#s# to [[#a#]]==\n " +  
				"[[Image:Information.svg|25px]] Thank you for your " +  
				"contribution#s# to Wikipedia. I noticed that you have " +  
				"posted content to the [[#a#]] article in a language other " +  
				"than English. When on the English-language Wikipedia, " +  
				"please always use English. Thank you! ~~~~");

    map.put("Test edit", 
              "==Recent edits to #a#==\n[[Image:Information.svg|25px]] Hello. I noticed that you recently made an edit that seemed to be a test. Your test worked! However, test edits on live articles disrupt Wikipedia and may confuse readers. If you want more practice editing, the [[Wikipedia:Sandbox|sandbox]] is the best place to do so. If you think I made a mistake, or if you have any questions, you can leave me a message on my talk page. Thanks, ~~~~");

    map.put("Uncited BLP change", 
                  "==Recent edits to #a#==\n[[Image:Information.svg|25px]] Hello. I noticed that you made an edit to a biography of a living person, but that you didn't support your changes with a citation to a [[WP:RS|reliable source]].  Wikipedia has a strict policy concerning [[WP:BLP|how we write about living people]], so please help us keep such articles accurate.  If you think I made a mistake, or if you have any questions, you can leave me a message on my talk page. Thanks, ~~~~");

    map.put("Puffery / Promotion",
        "==Recent edits to #a#==\n[[Image:Information.svg|25px]] Hello.  I noticed that you made an edit that introduces praise or promotional language to the [[#a#]] article.  On Wikipedia, we adhere to a [[WP:NPOV|neutral point of view (NPOV)]], and avoid promotional language or puffery.  Please read the NPOV policy page, as well as [[Wikipedia:Manual of Style/Words to watch|this page of language to avoid]] to better understand how to expand this article in a style suitable to an encyclopedia.  If you have questions, please see [[Wikipedia:Help desk|the Help Desk page]].  Thanks, ~~~~");

		map.put("General revert notify ", 
				"==Recent edit#s# to [[#a#]]==\n" + 
				"[[Image:Information.svg|25px]] Hello, and thank you " +  
				"for your recent contribution#s#. I appreciate the effort " + 
				"you made for our project, but unfortunately I had to " +  
				"undo your edit#s# because I believe the article was " +  
				"better before you made that change. Feel free to " +  
				"contact me directly if you have any questions. " +  
				"Thank you! ~~~~");
		
		map.put("Joke edit",
				"==Recent edit#s# to [[#a#]]==\n" + 
				"[[Image:Information.svg|25px]] " +   
				"[[Wikipedia:Introduction|Welcome]], and thank you for " + 
				"your attempt to lighten up Wikipedia. However, this is " +  
				"an encyclopedia and the articles are intended to be " +  
				"serious, so please don't make joke edits as you did " +  
				"to [[#a#]]. Readers looking for accurate information will " +  
				"not find them amusing. If you'd like to experiment " +  
				"with editing, try the [[Wikipedia:Sandbox|sandbox]], " +  
				"where you are given a good deal of freedom in what " +  
				"you write. Thank you! ~~~~");
		
		map.put("Un-encyclopedic details", 
				"==Recent edit#s# to [[#a#]]==\n" +  
				"[[Image:Information.svg|25px]] Hello, and thank you for " + 
				"your recent contribution#s#. While the content of your edit#s# " +   
				"may be true, I have removed it because its depth or " +  
				"nature of detail are not consistent with our objectives " +  
				"as an encyclopedia. I recognize that your edit was made " +  
				"in good faith and hope you will familiarize yourself " +  
				"with [[WP:NOT|what Wikipedia is not]] so we may " +  
				"collaborate in the future. Thank you! ~~~~");
				
		map.put("Blank",
				"==Recent edit#s# to [[#a#]]==\n" + 
				"[[Image:Information.svg|25px]]\n" + 
				"<!--insert custom message here-->\n" + 
				"Thank you! ~~~~");
		
		map.put("User custom #1", AGF_CUSTOM1);
		map.put("User custom #2", AGF_CUSTOM2);
		map.put("User custom #3", AGF_CUSTOM3);
		map.put("User custom #4", AGF_CUSTOM4);
		return(map);
	}
	
}
