package gui_support;

import java.util.ArrayList;
import java.util.List;

/**
 * Andrew G. West - diff_spacer.java - Revision diffs are rendered as HTML
 * in a JPane. This works well in the average case -- but lengthy URLs and 
 * formatting sometimes cause this Pane to become unnacceptably wide. 
 * This class examines the visible text in a diff-html-snippet, and inserts
 * whitespace where necessary to facilitate orderly line breaking.
 * 
 * Particularly troublesome is/was the fact that we don't want ZWS to 
 * exist inside HTML formatting tags, but that these tags sometime separate
 * visible text which has no whitespace between it.
 */
public class diff_whitespace{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Maximum number of characters to tolerate without a whitespace character.
	 */
	public static final int MAX_CHARS_WO_SPACE = 10;

	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Given the html of a diff-page, process the visible text to ensure that
	 * is acceptably whitespaced (as defined by a global param). 
	 * @param html diff-HTML over which to perform whitespace processing
	 * @return A modified version of 'html', such that there is no more than
	 * [MAX_CHARS_WO_SPACE] characters without a whitespace one. This applies
	 * only to -visible- text, that outside of HTML brackets.
	 */
	public static String whitespace_diff_html(String html) throws Exception{
		
			// The input will be re-constructed (with spaces), in a character-
			// by-character fashion into the following array
		List<Character> char_list = new ArrayList<Character>();
		
		boolean in_bracket = false;	// Are we currently inside an HTML tag?
		int chars_wo_space = 0;		// How many characters without a space?
		char cur_char;				// Current input character
		
		for(int i=0; i < html.length(); i++){
			cur_char = html.charAt(i);
			if(cur_char == '<') // Beginning of HTML tag, turn off ZWS insert
				in_bracket = true;
			
			if(!in_bracket){
				chars_wo_space++;
				if(Character.isWhitespace(cur_char))
					chars_wo_space = 0; // A hard-space resets counter
				if(chars_wo_space == MAX_CHARS_WO_SPACE){
					char_list.add(cur_char);
					char_list.add(gui_globals.ZWS_CHAR);
					chars_wo_space = 0;
				} else  // If upper bound reached, insert space; reset
					char_list.add(cur_char);
			} else // If not in-tag, ZWS, else ignore the procedure
				char_list.add(cur_char);
			
			if(cur_char == '>') // End of HTML tag, renable ZWS insertion
				in_bracket = false;
		} // Proceed character-by-character through input string

			// Convert character-Collection into char-Array, which enables
			// us to convert the structure back into a string
		char[] char_array = new char[char_list.size()];
		for(int i=0; i < char_list.size(); i++)
			char_array[i] = char_list.get(i);		
		String spaced = String.copyValueOf(char_array);

			// Final cleanup: HTML escape chars. are multi-character. The
			// insertion of a ZWS internally breaks them, thus correct.
		spaced = spaced.replaceAll("\\&[\u200B]*l[\u200B]*t[\u200B]*;|" + 
				"\\&[\u200B]*l[\u200B]*;[\u200B]*t[\u200B]*;", "\\&lt;");
		spaced = spaced.replaceAll("\\&[\u200B]*g[\u200B]*t[\u200B]*;|" + 
				"\\&[\u200B]*g[\u200B]*;[\u200B]*t[\u200B]*;", "\\&gt;");
		return(spaced);
	}
	
	/**
	 * Remove all zero-whitespace (ZWS) characters from a String.
	 * @param str Input string, possibly contain ZWS characters
	 * @return String 'str' with all ZWS characters removed
	 */
	public static String strip_zws_chars(String str){
		return(str.replace(gui_globals.ZWS, ""));
	}
	
	
	// ************************** DEPRECATED METHODS *************************
	//
	// These were useful in a previous approach to the problem
	
	/**
	 * Insert a zero-width-space character at specified intervals in a String.
	 * @param str Input string, in which ZWS characters should be placed
	 * @param n Number of characters between ZWS insertions
	 * @return String 'str', with a ZWS inserted every `n` characters
	 */
	public static String insert_zws_every_n_chars(String str, int n){
	
			// Proceed piece-wise (of n characters), through the input
		String str_remaining = str;
		String str_spaced = "";
		
		while(str_remaining.length() > n){
			str_spaced += str_remaining.substring(0, n) + gui_globals.ZWS;
			str_remaining = str_remaining.substring(n);
		} // Insert a ZWS after every 'n' characters
		str_spaced += str_remaining;
		return str_spaced;
	}
	
	/**
	 * Count the max number of adjacent non-whitespace characters in a string.
	 * This method does not make any distinction if the text is visible, that
	 * is, it treats all text the same regardless of format/language.
	 * @param str Input string -- that being examined
	 * @return Number of adjacent non-whitespace characters in 'str'.
	 */
	public static int max_chars_wo_space(String str){
		int max_chars = 0;
		String[] tokens = str.split("(\\s|" + gui_globals.ZWS + ")");
		for(int i=0; i < tokens.length; i++)
			max_chars = Math.max(max_chars, tokens[i].length());
		return (max_chars);
	}
	
}