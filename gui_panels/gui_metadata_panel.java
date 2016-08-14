package gui_panels;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URLEncoder;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import mediawiki_api.api_post;
import core_objects.stiki_utils;
import gui_edit_queue.gui_display_pkg;
import gui_support.gui_globals;
import gui_support.gui_settings;

/**
 * Andrew G. West - gui_metadata_panel.java - This class handles the visual
 * presentation and action-events on the 'metadata-panel' -- the GUI portion
 * that displays the RID/title/user/comment of an edit, and provides links
 * to Wikipedia for user-contributions, actual page, etc.
 */
@SuppressWarnings("serial")
public class gui_metadata_panel extends JPanel implements 
		ActionListener, ComponentListener{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Wrapper for RID currently being displayed to user; including metadata.
	 */
	private gui_display_pkg cur_pkg;
	
		// We forgoe a Javadoc description of these components. Basically,
		// each displays the appropriately-named metadata component of
		// the RID currently being displayed in the browser panel.
	private JTextField data_comment;
	private JTextField data_user;
	private JTextField data_title;
	private JTextField data_rid;
	private JTextField data_timestamp;
	
		// We forgoe a Javadoc description of these components. Basically,
		// for each metadata component above, these buttons constitute an 
		// on-Wikipedia link, which is opened in the user-browser. These links
		// provide detailed data, as their textual component suggests.
	private JButton link_user_cont;
	private JButton link_user_user;
	private JButton link_user_talk;
	private JButton link_title;
	private JButton link_title_hist;
	private JButton link_title_talk;
	private JButton link_rid_diff;
	
		// Not a link to Wikipedia as with the above; but formatted
		// in the same look-and-feel. These are "actions"
	private JButton link_user_ignore;
	private JButton link_rid_thank;
	
		// JComponents are grouped vertically into cols; each itself a panel.
	private JPanel intro_panel;
	private JPanel data_panel;
	private JPanel link_panel;
	
		// Finally, the columns are arranged in yet another panel. This is
		// distinct from the 'this' panel extended by this class, because
		// borders may be added. Panel [all_panel] contains only data.[
	private JPanel all_panel;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Create a [gui_metadata_panel], intializing the visual components, in
	 * addition to setting their positioning. Initialize to no RID.
	 */
	public gui_metadata_panel(){

			// Create and layout the introductory labels
		this.intro_panel = new JPanel(new GridLayout(5, 0, 0, 0));
		intro_panel.add(gui_globals.create_intro_label("SUMMARY:"));
		intro_panel.add(gui_globals.create_intro_label("EDITING-USER:"));
		intro_panel.add(gui_globals.create_intro_label("ARTICLE:"));
		intro_panel.add(gui_globals.create_intro_label("REVISION-ID:"));
		intro_panel.add(gui_globals.create_intro_label("TIME-STAMP:"));

			// Create and layout the labels that will display actual-metadata		
		this.data_panel = new JPanel();
		data_panel.setLayout(new GridLayout(5, 0, 0, 0));
		data_panel.add(this.data_comment = gui_globals.create_data_field(""));
		data_panel.add(this.data_user = gui_globals.create_data_field(""));
		data_panel.add(this.data_title = gui_globals.create_data_field(""));
		data_panel.add(this.data_rid = gui_globals.create_data_field(""));
		data_panel.add(this.data_timestamp = gui_globals.create_data_field(""));
		
			// Intialize the Wikipedia-links for more information
		this.link_user_cont = gui_globals.create_link("(contribs.)", false, this);
		this.link_user_user = gui_globals.create_link("(user)", false, this);
		this.link_user_talk = gui_globals.create_link("(talk)", false, this);
		this.link_user_ignore = gui_globals.create_link("(ignore)", false, this);
		this.link_title = gui_globals.create_link("(current)", false, this);
		this.link_title_hist = gui_globals.create_link("(hist.)", false, this);
		this.link_title_talk = gui_globals.create_link("(talk)", false, this);
		this.link_rid_diff = gui_globals.create_link("(wiki-diff)", false, this); 
		this.link_rid_thank = gui_globals.create_link("(thank)", false, this); 
		
			// Arrange links in a simple horizontal fashion
		JPanel panel_usr = new JPanel();
		panel_usr.setLayout(new BoxLayout(panel_usr, BoxLayout.X_AXIS));
		panel_usr.add(Box.createHorizontalStrut(gui_globals.INTRO_LABEL_SPACER));
		panel_usr.add(this.link_user_cont);
		panel_usr.add(this.link_user_user);
		panel_usr.add(this.link_user_talk);
		panel_usr.add(this.link_user_ignore);
		panel_usr.add(Box.createHorizontalGlue());
		JPanel panel_art = new JPanel();
		panel_art.setLayout(new BoxLayout(panel_art, BoxLayout.X_AXIS));
		panel_art.add(Box.createHorizontalStrut(gui_globals.INTRO_LABEL_SPACER));
		panel_art.add(this.link_title);
		panel_art.add(this.link_title_hist);
		panel_art.add(this.link_title_talk);
		panel_art.add(Box.createHorizontalGlue());
		JPanel panel_rev = new JPanel();
		panel_rev.setLayout(new BoxLayout(panel_rev, BoxLayout.X_AXIS));
		panel_rev.add(Box.createHorizontalStrut(gui_globals.INTRO_LABEL_SPACER));
		panel_rev.add(this.link_rid_diff);
		panel_rev.add(this.link_rid_thank);
		panel_rev.add(Box.createHorizontalGlue());

			// Stack the horizontal link-panels vertically
		this.link_panel = new JPanel();
		link_panel.setLayout(new GridLayout(5, 0, 0, 0));
		link_panel.add(Box.createVerticalGlue());
		link_panel.add(panel_usr);
		link_panel.add(panel_art);
		link_panel.add(panel_rev);
		
			// Arrange data-columns into a single column (horizontally)
		this.all_panel = new JPanel();
		all_panel.setLayout(new BoxLayout(all_panel, BoxLayout.X_AXIS));
		all_panel.add(intro_panel);
		all_panel.add(data_panel);
		all_panel.add(link_panel);		
		
			// CRUCIALLY, the 'intro' and 'link' panels content never changes.
			// Their size can be fixed to allow the 'data' (center) panel
			// to stretch and shrink on a per-data basis.
		intro_panel.setMaximumSize(new Dimension(intro_panel.
				getPreferredSize().width, Integer.MAX_VALUE));
		link_panel.setMaximumSize(new Dimension(link_panel.
				getPreferredSize().width, Integer.MAX_VALUE));
			
			// Finally, add the panel to the containing JPanel (which could
			// be bordered). So the 'all_panel' contains only data.  
		this.setLayout(new GridLayout(0,1));
		this.add(all_panel);
		this.addComponentListener(this);
	}
	

	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: For action-events occuring in this 'metadata-panel' --
	 * perform the expected action. Namely, for hyperlinks clicked, open
	 * the user browser to the corresponding URL.
	 */
	public void actionPerformed(ActionEvent event){
	
			// Note that page titles are encoded for links, but usernames are
			// not (user names can legally contain spaces -- separate u-scores,
			// for instance). However, this decision is more practically 
			// driven than based on any wiki documentation.
		try{
			
			if(event.getSource().equals(this.link_user_cont))
				gui_globals.open_url(this, "https://en.wikipedia.org/wiki/" +
					"Special:Contributions/" + this.cur_pkg.metadata.user);
			else if(event.getSource().equals(this.link_user_user))
				gui_globals.open_url(this, "https://en.wikipedia.org/wiki/" +
					"User:" + this.cur_pkg.metadata.user);
			else if(event.getSource().equals(this.link_user_talk))
				gui_globals.open_url(this, "https://en.wikipedia.org/wiki/" +
					"User_talk:" + this.cur_pkg.metadata.user +
					"?vanarticle=" + this.cur_pkg.metadata.title); // Twinkle
			else if(event.getSource().equals(this.link_title))
				gui_globals.open_url(this, "https://en.wikipedia.org/w/" +
					"index.php?title=" + URLEncoder.encode(
					this.cur_pkg.metadata.title, "UTF-8"));
			else if(event.getSource().equals(this.link_title_hist))
				gui_globals.open_url(this, "https://en.wikipedia.org/w/" +
					"index.php?title=" + URLEncoder.encode(
					this.cur_pkg.metadata.title, "UTF-8") + "&action=history");
			else if(event.getSource().equals(this.link_title_talk))
				gui_globals.open_url(this, "https://en.wikipedia.org/wiki/" +
					"Talk:" + this.cur_pkg.metadata.title);
			else if(event.getSource().equals(this.link_rid_diff))	
				gui_globals.open_url(this, "https://en.wikipedia.org/w/" +
					"index.php?oldid=" + this.cur_pkg.page_hist.get(
					this.cur_pkg.rb_depth).rid + "&diff=cur");
			else if(event.getSource().equals(this.link_user_ignore)){
				gui_settings.ignore_editor(this.cur_pkg.metadata.user);
				// gui_globals.pop_ignore_info(this, this.cur_pkg.metadata.user);
				link_user_ignore.setFont(gui_globals.get_link_font(false, true));
			} else if(event.getSource().equals(this.link_rid_thank)){
				cur_pkg.refresh_edit_token(); // unclear why needed
				api_post.THANKS_OUTCOME to = api_post.thank_rid(
						this.cur_pkg.metadata.rid, 
						"stiki", cur_pkg.get_token());
				if(to.equals(api_post.THANKS_OUTCOME.SUCCESS))
					link_rid_thank.setFont(gui_globals.get_link_font(false, true));
			}
			
		} catch(Exception e){
			
			JOptionPane.showMessageDialog(this,
	       		      "Error in the metadata interface,\n" +
	       		      "likely caused by network error \n" + e.getMessage(),
	       		      "Error: Problem in metadata pane",
	       		      JOptionPane.ERROR_MESSAGE);
			
		} // Map action-events (link-clicks) to their handlers
	}

	/**
	 * Set the metadata of the RID currently displayed.
	 * @param gui_pkg Wrapper for all details of current RID, incl. metadata
	 */
	public void set_displayed_rid(gui_display_pkg gui_pkg){
		this.cur_pkg = gui_pkg;
		
			// Data fields are generally straightforward
		data_rid.setText(this.cur_pkg.metadata.rid + "");
		data_title.setText(this.cur_pkg.metadata.title);
		data_user.setText(this.cur_pkg.metadata.user + 
				summarize_perms(this.cur_pkg.user_perms));
		data_timestamp.setText(time_ago_str(this.cur_pkg.metadata.timestamp));
		data_comment.setText(this.cur_pkg.metadata.comment);
		
			// We set tool tip text for all fields, but "comment" in particular
			// in the hope the ToolTip will be able to show complete
			// text that overruns STiki's horizontal space
		data_rid.setToolTipText(this.cur_pkg.metadata.rid + "");
		data_title.setToolTipText(this.cur_pkg.metadata.title);
		data_user.setToolTipText(this.cur_pkg.metadata.user + 
				summarize_perms(this.cur_pkg.user_perms));
		data_timestamp.setToolTipText(time_ago_str(this.cur_pkg.metadata.timestamp));
		data_comment.setToolTipText(this.cur_pkg.metadata.comment);
		
			// Link colors may need adjusted
		if(gui_pkg.user_has_talkpage)
			link_user_talk.setFont(gui_globals.get_link_font(false, false));
		else link_user_talk.setFont(gui_globals.get_link_font(false, true));
		
		if(gui_pkg.user_has_userpage)
			link_user_user.setFont(gui_globals.get_link_font(false, false));
		else link_user_user.setFont(gui_globals.get_link_font(false, true));
		
		if(gui_pkg.title_has_talkpage)
			link_title_talk.setFont(gui_globals.get_link_font(false, false));
		else link_title_talk.setFont(gui_globals.get_link_font(false, true));
		
		link_user_ignore.setFont(gui_globals.get_link_font(false, false));
		link_rid_thank.setFont(gui_globals.get_link_font(false, false));
		
		resize(); // Adjust space given to panel containing MD-items
	}
	
	/**
	 * Redraw the meta-panel such that the "data-panel" (that containing
	 * per-RID data), occupies (1) only the space it requires, if it is 
	 * available, or (2) only the space that is available, assuming/allowing 
	 * the "introduction" and "link" panels to remain at natural size.
	 * Thus, if a size reduction must take place, the data-panel is shortened. 
	 */
	public void resize(){
		
			// Intro and link sizes are fixed. Given this, and the current
			// panel size, how much space is there for the data column?
		int space_remaining = (this.all_panel.getSize().width - 
			(intro_panel.getSize().width + link_panel.getSize().width));	
		
			// Figure out how much space the data-column WANTS, which is 
			// the width of its widest component. Critically, we cannot
			// do this at the column-level, since this is precisely the
			// component we are resizing below.
		int d_width = Math.max(0, data_rid.getPreferredSize().width);
		d_width = Math.max(d_width, data_title.getPreferredSize().width);
		d_width = Math.max(d_width, data_user.getPreferredSize().width);
		d_width = Math.max(d_width, data_timestamp.getPreferredSize().width);
		d_width = Math.max(d_width, data_comment.getPreferredSize().width);
		
		if(d_width >= space_remaining) // Give data-panel all space available,
			d_width = space_remaining; // but no more than it wants

			// Fix the data-panels size accordingly
		data_panel.setMinimumSize(new Dimension(d_width, 0));
		data_panel.setPreferredSize(new Dimension(d_width, 
				data_panel.getPreferredSize().height));
		data_panel.setMaximumSize(new Dimension(d_width, Integer.MAX_VALUE));
		this.revalidate(); // Redraw the components!
	}
	
		// Interface compliance for ComponentEvents. The only event of interest
		// is a resizing, which should change space allocated to data-panel. 
	public void componentResized(ComponentEvent event){resize();}
	public void componentHidden(ComponentEvent event){}
	public void componentMoved(ComponentEvent event){}
	public void componentShown(ComponentEvent event){}
			
	
	// *************************** PRIVATE METHODS ***************************
    
    /**
     * Given a pst UNIX timestamp, return a verbose but succint string
     * describing "how" long ago it was. For example "1 day and 3 hours ago".
     * @param unix_ts UNIX timestamp, sometime before "now"
     * @return String describing interval between 'now' and 'unix_ts'
     */
    private String time_ago_str(long unix_ts){
    	
    		// Compute the time elapsed
    	int interval = (int) (stiki_utils.cur_unix_time() - unix_ts);
    	
    		// Separate interval into days/hours/minutes/seconds
    	int days = (interval/86400);
    	interval = interval % 86400;
    	int hours = (interval/3600);
    	interval = interval % 3600;
    	int minutes = (interval/60);
    	int seconds = interval % 60;
    	
    	String ta_str;
    	if(days > 0)
    		ta_str = (days + " days and " + hours + " hours ago");	
    	else{ // if days == 0
    		if(hours > 0)
    			ta_str = (hours + " hours and " + minutes + " minutes ago");
    		else // if hours == 0
    			ta_str = (minutes + " minutes and " + seconds + " seconds ago");
    	} // Turn into string, using only two broadest fields necessary
    	
    		// Now do a little grammar fix for singular instances
    	if(days == 1) ta_str = ta_str.replace("days", "day");
    	if(hours == 1) ta_str = ta_str.replace("hours", "hour");
    	if(minutes == 1) ta_str = ta_str.replace("minutes", "minute");
    	if(seconds == 1) ta_str = ta_str.replace("seconds", "second");
    	return(ta_str);
    }
    
    /**
     * Provided a set of user permissions, abreviate those permissions
     * and compile them into a more succinct String for GUI display.
     * @param user_perms Set of editor permissions, per API
     * @return String format of those permissions, abbreviated, per the
     * following Wikipedia standardized table. For brevity, only those
     * relevant to anti-vandal work are included:
     * 
     * 		autoconf	"autoconfirmed"
     * 		conf  		"confirmed"
     * 		review  	"reviewer"
     * 		rb  		"rollbacker"
     * 		admin  		"sysop"
	 */
    private String summarize_perms(Set<String> user_perms){
    	
    	if(user_perms.size() == 0)
    		return("");
    	
    	String out = " (";
    	if(user_perms.contains("autoconfirmed"))
    		out += "autoconf, ";
    	if(user_perms.contains("confirmed"))
    		out += "conf, ";
    	if(user_perms.contains("reviewer"))
    		out += "review, ";
    	if(user_perms.contains("rollbacker"))
    		out += "rb, ";
    	if(user_perms.contains("sysop"))
    		out += "admin, ";
    	
    	if(out.length() == 2) // no permissions captured
    		return(" (no permissions)");
    	out = out.substring(0, out.length() - 2); // trim trailing cruft
    	out += ")";
    	return(out);
    }
    
}
