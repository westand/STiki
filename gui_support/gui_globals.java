package gui_support;

import gui_menus.gui_text_menu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.HTMLDocument;

import core_objects.stiki_utils;

/**
 * Andrew G. West - gui_globals.java - This class implements the static
 * method and constants used in the programming of the STiki GUI.
 */
@SuppressWarnings("serial")
public class gui_globals{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Fixed width assigned to the left-sidebar.
	 * Changes 225->235 JUL-2016 to try to beautify Apple/Mac presentation
	 */
	public static final int LEFT_SIDEBAR_WIDTH = 235;
	
	/**
	 * Border, in pixels, which should separate major component panels.
	 */
	public static final int OUT_BORDER_WIDTH = 8;
	
	/**
	 * Border, in pixels, for purposes internal to major component panels.
	 */
	public static final int IN_BORDER_WIDTH = 3;
	
	/**
	 * Border, in pixels, which should be given to text-entry fields.
	 */
	public static final int TEXT_FIELD_BORDER = 3;
	
	/**
	 * Border, in pixels, given to space compacted/adjacent panels.
	 */
	public static final int PANEL_BORDER = 3;
	
	/**
	 * Border, in pixels, which should buffer a browser text from its
	 * surrounding window. This is differentiated from [TEXT_FIELD_BORDER]
	 * because browsers are typically more heavyweight structures.
	 */
	public static final int BROWSER_BORDER = 8;
	
	/**
	 * Space, in pixels, to use after label-semicolons, and the the data
	 * fields they introduce (i.e. "Name: [INTRO_LABEL_SPACER] Andrew West")
	 */
	public static final int INTRO_LABEL_SPACER = 10;

	/**
	 * Horizontal spacing, in pixels, between elements on the JMenuBar,
	 * or perhaps between elements and the vertical separators beween them.
	 */
	public static final int MENUBAR_HORIZ_SPACING= 10;
	
	/**
	 * Font to use when a "title-sized, bold" font is desired.
	 */
	public static final Font BOLD_TITLE_FONT = new Font(
			Font.SANS_SERIF, Font.BOLD, 14);
	
	/**
	 * Font to use when a "normal-sized, bold" font is desired.
	 */
	public static final Font BOLD_NORMAL_FONT = new Font(
			Font.SANS_SERIF, Font.BOLD, 12);
	
	/**
	 * Font to use when a "normal-sized, plain-face" font is desired.
	 */
	public static final Font PLAIN_NORMAL_FONT = new Font(
			Font.SANS_SERIF, Font.PLAIN, 12);
	
	/**
	 * Font to use when a "small-sized, plain-face" font is desired.
	 */
	public static final Font SMALL_NORMAL_FONT = new Font(
			Font.SANS_SERIF, Font.PLAIN, 10);
	
	/**
	 * Font to use when a "tiny-sized, plain-face" font is desired.
	 */
	public static final Font TINY_NORMAL_FONT = new Font(
			Font.SANS_SERIF, Font.PLAIN, 8);
	
	/**
	 * Font to use when a "normal-sized, serif, plain-face" font is desired.
	 * To be used in circumstances where large amounts of text are displayed.
	 */
	public static final Font PLAIN_SERIF_FONT = new Font(
			Font.SERIF, Font.PLAIN, 12);
	
	/**
	 * Default font to use in browsers (JEditorPanes displaying HTML).
	 */
	public static final Font DEFAULT_BROWSER_FONT = new Font(
			gui_settings.get_str_def(
			gui_settings.SETTINGS_STR.options_fontfam, Font.SANS_SERIF), 
			Font.PLAIN, 12);
	
	/**
	 * Horizontal rule/separator for use in menus.
	 */
	public static final Component HORIZ_MENU_SEP = 
			Box.createHorizontalStrut(gui_globals.MENUBAR_HORIZ_SPACING);
	

	// ************ REGEX *************
	
	/**
	 * String representation (unicode) of a Zero-Width-Space (ZWS). 
	 */
	public static final String ZWS = "\u200B";
	
