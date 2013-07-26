package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.pair;

/**
 * Andrew G. West - api_xml_edit_token.java - The SAX-XML parse handler
 * to obtain an edit token (as well as store the timestamp for that
 * token), for some page whose XML data is given to this class. 
 */
public class api_xml_edit_token extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private pair<String,String> token_result;
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
	
		if(qName.equals("page")){
			String token = attributes.getValue("edittoken");
			String time = attributes.getValue("starttimestamp");
				
				// Tranform time: 2010-02-18T04:48:31Z --> 20100218044831
				// Just remove all non-digit characters
			time = time.replaceAll("\\D*", "");	
			token_result = new pair<String,String>(token, time);
		}
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Pair whose first element is the edit token, and whose second
	 * element is the time the token was obtained (in YYYYMMDDHHMMSS format).
	 */
	public pair<String,String> get_result(){
		return (token_result);
	}
	
}
