package gui_support;

import java.util.Arrays;

/**
 * Andrew G. West - url_browse.java - Use of [java.awt.Desktop] works great
 * on Java 1.6 to conveniently open links in browser. It is not universally
 * supported on 1.5 for all OS (in particular, OS X). Thus, this hackish
 * class exists so everyone can open links. This is code from the 
 * "Bare Bones Browser Launch for Java", whose licensing data follows:
 * 
 * -------------------------------------------------------------------
 * 
 * Bare Bones Browser Launch for Java
 * 
 * Utility class to open a web page from a Swing application
 * in the user's default browser
 * 
 * Supports: Mac OS X, GNU/Linux, Unix, Windows XP/Vista/7
 * 
 * Latest Version: www.centerkey.com/java/browser
 * 
 * Author: Dem Pilafian
 *
 * Public Domain Software -- Free to Use as You Like
 * 
 * -------------------------------------------------------------------
 */
public class url_browse{
	
	// **************************** STATIC FIELDS ***************************

	/**
	 * If [java.awt.Desktop] is not supported, then these are the various
	 * browsers which we will try to open the URL with, assuming we can't
	 * use the default handler for a known OS.
	 */
	static final String[] browsers = {"google-chrome", "firefox", "opera", 
		"epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla"};
   
   /**
    * Error message if all efforts fail.
    */
   static final String error_msg = "Error attempting to launch web browser";

   
	// **************************** STATIC METHODS ***************************
   
   /**
    * Opens the specified web page in the user's default browser
    * @param url A web address (URL) to be opened
    */
   public static void openURL(String url){
	   url = adjust_protocol(url); // Possible HTTPS inclusion
	   try{Class<?> d = Class.forName("java.awt.Desktop");
		   d.getDeclaredMethod("browse", 
				   new Class[] {java.net.URI.class}).invoke(
						   d.getDeclaredMethod("getDesktop").invoke(null), 
						   new Object[] {java.net.URI.create(url)});
	   } // mimic java.awt.Desktop.getDesktop().browse() -- w/o import!
	   catch(Exception ignore){
    	  String osName = System.getProperty("os.name");
    	  try{if(osName.startsWith("Mac OS")){
    			  Class.forName("com.apple.eio.FileManager").getDeclaredMethod(
    					  "openURL", new Class[] {String.class}).invoke(
    							  null, new Object[] {url});
    		  } else if(osName.startsWith("Windows"))
    			  Runtime.getRuntime().exec(
    					  "rundll32 url.dll,FileProtocolHandler " + url);
    		  else{String browser = null;
    			  for(String b : browsers)
    				  if(browser == null && Runtime.getRuntime().exec(
    						  new String[]{"which", b}).getInputStream().read() != -1)
    					  Runtime.getRuntime().exec(new String[] {browser = b, url});
    			  if(browser == null)
    				  throw new Exception(Arrays.toString(browsers));
    		  } // Try OS-specific handlers where possible; try all for linux
    	  } // Failing internal to this creates a hard fail
    	  catch (Exception e){
    		  new Exception(error_msg); // pass up
    	  }
	   } // Try to use [awt.Desktop] out of the box
   }
   
   /**
    * The STiki GUI contains an option to "Use HTTPS." This intends chiefly to 
    * secure interaction with English Wikipedia over the HTTPS protocol. This 
    * is a simple switch for API functionality which is centralized in two 
    * classes. Matters are more complicated for the many links to English 
    * Wikipedia that are scattered elsewhere. Thus, rather than finding all of
    * those, we implement a simple filter here just before they are opened
    * in the browser (the only URL accesses besides those in API). 
    * @param url Some URL
    * @return Version of 'url' with possible protocol transition to HTTPS
    * if the "use HTTPs" option is enabled and 'url' is know to be one
    * that supports the protocol (i.e., Wikipedia itself). 
    */
   public static String adjust_protocol(String url){
	   if(gui_settings.get_bool_def(gui_settings.SETTINGS_BOOL.options_https, 
			   false) && url.contains(".wikipedia.org"))
		   return(url.replace("http://", "https://"));
	   else return(url);
   }
   
}
