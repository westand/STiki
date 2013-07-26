package learn_svm;

import java.util.List;

import core_objects.feature_set;
import learn_frontend.learn_interface;

/**
 * Andrew G. West - svm_frontend.java - Access methods to a STiki learning
 * module using a Support Vector Machine (SVM). For interface compliance.
 * 
 * Note that the SVM learning module was designed BEFORE registered user
 * edits were processesd by STiki. The lack of IP address leaves multiple
 * features un-calculate-able. SVM struggles with such "missing features",
 * and this would need to be a topic given much attention should this
 * module ever be put back into active use.
 */
public class svm_frontend implements learn_interface{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Frequency with which model should be retrained (in edits). 
	 */
	private static final int RETRAIN_INTERVAL = 500000;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Retrain the model
	 */
	public void train(List<feature_set> train_set){
			
			// Note: Long execution times (and the ability to still classify
			// while training), motivate the threading of theis process
		new svm_train(train_set);
	}
	
	/**
	 * Overriding: Classify (score) a feature set
	 */
	public double classify(feature_set features) throws Exception{
		return(svm_classify.classify(features));
	}

	/**
	 * Overriding: Retraining frequency of this method (in edits)
	 */
	public int retrain_interval(){
		return(RETRAIN_INTERVAL);
	}
	
}
