package gui_panels;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import gui_edit_queue.gui_display_pkg;
import gui_support.gui_globals;

/**
 * Andrew G. West - gui_giff_panel.java - This class implements the 
 * diff-browser; where diff-html is visually presented to the user.
 */
@SuppressWarnings("serial")
public class gui_diff_panel extends JPanel implements HyperlinkListener, 
		KeyListener{

	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * Font applied to the browser object. 
	 */
	public static Font browser_font = gui_globals.DEFAULT_BROWSER_FONT;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Browser window used to display HTML documents (diffs). 
	 */
	private JEditorPane browser;
	
	/**
	 * Scrollable window which wraps the browser window.
	 */
	private JScrollPane scroll_browser;
	
	/**
	 * Display object whose content is currently on display.
	 */
	private gui_display_pkg cur_content;
	
	/**
	 * Should X-links in diff-window content should be activated (click-able).
	 * Initial policy for the variable is also set here.
	 */
	private boolean activate_hyperlinks = false;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_diff_browswer] -- Initializing visual components.
	 * @param activate_hyperlinks If TRUE, then hyperlinks will be activated
	 * by default. If FALSE, they will not.
	 */
	public gui_diff_panel(){
		
			// Create the JEditorPane window
		this.browser = gui_globals.create_stiki_html_pane(null, true);
		this.browser.setBorder(BorderFactory.createEmptyBorder(
				gui_globals.BROWSER_BORDER, gui_globals.BROWSER_BORDER, 
				gui_globals.BROWSER_BORDER, gui_globals.BROWSER_BORDER));
		
			// Add a hyperlink-listener to the HTML, opening any 
			// links in a new and separate browser window.
		this.browser.addHyperlinkListener(this);
		
			// Register KeyListener with the browser
		this.browser.addKeyListener(this);
		
			// Make Pane scrollable and add to larger panel
		scroll_browser = new JScrollPane(this.browser);
		this.setLayout(new GridLayout(0,1));
		this.add(scroll_browser);
	}
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Change the content that is displayed in the browser window.
	 * @param content Display package containing the content that should be
	 * displayed. It should contain both linked and link-free versions.
	 */
	public void display_content(gui_display_pkg content) throws Exception{
	
		if(activate_hyperlinks) // Add links if necessary
			browser.setText(content.content_linked);
		else browser.setText(content.content);
		browser.setCaretPosition(0); // Scroll to top of pane
		this.cur_content = content;
	}
	
	/**
	 * Change the (default) font used in rendering HTML-diffs.
	 * @param font Font which should be the new default
	 */
	public void change_browser_font(Font font){
		browser_font = font;
		gui_globals.change_html_pane_font(this.browser, font);
	}
	
	/**
	 * Change/set the external hyperlink activation policy. This method
	 * also refreshes the current diff displayed to agree with the policy.
	 * @param activate TRUE if x-links should be "activated". False, otherwise.
	 */
	public void set_hyperlink_policy(boolean activate){
		
		if(this.activate_hyperlinks != activate){
			this.activate_hyperlinks = activate;
			
				// Realize that if we try to set the hyperlink policy very
				// early (i.e., when the "appearance" menu initializes and
				// tries to read/pass this setting from config), then the
				// browser might not have content; leading to null exception.
				// Here that exception is caught and ignored.
			try{if(activate) // Refresh currently displayed diff
					browser.setText(cur_content.content_linked);
				else browser.setText(cur_content.content);
			} catch(NullPointerException e){}
		} // only refresh/change if policy is different from previous
	}
	
	/**
	 * Return the current hyperlink activation policy for the diff browser.
	 * @return TRUE if hyperlinks are being activated; FALSE, otherwise
	 */
	public boolean get_hyperlink_policy(){
		return(this.activate_hyperlinks);
	}
	
	/**
	 * Interface: Return the HyperlinkListener associated w/the diff-browser.
	 */
	public void hyperlinkUpdate(HyperlinkEvent e){
		if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
			
				// Take the link-URL clicked, simplify, and open in browser.
				// Simplification involves conversion to an ASCII character
			 	// set (to weed out any unusual characters).
			String ascii_url;
			try{ascii_url = e.getURL().toString();
				ascii_url = new URL(ascii_url).toURI().toASCIIString();
			} catch(Exception exception){
				gui_globals.open_url(this, e.getURL().toString());
				return;        
			} // If conversion fails, just pass off to the browser, which
			  // may also fail, but will show a graphical warning message.
			
			gui_globals.open_url(this, ascii_url);
		}
	}

	
	// ********* KEY LISTENER *********

	/**
	 * Overriding: Map key presses to panel actions. In this case, we make
	 * arrows scroll up and down, and don't mess with default behavior
	 */
	public void keyPressed(KeyEvent ke){
		if(ke.getKeyCode() == KeyEvent.VK_DOWN){
			this.scroll_browser.getVerticalScrollBar().setValue(
				this.scroll_browser.getVerticalScrollBar().getValue() + 5 *
				this.scroll_browser.getVerticalScrollBar().getBlockIncrement());
		} // Handle the down button (to scroll down)
		else if(ke.getKeyCode() == KeyEvent.VK_UP){
			this.scroll_browser.getVerticalScrollBar().setValue(
				this.scroll_browser.getVerticalScrollBar().getValue() - 5 *
				this.scroll_browser.getVerticalScrollBar().getBlockIncrement());
		} // Handle the up button (to scroll up)
		else if(ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN){
			this.scroll_browser.getVerticalScrollBar().setValue(
				this.scroll_browser.getVerticalScrollBar().getValue() + 25 *
				this.scroll_browser.getVerticalScrollBar().getBlockIncrement());
		} // Handle the page-down button (larger scroll down)
		else if(ke.getKeyCode() == KeyEvent.VK_PAGE_UP){
			this.scroll_browser.getVerticalScrollBar().setValue(
				this.scroll_browser.getVerticalScrollBar().getValue() - 25 *
				this.scroll_browser.getVerticalScrollBar().getBlockIncrement());
		} // Handle the page-up button (larger scroll up)
	}

		// Interface compliance for the KeyListener
	public void keyReleased(KeyEvent ke){}
	public void keyTyped(KeyEvent ke){}
	
}
