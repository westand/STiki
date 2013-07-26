
package core_objects;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Andrew G. West - escape_string.java - The purpose of this class is to
 * escape Java strings -- as is required when examining the "comments"
 * field associated with Wikipedia edits (which may include carrriage
 * return escape sequences, regular expression messiness, etc.). This
 * code is just a reduced version of what is found in the Apache Commons.
 */
public class escape_string{

	// **************************** PUBLIC METHODS ***************************

	/**
	 * The lone public method. Escape a Java string.
	 * @param str Input string, the one that should be escaped
	 * @return Escaped version of 'str'. For example, "/n" becomes "//n"
	 */
    public static String escape(String str) throws Exception{
    	
        if(str == null) return null; // Don't escape empty string
     
        	// Allocate a writer for worst-case, do the escaping
        StringWriter writer = new StringWriter(str.length() * 2);
        escape_java_work(writer, str, false);
        return writer.toString();
    }
    
    /**
     * Unescape the work done by the escape() function.
     * @param str The string which is to be un-escaped
     * @return Un-escaped version of 'str'
     */
    public static String unescape(String str) throws Exception{
    	if(str == null) return null; // Edge-case
		StringWriter writer = new StringWriter(str.length());
		unescape_java(writer, str);
		return writer.toString();	
    }
    
	// *************************** PRIVATE METHODS ***************************
    
    /**
     * Method handling String-level details of escaping a Java string.
     * @param out Writer to which output will be written
     * @param str Input string to the method
     * @param esc_single TRUE if single quotes should be escaped, false
     * otherwise (not necessary in Java, but needed in JavaScript).
     */
    private static void escape_java_work(Writer out, String str, 
    		boolean esc_single) throws IOException{
    	
        int sz = str.length();
        for(int i = 0; i < sz; i++){
            char ch = str.charAt(i);
            if(ch > 0xfff){ // First handle Unicode sequences
                out.write("\\u" + hex(ch));
            } else if(ch > 0xff){
                out.write("\\u0" + hex(ch));
            } else if(ch > 0x7f){
                out.write("\\u00" + hex(ch));
            } else if(ch < 32){
                switch (ch){
                    case '\b': out.write('\\'); out.write('b'); break;
                    case '\n': out.write('\\'); out.write('n'); break;
                    case '\t': out.write('\\'); out.write('t'); break;
                    case '\f': out.write('\\'); out.write('f'); break;
                    case '\r': out.write('\\'); out.write('r'); break;
                    default :
                        if (ch > 0xf) out.write("\\u00" + hex(ch));
                        else out.write("\\u000" + hex(ch));
                        break;
                } // Then the non-Unicode escape characters
            } else{
                switch(ch){
                    case '\'':
                        if (esc_single) out.write('\\');
                        out.write('\'');
                        break;
                    case '"': out.write('\\'); out.write('"'); break;
                    case '\\': out.write('\\'); out.write('\\'); break;
                    // case '/': out.write('\\'); out.write('/'); break;
                    default : out.write(ch); break;
                } // And finally escape any slashes
            }
        } // Step through the input string one character at a time
    }

    /**
     * Return the hexadecimal code for a given character.
     * @param ch Character whose hexadecimal code is required
     * @return Hexidecimal code of character 'ch'
     */
    private static String hex(char ch){
        return Integer.toHexString(ch).toUpperCase();
    }
    
    /**
     * Unescape a provided Java String.
     * @param out Writer to which output will be written
     * @param str Input string
     */
    private static void unescape_java(Writer out, String str) 
    		throws IOException {

		int sz = str.length();
		StringBuffer unicode = new StringBuffer(4);
		boolean hadSlash = false;
		boolean inUnicode = false;
		for(int i = 0; i < sz; i++){
			char ch = str.charAt(i);
			if(inUnicode){
				unicode.append(ch);
				if(unicode.length() == 4){
					int value = Integer.parseInt(unicode.toString(), 16);
					out.write((char) value);
					unicode.setLength(0);
					inUnicode = false;
					hadSlash = false;
				} // We expect unicode escapes to be 4 characters
				continue;
			} // Process a block of hex code (forming a unicode char)
			if(hadSlash){
				hadSlash = false;
				switch(ch){
					case '\\': out.write('\\');	break;
					case '\'': out.write('\''); break;
					case '\"': out.write('"'); break;
					case 'r': out.write('\r'); break;
					case 'f': out.write('\f'); break;
					case 't': out.write('\t'); break;
					case 'n': out.write('\n'); break;
					case 'b': out.write('\b'); break;
					case 'u': inUnicode = true; break;
					default: out.write(ch); break;
				} // Iterate over non-unicode escapes
				continue;
			} else if(ch == '\\'){
				hadSlash = true;
				continue;
			} // The multiple slash is always tricky
			out.write(ch);
		} // Iterate over the entirety of the passed-in String
		if(hadSlash){
			out.write('\\');
		} // Tricky case of a slash at the end of string
    }
    
}
