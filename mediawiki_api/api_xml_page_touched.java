package mediawiki_api;

import java.util.Map;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import core_objects.stiki_utils;

/**
 * Andrew G. West - api_xml_page_touched.java - The SAX-XML parse handler,
 * which parses "last touched" data for a set of pages. 
 */
public class api_xml_page_touched extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private Map<String,Long> touch_times = new TreeMap<String,Long>();
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("page")){
			if(attributes.getValue("missing") == null){
				touch_times.put(attributes.getValue("title"), 
						stiki_utils.wiki_ts_to_unix(
						attributes.getValue("touched")));	
			} // Omit missing pages from output
		} // We expect one of these <pages> for each input value
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return A map from page titles to to the last timestamp at which
	 * they were touched. Pages that DNE will not have a map entry.
	 */
	public Map<String,Long> get_result(){
		return(touch_times);
	}
	
}
