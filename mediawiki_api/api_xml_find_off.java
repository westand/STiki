package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Andrew G. West - api_xml_find_off.java - The SAX-XML parse handler, which
 * given information about a rollback (flagging) occurence, queries the
 * MediaWiki API and attempts to produce the RID of the offending-edit (OE).
 */
public class api_xml_find_off extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private Long offending_rid_result;
	
	/**
	 * User (uppercase) who committed guilty edit,
	 */
	private String uc_off;
	
	/**
	 * Indication of completeness with XML data -- as some spurious XML
	 * appears after the main dataset, which we are not interested in.
	 * Further, this provides a short-circuit if offender quickly found.
	 */
	private boolean finished = false;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Construct an [api_xml_find_off].
	 * @param uc_offender User (uppercase) who committed guilty edit 
	 */
	public api_xml_find_off(String uc_offender_pass){
		this.offending_rid_result = (long) -1;
		this.uc_off = uc_offender_pass;
	}
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(finished)
			return;
		
		if(qName.equals("rev")){
			
			String user = attributes.getValue("user");
			if(user == null)
				return; // Some edits show 'userhidden' instead of 'user'
			else if(user.toUpperCase().equals(this.uc_off)){
				offending_rid_result = Long.parseLong(
						attributes.getValue("revid"));
				finished = true;
			} // If offender match, read RID, and mark as finished
			
		} // Only 'rev' tags contain data of interest, remainder is assumed
	}
	
	/**
	 * Overriding: Called whenever a closing tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName) 
			throws SAXException{
		if(qName.equals("revisions"))
			finished = true;		
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return RID of offending edit, or -1 it could not be found
	 */
	public Long get_result(){
		return (offending_rid_result);
	}
	
}
