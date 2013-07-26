package learn_adtree;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import core_objects.pair;
import core_objects.stiki_utils;

/**
 * Andrew G. West - adtree_builder.java - A script taking a raw ADTree model
 * (as formatted by Weka) -- and converting it into Java source code
 * that will actually implement that model. 
 */
public class adtree_builder{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * A list of those features for which "missing" flags exist.
	 */
	private static String[] POSSIBLE_MISSING = 
			{"TOD", "DOW", "TS_LP", "TS_RBU", "REP_COUNTRY"};
	
	
	// **************************** PUBLIC METHODS ***************************

	/* EXAMPLE SNIP OF AN INPUT FILE:
 
	: -1.361
	|  (1)IS_IP = true: 0.55
	|  |  (25)COMM_LENGTH < 30.5: 0.044
	|  |  (25)COMM_LENGTH >= 30.5: -0.436
	|  (1)IS_IP = false: -0.852
	|  |  (2)TS_R < 3122287.5: 0.998
	|  |  (2)TS_R >= 3122287.5: -0.704
	|  |  |  (15)COMM_LENGTH < 0.5: 0.796
	|  |  |  |  (24)NLP_ALPHA < 0.807: -0.608
	|  |  |  |  (24)NLP_ALPHA >= 0.807: 0.628
	|  |  |  (15)COMM_LENGTH >= 0.5: -0.06
	|  |  |  |  (16)TS_LP < 108.5: 0.681
	|  |  |  |  (16)TS_LP >= 108.5: -0.468
	|  |  |  |  |  (17)TS_R < 29618288: 0.938
	|  |  |  |  |  (17)TS_R >= 29618288: -1.026
	|  |  |  |  |  |  (19)NLP_DIRTY < 4.5: -1.793
	|  |  |  |  |  |  (19)NLP_DIRTY >= 4.5: 1.273
	|  (3)NLP_ALPHA < 0.001: -1.432
	|  (3)NLP_ALPHA >= 0.001: 0.157
	|  |  (4)REP_ARTICLE < 0.101: -0.258
	|  |  (4)REP_ARTICLE >= 0.101: 0.467 */
	
	
	/**
	 * STDOUT the Java source code implementation of a provided model.
	 * @param args One argument is required (1) File path to input model. 
	 * It is assumed the input is well formatted.
	 */
	public static void main(String[] args) throws Exception{
		
		BufferedReader in = stiki_utils.create_reader(args[0]);
		List<Integer> opened_node_list = new ArrayList<Integer>();
		pair<String,String> content;	// (condition,sum) pair
		int cur_node;					// identifier of current node
		int this_depth = 1;				// level of nesting at cur node
		int prev_depth = -1;			// level of nesting at prev node
		
		String line = in.readLine();
		if(line.charAt(0) != ':') // First line of model sets init value
			throw new Exception("First line invalid");
		else{System.out.println("double value = " + line.substring(2) + ";");}
		
		line = in.readLine();
		while(line != null){ // Now into actual tree processing
			
			this_depth = depth(line);
			if(this_depth <= prev_depth){
				for(int i=prev_depth; i >= this_depth; i--)
					System.out.println(print_n_tabs(i) + "}");	
			} // Handle closure onf any blocks first
			
				// Split node out into (id, condition, sum) components
			content = get_line_parts(line);
			cur_node = get_node_num(line);
			
			if(!opened_node_list.contains(cur_node)){
				System.out.println(print_n_tabs(this_depth) + 
						"if(" + condition_handler(content.fst) + "){");
				System.out.println(print_n_tabs(this_depth+1) + 
						"value += " + content.snd + ";");
				opened_node_list.add(cur_node);
			} else{
				System.out.println(print_n_tabs(this_depth) + 
						"else if(" + condition_handler(content.fst) + "){");
				System.out.println(print_n_tabs(this_depth+1) + 
						"value += " + content.snd + ";");
			} // Create new nodes or complete those "half" done
			
			line = in.readLine();	// Prepare for the next iteration
			prev_depth = this_depth;
		} // iterate over entirety of model file
			
			// Make final closures, and "return"
		for(int i=this_depth; i >= 0; i--)
			System.out.println(print_n_tabs(i) + "}");
		System.out.println("return(value);");
		in.close();	
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Return the "depth" at which some line lies in the ADTree model
	 * @param line Line in the model file for an ADTree
	 * @return Depth at which the node encoded by line "line" resides
	 * in the ADTree. Note that root-level nodes have depth zero (0). 
	 */
	private static int depth(String line){
		return(stiki_utils.num_matches_within("\\|", line) - 1);
	}
	
	/**
	 * Given an ADTree line/node, parse condition and sum.
	 * @param line Line in ADTree model file (i.e., a node).
	 * @return Pair, whose first element is the condition of 'line',
	 * and the second element is the 'sum' value.
	 */
	private static pair<String,String> get_line_parts(String line){
		String fst = line.split("\\)")[1].split(":")[0].trim();
		String snd = line.split("\\)")[1].split(":")[1].trim();
		return(new pair<String,String>(fst,snd));
	}
	
	/**
	 * Create a string having some quantity of tab characters.
	 * @param n Number of tab characters which return value should contain
	 * @return A string, containing 'n' tab (indent) characters
	 */
	private static String print_n_tabs(int n){
		String tabbed_string = "";
		for(int i=1; i<=n; i++)
			tabbed_string += "\t";
		return(tabbed_string);
	}
	
	/**
	 * Get the identifier (numerical) associated with some line/node
	 * @param line Line in ADTree model file (i.e., a node).
	 * @return Integer with which the node at 'line' is labeled
	 */
	private static int get_node_num(String line){
		return(Integer.parseInt(line.split("\\(")[1].split("\\)")[0]));
	}

	/**
	 * Rewrite the condition for Java code. Chiefly, allow for the 
	 * handling of "missing" features, and nominal ones.
	 * @param cond Conditional expression of ADTree, in String form
	 * @return Re-written version of condition, as required by
	 * special conditions for nominals and missing features.
	 */
	private static String condition_handler(String cond){
		
		String rebuilt_cond = "";
		String[] parts = cond.split(" ");
		
		try{Double.parseDouble(parts[2]);
			rebuilt_cond = cond;
		} catch(NumberFormatException e){
			rebuilt_cond = parts[0] + ".equals(\"" + parts[2] + "\")";
		} // Treat nominal features as strings in JAVA

				// Permit a flag to be raised for 'missing' features
		if(has_missing_flag(parts[0]))
			rebuilt_cond = "!" + parts[0] + "_MISSING && " + rebuilt_cond;		
		return(rebuilt_cond);
	}
	
	/**
	 * Given a feature name, determine if it is one that can be "missing"
	 * @param feature Name of a field in the feature set
	 * @return TRUE if 'feature' can take on a missing value; else, FALSE.
	 */
	private static boolean has_missing_flag(String feature){
		for(int i=0; i < POSSIBLE_MISSING.length; i++){
			if(POSSIBLE_MISSING[i].equals(feature))
				return(true);
		} // Just iterate over eligible fields vector
		return(false);
	}

}
