package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_page_missing.java - The SAX-XML parse handler for
 * determining if some PID maps to a missing/invalid page.
 */
public class api_xml_page_missing extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private boolean result_page_missing = false;
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
	
		if(qName.equals("page")){
			String missing = attributes.getValue("missing");
			if(missing != null)
				result_page_missing = true;
		} // If attribute appears, raise flag
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return If PID over which the API-call was made is missing/invalid
	 */
	public boolean get_result(){
		return(result_page_missing);
	}
	
}