	/**
	 * Character representation (unicode) of a Zero-Width-Space (ZWS). 
	 */
	public static final char ZWS_CHAR = '\u200B';
	
	/**
	 * String representation (unicode) of a soft-hyphen.
	 */
	public static final String SOFT_HYPHEN = "\u00AD";
	
	/**
	 * The '<' character has several forms, as encoded by this regex.
	 */
	public static final String LEFT_BRACK_REGEX = "(<|\\&lt;|\\&l;t;)";
	
	/**
	 * The '>' character has several forms, as encoded by this regex.
	 */
	public static final String RIGHT_BRACK_REGEX = "(>|\\&gt;|\\&g;t;)";
	
	/**
	 * Regex describing non-greedy tags of the form <?>, with all possible
	 * formats of the left and right bracket being considered.
	 */
	public static final String BRACKETED_REGEX = 
		LEFT_BRACK_REGEX + ".*?" + RIGHT_BRACK_REGEX;
	
	/**
	 * Regex describing non-greedy tags of the form <?>, with only literal
	 * forms of the left and right bracket being considered.
	 */
	public static final String STRICT_BRACKETED_REGEX = "<.*?>";
	
	
	// ************* MISC *************
	
	/**
	 * Given the over-use of the "PASS" button (relative to "INNOCENT"),
	 * we pop a pass-warning at certain "milestones" in pass usage
	 */
	public static final Set<Integer> PASS_WARN_POINTS = set_pass_warn_points();
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Create a JEditorPane designed for the display of an HTML page, and
	 * set its style such that is consistent with other STiki GUI elements
	 * @param URL URL to intialize the pane with. No content will be 
	 * displayed if the NULL value is passed.
	 * @param copyable If TRUE then one will be able to highlight and 
	 * copy-(paste) text out of this pane. Else, such functionality 
	 * will be disabled.
	 * @return A new STiki-style, JEditorPane, which expects HTML content.
	 * Null will be returned if a URL is passed at construction and
	 * retrieval of that URL results in an IOException.
	 * @throws IOException 
	 */
	public static JEditorPane create_stiki_html_pane(URL url, 
			final boolean copyable){
		
			// Create the pane and set basic properties
			// Copy-ability is an option; but must accomodate ZWS chars.
		JEditorPane pane;
		if(url != null){
			try{pane = new JEditorPane(url){
					public void copy(){
						if(copyable)
							stiki_utils.set_sys_clipboard(diff_whitespace.
									strip_zws_chars(this.getSelectedText()));
					} // Overwrite copy functionality to accomodate ZWS
					public void cut(){}};
			} catch(Exception e){
				System.err.println("Failed to open URL: " +
						url.toString() + " when initializing an HTML panel");
				e.printStackTrace();
				return(null);
			} // Report any URL failure
		} else{	pane = new JEditorPane(){
					public void copy(){
						if(copyable)
							stiki_utils.set_sys_clipboard(diff_whitespace.
									strip_zws_chars(this.getSelectedText()));
					} // Overwrite copy functionality to accomodate ZWS
					public void cut(){}};
		} // Allow for content initialization at construction
				
			// Some basic properties
		pane.setContentType("text/html");
		pane.setVisible(true);
		pane.setEditable(false);
		if(!copyable)
			pane.setHighlighter(null);
		else pane.addMouseListener(new gui_text_menu(pane)); // right-click
		
			// Use basic style-sheet rules to ensure HTML style
			// is consistent with that used by other GUI elements
		String css_rule = "body { " +
				"font-family: " +  DEFAULT_BROWSER_FONT.getFamily() + "; " +
				"font-size: " + DEFAULT_BROWSER_FONT.getSize() + "pt; }";
	    HTMLDocument html_doc = (HTMLDocument) pane.getDocument();
	    html_doc.getStyleSheet().addRule(css_rule);
	    return(pane);
	}
	
