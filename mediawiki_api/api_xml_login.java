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
 * As of early April 2010 -- this became a two step process. A login attempt
 * will return a "login token". This token must then be submitted along with 
 * the original credentials in a second post -- in order to confirm the login 
 * to Wikipedia (i.e., bind the cookie). This class handles both phases.
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
		    String pfx = attributes.getValue("cookieprefix"); // Generalize
			this.string_result = pfx + "UserName=";
			this.string_result += attributes.getValue("lgusername") + "; ";
			this.string_result += pfx + "UserID=";
			this.string_result += attributes.getValue("lguserid") + "; ";
			this.string_result += pfx + "Token=";
			this.string_result += attributes.getValue("lgtoken") + "; ";
			this.string_result += pfx + "_session=";
			this.string_result += attributes.getValue("sessionid");
		} // Login second phase: build cookie from response
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return The value returned is dependent on the "phase" of login 
	 * being attempted. If the initial phase (just a user/pass POST'ed), then
	 * this will return the login token. If the second phase (user/pass/token
	 * POST'ed), then this will return the session cookie. If any phase of the
	 * login attempt fails for any reason, the String will be NULL.
	 */
	public String get_result(){
		return (this.string_result);
	}
	
}
