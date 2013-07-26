package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_page_flagged.java - The SAX-XML parse handler, 
 * which given the server reponse (xml) to a query asking if a particular
 * page is flagged -- parses that response to a boolean answer.
 */
public class api_xml_page_flagged extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private boolean is_flagged = false;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
			// Painfully, if <flagged ...> appears in the response, it
			// implies the page is under pending-changes flags.
		if(qName.equals("flagged"))
			is_flagged = true;
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Whether or not the page queried about (whose response is the
	 * XML document parsed) has "pending-changes" set on it.
	 */
	public boolean get_result(){
		return (is_flagged);
	}
	

}
