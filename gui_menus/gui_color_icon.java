package gui_menus;

import javax.swing.*;  
import java.awt.*;  

public class gui_color_icon implements Icon{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Height of icon to be created
	 */
	private static int HEIGHT = 14;  
    
	/**
	 * Width of icon to be created
	 */
	private static int WIDTH = 14;  
     
	/**
	 * Color of icon to be created
	 */
	private Color color;
     
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [gui_color_icon] by providing the color desired
	 * @param color Java representation of a color
	 */
    public gui_color_icon(Color color){  
        this.color = color;  
    }  
    
    
	// **************************** PUBLIC METHODS ***************************
	
    /**
     * Interface compliance: Get the icons height
     */
    public int getIconHeight(){  
        return HEIGHT;  
    }  
  
    /**
     * Interface compliance: Get the icons width
     */
    public int getIconWidth(){  
        return WIDTH;  
    }  
  
    /**
     * Interface compliance: Paint the actual color over an icon
     */
    public void paintIcon(Component c, Graphics g, int x, int y){  
        g.setColor(color);  
        g.fillRect(x, y, WIDTH - 1, HEIGHT - 1);  
        g.setColor(Color.black);  
        g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);   
    }  

}
