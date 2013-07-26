package mediawiki_api;

import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Andrew G. West - api_xml_cat_members.java - The SAX-XML parse handler, 
 * which for a provided category, produces the members of that category
 * in set form.
 */
public class api_xml_cat_members extends DefaultHandler{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Category whose members are desired
	 */
	private String category;
	
	/**
	 * Whether or not to crawl into sub-categories when encountered.
	 */
	private boolean recursive;
	
	/**
	 * When 'recursive' is TRUE, we have to have some means of ensuring
	 * we are not crawling a cyclical graph pattern; this list helps.
	 */
	private Set<String> cats_traversed; 
	
	/**
	 * Recursively built set containing article members of 'category'
	 */
	private Set<String> members;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [api_xml_cat_members] object.
	 * @param category Category whose members are desired
	 * @param recursive Whether or not to crawl into sub-categories
	 * @param cats_traversed (sub)-Categories already visisted; avoid cycles
	 */
	public api_xml_cat_members(String category,	boolean recursive,  
			Set<String> cats_traversed){
		this.category = category;
		this.recursive = recursive;
		this.cats_traversed = cats_traversed;
		this.members = new TreeSet<String>();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Called whenever an opening tag is encountered.
	 */
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException{
		
		if(qName.equals("cm")){
			String title = attributes.getValue("title");
			members.add(title);
			if(recursive && title.startsWith("Category:") && 
					!cats_traversed.contains(title)){
				cats_traversed.add(title); // must be BEFORE traversal
				try{members.addAll(api_retrieve.process_cat_members(
						title, recursive, cats_traversed, null));
				} catch(Exception e){e.printStackTrace();}
			} // Traverse sub-categores if desired
			
		} else if(qName.equals("categorymembers") && 
				attributes.getValue("cmcontinue") != null){
			try{ members.addAll(api_retrieve.process_cat_members(
					this.category, recursive, cats_traversed,
					attributes.getValue("cmcontinue")));
			} catch(Exception e){e.printStackTrace();}
		} // Latter is the recursive case
	}
		
	/**
	 * Assuming the XML parse has been completed, this returns the result.
	 * @return Set containing all members of 'category'
	 */
	public Set<String> get_result(){
		return(members);
	}
	
}