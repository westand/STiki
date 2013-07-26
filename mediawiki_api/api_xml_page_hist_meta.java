package mediawiki_api;

import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.metadata;
import db_server.db_geolocation;

/**
 * Andrew G. West - api_xml_page_hist_meta.java - The SAX-XML parse handler for
 * parsing Wikimedia-API XML dumps which contain data for multiple rids.
 */
public class api_xml_page_hist_meta extends DefaultHandler{
	
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
	private Set<metadata> result_md_set;
	
	/**
	 * Queries sometimes return "bad"-RIDs, those about which no metadata
	 * is available. These RIDs are given their own XML section, and the
	 * boolean 'ignore_mode' tracks if our parse is internal to that section.
	 */
	private boolean ignore_mode;

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
	 * Construct a [api_xml_multiple_rids] -- intializing the return set.
	 */
	public api_xml_page_hist_meta(db_geolocation db_geo){
		this.db_geo = db_geo;
		this.ignore_mode = false;
		this.result_md_set = new HashSet<metadata>();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("badrevids"))
			ignore_mode = true;
		if(ignore_mode)
			return; // Exit if internal to 'bad-rid' section.
		
		else if(qName.equals("page")){
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
		}
	}

	/**
	 * Overriding: Called whenever a closing tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName) 
			throws SAXException{
		
		if(qName.equals("badrevids"))
			ignore_mode = false;
		if(ignore_mode)
			return; // Exit if internal to 'bad-rid' section.
			
		if(qName.equals("page"))
			this.reset_page_level();
		else if(qName.equals("rev")){
			
			try{ result_md_set.add(new metadata(id_rev, timestamp, title, 
					id_page, namespace, user, comment, rb_token, db_geo));
			} catch(Exception e){
				System.out.println("Failed to populate metadata object");
				System.out.println("RID in question is: " + id_rev);
				e.printStackTrace();
			} // Catch possible errors arising from time-stamp conversion
			
			this.reset_rev_level();
			
		} // End of revision tag; save revision metadata
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Set containing metadata objects for those RIDs whose XML
	 * representation was given to this handler.
	 */
	public Set<metadata> get_result(){
		return (result_md_set);
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * It may be possible for data-fields to 'carry-over' between revisions.
	 * (i.e., if one edit has a comment, and the second does not -- the first
	 * would persist and possibly be associated with the second edit). To
	 * prevent this, we reset the revision-level fields.
	 */
	private void reset_rev_level(){
		id_rev = "";
		timestamp = "";
		user = "";
		comment = "";
	}
	
	/**
	 * Similar in spirit to the above [reset_rev_level()], we also reset
	 * page-level fields when reading a page-closing tag.
	 */
	private void reset_page_level(){
		title = "";
		id_page = "";
		namespace = "";
	}
	
}
