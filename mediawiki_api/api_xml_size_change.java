package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Andrew G. West - api_xml_size_change.java - The SAX-XML parse handler, which
 * for a revision-ID, returns the size in bytes of the article on which RID
 * resides (PID), relative to its size after the previous edit on the same
 * page. For example, a value of "+4" would indicate that the RID added 4
 * bytes of content to the page. This number is NOT an edit distance, it
 * refers only to the amount of space necessary to store the page.
 */
public class api_xml_size_change extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private int change = 0;
	
	/**
	 * Calculating the size-change requires comparison of the sizes of two
	 * edits. This boolean tracks if the first of those has yet been processed.
	 */
	private boolean first_done = false;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("rev")){
			
				// For older edits, 'size' field may not exist. Set size to
				// zero. Nature of code is s.t. if any revision lacks the
				// 'size' field, the returned value will always be zero
			if(attributes.getValue("size") == null)
				change = 0;
			else{ 
				if(first_done) // Second time by, compute and store change
					change -= Integer.parseInt(attributes.getValue("size"));
				else{ // First time by, store raw size, flip flag
					change = Integer.parseInt(attributes.getValue("size"));
					first_done = true;	
				} // Are we looking at size of current-RID, or one before?
			} // Depending on age 'size' flag may or may not be present
		} // All interesting work happens internal to the 'rev' tag
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Difference, in bytes, of the size of the article on which
	 * 'rid' resides, relative to its size after the previous edit.
	 */
	public int get_result(){
		return (change);
	}
	
}