	/**
	 * Change the default font used in an HTML pane (JEditorPane).
	 * @param pane The JEditorPane object whose default font should be changed
	 * @param font The new default font which should be installed
	 */
	public static void change_html_pane_font(JEditorPane pane, Font font){
		String css_rule = "body { " +
				"font-family: " +  font.getFamily() + "; " +
				"font-size: " + font.getSize() + "pt; }";
		HTMLDocument html_doc = (HTMLDocument) pane.getDocument();
		html_doc.getStyleSheet().addRule(css_rule); // Just override previous?
		pane.repaint();
	}

    /**
     * Open the users web-browser and display a URL.
     * @param parent If needed, component from which error dialog should fire
     * @param url URL to be displayed in the web browser.
     */
    public static void open_url(Component parent, String url){
   
    		// Oversimplified hack for Wiki-URLs. Truly, an UTF-8 encoding 
    		// needs applied over ONLY the interesting-bits of the URL. 
    	url = url.replace(" ", "_");
    
        try{ url_browse.openURL(url); // Use the BulletproofLauncher
        } catch(Exception e){ 
        	JOptionPane.showMessageDialog(parent,
     		      "Resource cannot not be opened.\n" +
     		      "This is likely the result of:\n\n" +
     		      "(a) an ill-formed URL. This does not\n" +
     		      "necessarily indicate a broken link\n" +
     		      "or erroneous Wiki-formatting\n\n" +
     		      "(b) You are using Java 1.5 or earlier\n" +
     		      "on an OS that does not support awt.Desktop\n" +
     		      "and we were unsuccessful in more hack-ish\n" +
     		      "attempts to open a browser application.\n" +
     		      "Please report this error.",
     		      "Warning: Error opening URL",
     		      JOptionPane.WARNING_MESSAGE);
        } // Launcher throws an error
    }
	
	/**
	 * Produce a titled border of the style used by the STiki GUI.
	 * @param title Title text which should be shown on the border
	 * @return A TitledBorder with 'title' center straddling the border.
	 */
	public static CompoundBorder produce_titled_border(String title){
		
			// Create the three borders outlining major components
		title = title.toUpperCase();
		TitledBorder border = BorderFactory.createTitledBorder(title);
		border.setTitleJustification(TitledBorder.CENTER);
		border.setTitleFont(gui_globals.BOLD_TITLE_FONT);
		Border buffer_out = BorderFactory.createEmptyBorder(OUT_BORDER_WIDTH, 
				OUT_BORDER_WIDTH, OUT_BORDER_WIDTH, OUT_BORDER_WIDTH);
		Border buffer_in = BorderFactory.createEmptyBorder(IN_BORDER_WIDTH, 
				IN_BORDER_WIDTH, IN_BORDER_WIDTH, IN_BORDER_WIDTH);
		
			// Compound the borders for each use
		Border imd = BorderFactory.createCompoundBorder(buffer_out, border);
		return(BorderFactory.createCompoundBorder(imd, buffer_in));
	}
	
	/**
	 * Create a multiline-plain-text JLabel which is left-justified.
	 * @param text Text which should appear on the JLabel
	 * @return A new JLabel object, with left-justified "text", and with 
	 * "text" wrapped onto multiple lines, if necessary.
	 */
	public static JLabel plain_multiline_label(String text){
		JLabel label = new JLabel("<HTML>" + text + "</HTML>");
		label.setFont(gui_globals.PLAIN_NORMAL_FONT);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		return(label);
	}
	
	/**
	 * Create a multiline-plain-text JLabel which is completely centered.
	 * @param text Text which should appear on the JLabel
	 * @return A new JLabel object, with centered-text "text", and with 
	 * "text" wrapped onto multiple lines, if necessary.
	 */
	public static JLabel plain_centered_multiline_label(String text){
		JLabel label = new JLabel("<HTML><CENTER>" + text + "</CENTER></HTML>");
		label.setFont(gui_globals.PLAIN_NORMAL_FONT);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		return(label);
	}
	
