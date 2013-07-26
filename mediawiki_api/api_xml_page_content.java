package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_page_content.java - The SAX-XML parse handler,
 * which given the XML response to an article content request -- parses 
 * out that content as a String, and returns it to the caller.
 */
public class api_xml_page_content extends DefaultHandler{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private StringBuilder page_content = new StringBuilder();
	
	/**
	 * Given that edit content is between <rev> and </rev> tags,
	 * we must calculate if the opening tag has been encountered so 
	 * we know when to begin accumulating content text.
	 */
	private boolean at_content = false;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		if(qName.equals("rev"))
			at_content = true;
	}
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName){
		if(qName.equals("rev"))
			at_content = false;
	}

	/**
	 * Overriding: Called to handle text between two tags.
	 */
	public void characters(char[] ch, int start, int length){
		
			// Only care about intra-tag text if it is content.
			// Recall, this method may be called multiple times if a large
			// text block is encountered, thus we "+=" rather than just set.
		if(at_content)
			page_content.append(String.valueOf(ch, start, length));
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Whether or not the edit-attempt that inititiated the response
	 * XML processed by this class was actually comittted.
	 */
	public String get_result(){
		return(this.page_content.toString());
	}
	
}
