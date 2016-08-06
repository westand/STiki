package gui_panels;

import executables.stiki_frontend_driver;
import executables.stiki_frontend_driver.FB_TYPE;
import gui_support.gui_globals;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.plaf.metal.MetalBorders;

import core_objects.stiki_utils.QUEUE_TYPE;

/**
 * Andrew G. West - gui_button_panel.java - This class implements the panel
 * containing the guilty/innocent/pass, as well as "back" buttons.
 */
@SuppressWarnings("serial")
public class gui_button_panel extends JPanel implements 
		ActionListener, KeyListener{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Button corresponding to "guilty" classifications.
	 */
	private JButton button_guilty;
	
	/**
	 * Button corresponding to the "guilty" classification, but also
	 * immediately issuing a 4im warning.
	 */
	private JButton button_4im;
	
	/**
	 * Button corresponding to "good faith revert" classifications
	 */
	private JButton button_agf;
	
	/**
	 * Button corresponding to "pass" classifications.
	 */
	private JButton button_pass;
	
	/**
	 * Button corresponding to "innocent" classifications.
	 */
	private JButton button_innocent;
	
	/**
	 * "Back" button to return to the previously classified edit. 
	 */
	private JButton button_back;
	
	/**
	 * Current GUI mode in use (spam, vandalism, etc.).
	 */
	private QUEUE_TYPE cur_type;
	
	/**
	 * Parent class (the frontend manager). Given that the classification
	 * buttons are so integral to GUI operation, this connection is important.
	 */
	private stiki_frontend_driver parent;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_button_panel()], and set the initial state 
	 * @param parent GUI manager class, to pass off button press actions
	 * @param type Initial queue type (spam, vandalism), so that the
	 * panel state can be initialized accordingly.
	 */
	public gui_button_panel(stiki_frontend_driver parent, QUEUE_TYPE type){
		
			// Critically, the GUI parent class 
		this.parent = parent;
		
			// Initialize the individual buttons
		button_guilty = new JButton("Vandalism");
		button_4im = new JButton("4im");
		button_agf = new JButton("Good Faith Revert");
		button_pass = new JButton("Pass");
		button_innocent = new JButton("Innocent");
		button_back = new JButton("<HTML><CENTER>&lt;<BR><BR>B<BR>A<BR>" +
				"C<BR>K<BR><BR>&lt;</CENTER></HTML>");

			// Set the button mnemonics
		button_guilty.setMnemonic(KeyEvent.VK_V);
		button_4im.setMnemonic(KeyEvent.VK_4);
		button_agf.setMnemonic(KeyEvent.VK_G);
		button_pass.setMnemonic(KeyEvent.VK_P);
		button_innocent.setMnemonic(KeyEvent.VK_I);
		button_back.setMnemonic(KeyEvent.VK_B); // Underscore fails due to HTML
		
			// Set the buttons to a non-bold, font style
		button_guilty.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_4im.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_agf.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_pass.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_innocent.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_back.setFont(gui_globals.TINY_NORMAL_FONT);
		
			// Add action/key-listeners to the buttons
		button_guilty.addActionListener(this);
		button_4im.addActionListener(this);
		button_agf.addActionListener(this);
		button_pass.addActionListener(this);
		button_innocent.addActionListener(this);
		button_back.addActionListener(this);
		button_guilty.addKeyListener(this);
		button_4im.addKeyListener(this);
		button_agf.addKeyListener(this);
		button_pass.addKeyListener(this);
		button_innocent.addKeyListener(this);
		button_back.addKeyListener(this);
		
			// Nullify space repeating on all buttons
		nullify_space_press(button_guilty);
		nullify_space_press(button_4im);
		nullify_space_press(button_agf);
		nullify_space_press(button_pass);
		nullify_space_press(button_innocent);
		nullify_space_press(button_back);
	
			// Make the "back" and "4im" buttons as tight as possible
		button_back.setBorder(null); // buttons have default border; kill it
		button_back.setBorder(BorderFactory.createCompoundBorder(
		        BorderFactory.createEmptyBorder(0, 0, 0, 0), 
		        new MetalBorders.ButtonBorder()));
		button_back.setMargin(new Insets(0, 0, 0, 0));
		button_back.setMaximumSize(button_back.getPreferredSize());
		button_back.setEnabled(false); // Initially un-usable
		button_4im.setMargin(new Insets(button_4im.getMargin().top, 0, 
				button_4im.getMargin().bottom, 0)); // Insets: tlbr
		
			// Tiny sub-sub-pane for "revert" + "4im" warnings
		JPanel guilty_subpanel = new JPanel();
		guilty_subpanel.setLayout(
				new BoxLayout(guilty_subpanel, BoxLayout.X_AXIS));
		guilty_subpanel.add(button_guilty);
		guilty_subpanel.add(button_4im);
		
			// Depending on whether we are using Mac or Windows, the 
			// 'AGF' button may be the widest compontent, or it may
			// be the "guilty subpanel" since Apple places a transparent
			// border around buttons it is impossible to remove without
			// destroying the look and feel. 
			// 
			// We size all other buttons/panels according to the widest 
			// component. Observe we also set the [button_guilty] preferred 
			// size, so it consumes available space in its sub-sub-panel
			// 
		int pref_width = button_agf.getPreferredSize().width;
		pref_width = Math.max(guilty_subpanel.getPreferredSize().width, 
				pref_width);
		
		guilty_subpanel.setPreferredSize(new Dimension(
				pref_width, guilty_subpanel.getPreferredSize().height));
		button_guilty.setPreferredSize(new Dimension(
				pref_width, button_guilty.getPreferredSize().height));
		button_agf.setPreferredSize(new Dimension(
				pref_width, button_agf.getPreferredSize().height));
		button_pass.setPreferredSize(new Dimension(
				pref_width, button_pass.getPreferredSize().height));
		button_innocent.setPreferredSize(new Dimension(
				pref_width, button_innocent.getPreferredSize().height));
		
			// ... and some minimums ... 
		guilty_subpanel.setMinimumSize(new Dimension(
				pref_width, guilty_subpanel.getPreferredSize().height));
		button_agf.setMinimumSize(new Dimension(
				pref_width, button_agf.getPreferredSize().height));
		button_pass.setMinimumSize(new Dimension(
				pref_width, button_pass.getPreferredSize().height));
		button_innocent.setMinimumSize(new Dimension(
				pref_width, button_innocent.getPreferredSize().height));
			
			// Provide even vertical spacing between class-buttons
		JPanel button_subpanel = new JPanel();
		button_subpanel.setLayout(new BoxLayout(button_subpanel, 
				BoxLayout.Y_AXIS));
		button_subpanel.add(Box.createVerticalGlue());
		button_subpanel.add(gui_globals.center_comp_with_glue(guilty_subpanel));
		button_subpanel.add(Box.createVerticalGlue());
		button_subpanel.add(gui_globals.center_comp_with_glue(button_agf));
		button_subpanel.add(Box.createVerticalGlue());
		button_subpanel.add(gui_globals.center_comp_with_glue(button_pass));
		button_subpanel.add(Box.createVerticalGlue());
		button_subpanel.add(gui_globals.center_comp_with_glue(button_innocent));
		button_subpanel.add(Box.createVerticalGlue());
		
			// Align all components in the larger, parent panel
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(Box.createHorizontalGlue());
		this.add(button_back);
		this.add(Box.createHorizontalGlue());
		this.add(Box.createHorizontalGlue());
		this.add(button_subpanel);
		this.add(Box.createHorizontalGlue());
		this.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			// Set intial state
		change_type_setup(type);
	}
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Change the label of the "guilty/undo" button to reflect queue changes
	 * @param type Type of queue being user (spam, vandalism, etc.)
	 */
	public void change_type_setup(QUEUE_TYPE type){
		
		if(this.cur_type != type){		
			if(type.equals(QUEUE_TYPE.VANDALISM)){
				button_guilty.setText("Vandalism");
				button_guilty.setMnemonic(KeyEvent.VK_V);
			} else if(type.equals(QUEUE_TYPE.LINK_SPAM)){
				button_guilty.setText("Link Spam");
				button_guilty.setMnemonic(KeyEvent.VK_S);
			} // Setup the button text and mnemonic
			this.cur_type = type;
		} // Note: Text length should never exceed that of "vandalism" case
	}
	
	/**
	 * Set whether or not the "back" button should be enabled.
	 * @param enabled TRUE if the "back" button should be enabled. Else, FALSE.
	 */
	public void back_button_enabled(boolean enabled){
		this.button_back.setEnabled(enabled);
	}
	
	
	// ******** ACTION INTERFACES *********
	
	/**
	 * Overriding: Map button-clicks to event-handlers
	 */
	public void actionPerformed(ActionEvent event){
		try{
			if(event.getSource().equals(this.button_innocent))
				parent.class_action(FB_TYPE.INNOCENT);
			else if(event.getSource().equals(this.button_pass))
				parent.class_action(FB_TYPE.PASS);	
			else if(event.getSource().equals(this.button_agf))
				parent.class_action(FB_TYPE.AGF);
			else if(event.getSource().equals(this.button_guilty))
				parent.class_action(FB_TYPE.GUILTY);
			else if(event.getSource().equals(this.button_4im))
				parent.class_action(FB_TYPE.GUILTY_4IM);
			else if(event.getSource().equals(this.button_back))
				parent.back_button_pressed();
		} catch(Exception e){
			System.out.println("Error internal to button-press handler: ");
			e.printStackTrace();
		} // Interface compliance necessitates try-catch block
	}
	
	/** 
	 * Overriding: Mappings for keyboard events. This enables keyboard
	 * classification without use of the ALT-mnemonic, under certain criteria
	 * (i.e., when one of the buttons has focus (was last used)).
	 */
	public void keyPressed(KeyEvent ke){
		try{	
			
			if(ke.isAltDown())
				return; // Prevent double-actions for mnemonics
			
				// Map keys to events; get focus for visual intuitiveness.
				// Extra check needed on "guilty" based on mode
			if((ke.getKeyChar() == 'v' || ke.getKeyChar() == 'V') && 
					this.cur_type == QUEUE_TYPE.VANDALISM){
				parent.class_action(FB_TYPE.GUILTY);
				this.button_guilty.requestFocusInWindow();
			} else if((ke.getKeyChar() == 's' || ke.getKeyChar() == 'S') && 
					this.cur_type == QUEUE_TYPE.LINK_SPAM){
				parent.class_action(FB_TYPE.GUILTY);
				this.button_guilty.requestFocusInWindow();
			} else if(ke.getKeyChar() == '4'){
				parent.class_action(FB_TYPE.GUILTY_4IM);
				this.button_4im.requestFocusInWindow();
			} else if(ke.getKeyChar() == 'g' || ke.getKeyChar() == 'G'){
				parent.class_action(FB_TYPE.AGF);
				this.button_agf.requestFocusInWindow();
			} else if(ke.getKeyChar() == 'p' || ke.getKeyChar() == 'P'){
				parent.class_action(FB_TYPE.PASS);
				this.button_pass.requestFocusInWindow();
			} else if(ke.getKeyChar() == 'i' || ke.getKeyChar() == 'I'){
				parent.class_action(FB_TYPE.INNOCENT);
				this.button_innocent.requestFocusInWindow();
			} else if(ke.getKeyChar() == 'b' || ke.getKeyChar() == 'B'){
				parent.back_button_pressed();
				this.button_back.requestFocusInWindow();
			} 
			
				// Given that the button panel should dominate focus,
				// map other panels KeyEvents ontop of this one.
				//
				// First up; scrolling functionality in the diff browser
			if(ke.getKeyCode() == KeyEvent.VK_DOWN || 
					ke.getKeyCode() == KeyEvent.VK_UP || 
					ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN || 
					ke.getKeyCode() == KeyEvent.VK_PAGE_UP)
				parent.diff_browser.keyPressed(ke);
			
		} catch(Exception e){
			System.out.println("Error internal to classification-handler: ");
			e.printStackTrace();
		} // Interface compliance necessitates try-catch block
	}
	
		// Interface compliance methods for the KeyListener. Unfortunately,
		// we can't just extend the adapter -- no multiple inheritance in Java.
	public void keyReleased(KeyEvent ke){}
	public void keyTyped(KeyEvent ke){}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Disable the "SPACEBAR" from acting as a repeater/click on some button. 
	 * @param button Button on which space repeating should be nullified.
	 */
	private void nullify_space_press(JButton button){
		KeyStroke pressed = KeyStroke.getKeyStroke("pressed SPACE");
		KeyStroke released = KeyStroke.getKeyStroke("released SPACE");
		InputMap im = button.getInputMap();
		im.put(pressed, "none");
		im.put(released, "none");
	}
	
}
