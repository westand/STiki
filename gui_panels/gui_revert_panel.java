package gui_panels;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gui_edit_queue.gui_display_pkg;
import gui_support.gui_globals;
import gui_support.gui_revert_and_warn;
import gui_support.gui_revert_and_warn.RV_STYLE;
import gui_support.gui_revert_and_warn.WARNING;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Andrew G. West - gui_reverted_panel.java - This class displays the 
 * "last reverted" panel -- which contains information about the last 
 * revision for which the "vandalism" classification was made. This includes
 * data such as (1) the nature/success of the revert, (2) the warning
 * level issued, if applicable, (3) a link to the offending-user contribs.
 */
@SuppressWarnings("serial")
public class gui_revert_panel extends JPanel implements ActionListener{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Offending-editor for which reversion was attempted.
	 */
	private String guilty_user;
	
	/**
	 * Article title on which the offending edit took place.
	 */
	private String last_page;

		// These are the panels/labels/links that are shown in the "last
		// revert" panel. They are intuitive and boring, no Java-doc needed.
	private JPanel user_panel;
	private JLabel label_user;
	private JLabel data_warning;
	private JButton link_contribs;
	private JButton link_talk;
	private JButton link_page;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Create a [gui_reverted_panel], intializing the visual components, in
	 * addition to setting their positioning. Initialize to blank
	 */
	public gui_revert_panel(){
				
			// Create panel that displays offending-user links
		link_contribs = gui_globals.create_small_link("(edits)", false, this);
		link_talk = gui_globals.create_small_link("(talk)", false, this);
		link_page = gui_globals.create_small_link("(article)", false, this);
		JPanel link_panel = new JPanel();
		link_panel.setLayout(new BoxLayout(link_panel, BoxLayout.X_AXIS));
		link_panel.add(link_contribs);
		link_panel.add(link_talk);
		link_panel.add(link_page);
		
			// Provide panel to specify offending user (with links)
		label_user = gui_globals.create_data_label("");
		user_panel = new JPanel();
		user_panel.setLayout(new BoxLayout(user_panel, BoxLayout.Y_AXIS));
		user_panel.add(gui_globals.center_comp_with_glue(label_user));
		user_panel.add(gui_globals.center_comp_with_glue(link_panel));
		user_panel.setVisible(false); // Invisible until first RV
		
			// Create panel to display warning-message
		data_warning = gui_globals.plain_centered_multiline_label(
				"No prior revert:<BR>No warning data");
		
			// Arrange above panels vertically, with spacing constants
		JPanel all_panel = new JPanel();
		all_panel.setLayout(new BoxLayout(all_panel, BoxLayout.Y_AXIS));
		all_panel.add(Box.createVerticalGlue());
		all_panel.add(gui_globals.center_comp_with_glue(user_panel));
		all_panel.add(Box.createVerticalGlue());
		all_panel.add(gui_globals.center_comp_with_glue(data_warning));
		all_panel.add(Box.createVerticalGlue());
		
			// Internal panel is used so border can be added; practically
			// unusual but aesthetically needed w/panel this small.
		all_panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		this.setLayout(new GridLayout(0,1));
		this.add(all_panel);
	
			// Set initial state (no offending user -> no user links)
		this.link_contribs.setVisible(false);
		this.link_talk.setVisible(false);
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Change the data-displayed in the "last revert" panel.
	 * @param user_reverted User who made edit for which reversion was last
	 * attempted; needed so that links-map to his/her talk/contribs pages.
	 * @param page_reverted Page on which RB/undo was attempted.
	 * @param rv_style Description of the revert/rollback technique used
	 * @param outcome Description of the warning outcome
	 */
	public void update_display(gui_display_pkg edit_pkg,
			gui_revert_and_warn.RV_STYLE rv_style, 
			gui_revert_and_warn.WARNING outcome){
		
			// Set guilty-user; which also sets up links
		this.guilty_user = edit_pkg.metadata.user;
		this.last_page = edit_pkg.metadata.title;
		this.label_user.setText(trim_editor(edit_pkg.metadata.user));
		this.user_panel.setVisible(true); // Make everything visible
		this.link_contribs.setVisible(true);
		this.link_talk.setVisible(true);
		this.link_page.setVisible(true);
		
			// May need to swap out link color on the user-talk
			// Remember the "user_has_talkpage" query is done prior to our
			// intervention; so if we warned to talkpage, it now exists
			//
			// The 'else' case here captures all warning cases, as well
			// as NO_CUSTOM_MSG which implies a post to user talk page
		if((!edit_pkg.user_has_talkpage) &&
				(outcome.equals(WARNING.NO_EDIT_TOO_OLD) || 
				outcome.equals(WARNING.NO_USER_BLOCK) || 
				outcome.equals(WARNING.NO_BEATEN) || 
				outcome.equals(WARNING.NO_ERROR) || 
				outcome.equals(WARNING.NO_OPT_OUT) || 
				outcome.equals(WARNING.NO_AIV_TIMING)))
			link_talk.setFont(gui_globals.get_link_font(true, true));
		else link_talk.setFont(gui_globals.get_link_font(true, false));
			
			// Set information about any warning that took place. 
			// First line of component will always be the guilty-user
			// This leaves two-terse lines, dependent on warning outcome
		String warn = get_warn_message(rv_style, outcome);
		data_warning.setText("<HTML><CENTER>" + warn + "</CENTER></HTML>");
	}
	
	/**
	 * Overriding: For action-events occuring in this panel. Namely
	 * this is used to map links to browser page display.
	 */
	public void actionPerformed(ActionEvent event){
		
		if(event.getSource().equals(this.link_contribs))	
			gui_globals.open_url(this, "http://en.wikipedia.org/wiki/" +
					"Special:Contributions/" + this.guilty_user);
		else if(event.getSource().equals(this.link_talk))	
			gui_globals.open_url(this, "http://en.wikipedia.org/wiki/" +
					"User_talk:" + this.guilty_user +
					"?vanarticle=" + this.last_page); // Twinkle
		else if(event.getSource().equals(this.link_page))	
			gui_globals.open_url(this, "http://en.wikipedia.org/wiki/" +
					this.last_page);
		
	}
	
	
	// *************************** PRIVATE METHODS ***************************
    
	/**
	 * Given revert outcome/warning, produce a terse human-readable summary.
	 * @param outcome Object desc. if a warning took place, and details about
	 * @return Human-readable summary of the revert outcome/warning/style, with
	 * all line breaks being an HTML line-break (i.e., <BR>).
	 */
	private static String get_warn_message(
			gui_revert_and_warn.RV_STYLE rv_style,
			gui_revert_and_warn.WARNING outcome){
		
		String warn_message = "";
		if(rv_style.equals(RV_STYLE.SIMPLE_UNDO) || 
				rv_style.equals(RV_STYLE.RB_SW_ONE) || 
				rv_style.equals(RV_STYLE.RB_SW_MULTIPLE) ||
				rv_style.equals(RV_STYLE.NOGO_SW))
			warn_message += "Undid ";
		else warn_message += "RB'ed "; // First, the action used
		
		if(outcome.equals(WARNING.NO_BEATEN) || 
				outcome.equals(WARNING.NO_ERROR))
			warn_message += "0 edits<BR>";
		else if(rv_style.equals(RV_STYLE.SIMPLE_UNDO) || 
				rv_style.equals(RV_STYLE.RB_NATIVE_ONE) || 
				rv_style.equals(RV_STYLE.RB_SW_ONE))	
			warn_message += "1 edit<BR>";
		else warn_message += "2+ edits<BR>"; // Next, quantity undone
		
			// Finally, handle warnings and other criteria
		if(outcome.equals(WARNING.NO_BEATEN))
			warn_message += "<FONT COLOR=\"red\"><B>beaten to revert?<BR>" +
					"check page hist</B></FONT>";
		else if(outcome.equals(WARNING.NO_ERROR))
			warn_message += "<FONT COLOR=\"red\"><B>conflict or error<BR>" +
					"check page hist</B></FONT>";
		else if(outcome.equals(WARNING.NO_EDIT_TOO_OLD))
			warn_message += "no warning given<BR>(edit(s) too old)";
		else if(outcome.equals(WARNING.NO_USER_BLOCK))
			warn_message += "no warning given<BR>(user blocked)";
		else if(outcome.equals(WARNING.NO_OPT_OUT))
			warn_message += "no warning given<BR>(STiki opt-out)";
		else if(outcome.equals(WARNING.NO_AIV_TIMING))
			warn_message += "no AIV report<BR>warned since edit";
		else if(outcome.equals(WARNING.NO_CUSTOM_MSG))
			warn_message += "no warning given<BR>AGF notify sent";
		else if(outcome.equals(WARNING.YES_UW1))
			warn_message += "issued warning<BR>at warn-level 1";
		else if(outcome.equals(WARNING.YES_UW2))
			warn_message += "issued warning<BR>at warn-level 2";
		else if(outcome.equals(WARNING.YES_UW3))
			warn_message += "issued warning<BR>at warn-level 3";
		else if(outcome.equals(WARNING.YES_UW4))
			warn_message += "issued warning<BR>at warn-level 4";
		else if(outcome.equals(WARNING.YES_AIV))
			warn_message += "reported to AIV<BR>(will be blocked)";
		else // if(outcome.equals(WARNING.YES_AIV_4IM))
			warn_message += "reported to AIV<BR>(special 4im case)";
		return(warn_message);
	}	
	
	/**
	 * Trim the editor name so it does not overflow the panel width.
	 * @param editor Name of the offending editor
	 * @return 'editor', or if 'editor' is too long, a trimmed version
	 */
	private static String trim_editor(String editor){
		if(editor.length() > 15)
			editor = editor.substring(0, 12) + "...";
		return(editor);
	}
}
