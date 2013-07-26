package gui_panels;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import core_objects.metadata;
import core_objects.stiki_utils;
import core_objects.stiki_utils.QUEUE_TYPE;

import gui_support.gui_globals;
import gui_support.gui_settings;


/**
 * Andrew G. West - gui_comment_panel.java - This class the "comment field",
 * where users can customize the revision commment left with reversions.
 * It also has checkboxes for "warning" and "rollback" options.
 * The class implements the visual elements, and action items on them. 
 */
@SuppressWarnings("serial")
public class gui_comment_panel extends JTabbedPane implements ActionListener{
	
	// ***************************** STATIC FIELDS ***************************
	
	/**
	 * This panel is "tabbed" in layout, with each tab holding details for a
	 * certain kind of revert. This 'enum' names those tabs, and this
	 * constant must be passed with all accessors/modifiers. The "void"
	 * case should be passed when interaction with class is meaningless.
	 */
	public enum COMMENT_TAB{SPAM, VAND, AGF, VOID};
	
	/**
	 * Default text for comments of the "vandalism" form
	 */
	private static final String DEF_TEXT_VAND = "Reverted #q# edit#s# by " +
			"[[Special:Contributions/#u#|#u#]] identified as " +
			"test/vandalism using [[WP:STiki|STiki]]";
	
	/**
	 * Default text for comments of the "spam" form
	 */
	private static final String DEF_TEXT_SPAM = "Reverted #q# edit#s# by " +
			"[[Special:Contributions/#u#|#u#]] identified as " +
			"external link spam using [[WP:STiki|STiki]]";
	
	/**
	 * Default text for comments of the "AGF" form
	 */
	private static final String DEF_TEXT_AGF = 
			"Reverted #q# [[WP:AGF|good faith]] " +
			"edit#s# by [[Special:Contributions/#u#|#u#]] " +
			"using [[WP:STiki|STiki]]";
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Panels storing entire comment-setups; these are what we tab.
	 */
	private JPanel tab_vand;
	private JPanel tab_spam;
	private JPanel tab_agf;
	
	/**
	 * Comment fields where the revision string can be authored.
	 */
	private JTextArea comment_field_vand;
	private JTextArea comment_field_spam;
	private JTextArea comment_field_agf;
	
	/**
	 * Buttons that will allow the user to reset the comment field to default.
	 */
	private JButton comment_button_default_vand;
	private JButton comment_button_default_spam;
	private JButton comment_button_default_agf;
	
	/**
	 * Checkbox to indicate if offending user should receive warning.
	 */
	private JCheckBox warn_checkbox_vand;
	private JCheckBox warn_checkbox_spam;
	private JCheckBox warn_checkbox_agf;
	
