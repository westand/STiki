package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_block_status.java - The SAX-XML parse handler, 
 * which given the server reponse (xml) to a query asking if a particular
 * user/IP is blcoked -- parses that response to a boolean answer.
 */
public class api_xml_block_status extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private boolean is_blocked = false;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
			// Painfully, if <block ...> appears in the response, it
			// implies the user is blocked. The tag contains block details
			// which are not important for purposes of this parse.
		if(qName.equals("block"))
			is_blocked = true;
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Whether or not the user queryed about (whose response is the
	 * XML document parsed) is currently blocked on Wikipedia.
	 */
	public boolean get_result(){
		return (is_blocked);
	}
	
}
