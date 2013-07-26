package gui_menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Andrew G. West - gui_text_menu.java - This class implements the 
 * "right-click menu" which is associated with a body of text (in our
 * case a JEditorPane). If text is highlighted, the menu will permit one
 * operation: The "copy" of that text.
 */
public class gui_text_menu extends MouseAdapter implements ActionListener{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Pop-up menu to be displayed on right-click
	 */
	private JPopupMenu menu;

	/**
	 * Lone item in said menu: the "copy" option 
	 */
	private JMenuItem item_copy;

	/**
	 * Text box in which the right-click functionality will exist
	 */
	private JEditorPane parent;

	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_text_menu].
	 * @param parent Text-field over which "copy" will be available
	 */
	public gui_text_menu(JEditorPane parent){
		this.parent = parent;
		item_copy = gui_menu_bar.create_menu_item("Copy", KeyEvent.VK_C);
		menu = new JPopupMenu();
		menu.add(item_copy);
		item_copy.addActionListener(this);
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Fire the menu when mouse button is: (a) released
	 */
	public void mouseReleased(MouseEvent me){
		show_popup(me);
	}
	
	/**
	 * Overriding: Fire the menu when mouse button is: (b) pressed 
	 */
	public void mousePressed(MouseEvent me){
		show_popup(me);
	}
	
	/**
	 * Overriding: Map menu clicks to actions
	 */
	@Override
	public void actionPerformed(ActionEvent ae){	
		if(ae.getSource().equals(this.item_copy)) // the only option
			parent.copy();
	}

	
	//*************************** PRIVATE METHODS ***************************

	/**
	 * Fire the pop-up menu, providing the following conditions are met:
	 * (1) The menu has been initialized,
	 * (2) The mouse-click was a right-click action
	 * (3) The text-box has some quantity of text-highlighted
	 * @param me Mouse click event (contains which button was pressed)
	 */
	private void show_popup(MouseEvent me){
		if(menu != null && me.isPopupTrigger() && 
				parent.getSelectedText() != null && 
				!parent.getSelectedText().isEmpty()){
			menu.show(me.getComponent(), me.getX(), me.getY());
		}
	}

}