	/**
	 * Queue type currently in use (in case comment needs reset to default)
	 */
	private QUEUE_TYPE queue_type = null;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_comment_panel], instansiating comps. and positioning.
	 * @param Initial queue type being pulled from (spam, vandalism, etc.)
	 */
	public gui_comment_panel(QUEUE_TYPE type){
		
	    	// First initialize the individual panes, tab them
		tab_vand = create_comment_tab(COMMENT_TAB.VAND);
		tab_spam = create_comment_tab(COMMENT_TAB.SPAM); 
		tab_agf = create_comment_tab(COMMENT_TAB.AGF); 
		this.addTab("Vand.", tab_vand);
	    this.addTab("Good-faith", tab_agf);
	    
	    	// Initialization business
	    initialize_comments_warns();
	    change_queue_type(type);
	}

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Interface compliance: Take an action completed on a visual GUI
	 * element, and perform the expected model/view behavior.
	 */
	public void actionPerformed(ActionEvent event){
		if(event.getSource().equals(this.comment_button_default_vand)){
			this.make_comment_default(COMMENT_TAB.VAND);
		} else if(event.getSource().equals(this.comment_button_default_spam)){
			this.make_comment_default(COMMENT_TAB.SPAM);
		} else if(event.getSource().equals(this.comment_button_default_agf)){
			this.make_comment_default(COMMENT_TAB.AGF);
		}  // Only actions are "default" button presses; map to right tab
	}

	/**
	 * Return the content of the comment field, replacing any of the
	 * assigned placeholders with the appropriate content.
	 * @param metadata Metadata object wrapping edit being reverted
	 * @param rb_depth Number of edits about to be reverted
	 * @param ct Tab of the comment pane whose comment is desired
	 * @return Current (edit-specific) content of the comment field.
	 */
	public String get_comment(metadata meta, int rb_depth, COMMENT_TAB ct){
		
			// Currently supported placeholders:
			// #u# - User (guilty) who made the edit being reverted.
			// #a# - Article on which the offending edit was placed.
			// #q# - Quantity of edits that will be reverted/RB'ed
			// #s# - Plurality switch. Returns "s" if #q# > 1; "" otherwise.
			// #t# - Time (in seconds) which offending edit survived. 
			
		String comment = get_comment(ct);
		comment = comment.replaceAll("#u#", meta.user);
		comment = comment.replaceAll("#a#", meta.title);
		comment = comment.replaceAll("#q#", rb_depth + "");
		comment = comment.replaceAll("#t#", 
				"" + (stiki_utils.cur_unix_time()-meta.timestamp));
		
		if(rb_depth > 1)
			comment = comment.replaceAll("#s#", "s");
		else comment = comment.replaceAll("#s#", "");
		
		return(comment);
	}	
	
	/**
	 * Return the RAW content of the comment field, placeholders will not
	 * be assigned. This is useful from a settings perspective.
	 * @param ct Tab of the comment pane whose comment is desired
	 * @return Content of the comment field indicated
	 */
	public String get_comment(COMMENT_TAB ct){
		String comment = ""; // Fetch the comment by pane
		if(ct.equals(COMMENT_TAB.VAND))
			comment = comment_field_vand.getText();
		else if(ct.equals(COMMENT_TAB.SPAM))
			comment = comment_field_spam.getText();
		else if(ct.equals(COMMENT_TAB.AGF))
			comment = comment_field_agf.getText();
		return(comment);
	}
	
	/**
	 * Examine whether the "warn user" checkbox is selected or not.
	 * @param ct Which tab of the comment pane to investigate
	 * @return TRUE if the "warn user" checkbox is selected. FALSE, otherwise.
	 */
	public boolean get_warn_status(COMMENT_TAB ct){
		if(ct.equals(COMMENT_TAB.VAND))
			return(this.warn_checkbox_vand.isSelected());
		else if(ct.equals(COMMENT_TAB.SPAM))
			return(this.warn_checkbox_spam.isSelected());
		else if(ct.equals(COMMENT_TAB.AGF))
			return(this.warn_checkbox_agf.isSelected());
		else return(false);
	}
	
	/**
	 * Change the comment field (i.e. when the queue type changes)
	 * @param type Type of queue currently in use (spam, vandalism, etc.)
	 */
	public void change_queue_type(QUEUE_TYPE type){
		if(this.queue_type == type)
			return; // Just break immediately if no change
		this.queue_type = type;
		if(type.equals(QUEUE_TYPE.VANDALISM)){
			this.setComponentAt(0, tab_vand);
			this.setTitleAt(0, "Vand.");
		} else if(type.equals(QUEUE_TYPE.LINK_SPAM)){
			this.setComponentAt(0, tab_spam);
			this.setTitleAt(0, "Spam");
		} // swap tabs and change title
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Initialize a comment panel (to be a tab in a larger environment) 
	 * @param comment_field Persistent textarea for comment input
	 * @param default_button Button resetting the textarea
	 * @param warn_cb Checkbox whether warning should take place.
	 * @return JPanel with initialized and arranged objects, per arguments
	 */
	private JPanel create_comment_tab(COMMENT_TAB ct){
		
		JCheckBox warn_cb = new JCheckBox("Warn Offending Editor?", true);
		warn_cb.setMnemonic(KeyEvent.VK_W);
		warn_cb.setFont(gui_globals.PLAIN_NORMAL_FONT);
		
			// Comment field needs wordwrapped
	    JTextArea comment_field = new JTextArea(5,0);
	    comment_field.setFont(gui_globals.SMALL_NORMAL_FONT);
	    comment_field.setLineWrap(true);
	    comment_field.setWrapStyleWord(true);
	    
			// Beautify the text-area, enable scrolling if necessary
	    comment_field.setBorder(BorderFactory.createEmptyBorder(
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER, 
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER));
	    JScrollPane comment_scrollable = new JScrollPane(comment_field);
	    comment_scrollable.setVerticalScrollBarPolicy(
	    		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); 
    
	    	// Bound the dimensions of the (greedy) scrollable-area
	    int comment_height = comment_scrollable.getPreferredSize().height;
	    comment_scrollable.setMinimumSize(new Dimension(0, comment_height));
	    comment_scrollable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 
	    		comment_height));
	   
	    	// Default button is straightforward
	    JButton default_button = new JButton("Default");
	    default_button.setMnemonic(KeyEvent.VK_D);
	    default_button.setFont(gui_globals.PLAIN_NORMAL_FONT);
	    default_button.addActionListener(this);
	    
	    	// Then add them to this panel according to layout
	    	// using special glue centering where necessary
	    JPanel panel = new JPanel();
	    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    	panel.add(Box.createVerticalGlue());
    	panel.add(gui_globals.center_comp_with_glue(warn_cb));
	    panel.add(Box.createVerticalGlue());
	    panel.add(gui_globals.center_comp_with_glue(comment_scrollable));
	    panel.add(Box.createVerticalGlue());
	    panel.add(gui_globals.center_comp_with_glue(default_button));
	    panel.add(Box.createVerticalGlue());
	    panel.setBorder(BorderFactory.createEmptyBorder(
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER, 
	    		gui_globals.TEXT_FIELD_BORDER, gui_globals.TEXT_FIELD_BORDER));
	    
	    if(ct.equals(COMMENT_TAB.VAND)){
	    	comment_field_vand = comment_field;
	    	comment_button_default_vand = default_button;
	    	warn_checkbox_vand = warn_cb;
	    	panel.setPreferredSize(new Dimension( // force better spacing
	    			panel.getPreferredSize().width, 
	    			(int) (panel.getPreferredSize().height * 1.25)));
	    } else if(ct.equals(COMMENT_TAB.SPAM)){
	    	comment_field_spam = comment_field;
	    	comment_button_default_spam = default_button;
	    	warn_checkbox_spam = warn_cb;
	    	panel.setPreferredSize(new Dimension( // force better spacing
	    			panel.getPreferredSize().width, 
	    			(int) (panel.getPreferredSize().height * 1.25)));
	    } else if(ct.equals(COMMENT_TAB.AGF)){
	    	comment_field_agf = comment_field;
	    	comment_button_default_agf = default_button;
	    	warn_checkbox_agf = warn_cb;
	    	warn_checkbox_agf.setSelected(false);
	    	warn_checkbox_agf.setEnabled(false); // turn off for end users
	    } // Map generic objects to tab-specific ones
	    
		return(panel);
	}
	
	/**
	 * Initialize the comment fields and warning checkboxes. We first attempt 
	 * to get comments from persistent user settings; if that fails we 
	 * use the hard-coded defaults per this file.
	 */
	private void initialize_comments_warns(){
		comment_field_vand.setText(
				gui_settings.get_str_def(
						gui_settings.SETTINGS_STR.comment_vand2, DEF_TEXT_VAND));
		comment_field_spam.setText(
				gui_settings.get_str_def(
						gui_settings.SETTINGS_STR.comment_spam2, DEF_TEXT_SPAM));
		comment_field_agf.setText(
				gui_settings.get_str_def(
						gui_settings.SETTINGS_STR.comment_agf3, DEF_TEXT_AGF));
		
		warn_checkbox_vand.setSelected(
				gui_settings.get_bool_def(
						gui_settings.SETTINGS_BOOL.warn_vand, true));
		warn_checkbox_spam.setSelected(
				gui_settings.get_bool_def(
						gui_settings.SETTINGS_BOOL.warn_spam, true));
		warn_checkbox_agf.setSelected(
				gui_settings.get_bool_def(
						gui_settings.SETTINGS_BOOL.warn_agf, false));
	}

	/**
	 * Change the comment field to the default text.
	 * @param ct Which tab of the comment pane this action maps to
	 */
	private void make_comment_default(COMMENT_TAB ct){
		if(ct.equals(COMMENT_TAB.VAND))
			comment_field_vand.setText(DEF_TEXT_VAND);
		else if(ct.equals(COMMENT_TAB.SPAM))
			comment_field_spam.setText(DEF_TEXT_SPAM);
		else if(ct.equals(COMMENT_TAB.AGF))
			comment_field_agf.setText(DEF_TEXT_AGF);
	}
	
}
