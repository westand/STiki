package mediawiki_api;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_user_edits_date.java - The SAX-XML parse handler, 
 * which for a user-name, and a provided timestamp, returns the total count 
 * number of edits that account had on the wiki before that date. Note 
 * that passed parameters can considerably constraint this count operation.
 */
public class api_xml_user_edits_date extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Username whose edit count is desired (for recursive purposes).
	 */
	private String user;
	
	/**
	 * Namespace in which edit count is being measured
	 */
	private int ns;
	
	/**
	 * If the edit count ever exceeds this threshold, break recursion, 
	 * stop counting, and exit immediately.
	 */
	private long break_num;
	
	/**
	 * Integer on [1,500] indicating how many edits should be fetched at a
	 * time. Useful when letting low 'break_num' input.
	 */
	private int batch_size;
	
	
	/////
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 * 
	 * This is the number of edits we have COUNTED UP TO THIS POINT.
	 * Note the recursive nature of computation.
	 */
	private long edits;

	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [api_xml_usr_edits] object.
	 * @param user Username whose edit count is desired
	 * @param ns Namespace in which edit count is being measured
	 * @param start_num Number at which to start counting (in the recursive
	 * case, this will be the number of edits from previous iterations)
	 * @param break_num If the edit count ever exceeds this threshold,
	 * break recursion, stop counting, and exit immediately.
	 * @param break_size Integer on [1,500] indicating how many edits should 
	 * be fetched at a time. Useful when letting low 'break_num' input.
	 */
	public api_xml_user_edits_date(String user, int ns, 
			long start_num, long break_num, int batch_size){
		this.user = user;
		this.ns = ns;
		this.edits = start_num;
		this.break_num = break_num;
		this.batch_size = batch_size;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("usercontribs") && 
			attributes.getValue("uccontinue") != null){
			
			String continue_key = attributes.getValue("uccontinue");
			
			try{edits = api_retrieve.process_user_edits(this.user, this.ns, 
					0, this.edits, this.break_num,
					continue_key, this.batch_size);
			} catch(Exception e){e.printStackTrace();} 
			
		} else if(qName.equals("item")){

			this.edits++;
			if(this.edits >= this.break_num)
				return; // breaking logic
		}
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Edit count of the user passed at construction, as of a 
	 * timestamp encoded in the input URL.
	 */
	public long get_result(){
		return(edits);
	}
	
}