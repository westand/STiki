package mediawiki_api;

import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_page_cats.java - The SAX-XML parse handler, 
 * which for a provided article, returns its category memberships.
 */
public class api_xml_page_cats extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Page whose memberships are desired.
	 */
	private String page;
	
	/**
	 * Whether we are looking at "hidden" or "visible" categories, note
	 * that these options are mutually exclusive.
	 */
	private boolean hidden;
	
	/**
	 * Recursively built set containing category memberships of 'page'
	 */
	private Set<String> memberships;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [api_xml_page_cats] object.
	 * @param page Page whose memberships are desired
	 * @param hidden Whether we are looking at "hidden" or "visible" 
	 * categories, note that these options are mutually exclusive.
	 */
	public api_xml_page_cats(String page, boolean hidden){
		this.page = page;
		this.hidden = hidden;
		this.memberships = new TreeSet<String>();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("cl")){
			memberships.add(attributes.getValue("title"));
		} else if(qName.equals("categories") && 
				attributes.getValue("clcontinue") != null){
			try{memberships.addAll(api_retrieve.process_page_cats(
					page, hidden, attributes.getValue("clcontinue")));
			} catch(Exception e){e.printStackTrace();}
		} // Latter is the recursive case
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Set containing all category membersips of 'page' with
	 * visibility per the 'hidden' setting
	 */
	public Set<String> get_result(){
		return(memberships);
	}

}
