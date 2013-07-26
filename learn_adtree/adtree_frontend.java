package learn_adtree;

import java.util.List;

import core_objects.feature_set;
import core_objects.stiki_utils;
import learn_frontend.learn_interface;

/**
 * Andrew G. West - adtree_frontend.java - Access methods to a STiki learning
 * module using an Alternating Decision Tree (ADTree).
 */
public class adtree_frontend implements learn_interface{

	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Frequency with which model should be retrained (in edits). Note that
	 * since training is done off-line for ADTrees (using Weka), a special 
	 * integer code is used here so STiki never initates retraining.
	 */
	private static final int RETRAIN_INTERVAL = -1;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Overriding: Retrain the model
	 */
	public void train(List<feature_set> train_set){
		return; // Retraining is done off-line for ADTrees
	}
	
	/**
	 * Overriding: Classify (score) a feature set. This method first 
	 * sets-up NOMINAL and MISSING fields before passing set to model.
	 */
	public double classify(feature_set feats) throws Exception{
		
			// ADTrees have the ability to handle feature-vectors for which
			// field values are missing.
		boolean TOD_MISSING = (!feats.IS_IP); // No geolocated for reg'd
		boolean DOW_MISSING = (!feats.IS_IP); // No geolocated for reg'd
		boolean TS_LP_MISSING = (feats.TS_R == -1); // If no prev. page edit
		boolean TS_RBU_MISSING = (feats.TS_RBU == -1); // If no RB history
		boolean REP_COUNTRY_MISSING = (!feats.IS_IP || 
				feats.REP_COUNTRY == -1.0); // No IP GEO + GEO failures
		
			// Similarly, ADTrees also support nominal attributes -- so 
			// some feats are converted numerical->nominal (string enums).
		String NOM_IS_IP;
		if(feats.IS_IP) NOM_IS_IP = "true";
		else NOM_IS_IP = "false";
		
		String NOM_DOW;
		if(feats.DOW == 1) NOM_DOW = "sun";
		else if(feats.DOW == 2) NOM_DOW = "mon";
		else if(feats.DOW == 3) NOM_DOW = "tue";
		else if(feats.DOW == 4)	NOM_DOW = "wed";
		else if(feats.DOW == 5) NOM_DOW = "thu";
		else if(feats.DOW == 6) NOM_DOW = "fri";
		else NOM_DOW = "sat"; // if(feats.DOW == 7)
		
			// Humongous call to the model file to score
		double score = (adtree_model.score(NOM_IS_IP, feats.REP_USER, 
				feats.REP_ARTICLE, feats.TOD, TOD_MISSING, NOM_DOW, 
				DOW_MISSING, feats.TS_R, feats.TS_LP, TS_LP_MISSING, 
				feats.TS_RBU, TS_RBU_MISSING, feats.COMM_LENGTH, 
				feats.BYTE_CHANGE, feats.REP_COUNTRY, REP_COUNTRY_MISSING, 
				feats.NLP_DIRTY, feats.NLP_CHAR_REP, 
				feats.NLP_UCASE, feats.NLP_ALPHA));
		
			// Given that ADTree score output is a logistic function, this
			// can be used to normalize/probabilize nicely on [0,1].
		return(stiki_utils.logistic_cdf(score));
	}

	/**
	 * Overriding: Retraining frequency of this method (in edits)
	 */
	public int retrain_interval(){
		return(RETRAIN_INTERVAL);
	}
	
}
