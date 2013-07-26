package mediawiki_api;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_latest_page.java - The SAX-XML parse handler
 * to parse out the RID of the last edit(s) made on some article(s).
 */
public class api_xml_latest_page extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private Map<Long,Long> pid_rid_result;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [api_xml_latest_page] object; initialize output struct.
	 */
	public api_xml_latest_page(){
		this.pid_rid_result = new HashMap<Long,Long>();	
	}
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
	
		if(qName.equals("page")){
			long rid, pid = Long.parseLong(attributes.getValue("pageid"));
			if(attributes.getValue("lastrevid") != null)
				rid = Long.parseLong(attributes.getValue("lastrevid"));
			else rid = -1;
			this.pid_rid_result.put(pid, rid);
		} // data lines contained within one tag and its attributes
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return RID(s) of last edit made on article(s) (IDs passed)
	 */
	public Map<Long,Long> get_result(){
		return (pid_rid_result);
	}
	
}
