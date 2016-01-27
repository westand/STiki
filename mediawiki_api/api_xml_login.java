package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_login.java - The SAX-XML parse handler
 * to parse the response after a login POST has been attempted. The response
 * contains session variables necessary to build a `cookie' -- which will
 * then accompany future requests, providing a means to identify the user.
 * This class builds that cookie object (a string).
 * 
 * April 2010 -- this became a two step process. A login attempt
 * will return a "login token". This token must then be submitted along with 
 * the original credentials in a second post -- in order to confirm the login 
 * to Wikipedia (i.e., bind the cookie). This class handles both phases.
 * 
 * January 2016 -- whereas we previously manually constructed 
 * cookie-strings from returned API values, WMF broke this functionality
 * with SessionManager updates. We now use a Java CookieManager to handle 
 * cookies (which also enables removal of expired cookies, etc.), 
 * requiring less explicit treatment in the class. Instead of returning
 * a full cookie, the second-phase of this parser now returns a value
 * that wraps the "success" of the login.
 */
public class api_xml_login extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private String string_result = null;
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
			
		if(!qName.equals("login"))
			return; // Only interested in one tag, return if other seen
			
		String result = attributes.getValue("result");
		if(result.toUpperCase().equals("NEEDTOKEN")){
			this.string_result = attributes.getValue("token");
		} // Login first phase: retrieve the edit token for credentials
		
		else if(result.toUpperCase().equals("SUCCESS")){
			
			this.string_result = "SUCCESS"; // new 2016-01
		    
			/* CODE REMOVED IN 2016-01:
			 * String pfx = attributes.getValue("cookieprefix"); // Generalize
			this.string_result = pfx + "UserName=";
			this.string_result += attributes.getValue("lgusername") + "; ";
			this.string_result += pfx + "UserID=";
			this.string_result += attributes.getValue("lguserid") + "; ";
			this.string_result += pfx + "Token=";
			this.string_result += attributes.getValue("lgtoken") + "; ";
			this.string_result += pfx + "Session=";
			this.string_result += attributes.getValue("sessionid");*/
			
		} // Login second phase: determine if login successful
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return The value returned is dependent on the "phase" of login 
	 * being attempted. If the initial phase (just a user/pass POST'ed), then
	 * this will return the login token. If the second phase (user/pass/token
	 * POST'ed), then this will return "SUCCESS" if logged in succeed.
	 * As of 1/2016 a separate upstream CookieManger handles cookies. 
	 * If any phase fails, the return String will be NULL.
	 */
	public String get_result(){
		return (this.string_result);
	}
	
}
