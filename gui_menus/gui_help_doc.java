package gui_menus;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import gui_support.gui_filesys_images;
import gui_support.gui_globals;
import gui_support.url_browse;

/**
 * Andrew G. West - gui_help_doc.java - Herein the GUI help document is
 * authored in HTML format. A single access method is provided which
 * permits section-wise access to the document. 
 */
public class gui_help_doc{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Fixed dimension, in pixels, of the width the help-pane.
	 */
	public static final int HELP_WIN_WIDTH = 595;
	
	/**
	 * Fixed dimension, in pixels, of the height the help-pane.
	 */
	public static final int HELP_WIN_HEIGHT = 700;
	
	/**
	 * Relative file-path to the location the help document (HTML). This
	 * path should be relative to the project root.
	 */
	public static final String HELP_FILEPATH = "stiki_help.html";
	
		// Not java-doc-ed. Each string constant is a section 
		// anchor in the HTML file located at [HELP_FILEPATH]
	public static final String ANCHOR_FULL = 	"sec_top";
	public static final String ANCHOR_QUEUE = 	"sec_queue";
	public static final String ANCHOR_STIKI_S =	"sec_stiki_scoring";
	public static final String ANCHOR_FILTERS = "sec_filter";
	public static final String ANCHOR_BROWSER = "sec_browser";
	public static final String ANCHOR_METADATA ="sec_metadata";
	public static final String ANCHOR_CLASS = 	"sec_class";
	public static final String ANCHOR_LOGIN = 	"sec_login";
	public static final String ANCHOR_COMMENT = "sec_comment";
	public static final String ANCHOR_LASTRV =  "sec_lastrevert";
	public static final String ANCHOR_PFORM = 	"sec_performance";
	public static final String ANCHOR_ORT = 	"sec_ort";
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Display the a section (or all) of the help document in a pop-up frame. .
	 * @param parent Component from which the pop-up should be launched
	 * @param section Section (or all) of the help-doc to display, One of the
	 * ANCHOR_* constants associated with this class should be passed.
	 */
	public static void show_help(Component parent, String section) 
			throws Exception{
		
			// Create the frame, its sizing, and basic properties
		JFrame frame = new JFrame();
		Dimension screen_size = frame.getToolkit().getScreenSize();
		int win_locx = ((screen_size.width - HELP_WIN_WIDTH) / 2);
	    int win_locy = ((screen_size.height - HELP_WIN_HEIGHT) / 2);
		frame.setBounds(win_locx, win_locy, HELP_WIN_WIDTH, HELP_WIN_HEIGHT);
		frame.setTitle("STiki Help Pane");
		frame.setIconImage(gui_filesys_images.ICON_64);
	
			// Add help-doc HTML pane, make scrollable, add to frame
		JEditorPane content = get_help_doc(section);
		JScrollPane content_scrollable = new JScrollPane(content);
		frame.add(content_scrollable);
		
			// Beautify the frame with borders
		Border empty_border = BorderFactory.createEmptyBorder(
				gui_globals.BROWSER_BORDER, gui_globals.BROWSER_BORDER, 
				gui_globals.BROWSER_BORDER, gui_globals.BROWSER_BORDER);
		Border outline_border = BorderFactory.createLineBorder(Color.BLACK, 1);
		content.setBorder(empty_border);
		content_scrollable.setBorder(BorderFactory.
				createCompoundBorder(empty_border, outline_border));
			
			// Add a hyperlink-listener to the HTML, modifying the default
			// slightly so that anchor links are properly handled
		content.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e){
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
					JEditorPane src = (JEditorPane) e.getSource();
					if(e.getDescription().startsWith("#")){
						src.scrollToReference(e.getDescription().substring(1));
					} else{ url_browse.openURL(e.getURL().toString()); }
		}	}	});

			// Make visible; already scrolled at creation
		frame.setVisible(true);
	}
	

	// *************************** PRIVATE METHODS ***************************

	/**
	 * Return the entire help-document as a Java string
	 * @return JEditorPane containing HTML of the help document
	 */
	private static JEditorPane get_help_doc(String section) throws Exception{
		
		/*String cur_line = "";
		StringBuilder help_doc = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(url.openStream());
		BufferedReader in = new BufferedReader(isr);
		do{ help_doc.append(cur_line);
			cur_line = in.readLine();
		} while(cur_line != null); */
		
		URL url = new URL(null, gui_help_doc.class.getResource(
				HELP_FILEPATH).toExternalForm() + "#" + section);
		return(gui_globals.create_stiki_html_pane(url, true));	
	}
	
}
