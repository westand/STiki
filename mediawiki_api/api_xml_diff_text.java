package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_diff_textjava - The SAX-XML parse handler, which
 * outputs the HTML-marked-up DIFF between edit(s).
 */
public class api_xml_diff_text extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private StringBuilder diff_string_result = new StringBuilder();
	
	/**
	 * All interesting data lies between <diff> and </diff> tags, this
	 * boolean tracks if we are between the opening and closing thereof.
	 */
	private boolean diff_active = false;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("diff"))
			diff_active = true;
	}
	
	/***
	 * Overriding: Returns text between opening and closing tags.
	 * REMEMBER: May be called multiple times; must concatenate.
	 */
	public void characters(char[] ch, int start, int length) 
			throws SAXException{
	
		if(diff_active) // Only interested in <diff> text
			diff_string_result.append(String.valueOf(ch, start, length));
	}
	
	/**
	 * Overriding: Called whenever a closing tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName) 
			throws SAXException{
		
		if(qName.equals("diff"))
			diff_active = false;
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return HTML-marked-up diff-text
	 */
	public String get_result(){
		return(diff_string_result.toString());
	}
	
}
