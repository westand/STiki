package gui_support;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Andrew G. West - gui_filesys_images.java - This class handles the location
 * and retrieval of image data that resides on the local filesystem.
 * This includes GUI components, icons, STiki logos, etc.
 */
public class gui_filesys_images{
	
	// **************************** PUBLIC FIELDS ****************************

	/**
	 * Relative-path (relative to the location of this class), where image/
	 * icon files are stored. Note the use of the forward-slash. Oddly,
	 * [File.Separator] does not work here. The forward-slash works and appears
	 * to be system independent, maybe something to do with [get_path()]?
	 */
	public static final String IMG_BASE = "icons/";

		// Individual images, located using relative paths
	public static final Image ICON_16 = get_img(IMG_BASE + "icon_16.png");
	public static final Image ICON_20 = get_img(IMG_BASE + "icon_20.png");
	public static final Image ICON_32 = get_img(IMG_BASE + "icon_32.png");
	public static final Image ICON_64 = get_img(IMG_BASE + "icon_64.png");
	public static final Image ICON_128 = get_img(IMG_BASE + "icon_128.png");
	public static final Image ICON_200 = get_img(IMG_BASE + "icon_200.png");


	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Return a list containing the STiki icon, in varying sizes.
	 * @return List containing varying sizes of the STiki icon
	 */
	public static List<Image> get_icon_set() throws Exception{		
		List<Image> icon_list = new ArrayList<Image>();
		icon_list.add(ICON_16);
		icon_list.add(ICON_20);
		icon_list.add(ICON_32);
		icon_list.add(ICON_64);
		icon_list.add(ICON_128);
		icon_list.add(ICON_200);
		return(icon_list);
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Given the path of an image, relative to this class, get that image.
	 * @param rel_path Path to the image, relative to this class location.
	 * @return Image object, containing data found at 'rel_path'
	 */
	private static Image get_img(String rel_path){
		URL url = gui_filesys_images.class.getResource(rel_path);
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		return(image);
	}

}
