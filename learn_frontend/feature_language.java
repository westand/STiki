package learn_frontend;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import core_objects.pair;
import core_objects.stiki_utils;

import mediawiki_api.api_retrieve;

/**
 * Andrew G. West - feature_language.java - This class handles the calculation
 * of all language-based vandalism features. The diff-text is fetched at
 * construction (from the MediaWiki API), and all get-feature calls
 * (the public methods), calculate over that diff-text.
 */
public class feature_language{
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Regular expressions that are highly suggestive of inappropriate content.
	 * Inspired by: http://en.wikipedia.org/wiki/User:ClueBot/Source -- but
	 * we can be more aggressive since false-positives aren't comitted.
	 */
	public static final String[] DIRTY_WORDS = {
		"SUCK",	"STUPID", "HAHA", "PIMP", "DUMB", "HOMO", "GAY", "SLUT", 
		"DAMN", "ASS", "RAPE", "POOP", "COCK", "LOL", "CRAP", "NAZI",
		"FUCK", "BITCH", "PUSSY", "PENIS", "VAG", "WHORE", "SHIT", "NIGG", 
		"WANKER", "CUNT", "FAG", "CHINK", "PISS", "CUM", "WAS HERE", 
		"LOL", "WEED", "DICK", "ORGY", "BLAH", "WTF", ":-\\)", "ANUS", 
		"HELLO", "BOLD", "ITALIC", "ROX", "PEE"};
	
	// AGW -- words I've seen that potentially could be added:
	// "rocks", "rox", "hey", "love", "worst", 
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Diff text from the revision provided at construction to the previous
	 * edit on the same page. This text contains HTML-table formatting, as
	 * well as contextual data, and should be further processed.
	 */
	private String raw_diff_text;
	
	/**
	 * Reduction from field [raw_diff_text] such that we have only edit-text
	 * that appears in the most-recent edit, and only that text which was
	 * added relative to the previous version on the same page.
	 * 
	 * Note this version works at the token level. Only added tokens
	 * will appear in this output (contrast with [added_blocks])
	 */
	private List<String> added_tokens;
	
	/**
	 * Relative to [added_tokens] the content of this variable will contain
	 * "blocks" of added text. Even if a single word is modified in a block
	 * (all that [added_tokens] stores), this structure will contain the
	 * entire line/paragraph -- and therefore may be useful in contextualizing
	 * extremely low-level changes. 
	 */
	private List<String> added_blocks;
	
	/**
	 * Complement to [added_text]. Those tokens removed by revision.
	 */
	private List<String> removed_tokens;
	
	/**
	 * Complement to [added_blocks]. Those blocks removed by revision.
	 */
	private List<String> removed_blocks;
	
	/**
	 * Compiled REGEX pattern containing the words in [DIRTY_REGEX] with
	 * word boundary specififers added to both beginning and end of word.
	 */
	private Pattern DIRTY_BOUND;
	
	/**
	 * Compiled REGEX pattern containing the words in [DIRTY_REGEX] -- this
	 * version contains no word-boundary specifiers, which permits the
	 * dirty phrases to be found internal to larger words.
	 */
	private Pattern DIRTY_EMBED;
	
	
	// ***************************** CONSTRUCTORS ****************************

