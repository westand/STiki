package mediawiki_api;

import mediawiki_api.api_post.EDIT_OUTCOME;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_edit_reponse.java - The SAX-XML parse handler,
 * which given the XML response following a POST which attempted to make
 * an edit (revert) to Wikipedia -- determines if that edit was committed.
 */
public class api_xml_edit_response extends DefaultHandler{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Given the event-driven nature of the class, methods cannot simply
	 * return objects as we'd like. Instead, we store the result and
	 * then make an explicit method call to retrieve it.
	 */
	private EDIT_OUTCOME edit_result = EDIT_OUTCOME.ERROR;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("edit")){
			if(attributes.getValue("result").toUpperCase().equals("SUCCESS"))
				this.edit_result = EDIT_OUTCOME.SUCCESS;
			if(attributes.getValue("nochange") != null)
				this.edit_result = EDIT_OUTCOME.BEATEN;
		} // Fist look at "result" field of the <edit> tag. If it reports 
		  // success, also check for "nochange" -- which occurs if someone
		  // beats the editor to revert action.
		
		if(qName.equals("error")){
			try{System.err.println("An edit (not rollback) failed to commit. " +
					"The server returned code \"" + 
					attributes.getValue("code") + 
					"\" and details \"" + attributes.getValue("info") + "\"");
			} catch(Exception e){} // Debugging output for edit failure
		}
		
		/* Example error message: <api servedby="srv294"> <error code="notitle" 
		 * info="The title parameter must be set" /></api>*/
	}

	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Outcome of the edit-attempt that inititiated the response
	 * XML processed by this class.
	 */
	public EDIT_OUTCOME get_result(){
		return(this.edit_result);
	}
	
}
