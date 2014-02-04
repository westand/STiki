package mediawiki_api;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.pair;
import core_objects.metadata;
import db_server.db_geolocation;

/**
 * Andrew G. West - api_xml_deleted_revs.java - The SAX-XML parse handler for
 * parsing Wikimedia-API XML dumps which contain data for deleted revisions.
 */
public class api_xml_deleted_revs extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * User whose deleted revisions are being calculated.
	 */
	private String user;
	
	/**
	 * Whether or not revision content is being compiled for output
	 */
	private boolean do_content;
	
	/**
	 * Login cookie of calling user; providing evidence the user has sufficent
	 * permissions to make this protected call.
	 */
	private String cookie;
	
	/**
	 * Output list of deleted revisions and metadata.
	 */
	private List<pair<metadata, String>> deleted;
	
	/**
	 * Tracker whether or not we are internal to a "<rev></rev>" tag,
	 * which is where revision content is stored.
	 */
	private boolean rev_open = false;

		// Datafields towards building metadata objects and content
	private StringBuilder content;
	private String rid;			// ID# assigned to an individual revision
	private String timestamp;	// Timestamp at which an edit was made
	private String title;		// Title of a Wikipedia article
	private String namespace;	// Namespace in which 'title' resides
	private String comment;		// Comment associated with an edit
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [api_xml_deleted_revs] -- intializing the return set.
	 */
	public api_xml_deleted_revs(String user, boolean do_content, String cookie){
		this.user = user;
		this.do_content = do_content;
		this.cookie = cookie;
		this.deleted = new ArrayList<pair<metadata, String>>();
		reset_rev_level();
		reset_page_level();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
				
		if(qName.equals("page")){
			title = attributes.getValue("title");
			namespace = attributes.getValue("ns");
		} else if(qName.equals("rev")){
			rid = attributes.getValue("revid");
			timestamp = attributes.getValue("timestamp");
			comment = attributes.getValue("comment");
			rev_open = true;
		} else if(qName.equals("deletedrevs")){
			if(attributes.getValue("drstart") != null){
				try{deleted.addAll(api_retrieve.process_deleted_revs(
							user, do_content, attributes.getValue("drstart"), cookie));
				} catch(Exception e){
					System.out.println("Failed to parse deleted RID: " + rid);
					e.printStackTrace();
				} // try-catch for interface compliance				
			} // make sure we have a continue code
		}
	}
	
	/**
	 * Overriding: Called to handle text between two tags.
	 */
	public void characters(char[] ch, int start, int length){
		
			// Only care about intra-tag text if it is content.
			// Recall, this method may be called multiple times if a large
			// text block is encountered, thus we "+=" rather than just set.
		if(rev_open && do_content)
			content.append(String.valueOf(ch, start, length));
	}

	/**
	 * Overriding: Called whenever a closing tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName) 
			throws SAXException{
		
		if(qName.equals("page"))
			this.reset_page_level();
		else if(qName.equals("rev")){
			try{deleted.add(new pair<metadata,String>(
						new metadata(rid, timestamp, title, "-1", namespace, 
						user, comment, null, "", (db_geolocation) null), 
						content.toString()));
			} catch(Exception e){
				System.out.println("Failed to parse deleted RID: " + rid);
				e.printStackTrace();
			} // try-catch needed for interface compliance (arg parsing)
			this.reset_rev_level();
			rev_open = false;
		} // End of revision tag; save revision metadata
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return List containing deleted revision data for a user; read 
	 * the generating method documentation for more information.
	 */
	public List<pair<metadata, String>> get_result(){
		return(deleted);
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * It may be possible for data-fields to 'carry-over' between revisions.
	 * (i.e., if one edit has a comment, and the second does not -- the first
	 * would persist and possibly be associated with the second edit). To
	 * prevent this, we reset the revision-level fields.
	 */
	private void reset_rev_level(){
		rid = "";
		timestamp = "";
		comment = "";
		content = new StringBuilder("");
	}
	
	/**
	 * Similar in spirit to the above [reset_rev_level()], we also reset
	 * page-level fields when reading a page-closing tag.
	 */
	private void reset_page_level(){
		title = "";
		namespace = "";
	}
	
}