	/**
	 * Construct a [feature_language] by providing an RID. This fetches the 
	 * diff-text from the MW-API, over which the public methods calculate.
	 * @param rid Revision-ID whose language-based features will be calc'ed
	 */
	public feature_language(long rid) throws Exception{
		
		this.raw_diff_text = api_retrieve.process_diff_prev(rid);
		
		pair<List<String>,List<String>> additions = 
				only_added_text(this.raw_diff_text);
		this.added_tokens = additions.fst;
		this.added_blocks = additions.snd;
		
		pair<List<String>,List<String>> removals = 
				only_removed_text(this.raw_diff_text);
		this.removed_tokens = removals.fst;
		this.removed_blocks = removals.snd;
		
			// Compile regular expressions for "dirty" words
		String bound_string = "", embed_string = "";
		for(int i=0; i < DIRTY_WORDS.length; i++){
			bound_string += "\\b" + DIRTY_WORDS[i] + "\\b|";
			embed_string += DIRTY_WORDS[i] + "|";
		} // Just build words into an OR separated REGEX
		bound_string = bound_string.substring(0, bound_string.length()-1);
		embed_string = embed_string.substring(0, embed_string.length()-1);
		DIRTY_BOUND = Pattern.compile(bound_string);
		DIRTY_EMBED = Pattern.compile(embed_string);
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Return the extent to which the revision-addition is inappropriate. 
	 * @return A score speaking to the number of reg-ex matches from
	 * [DIRTY_REGEX], and content-additions made by the RID provided at 
	 * construction. Note that a single reg-ex may be matched more than once 
	 * by the content. A regex match with word-boundaries scores 5 points,
	 * and word-embedded regex matches receive a single point. For example,
	 * " penis " scores 5, but "youpenisare" would score 1.
	 */
	public int dirty_regex_score(){
		
			// Note on scoring: Notice that in-code that "bound" gets 4
			// points. In effect, such matches will get 5 because any bound
			// match will also be an embedded one and get a point there.
		
		String add_block;
		int total_score = 0, bound, embed;
		Iterator<String> additions = this.added_tokens.iterator();
		while(additions.hasNext()){
			add_block = additions.next().toUpperCase();
			bound = stiki_utils.num_matches_within(DIRTY_BOUND, add_block);
			embed = stiki_utils.num_matches_within(DIRTY_EMBED, add_block);
			total_score += ((bound * 4) + embed);
		} // Iterate over blocks of content addition
		return(total_score);
	}
	
	/**
	 * Return the number of chars. in the longest repitition of any single
	 * character in the revision-addition of the RID given at construction.
	 * @return Number of chars in the longest repetition of any single char.
	 * In the case of alpha-chars, case does not matter.
	 */
	public int longest_char_repetition(){
		
		String add_block;
		char cur_char, prev_char = ' ';
		int longest_rep = 0, current_rep = 1;
		
		Iterator<String> additions = this.added_tokens.iterator();
		while(additions.hasNext()){
			add_block = additions.next().toUpperCase();
			for(int i=0; i < add_block.length(); i++){
				cur_char = add_block.charAt(i);
				if(cur_char == prev_char){
					current_rep++;
					longest_rep = Math.max(longest_rep, current_rep);
				} else 
					current_rep = 1;
				prev_char = cur_char;
			} // Iterate over all characters in an addition block
			current_rep = 0;
		} // Iterate over all add-blocks (repetition may not span)
		return(longest_rep);
	}
	
	/**
	 * Return the percentage of added alpha-text that is uppercase. 
	 * @return Percentage of added alphabetical text that is uppercase. If
	 * the revision added no alpha-characters, then zero (0.0) is returned.
	 */
	public double percentage_uppercase(){
		
		String content;	// Current new-text-block being processed
		char cur_char;	// Current character of 'content' under focus
		int alpha_chars = 0, uc_chars = 0;
		Iterator<String> iter = this.added_tokens.iterator();
		
		while(iter.hasNext()){
			content = iter.next();
			for(int i=0; i < content.length(); i++){
				cur_char = content.charAt(i);
				if(Character.isLetter(cur_char)) alpha_chars++;
				if(Character.isUpperCase(cur_char)) uc_chars++;
			} // Sum both alpha, and alpha-uppercase chars
		} // Iterate over all blocks of added text
		
		if(alpha_chars == 0)
			return(0.0); // Avoid DBZ, just return zero instead
		else return((1.0 * uc_chars)/(1.0 * alpha_chars));
	}
	
	/**
	 * Return the percentage of the edit addition which is alpha-characters
	 * (as opposed to numerical ones, or symbols).
	 * @return Percentage of the edit addition which is alpha-characters, or
	 * 0.0 if there were no characters added.
	 */
	public double percentage_alpha(){
		
		String content;	// Current new-text-block being processed
		int alpha_chars = 0, all_chars = 0;
		Iterator<String> iter = this.added_tokens.iterator();
		
		while(iter.hasNext()){
			content = iter.next();
			for(int i=0; i < content.length(); i++){
				all_chars++;
				if(Character.isLetter(content.charAt(i))) alpha_chars++;
			} // Sum both alpha and all characters
		} // Iterate over all blocks of added text
		
		if(all_chars == 0)
			return(0.0); // Avoid DBZ, just return zero instead
		else return((1.0 * alpha_chars)/(1.0 * all_chars));
	}
	
	/**
	 * Return all tokens ADDED per the revision provided at construction.
	 * @return A List containing all tokens added. Each element (a string),
	 * represents a different portion of text added.
	 */
	public List<String> get_added_tokens(){
		return(this.added_tokens);
	}
	
	/**
	 * Return all text blocks ADDED per the revision provided at construction.
	 * @return A List containing all text blocks added. Note that a "block"
	 * is larger than the "token" version. Even if only one token is changed
	 * in a paragraph, this output will contain the entire paragraph of
	 * modification, as is useful for contextualizing tokens.
	 */
	public List<String> get_added_blocks(){
		return(this.added_blocks);
	}

	/**
	 * Complement to [get_added_tokens()].
	 */
	public List<String> get_removed_tokens(){
		return(this.removed_tokens);
	}
	
	/**
	 * Complement to [get_added_blocks()].
	 */
	public List<String> get_removed_blocks(){
		return(this.removed_blocks);
	}
	
	
	// *************************** PRIVATE METHODS ***************************
		
	/**
	 * Reduce a raw edit-diff to only additions made in the most recent edit.
	 * @param raw_diff_text Raw-diff, incl. HTML-formatting
	 * @return A pair of lists, which broadly contain a subset of the data 
	 * contained in 'raw_diff_text' with all HTML-formatting stripped out, 
	 * all previous edit-data removed, and all contextual data deleted -- 
	 * such that the returned Strings contain only those tokens (words?) 
	 * added by the most recent edit. 
	 * 
	 * The first element of the pair contains only added tokens. If only one
	 * word in a paragraph is changed, the list will contain only that
	 * single word.
	 * 
	 * The second element of the paper contains added blocks. If one word
	 * is changed, the list will contain the entire paragraph. This can
	 * prove useful in contextualizing the token-level changes that the
	 * first element tracks. 
	 */
	private static pair<List<String>,List<String>> only_added_text(
			String raw_diff_text){
		
			// The return sets
		List<String> added_tokens = new ArrayList<String>();
		List<String> added_blocks = new ArrayList<String>();
		
			// Get some working-text, remove stupid tags
		String working_text = raw_diff_text;
		working_text = working_text.replaceAll("<div>|</div>", "");

			// Extract table cells relevant to addition/deletion of content
		String regex = "<td class=\"diff-(addedline|deletedline)\">([^<]*" +
				"<(span|del|ins) class=\"(diffchange|diffchange diffchange-inline)\">" +
				"[^<]*</(span|del|ins)>[^<]*|[^<]*)*</td>";
		List<String> changes = stiki_utils.
				all_pattern_matches_within(regex, working_text);
		
			// Initalize variables for cell-processing loop
		String content_cell, temp;
		boolean has_deleted = false;
		Iterator<String> iter = changes.iterator();
		String span_regex = "<(span|del|ins) class=\"(diffchange|" +
				"diffchange diffchange-inline)\">[^<]*</(span|del|ins)>";
		
		while(iter.hasNext()){
			content_cell = iter.next();	
			if(content_cell.startsWith("<td class=\"diff-deletedline\">")){
				has_deleted = true; // Mark 'deleted' cell un-processed
			} else if(content_cell.startsWith("<td class=\"diff-addedline\">")){
				
				temp = content_cell.replaceAll("<[^<]*>", "");
				added_blocks.add(temp);
				if(has_deleted){ // If addition has deletion partner (spans)
					Iterator<String> spans;
					spans = stiki_utils.all_pattern_matches_within(
							span_regex, content_cell).iterator();
					while(spans.hasNext())
						added_tokens.add(spans.next().replaceAll(
								"<(span|del|ins) class=\"(diffchange|" +
								"diffchange diffchange-inline)\">|</(span|del|ins)>", ""));
				} else // If addition stands-alone
					added_tokens.add(temp);

				has_deleted = false;
			} // Strategy for processing addition-cells depends on the
			  // presence of an adjacent deletion-cell
		} // Process over all cells containing added/deleted content 
	
		return(new pair<List<String>,List<String>>(added_tokens, added_blocks));
	}
	
	/**
	 * This method is the complement of [only_added_text()].
	 * 
	 * Note that this could also be done by just reversing the RID order
	 * in the 'diff' call given to the server, and then recalling the 
	 * [only_added_text()] method. However, we choose to implement this 
	 * version in-code to reduct network traffic.
	 */
	private static pair<List<String>,List<String>> only_removed_text(
			String raw_diff_text){
		
			// The return sets
		List<String> removed_tokens = new ArrayList<String>();
		List<String> removed_blocks = new ArrayList<String>();
			
			// Get some working-text, remove stupid tags
		String working_text = raw_diff_text;
		working_text = working_text.replaceAll("<div>|</div>", "");

			// Extract table cells relevant to addition/deletion of content
		String regex = "<td class=\"diff-(addedline|deletedline)\">([^<]*" +
				"<(span|del|ins) class=\"(diffchange|diffchange diffchange-inline)\">" +
				"[^<]*</(span|del|ins)>[^<]*|[^<]*)*</td>";
		List<String> changes = stiki_utils.
				all_pattern_matches_within(regex, working_text);
		
			// Initalize variables for cell-processing loop
		String content_cell, deleted_cell="", temp;
		boolean has_deleted = false;
		Iterator<String> iter = changes.iterator();
		String span_regex = "<(span|del|ins) class=\"(diffchange|" +
				"diffchange diffchange-inline)\">[^<]*</(span|del|ins)>";
		
		while(iter.hasNext()){
			content_cell = iter.next();		
			if(has_deleted){
				has_deleted = false;
				temp = deleted_cell.replaceAll("<[^<]*>", "");
				removed_blocks.add(temp);
				if(content_cell.startsWith("<td class=\"diff-addedline\">")){
					Iterator<String> spans;
					spans = stiki_utils.all_pattern_matches_within(
							span_regex, deleted_cell).iterator();
					while(spans.hasNext())
						removed_tokens.add(spans.next().replaceAll(
								"<(span|del|ins) class=\"(diffchange|" +
								"diffchange diffchange-inline)\">|</(span|del|ins)>", ""));
				} else 	// ^ If addition line, just parse out red text
						// \/ If not, all cell text was removed
					removed_tokens.add(temp);					
			} // Need to read-ahead one cell to determine handling
												
			if(content_cell.startsWith("<td class=\"diff-deletedline\">")){
				deleted_cell = content_cell;
				has_deleted = true;
			} // If deleted cell, save content and look ahead
			
		} // Process over all cells containing added/deleted content 
		
		if(has_deleted){ 
			temp = deleted_cell.replaceAll("<[^<]*>", "");
			removed_tokens.add(temp);
			removed_blocks.add(temp);
		} // Edge-condition, if last del-cell in isolation (no look ahead)
			
		return(new pair<List<String>,List<String>>(
				removed_tokens, removed_blocks));
	}
	
}