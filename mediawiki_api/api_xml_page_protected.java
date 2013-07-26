package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_page_protected.java - The SAX-XML parse handler, 
 * which given the server reponse (xml) to a query asking if a particular
 * page is edit-protected -- parses that response to a boolean answer.
 */
public class api_xml_page_protected extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private boolean is_protected = false;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("flagged")){
			if(attributes.getValue("type").equals("edit"))
				is_protected = true;
		} // Tags of the form below indicate edit-level protection:
		  // <pr type="edit" level="autoconfirmed" expiry="infinity" />		
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Whether or not the page queryed about (whose response is the
	 * XML document parsed) current has any edit-protections.
	 */
	public boolean get_result(){
		return (is_protected);
	}
	
}
