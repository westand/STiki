package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.stiki_utils;

/**
 * Andrew G. West - api_xml_autoconfirmed.java -  The SAX-XML parse handler, 
 * which given XML about a user, confirms that the user is the one of
 * interest, and determines if the user has 'autoconfirm' status
 */
public class api_xml_autoconfirmed extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Minimum number of edits to reach autconfirmed status (per 2010/06/24).
	 */
	private static int MIN_EDITS = 10;
	
	/**
	 * Min. number of seconds to reach autoconfirmed status (per 2010/06/24).
	 */
	private static int MIN_TIME_SECS = (60*60*24) * 4;
	
	/**
	 * User whose 'autoconfirmed' status is being checked.
	 */
	private String user;
	
	/**
	 * Eventual result; Is 'user' autoconfirmed?
	 */
	private boolean autoconfirmed_result = false;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [api_xml_autoconfirmed].
	 * @param user User whose autoconfirm status is being queried
	 */
	public api_xml_autoconfirmed(String user){
		this.user = user;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		// Below is an example of the line of interest:
		// <u name="user" editcount="0" registration="2008-05-04T06:52:17Z" />
		
		if(qName.equals("u")){
			if(!attributes.getValue("name").equals(this.user))
				return; // If the queried user did not exist, fail
			
			if(Integer.parseInt(attributes.getValue("edit_count")) < MIN_EDITS)
				return; // If insufficient edits 
			
			if(stiki_utils.wiki_ts_to_unix(attributes.getValue("registration")) 
					+ MIN_TIME_SECS > stiki_utils.cur_unix_time())
				return; // If insufficient time since registration
			
				// If made it this far, all criteria have been met
			this.autoconfirmed_result = true;
			
		} // Only 'u' tags contain data of interest, remainder is assumed
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Autoconfirm status of user passed in at construction.
	 */
	public boolean get_result(){
		return (autoconfirmed_result);
	}
	

}
