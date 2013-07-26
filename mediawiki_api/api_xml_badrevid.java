package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_badrevid.java - The SAX-XML parse handler for
 * determining if some RID returns "badrevid" per Wikipedia.
 */
public class api_xml_badrevid extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private boolean result_badrevid = false;
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("badrevids")){
			result_badrevid = true;
		} // A single instance of tag is sufficient to raise flag
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return If RID over which the API-call was made is a "badrevid"
	 */
	public boolean get_result(){
		return(result_badrevid);
	}
	
}
