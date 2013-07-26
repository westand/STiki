package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.stiki_utils;

/**
 * Andrew G. West - api_xml_page_prior.java - The SAX-XML parse handler, which
 * for a page-ID and revision-ID, returns the time-stamp of the edit made on
 * that page, prior to the provided RID.
 */
public class api_xml_page_prior extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private Long rid_page_prior_result = (long) -1;
	
	/**
	 * Crucially, we want to SKIP the first timestamp we encounter, because
	 * it corresponds to the RID provided. This boolean  tracks if that 
	 * (first) edit/timestamp has been encountered yet in the parse.
	 */
	private boolean first_done = false;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("rev")){
			if(!first_done)
				first_done = true;
			else this.rid_page_prior_result = stiki_utils.wiki_ts_to_unix(
					attributes.getValue("timestamp"));
		}  // Skip first TS (current edit), read out prior
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Timestamp of last edit made on page (prior to a provided
	 * RID), or negative one (-1), if no such edit could be located.
	 */
	public Long get_result(){
		return (rid_page_prior_result);
	}
	
}
