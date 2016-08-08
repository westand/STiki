package gui_edit_queue;

import executables.stiki_frontend_driver;
import gui_support.gui_settings;

/**
 * Andrew G. West - edit_queue_filters.java - Centralized logic for 
 * the user enabled "edit queues" that determine whether an edit will
 * (not) be enqueued due to some property. 
 */
public class edit_queue_filters{
	
	//**************************** PUBLIC METHODS ****************************
	
	/**
	 * Test a provided edit against all user-enabled filters
	 * @param parent Parent GUI, primarily to enable access to the "filters"
	 * menu to see which filters are enabled
	 * @param cur_edit Wrapper for edit being tested against filters
	 * @return NULL if the edit should not be enqueued based on enabled
	 * filters. Otherwise, return the passed 'cur_edit'
	 */
	public static gui_display_pkg run_all_filters(stiki_frontend_driver parent, 
			gui_display_pkg cur_edit){
		
			// Be cautious here: A filter being "unchecked" often means
			// it is in fact "enabled", and meeting a filters criteria
			// affirmatively sometimes means edit rejection.
			//
			// BE CAREFUL WITH THIS MIXED POLARITY

		if(!parent.menu_bar.get_filter_menu().get_privileged_status()){
			if(filter_privileged_editor(cur_edit))
				return(null);
		}
		
		if(!parent.menu_bar.get_filter_menu().get_numerical_status()){	
			if(filter_minor_numerical(cur_edit))
				return(null);
		}
		
		if(gui_settings.editor_is_ignored(cur_edit.metadata.user)){
			return(null);
		}
			
		return(cur_edit);
	}
	
	
	//**************************** PRIVATE METHODS ***************************
	
	
	/**
	 * Determine whether an edit was made by a privileged editor. 
	 * @param cur_edit Wrapper for edit being tested against filter
	 * @return TRUE if edit was made by a privileged user. FALSE, otherwise.
	 */
	private static boolean filter_privileged_editor(gui_display_pkg cur_edit){
		
		if(cur_edit.user_perms.contains("reviewer")  || 
				cur_edit.user_perms.contains("rollbacker")  || 
				cur_edit.user_perms.contains("administrator") ||
				cur_edit.user_perms.contains("sysop"))
			return(true);
		else return(false);
	}
	
	/**
	 * Determine whether an edit is "numerically minor" in nature
	 * @param cur_edit Wrapper for edit being tested against filter
	 * @return TRUE if edit is "numerically minor". FALSE, otherwise
	 */
	private static boolean filter_minor_numerical(gui_display_pkg cur_edit){
	
		if(cur_edit.removed_tokens.size() == 1 && 
				cur_edit.added_tokens.size() == 1){
			
			  try{  // Remove possible thousand(s) separators
				  
			    double prev = Double.parseDouble(
			    		cur_edit.removed_tokens.get(0).replace(",","")); 
			    double now = Double.parseDouble(
			    		cur_edit.added_tokens.get(0).replace(",",""));
			    if((now/prev) > 0.5 && (now/prev) < 2.0)
			    	return(true);
			    else return(false);
			    
			  } catch(NumberFormatException nfe){  
			    return(false);  
			  }  // If mods are not numerical; parse will try-catch
		} // Only consider edits with one token modification
		
		return(false);
	}
	
}
