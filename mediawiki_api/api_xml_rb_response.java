package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_rb_reponse.java - The SAX-XML parse handler,
 * which given the XML response following a POST which attempted to make
 * a (native) rollback to Wikipedia -- determines if RB edit was committed --
 * and if that is the case, the EARLIEST edit rolled back.
 */
public class api_xml_rb_response extends DefaultHandler{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private long earliest_rid_rbed;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("rollback")){
	
			if((attributes.getValue("revid") == null) || 
					(attributes.getValue("old_revid") == null) ||
					(attributes.getValue("last_revid") == null)){
				earliest_rid_rbed = -1;
			} else if(attributes.getValue("revid") == 
					attributes.getValue("old_revid")){
				earliest_rid_rbed = 0;
			} else{
				earliest_rid_rbed = 
					Long.parseLong(attributes.getValue("last_revid"));
			} // Check error conditions, then typical response
		} // All data should be contained within a single <rollback> tag
		
		if(qName.equals("error")){
			try{
				if(!attributes.getValue("code").equals("alreadyrolled")){
					System.err.println("A rollback failed to commit. " +
							"The server returned code \"" + 
							attributes.getValue("code") + "\" and details \"" + 
							attributes.getValue("info") + "\"");
					if(attributes.getValue("code").equals("badtoken"))
						earliest_rid_rbed = -2; // Special error code
					if(attributes.getValue("code").equals("assertuserfailed"))
						earliest_rid_rbed = -3; // Special error code
				} // Two error case are explicitly handled
			} catch(Exception e){} // Debugging output for edit failure
		}
		
		/* Example error message: <api servedby="srv294"> <error code="notitle" 
		 * info="The title parameter must be set" /></api>*/
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return The earliest edit RID affected by this action. Zero (0)
	 * is returned if the rollback is not made, because it would make no 
	 * change. The result will be negative if there is an error. In
	 * particular, (-2) is reserved for 'badtoken' errors.
	 */
	public long get_result(){
		return(this.earliest_rid_rbed);
	}
	
}
