package mediawiki_api;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.pair;

/**
 * Andrew G. West - api_xml_joint_contribs.java - The SAX-XML parse handler 
 * for parsing XML responses that contain contribution data, but uniquely,
 * have this for multiple authors.
 */
public class api_xml_joint_contribs extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * List of users for whose contributions we are interested in. This
	 * is passed in, in case we need to make recursive calls to retrieve 
	 * "additional" results that would not fit in a single response. 
	 */
	private List<String> USERS;
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private List<pair<Long,Long>> CONTRIBS;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [api_xml_joint_contribs] -- intializing the return set.
	 */
	public api_xml_joint_contribs(List<String> users){
		this.USERS = users;
		this.CONTRIBS = new ArrayList<pair<Long,Long>>();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		try{ 
			if(qName.equals("item")){
				this.CONTRIBS.add(new pair<Long,Long>(
						Long.parseLong(attributes.getValue("revid")),
						Long.parseLong(attributes.getValue("pageid"))));
			} else if(qName.equals("usercontribs")){
				if(attributes.getValue("ucstart") != null){
					String next_ts = attributes.getValue("ucstart");
					this.CONTRIBS.addAll(api_retrieve.process_joint_contribs(
							USERS, next_ts));
				} else if (attributes.getValue("uccontinue") != null){
					String next_ts = attributes.getValue("ucstart").split("|")[1];
					this.CONTRIBS.addAll(api_retrieve.process_joint_contribs(
							USERS, next_ts));
				} // Depending on the # of users, continue condition varies
			} // Either dealing with a revision of continuation condition
		} catch(Exception e){
			e.printStackTrace(); 
		} // Try-catch required per interface compliance
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return A list containing pairs. The first element will be an RID.
	 * The second element will be an PID. The output will contain these pairs
	 * for ALL edits made by a user in the provided set, after some time.
	 */
	public List<pair<Long,Long>> get_result(){
		return(CONTRIBS);
	}
		
}
