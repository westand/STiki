package learn_svm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URL;

import core_objects.feature_set;
import core_objects.stiki_utils;

/**
 * Andrew G. West - svm_classify.java - Given the feature set of an edit,
 * this class facilitates the comparison of these features against a 
 * previously calculated machine-learning model, producing a classification.
 */
public class svm_classify{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Absolute folder-path where this class resides on file-system. 
	 * All helper files/scripts are internal to this folder.
	 */
	private final static String BASE_DIR = abs_folder_of_this_class();
	
	/**
	 * File-path where feature lines in need of classification should be 
	 * written. This exact filename must be in [classify_example.sh]
	 */
	private final static String OUT_CLASS = BASE_DIR + "class_queue.txt";
	
	/**
	 * File-path where feature lines that have been classified are written 
	 * (i.e., the results). This must correspond with [classify_example.sh].
	 */
	private final static String IN_RESULT = BASE_DIR + "class_result.txt";
	
	/**
	 * File path for the shell-script that initiates classification.
	 */
	private static final String CLASS_SCRIPT = BASE_DIR + "svm_classify.sh";
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Given a feature set, produce a class. score against the current model.
	 * Synchronization prevents file over-writes from concurrency. 
	 * @param features Feature-set to be classified
	 * @return Classification score assigned to `features'
	 */
	public static synchronized double classify(feature_set feats)
			throws Exception{
		
			// Write the feature set to a text file on local filesystem
		BufferedWriter out = stiki_utils.create_writer(OUT_CLASS, false);
		out.write(svm_normalizer.normalize_and_output(feats));
		out.write("\n"); // THIS LINE IS CRITICAL, for some reason
		out.flush();
		out.close();
		
			// Run shell script to classify, wait for completion
		Runtime rtime = Runtime.getRuntime();
		Process child = rtime.exec("/bin/sh " + CLASS_SCRIPT);
		child.waitFor();
		child.destroy(); // Unclear, but appears this frees resources, and not
						 // having may provide "too many files open" error.
		
			// Read classification result
		BufferedReader in = stiki_utils.create_reader(IN_RESULT);
		String result_line = in.readLine(); // Assume just one result
		in.close();
		
			// Parse out the predicted label, and return
		String[] line_parts = result_line.split(" ");
		return (Double.parseDouble(line_parts[0]));
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Return the absolute file-folder of this class on the local file-system.
	 * @return Absolute folder-path of this class
	 */
	public static String abs_folder_of_this_class(){
		
			// This feels very hacky: Open up a pointer to this class, then
			// get the location of this class, chop off file portion
		URL url = svm_classify.class.getResource("svm_classify.java");
		String folder = url.getPath();
		return(folder.replace("svm_classify.java", ""));
	}
	
}
