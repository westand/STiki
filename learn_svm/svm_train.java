package learn_svm;

import java.io.BufferedWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import core_objects.feature_set;
import core_objects.stiki_utils;

/**
 * Andrew G. West - train_svm.java - Given a training set, this class
 * contains the public method necessary to perform retraining, with 
 * the support of BASH shell script [svm_learn.sh]. 
 * 
 * In particular, this class/method is not ran in a separate thread. Care
 * should be taken that the caller has its own thread outside of the main
 * RID-queue-pop thread.
 */
public class svm_train implements Runnable{

	// **************************** PRIVATE FIELDS ***************************

	/**
	 * Training set being used for this training run.
	 */
	private List<feature_set> train_set;
	
	/**
	 * Absolute folder-path where this class resides on file-system. 
	 * All helper files/scripts are internal to this folder.
	 */
	private final static String BASE_DIR = abs_folder_of_this_class();
	
	/**
	 * File path where the training set should be written. 
	 */
	private static final String TRAIN_FILE = BASE_DIR + "train_set.txt";
	
	/**
	 * File path for the shell-script that initiates training.
	 */
	private static final String TRAIN_SCRIPT = BASE_DIR + "svm_learn.sh";

	
	// **************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [svm_train] object, and do training.
	 * @param train_set Features forming the basis of the training set
	 */
	public svm_train(List<feature_set> train_set){
		this.train_set = train_set;
		Thread thread = new Thread(this, new Random().toString());
		thread.start();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Given a set of training edits, train an SVM model over them (as
	 * handled by a BASH shell script on the filesystem).
	 */
	public void run(){
		
		try{	
				// Write normalized feature-sets (training examples) to file
			BufferedWriter out = stiki_utils.create_writer(TRAIN_FILE, false);			
			Iterator<feature_set> iter = train_set.iterator();
			while(iter.hasNext()){
				out.write(svm_normalizer.normalize_and_output(iter.next()));
				out.write("\n");
			} // Write all edit-feature-data (normalized) to training file
			out.flush();
			out.close();
			
				// Run shell script to train, wait for completion
			Runtime rtime = Runtime.getRuntime();
			Process child = rtime.exec("/bin/sh " + TRAIN_SCRIPT);
			child.waitFor();
			
		} catch(Exception e){}; // Assume success, presumably the shell scripts
								// won't overwrite old model if attempt fails
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Return the absolute file-folder of this class on the local file-system.
	 * @return Absolute folder-path of this class
	 */
	public static String abs_folder_of_this_class(){
		
			// This feels very hacky: Open up a pointer to this class, then
			// get the location of this class, chop off file portion
		URL url = svm_train.class.getResource("svm_train.java");
		String folder = url.getPath();
		return(folder.replace("svm_train.java", ""));
	}
			
}
