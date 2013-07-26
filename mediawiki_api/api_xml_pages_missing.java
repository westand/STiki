package mediawiki_api;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_pages_missing.java - The SAX-XML parse handler
 * that given a set of page titles determines which are missing.
 */
public class api_xml_pages_missing extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 * 
	 * Here it is the (sub)set of missing pages provided at construction.
	 */
	private Set<String> result_missing_pages = new TreeSet<String>(); 
	
	/**
	 * Map which stores the normalization of page names (from normalized 
	 * mapping back to unnormal input). The API gives us this in response to 
	 * our query. We use it so we can turn the "normalized missing pages"
	 * back into a set which mirrors the titles provided at input.
	 */
	private Map<String,String> normalize_map = new TreeMap<String,String>();
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
	
		if(qName.equals("n")){
			String from = attributes.getValue("from");
			String to = attributes.getValue("to");
			normalize_map.put(to, from);
		} // Build normalization map pointing back towards our input
 		
		else if(qName.equals("page")){
			if(attributes.getValue("missing") != null){
				String normal_title = attributes.getValue("title");
				if(normalize_map.containsKey(normal_title))
					result_missing_pages.add(normalize_map.get(normal_title));
				else result_missing_pages.add(normal_title);
			} // Indication of missing page; look to map for normalization
		} // Check for missing titles, de-normalize for input consistency.
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Set containing page titles that do NOT exist. This is a 
	 * subset of the titles provided at construction.
	 */
	public Set<String> get_result(){
		return(result_missing_pages);
	}
	
	/*
	 * <?xml version="1.0"?>
		<api>
		  <query>
		    <normalized>
		      <n from="WP:STiki" to="Wikipedia:STiki" />
		      <n from="example3" to="Example3" />
		      <n from="Main_Page" to="Main Page" />
		    </normalized>
		    <pages>
		      <page ns="0" title="Example2" missing="" />
		      <page ns="0" title="Example3" missing="" />
		      <page pageid="1646233" ns="0" title="Example" />
		      <page pageid="15580374" ns="0" title="Main Page" />
		      <page pageid="26352953" ns="4" title="Wikipedia:STiki" />
		    </pages>
		  </query>
		</api>
	 */
	
	
}
