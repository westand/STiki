package executables;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import offline_review_tool.ort_edit_queue;

import core_objects.stiki_utils;

import gui_panels.gui_diff_panel;
import gui_panels.gui_metadata_panel;
import gui_support.gui_filesys_images;
import gui_support.gui_globals;


/**
 * Andrew G. West - offline_review_driver.java - The "offline review tool"
 * (ORT) is an extremely stripped down version of STiki. It pulls RIDs from
 * a user-provided text-file, and writes binary-classifications (per the
 * users determination) back to a text-file.
 * 
 * The offline-tool does not operate over live Wikipedia. Wikipedia is
 * only used to query metadata and edit diffs. This tool is thought to be
 * useful for researchers, who must quickly examine edits for some property.
 */
@SuppressWarnings("serial")
public class offline_review_driver extends JFrame implements 
		ActionListener, KeyListener{
	
	// **************************** PUBLIC FIELDS ****************************
	// *********** ACTIONS ************
	
	/**
	 * Classification types available to end-users.
	 */
	public enum CLASS_TYPE{MARKED, UNMARKED};
	
	
	// ******* VISUAL ELEMENTS ********
	//
	// Visual components made public, so sub-classes are able to alter
	// the visual GUI appearence without extensive passing
	
	/**
	 * Panel showing the visually-colored edit diffs.
	 */
	public gui_diff_panel diff_browser;
	
	/**
	 * Panel where edit properties are displayed (article, user, time-stamp).
	 */
	public gui_metadata_panel metadata_panel;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Number of threads to use in back-end support. (i.e., non-GUI items such
	 * as fetching data from MediaWiki).
	 */
	private static final int NUM_NON_GUI_THREADS = 8;
	
	/**
	 * Service dispatching work to available threads.
	 */
	private static ExecutorService WORKER_THREADS;
	
	/**
	 * Edit queue operating over provided text file (containing RIDs). 
	 */
	private static ort_edit_queue edit_queue;
	
	/**
	 * This is the write to the "output_file", essentially a mirror of the
	 * input RID file, except that this version writes those RIDs alongside
	 * the classifications provide by this tool.
	 */
	private static BufferedWriter out_file;
	
	
	// ******* LOCAL COMPONENTS *******
	//
	// JComponents intialized and handled by this class (class. buttons)
	
	private JPanel button_panel;
	private JButton button_marked;
	private JButton button_unmarked;
	
	
	// ***************************** MAIN METHOD *****************************
	
	/**
	 * Driver method. Launch the offline-review tool. This method is a test-
	 * harness for a driver which is expected to normally be launched
	 * from within the STiki tool.
	 * @param args One (1) argument is required. The path to a file containing
	 * only RIDS to enqueue/examine.
	 */
	public static void main(String[] args) throws Exception{
		new offline_review_driver(args[0]);
	}
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Constructing an [offline_review_driver] with no arguments will launch
	 * the file browser so that an RID file can be chosen. Then, the 
	 * offline review tool (ORT) will be launched over that file.  
	 */
	public offline_review_driver() throws Exception{
		visual_setup(); // Setup visual first, so JFC isn't floating
		JFileChooser fc = new JFileChooser();
		if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			enqueue_rid_file(fc.getSelectedFile().getAbsolutePath());
		else return;	
	}
	
	/**
	 * Constructing an [offline_review_driver] with the path to a file,
	 * will create the GUI, and enqueue edits from that file.
	 */
	public offline_review_driver(String rid_file) throws Exception{
		visual_setup();
		enqueue_rid_file(rid_file);
	}

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Setup the GUI of the STiki Offline-Review-Tool (ORT).
	 */
	public void visual_setup() throws Exception{
		
			// Visual Components: Initialize all main-window components 
		initialize_button_panel();
	    metadata_panel = new gui_metadata_panel();
		diff_browser = new gui_diff_panel(); // Activate hyperlinks
		
			// Then populate the GUI layout
		JPanel center_panel = new JPanel(new BorderLayout(0,0));
		diff_browser.setBorder(gui_globals.
				produce_titled_border("DIFF-Browser"));
		center_panel.add(diff_browser, BorderLayout.CENTER);
		center_panel.add(initialize_bottom_panel(), BorderLayout.SOUTH);
	    this.getContentPane().add(center_panel, BorderLayout.CENTER);
	    
			// Determine the size, positioning of the main frame
		Dimension screen_size = this.getToolkit().getScreenSize();
	    int win_width = (screen_size.width * 8 / 10);
	    int win_height = (screen_size.height * 8 / 10);
	    int win_locx = ((screen_size.width - win_width) / 2);
	    int win_locy = ((screen_size.height - win_height) / 2);
	    this.setBounds(win_locx, win_locy, win_width, win_height);
	    
	    	// Set frame properties and make visible
	    this.setTitle("STiki: Offline Review Tool (ORT)");
	    this.setIconImage(gui_filesys_images.ICON_64);
	    this.setVisible(true);
	    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	    this.addWindowListener(get_exit_handler());
	}
	
	/**
	 * Provided that the GUI has been intialized (via [visual_setup()]),
	 * provide the GUI a queue of edits to display, in the form of a text file.
	 * @param File path to a text file which defines the edit
	 * queue. One RID per line, and nothing else.
	 */
	public void enqueue_rid_file(String rid_file) throws Exception{
		
		BufferedReader rid_reader = null;
		try{ // First, open RID-file, and mirrored CLASS (*.marked) file.
			rid_reader = stiki_utils.create_reader(rid_file);
			out_file = stiki_utils.create_writer(
					determine_out_file(rid_file), false);
		} catch(Exception e){
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, 
					"The RID file passed to the Offline Review Tool (ORT)\n" +
					"could not be opened. ORT will now shut-down. Check the\n" +
					"STiki help-file for further documentation for the\n" +
					"required format.", "Error: Passed in RID file failed", 
					JOptionPane.ERROR_MESSAGE);
			return;
		} // If RID file cannot be opened, show error and exit
		
			// Prepare work threads (diff fetching)
		WORKER_THREADS = Executors.newFixedThreadPool(NUM_NON_GUI_THREADS);
		edit_queue = new ort_edit_queue(WORKER_THREADS, rid_reader);
	    
	    this.advance_revision(); // Initialize the window with an edit
	    this.button_marked.requestFocusInWindow(); // Activate hotkeys
	}
	

	// ******** EVENT HANDLERS ********
	
	/**
	 * Overriding: Map button-clicks to event-handlers
	 */
	public void actionPerformed(ActionEvent event){
		try{if(event.getSource().equals(this.button_marked))
				this.class_action(CLASS_TYPE.MARKED);
			else if(event.getSource().equals(this.button_unmarked))
				this.class_action(CLASS_TYPE.UNMARKED);	
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
			if(ke.getKeyChar() == 'm' || ke.getKeyChar() == 'M'){
				this.class_action(CLASS_TYPE.MARKED);
				this.button_marked.requestFocusInWindow();
			} else if(ke.getKeyChar() == 'u' || ke.getKeyChar() == 'U'){
				this.class_action(CLASS_TYPE.UNMARKED);
				this.button_unmarked.requestFocusInWindow();
			} // Map keys to events, also get focus for visual intuitiveness
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
	 * Given the location of the input file (that containing RIDs to examine),
	 * produce the output the file path (RIDS + classifications).
	 * @param in_rid_file File path the the input RID file
	 * @return A string describing the path where the output (classification)
	 * file should be output. It will be identical to the input file, 
	 * except for the extension, which will be of the form (*.marked).
	 */
	private String determine_out_file(String in_rid_file){
		
			// Hopefully this method is file-system independent
		String out_file;
		if(in_rid_file.contains(".")){
			out_file = in_rid_file.split("\\.")[0] + ".marked";
		} else { // If file has extension, change it for results
			out_file = in_rid_file + ".marked";
		} // If no extension, simply concantenate one one
		return(out_file);
	}
	
	/**
	 * Move visual components forward to the next revision.
	 */
	private void advance_revision() throws Exception{
		edit_queue.next_rid();
		diff_browser.display_content(edit_queue.get_cur_edit());
		metadata_panel.set_displayed_rid(edit_queue.get_cur_edit());
	}

	/**
	 * Handle a press of the classification buttons, writing the classfication
	 * to the output text file.
	 * @param class_type Type of classification being left
	 */
	private void class_action(CLASS_TYPE class_type) throws Exception{
		
		if(edit_queue.get_cur_edit().metadata.rid == 0)
			return; // Don't allow end-queue placeholder to be classified
		
		if(class_type.equals(CLASS_TYPE.MARKED)){
			out_file.write(edit_queue.get_cur_edit().metadata.rid + ",1\n");
			out_file.flush();
		} else{ // if(class_type.equals(CLASS_TYPE.UNMARKED)
			out_file.write(edit_queue.get_cur_edit().metadata.rid + ",0\n");
			out_file.flush();
		}  // Write classification to output file
		this.advance_revision(); // No matter the classification, advance RID
	}
	
	
	// ******** VISUAL-CENTRIC ********

	/**
	 * Initialize the panel containing marked/unmarked buttons.
	 */
	private void initialize_button_panel(){
		
			// Initialize the individual buttons, mnemonics, style
		button_marked = new JButton("Mark");
		button_unmarked = new JButton("Unmark");
		button_marked.setMnemonic(KeyEvent.VK_M);
		button_unmarked.setMnemonic(KeyEvent.VK_U);
		button_marked.setFont(gui_globals.PLAIN_NORMAL_FONT);
		button_unmarked.setFont(gui_globals.PLAIN_NORMAL_FONT);

			// Add action/key-listeners to the buttons
		button_marked.addActionListener(this);
		button_unmarked.addActionListener(this);
		button_marked.addKeyListener(this);
		button_unmarked.addKeyListener(this);
		
			// Classification label (instructions)
		JLabel class_label = gui_globals.plain_centered_multiline_label(
				"Classify using buttons,<BR>or 'm' and 'u' hotkeys");
		class_label.setFont(gui_globals.SMALL_NORMAL_FONT);
		
			// We assume the 'unmarked' button to be the largest of
			// the classification buttons. Taking its preferred size, 
			// we size all other buttons equivalently
		Dimension largest_pref_button_dim = button_unmarked.getPreferredSize();
		button_marked.setMaximumSize(largest_pref_button_dim);
		button_unmarked.setMaximumSize(largest_pref_button_dim);
	
			// Everything is stored in subpanel
		JPanel button_subp = new JPanel();
		button_subp.setLayout(new BoxLayout(button_subp, BoxLayout.Y_AXIS));
		button_subp.add(button_marked);
		button_subp.add(Box.createVerticalGlue());
		button_subp.add(button_unmarked);
		button_subp.setAlignmentX(Component.LEFT_ALIGNMENT);
		
			// Which is then centered in a larger panel
		button_panel = new JPanel();
		button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));
		button_panel.add(Box.createVerticalGlue());
		button_panel.add(gui_globals.center_comp_with_glue(button_subp));
		button_panel.add(Box.createVerticalGlue());
		button_panel.add(class_label);
	}

	/**
	 * Intialize the GUI bottom panel (metadata and button panels).
	 * @return JPanel object which composes the "bottom panel"
	 */
	private JPanel initialize_bottom_panel(){
		
			// Beautify components going in bottom-panel
		button_panel.setBorder(gui_globals.
				produce_titled_border("Classify"));
		metadata_panel.setBorder(gui_globals.
				produce_titled_border("Edit Properties"));

			// Straightforward layout
		JPanel bottom_panel = new JPanel(new BorderLayout(0,0));
		bottom_panel.add(button_panel, BorderLayout.WEST);
		bottom_panel.add(metadata_panel, BorderLayout.CENTER);
		return(bottom_panel);
	}
	
	/**
	 * Return the event-handler for when the main frame is exited 
	 * @return An event-handler (to be added as listener) for the exit process.
	 */
	private WindowAdapter get_exit_handler(){
		 WindowAdapter win_close = new WindowAdapter(){
			 public void windowClosing(WindowEvent w){
				 try{out_file.flush();
				 	 out_file.close();
				 	 edit_queue.shutdown();
					 WORKER_THREADS.shutdownNow(); // Kill infinite loops
					 System.exit(0); 
				 } catch(Exception e){
					 System.out.println("Error during STiki-ORT shutdown:");
					 e.printStackTrace();
					 System.exit(0);
				 } // Try-catch required for interface compliance
			 }  // Smoothly shut-down all threads
		}; // An anonymous class implements the only WindowEvent we care about
		return(win_close);
	}
	
}
