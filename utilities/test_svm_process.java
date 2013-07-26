package utilities;

import learn_frontend.learn_interface;
import learn_frontend.train_sets;
import learn_svm.svm_frontend;

import core_objects.feature_set;
import db_server.db_features;
import db_server.stiki_con_server;

/**
 * Andrew G. West - test_svm_process.java - A simple class that provides
 * a hook into the [train_svm] and [feature_classify] classes so that SVM
 * testing can be done offline, without affecting ongoing back-end processing.
 */
public class test_svm_process{
	
	// **************************** PUBLIC METHODS ***************************
	
	// CASUAL NOTES TAKEN DURING EXPERIMENTATION PROCESS
	//
	// RIDs 346204251 thru 346214251, a range of 10k edits, has 1804 edits
	// eligible to be trained over (in NS0, made by anonymous user). 
	
	/**
	 * Driver method. Train and classify over some RIDs.
	 * @args Three arguments are required, both to be parsed as type 'long';
	 * the (1) start and (2) end RIDs for the training set, as well as the
	 * (3) number of edits to classify, beginning sequentially at (end-RID+1).
	 */
	public static void main(String[] args) throws Exception{
		
		@SuppressWarnings("unused")
		long rid_train_start = Long.parseLong(args[0]);
		long rid_train_end = Long.parseLong(args[1]);
		
			// Train the classifier
		stiki_con_server con_server = new stiki_con_server();
		learn_interface svm_module = new svm_frontend();
		svm_module.train(train_sets.get_smart_set(con_server, rid_train_end));
		
		System.out.println("Training phase complete!");
		
		long start_rid = (rid_train_end + 1);
		long last_classify_rid = (start_rid + Long.parseLong(args[2]));
		
		db_features db_feat = new db_features(con_server);
		feature_set feats; 	// Feature set associated with 'cur_rid'
		double score; 		// Classification score associated with 'feats'
		
		for(long cur_rid=start_rid; cur_rid <= last_classify_rid; cur_rid++){
			feats = db_feat.feature_set_by_rid(cur_rid);
			if(feats == null)
				continue;
			score = svm_module.classify(feats);
			System.out.println("rid: " + cur_rid + " - score: " + score);
		} // Score the requested interval of edits
	
			// Shut everthing down cleanly
		db_feat.shutdown();
		con_server.con.close();
	}

}
