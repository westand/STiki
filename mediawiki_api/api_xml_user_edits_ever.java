package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_user_edits_ever.java - The SAX-XML parse handler, 
 * which for a user-name, returns their total edit count (as of "now").
 */
public class api_xml_user_edits_ever extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 * 
	 * This is the edit count of the user who was encoded in the input URL.
	 */
	private long edits = -1L;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("user")){
			if(attributes.getValue("editcount") != null)
				this.edits = Long.parseLong(attributes.getValue("editcount"));
		}
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Whether or not the user queryed about (whose response is the
	 * XML document parsed) is currently blocked on Wikipedia.
	 */
	public long get_result(){
		return(edits);
	}
	
}