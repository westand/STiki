package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_size_time.java -  The SAX-XML parse handler, 
 * which given XML regarding an edit in a particular point in page history,
 * extracts the size of that page
 */
public class api_xml_size_time extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Eventual result; Is 'user' autoconfirmed?
	 */
	private int page_size = 0;
	
	
	// ***************************** CONSTRUCTORS ****************************	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{

		if(qName.equals("rev"))
			page_size = Integer.parseInt(attributes.getValue("size"));
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Size of the page in bytes, at timestamp provided as argument.
	 * Zero will be returned if any error occurs.
	 */
	public int get_result(){
		return (page_size);
	}
	

}
