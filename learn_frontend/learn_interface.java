package learn_frontend;

import java.util.List;

import core_objects.feature_set;

/**
 * Andrew G. West - learn_interface.java - A simple interface which wraps
 * a learning "module", for example "SVM", or "ADTree".
 * 
 * Intuitively this object wraps both training and classification components.
 */
public interface learn_interface{
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Retrain the model (if applicable).
	 * @param training_set List of features over which to train.
	 */
	public void train(List<feature_set> training_set) throws Exception;
	
	/**
	 * Classify a feature set, returning a real valued score speaking to
	 * the probability that the associated set is vandalism.
	 * @param features The features associated with some edit
	 * @return A real valued score speaking the probability that the edit
	 * associated with 'features' is vandalism. High scores should be
	 * indicative of vandalism; low scores less so. These values only 
	 * need be relatively (not absolutely) interpretable.
	 */
	public double classify(feature_set features) throws Exception;
	
	/**
	 * How often the learning model should be re-trained (in edits)
	 * @return An integer; "the model should be retrained every [x] edits". 
	 * Note that if this value is negative one (-1), this is an indication
	 * the model should NEVER be (re)trained.
	 */
	public int retrain_interval();

}
