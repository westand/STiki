package mediawiki_api;

import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_xlink.java - The SAX-XML parse handler, which
 * outputs the external links appearing on some page
 */
public class api_xml_xlink extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Set containing the external link URLs on some article.
	 */
	private Set<String> result_set = new TreeSet<String>();
	
	/**
	 * Buffer storing link currently under construction.
	 */
	private StringBuffer cur_link = new StringBuffer();
	
	/**
	 * All interesting data lies between <el> and </el> tags, this
	 * boolean tracks if we are between the opening and closing thereof.
	 */
	private boolean el_active = false;

	/**
	 * Page ID (PID) of article whose external links are being enumerated.
	 * Required in order to make recursive calls if > 500 results. 
	 */
	private long pid; 
	
	
	// ***************************** CONSTRUCTORS ****************************

	/**
	 * Construct a [api_xml_xlink].
	 * @param pid Page ID (PID) of article whose xlinks are being enumerated
	 */
	public api_xml_xlink(long pid){
		this.pid = pid;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("el"))
			el_active = true;
		else if(qName.equals("extlinks")){
			try{if(attributes.getValue("eloffset") != null){
					result_set.addAll(api_retrieve.process_xlinks(
							pid, Integer.parseInt(
							attributes.getValue("eloffset"))));	
				} // If > 500 links in current query, go recursive
			} catch(Exception e){
				System.err.println("Error in recursive xlink search:");
				e.printStackTrace();
			} // Try-catch for interface compliance
		}
	}
	
	/***
	 * Overriding: Returns text between opening and closing tags.
	 * REMEMBER: May be called multiple times; must concatenate.
	 */
	public void characters(char[] ch, int start, int length) 
			throws SAXException{
	
		if(el_active) // Only interested in <el> text
			cur_link.append(String.valueOf(ch, start, length));
	}
	
	/**
	 * Overriding: Called whenever a closing tag is encountered.
	 */
	public void endElement(String uri, String localName, String qName) 
			throws SAXException{
		
		if(qName.equals("el")){
			el_active = false;
			result_set.add(cur_link.toString().replace("&amp;", "&"));
			cur_link = new StringBuffer();
		} // On closing, write finished link, prepare for next
	}
	
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Set containing external link URLs appearing on
	 * the page (actually, 'pid') provided at construction.
	 */
	public Set<String> get_result(){
		return(result_set);
	}
	
}