	/**
	 * BoxLayout's require all elements to have the same justification.
	 * Thus, if one wants centered and left-justified elements, this
	 * method/hack is used, with uses HorizontalGlue to center data on a
	 * fully sized, but left-justified panel.
	 * @param comp Component which should be centered
	 * @return Panel containing only `comp' -- the panel is left justified,
	 * but 'comp' will be centered within. The panel is width greedy.
	 */
	public static JPanel center_comp_with_glue(Component comp){
		JPanel newpanel = new JPanel();
		newpanel.setLayout(new BoxLayout(newpanel, BoxLayout.X_AXIS));
		newpanel.add(Box.createHorizontalGlue());
		newpanel.add(comp);
		newpanel.add(Box.createHorizontalGlue());
		newpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return(newpanel);	
	}
	
	/**
	 * Create a panel containing a horizontal separator.
	 * @return JPanel containing a fixed width horizontal separator
	 */
	public static JPanel create_horiz_separator(){
		JPanel sep_panel = new JPanel();
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		sep_panel.add(sep);
		sep_panel.setLayout(new GridLayout(0,1));
		sep_panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		return(sep_panel);	
	}
	
	/**
	 * Create a panel containing a vertical separator.
	 * @return JPanel containing a fixed width vertical separator
	 */
	public static JPanel create_vert_separator(){
		JPanel sep_panel = new JPanel();
		JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
		sep_panel.add(sep);
		sep_panel.setLayout(new GridLayout(1,0));
		sep_panel.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
		return(sep_panel);	
	}

