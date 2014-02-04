package gui_support;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/**
 * Andrew G. West - gui_colorpicker.java - Creating a simple "color picker"
 * dialog to aid with GUI and diff-panel customization.
 */
public class gui_colorpicker extends JDialog implements ActionListener{

	// **************************** PRIVATE FIELDS **************************
	
	/**
	 * ID for serialization purposes
	 */
	private static final long serialVersionUID = -2829688539123672454L;

	/**
	 * The color choosing widget built into Java
	 */
    protected JColorChooser tcc;
    
    /**
     * Button used to indicate final color selection
     */
    protected JButton button;
    
    /**
     * Whether or not the button has been pressed yet or not
     */
    private boolean button_pressed = false;
    
    /**
     * Color currently chosen, represented in hex format (w/"#")
     */
    private String color;
    
    
	// **************************** TEST HARNESS ****************************
	
	/**
	 * Test harness for the color picker
	 * @param args No arguments are taken
	 */
	public static void main(String[] args) throws Exception{
		JFrame frame= new JFrame();
		System.out.println(dialog_response(frame, "f012f9"));
	}
	
	
	// **************************** CONSTRUCTOR *****************************	
	
	/**
	 * Construct a [gui_colorpicker] object. It is unusual to create one
	 * directly, as this constructor does not have output functionality,
	 * instead one should use the [dialog_response()] method below.
	 * @param frame Parent frame from which this dialog will be popped
	 * @param init_color Initial color to be displayed in the color
	 * selector. The expected format is RGB hex.
	 */
	public gui_colorpicker(JFrame frame, String init_color) throws Exception{
		
		super(frame,  Dialog.ModalityType.TOOLKIT_MODAL);
		
			// Setup the color chooser; do removals
        tcc = new JColorChooser();
        tcc.setColor(gui_globals.hex_to_rgb(init_color));
        tcc.setBorder(BorderFactory.createTitledBorder(
        		"Choose a color"));
        onlyRGBpicker();
		removeTransparencySlider();
        
			// Button will close the frame
		button = new JButton("Done");
		button.addActionListener(this);
        
			// Add components to an inner panel
		JPanel panel = new JPanel(new BorderLayout());
        panel.add(tcc, BorderLayout.CENTER);
        panel.add(button, BorderLayout.SOUTH);
        panel.setVisible(true);
        
        	// Setup the frame and display
        this.setSize(300, 300);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setTitle("Please select a color");
        this.setLayout(new BorderLayout());
        this.getContentPane().add(panel);
        this.pack();
        this.setVisible(true);
	}
	
	
	// **************************** PUBLIC METHODS **************************
	
	/**
	 * Primary method to be called within this class. Pops the dialog,
	 * waits for a response, and returns that response.
	 * @param frame Parent frame from which this dialog will be popped
	 * @param init_color Initial color to be displayed in the color
	 * selector. The expected format is RGB hex.
	 * @return Color selected in hex format.
	 */
	public static String dialog_response(JFrame frame, String init_color) 
			throws Exception{
		
		gui_colorpicker cp = new gui_colorpicker(frame, init_color);
		while(!cp.button_pressed)
			Thread.sleep(50);
		return(cp.color);
	}
	
	@Override
	public void actionPerformed(ActionEvent ae){
		if(ae.getSource().equals(this.button)){
			 color = gui_globals.color_to_hex(tcc.getColor());
			 button_pressed = true;
			 this.dispose();
		}
	}

	
	// *************************** PRIVATE METHODS **************************
	
	/**
	 * Take the default Java JColorChooser and strip from it all color
	 * selection tabs except for the "RGB" one
	 */
	private void onlyRGBpicker(){
		AbstractColorChooserPanel[] panels = this.tcc.getChooserPanels();
		for(AbstractColorChooserPanel accp : panels){
			if(!accp.getDisplayName().equals("RGB"))
				this.tcc.removeChooserPanel(accp);
		}
	 }

	/**
	 * Remove the "transparency" or "alpha" channel sliders (and respective
	 * labels and indicators) from the default JColorChooser.
	 */
	 private void removeTransparencySlider() throws Exception{
		 
		 AbstractColorChooserPanel[] colorPanels = tcc.getChooserPanels();
		 for(int i=0; i < colorPanels.length; i++){
			 
			 	// Just doing reflection down through the objects until
			 	// we reach appropriate depth to set invisible.
			 
			 AbstractColorChooserPanel cp = colorPanels[i];
			 Field f = cp.getClass().getDeclaredField("panel");
			 f.setAccessible(true);

			 Object colorPanel = f.get(cp);
			 Field f2 = colorPanel.getClass().getDeclaredField("spinners");
			 f2.setAccessible(true);
			 Object spinners = f2.get(colorPanel);
	        
			 Object transpSlispinner = Array.get(spinners, 3);
			 Field f3 = transpSlispinner.getClass().getDeclaredField("slider");
	         f3.setAccessible(true);

	         JSlider slider = (JSlider) f3.get(transpSlispinner);
	         slider.setEnabled(false);
	         slider.setVisible(false);
	        
	         Field f4 = transpSlispinner.getClass().getDeclaredField("spinner");
	         f4.setAccessible(true);
	         JSpinner spinner = (JSpinner) f4.get(transpSlispinner);
	         spinner.setEnabled(false);
	         spinner.setVisible(false);
	        
	         Field f5 = transpSlispinner.getClass().getDeclaredField("label");
	         f5.setAccessible(true);
	         JComponent label = (JComponent) f5.get(transpSlispinner);
	         label.setEnabled(false);
	         label.setVisible(false);
		 }
	 }

}
