package mediawiki_api;

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_user_perm.java - The SAX-XML parse handler, which
 * given a user-ID, queries the API to determine his/her permission groups.
 */
public class api_xml_user_perm extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private Set<String> user_perms;
		
	/**
	 * The XML tag of interest is [g] (for individual groups). We track
	 * when we are internal to such a tag (i.e., between <g> and </g>).
	 */
	private boolean g_active = false;
	
	
	// ************************* PUBLIC STATIC METHODS ***********************
	
	/**
	 * Determine if a permissions set contains rollback rights.
	 * @param Permission set, obtained by querying this class for some user
	 * @returns TRUE if set has RB (rollbacker or sysop); FALSE, otherwise
	 */
	public static boolean has_rollback(Set<String> perms){
		if(perms.contains("rollbacker") || perms.contains("sysop"))
			return true;
		else return false;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Construct an [api_xml_user_perm].
	 * @param user_pass User whose editing privileges are being examined.
	 */
	public api_xml_user_perm(String user_pass){
		this.user_perms = new HashSet<String>();
	}
	
	/***
	 * Overriding: Returns text between opening and closing tags.
	 */
	public void characters(char[] ch, int start, int length) 
			throws SAXException{
	
			// Get the intra-tag string, examine permissions
			//	
			// Note that StringBuilder is not used here due to known
			// small size of the text to be returned/appended.
		String str = String.valueOf(ch, start, length).trim();
		if(g_active)
			this.user_perms.add(str);
	}
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		if(qName.equals("g"))
			g_active = true;
	}
		
	/**
	 * Overriding: Called whenever a closing tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName) 
			throws SAXException{
		if(qName.equals("g"))
			g_active = false;
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return TRUE if passed user has RB permission; FALSE, otherwise.
	 */
	public Set<String> get_result(){
		return (user_perms);
	}
	
}
