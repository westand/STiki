package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.stiki_utils;

/**
 * Andrew G. West - api_xml_user_first.java - The SAX-XML parse handler, which
 * given a user-ID, returns the time-stamp of the first edit made by that user.
 */
public class api_xml_user_first extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private Long user_first_edit_ts_result = (long) -1;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("item")){
			this.user_first_edit_ts_result = stiki_utils.wiki_ts_to_unix(
					attributes.getValue("timestamp"));
		}  // Only interested in a single attribute of a single tag
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return UNIX timestamp  of user's first edit, or negative (-1) if 
	 * this data could not be located (recall, zero is a legal result).
	 */
	public Long get_result(){
		return (user_first_edit_ts_result);
	}
	
}