	/**
	 * Create a label to introduce a field/link -- per STiki style. 
	 * (currently, this means bold and right-justified, with RHS spacer).
	 * @param text Text which the label should contain
	 * @return A STiki-styled introductory-JLabel, with text 'text'
	 */
	public static JLabel create_intro_label(String text){
		JLabel label = new JLabel(text);
		label.setFont(gui_globals.BOLD_NORMAL_FONT);
		label.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
		label.setHorizontalAlignment(JLabel.RIGHT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 
				gui_globals.INTRO_LABEL_SPACER)); // Right-justified border
		return(label);
	}
	
	/**
	 * Create a text label that should contain data -- per the STiki style.
	 * @param text Text which the label should contain
	 * @return A STiki-styled data-JLabel, with text 'text'
	 */
	public static JLabel create_data_label(String text){
		JLabel label = new JLabel(text);
		label.setFont(gui_globals.PLAIN_NORMAL_FONT);
		return(label);
	}
	
	/**
	 * Create a text-field that should contain data -- per the STiki style.
	 * @param text Text which the field should contain
	 * @return A STiki-styled data-JTextField, with text 'text'
	 */
	public static JTextField create_data_field(String text){
		JTextField field = new JTextField(text);
	    field.setBorder(null);
	    field.setOpaque(false);
	    field.setEditable(false);
	    field.setFont(gui_globals.PLAIN_NORMAL_FONT);
		return(field);
	}

    /**
     * Create a JButton with text that appears as a hyperlink.
     * @param text Hyperlink-text. That to be displayed on the button
     * @param red_color If "true" then the returned link will be red,
	 * consistent with non-existing links. If "false", then the link will
	 * be a blue color consistent with existing links.
     * @param listener ActionListener handling button events
     */
    public static JButton create_link(String text, boolean red_color, 
    		ActionListener listener){
    	
    		// Note Mac and Windows both respect the empty border consistently
    	JButton button = new JButton(text);
    	button.setFont(gui_globals.get_link_font(false, red_color));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(JButton.LEFT_ALIGNMENT);
        button.setBorder(null); // buttons have default border; kill it
        button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setBackground(Color.LIGHT_GRAY);
        button.addActionListener(listener);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0));
        return(button);
    }
    
    /**
     * Identical to "create_link()" except that this version creates
     * a hyperlink of smaller font size.
     */
    public static JButton create_small_link(String text, boolean red_color,
    		ActionListener listener){
    	
    		// Note Mac and Windows both respect the empty border consistently
    	JButton button = new JButton(text);
    	button.setFont(gui_globals.get_link_font(true, red_color));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(JButton.LEFT_ALIGNMENT);
        button.setBorder(null); // buttons have default border; kill it
        button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setBackground(Color.LIGHT_GRAY);
        button.addActionListener(listener);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(0, 0, 0, 0)); // top-left-bottom-right
        return(button);
    }
    
	/**
	 * Return a font characteristic of the link style (blue, underlined).
	 * @param small If "true" return a small sized font, else make the 
	 * link font a more "normal" sized one.
	 * @param red_color If "true" then the returned link will be red,
	 * consistent with non-existing links. If "false", then the link will
	 * be a blue color consistent with existing links.
	 * @return A font suitable for creating text that appears as a hyperlink
	 */
	public static Font get_link_font(boolean small, boolean red_color){
		Font font;
		if(!small)
			font = gui_globals.PLAIN_NORMAL_FONT;
		else font = gui_globals.SMALL_NORMAL_FONT;
		Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
		map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		
		if(red_color)
			map.put(TextAttribute.FOREGROUND, new Color(153, 0, 0));
		else map.put(TextAttribute.FOREGROUND, new Color(0, 0, 153));
	
		font = font.deriveFont(map);
		return(font);
	}
    
	/**
	 * Create a JCheckBoxMenuItem per STiki's style.
	 * @param text Text to label the JCheckboxMenuItem (CBMI) being created
	 * @param keyevent Mnemonic KeyEvent character to associate with the CBMI
	 * @param enabled Whether or not the returned CBMI is enabled
	 * @param selected Whether or not the returned CBMI is selected
	 * @return New JRB, labeled as 'text' with mnenomonic 'keyevent'.
	 */
	public static JCheckBoxMenuItem checkbox_item(String text, int keyevent, 
			boolean enabled, boolean selected){
		JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem(text);
		cbmi.setFont(gui_globals.PLAIN_NORMAL_FONT);
		cbmi.setMnemonic(keyevent);
		cbmi.setEnabled(enabled);
		cbmi.setSelected(selected);
		return(cbmi);
	}
	
	/**
	 * Create a JRadioButtoMenuItem per STiki's style.
	 * @param text Text to label the JRadioButtonMenuItem (JRB) being created
	 * @param keyevent Mnemonic KeyEvent character to associate with the JRB
	 * @param enabled Whether or not the returned JRB is enabled
	 * @param selected Whether or not the returned JRB is selected
	 * @return New JRB, labeled as 'text' with mnenomonic 'keyevent'.
	 */
	public static JRadioButtonMenuItem radiobutton_item(String text, 
			int keyevent, boolean enabled, boolean selected){
		JRadioButtonMenuItem jrb = new JRadioButtonMenuItem(text);
		jrb.setFont(gui_globals.PLAIN_NORMAL_FONT);
		jrb.setMnemonic(keyevent);
		jrb.setEnabled(enabled);
		jrb.setSelected(selected);
		return(jrb);
	}

	/**
	 * Return a set indicating at while "milestones" a user should be warned
	 * about over-use of the pass button, where "milestones" are defined to
	 * be career uses of the pass button.
	 * @return Set containing "pass use" milestones at which to pop the
	 * "pass overuse warning".
	 */
	public static Set<Integer> set_pass_warn_points(){
		Set<Integer> warn_points = new TreeSet<Integer>();
		warn_points.add(10); // Somewhat arbitrary but less frequent notifies
		warn_points.add(50);
		warn_points.add(100);
		warn_points.add(250);
		warn_points.add(500);
		warn_points.add(1000);
		return(warn_points);
	}
	
	 /**
	  * Given a Java color object, return its hex representation
	  * @param c Java color object
	  * @return Hex representation of 'c', preceeded by "#"
	  */
	 public static String color_to_hex(Color c){
		 StringBuilder sb = new StringBuilder("#");

		 if(c.getRed() < 16) 
			 sb.append('0');
		 sb.append(Integer.toHexString(c.getRed()));

		 if(c.getGreen() < 16) 
			 sb.append('0');
		 sb.append(Integer.toHexString(c.getGreen()));

		 if(c.getBlue() < 16) 
			 sb.append('0');
		 sb.append(Integer.toHexString(c.getBlue()));

		 return sb.toString();
	}
	
	/**
	 * Convert a hex representation of a color to a Java color
	 * @param colorStr Color described in hex representation. May or may
	 * not include the preceeding '#' character.
	 * @return Java color representation of 'colorStr'
	 */
	public static Color hex_to_rgb(String colorStr){
		
		if(colorStr.charAt(0) == '#')
			colorStr = colorStr.substring(1);
		return new Color(
			Integer.valueOf(colorStr.substring(0,2), 16),
			Integer.valueOf(colorStr.substring(2,4), 16),
			Integer.valueOf(colorStr.substring(4,6), 16));
	}
	
	/**
	 * Pop the warning dialog w.r.t over-use of the "PASS" classification.
	 * @param parent Component from which the dialog should be popped
	 * @param times_past_used The number of times "pass" has been used
	 */
	public static void pop_overused_pass_warning(JComponent parent, 
			int times_past_used){	
		JOptionPane.showMessageDialog(parent,
				"STiki monitors your use of the \"pass\" button and asks\n" +
				"you to be careful with its use. If you are uncertain of\n" +
				"the classification, please use \"pass\" only when the\n" +
				"differing knowledge of another STiki user may result in\n" +
				"a \"vandalism\" or \"good faith revert\" decision. If it\n" +
				"is likely that other STiki users will also be uncertain,\n" +
				"please default to using the \"innocent\" button.\n" +
				"\n" +
				"This approach is intended to prevent the same edit being\n" +
				"shown to multiple STiki users, none of whom can make a\n" +
				"decision; that would be wasteful of STiki users' time.\n" +
				"Remember that STiki is not the final say on an edit.\n" +
				"Article watchlisters and others with more knowledge of\n" +
				"the subject will also see the change and may revert it.\n" +
				"\n" +
				"This message will become less frequently shown as your\n" +
				"number of classifications grows. Thanks!\n\n",
	 		    "Info: Potential overuse of PASS button",
	 		    JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Pop the warning dialog regrading "don't template the regulars".
	 * @param parent Component from which the dialog should be popped
	 */
	public static void pop_dttr_warning(JComponent parent){	
		JOptionPane.showMessageDialog(parent,
				"The user you are about to revert has at least 50\n" +
				"article edits. Experienced users are not infallible,\n" +
				"so if upon reconsideration the edit is unconstructive,\n" +
				"it should be reverted.\n" +
				"\n"  +
				"However, when dealing with such users, wiki-etiquette\n" +
				"dictates that a personalized user message or article\n" +
				"talk page thread is preferred to issuing a standardized\n" +
				"warning template, i.e., \"don't template the regulars\"\n" +
				"(see [[WP:DTTR]]).\n" + 
				"\n" + 
				"You will now be returned to the STiki window to\n" +
				"re-inspect the edit. Whichever option you choose this\n" +
				"time will be applied as normal.\n\n",
	 		    "Warning: Reverting a user w/50+ edits",
	 		    JOptionPane.WARNING_MESSAGE);
	}
	
	/**
	 * Pop the dialog to be shown after a user is "ignored"
	 * @param parent Component from which the dialog should be popped 
	 * @param editor Username of editor who has been ignored
	 */
	public static void pop_ignore_info(JComponent parent, String editor){
		JOptionPane.showMessageDialog(parent,
				"No more edits from " + editor + " will be shown during\n" +
				"this STiki session. The ignore list resets at STiki restart.",
	 		    "Info: Editor has been ignored",
	 		    JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Pop the dialog if one tries to "thank" an unregistered editor
	 * @param parent Component from which the dialog should be popped 
	 */
	public static void pop_thank_unregy_info(JComponent parent){
		JOptionPane.showMessageDialog(parent,
				"One cannot \"thank\" unregistered editors",
	 		    "Info: Can't thank unregistered editors",
	 		    JOptionPane.WARNING_MESSAGE);	
	}
	
}
