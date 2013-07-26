package mediawiki_api;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.pair;
import core_objects.stiki_utils;

/**
 * Andrew G. West - api_xml_block_hist.java - The SAX-XML parse handler, 
 * which given the server reponse (xml) to a query regarding a user's block
 * history, parses that history into a Java-friendly form.
 */
public class api_xml_block_hist extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private List<pair<Long,String>> blk_hist = 
			new ArrayList<pair<Long,String>>();

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{

		if(qName.equals("item")){
			Long timestamp = stiki_utils.wiki_ts_to_unix(
					attributes.getValue("timestamp"));
			String action = attributes.getValue("action");
			blk_hist.add(new pair<Long,String>(timestamp, action));
		} // Per query, these are known to be block items
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return A list of all block/unblock actions. The list goes from
	 * newer->older. Elements are pairs, whose first element is the
	 * UNIX timestamp of the action, and the second element is either
	 * "block" or "unblock".
	 */
	public List<pair<Long,String>> get_result(){
		return (blk_hist);
	}
	
}
