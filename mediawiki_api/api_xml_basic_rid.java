package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.metadata;
import db_server.db_geolocation;

/**
 * Andrew G. West - api_xml_basic_rid.java - The SAX-XML parse handler for
 * parsing the metadata of a single RID edit. 
 */
public class api_xml_basic_rid extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * DB-handler for IP-to-country mappings.
	 */
	private db_geolocation db_geo;
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private metadata result_metadata = null;
	
	/**
	 * An indication if (caught) error has already occured in the parse
	 * process (primarily, if the passed RID is a `bad' one). 
	 */
	private boolean error = false;
	
		// Datafields of metadata object, found in XML
	private String id_rev;		// ID# assigned to an individual revision
	private String timestamp;	// Timestamp at which an edit was made
	private String title;		// Title of a Wikipedia article
	private String id_page;		// ID# associated with page title
	private String namespace;	// Namespace in which 'title' resides
	private String user;		// IP/User-name of editor
	private String comment;		// Comment associated with an edit
	private String rb_token;	// Rollback token for this edit
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [api_xml_basic_rid].
	 * @param db_geo DB-handler for IP-to-country mappings.
	 */
	public api_xml_basic_rid(db_geolocation db_geo){
		this.db_geo = db_geo;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(error) // Don't risk exceptions if something already wrong
			return;
	
		if(qName.equals("page")){
			title = attributes.getValue("title");
			id_page = attributes.getValue("pageid");
			namespace = attributes.getValue("ns");
		} else if(qName.equals("rev")){
			id_rev = attributes.getValue("revid");
			timestamp = attributes.getValue("timestamp");
			comment = attributes.getValue("comment");
			rb_token = attributes.getValue("rollbacktoken");
			
			user = attributes.getValue("user");
			if(user == null) // Some edits show 'userhidden' instead of 'user'
				user = attributes.getValue("userhidden");
			
		} else if(qName.equals("badrevids")){
			error = true;
		} // Tags "page" and "rev" are of interest, "badrevids" are errors
	}
	
	/**
	 * Overriding: Called whenever a closing tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName) 
			throws SAXException{
		
		if(!error && qName.equals("rev")){
			try{ 
				result_metadata = new metadata(id_rev, timestamp, 
					title, id_page, namespace, user, comment, rb_token, db_geo);
			} catch(Exception e){
				System.out.println("Failed to populate metadata object:");
				System.out.println("RID in question is: " + id_rev);
				e.printStackTrace();
			} // Catch possible errors from time-stamp conversion
		} // End of revision tag; save revision metadata
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Basic metadata about the RID over which the API-call was made.
	 */
	public metadata get_result(){
		return (result_metadata);
	}
	
}
