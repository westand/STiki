package learn_svm;

import core_objects.feature_set;

/**
 * Andrew G. West - svm_normalizer.java - Though features are most 
 * intuitive in their raw form (e.g., COMM_LENGTH = 42 chars.), features need 
 * to be altered in order to be useful for SVM learning. Namely, features must 
 * be, (1) normalized onto [0,1], and (2) have polarity such that good behavior
 * always tends towards the origin. This class performs those tasks for a
 * feature set, outputting a descriptor useful to the [svm_learn] 
 * and [svm_classify] processes.
 */
public class svm_normalizer{
	
	// **************************** PRIVATE FIELDS ***************************
	
		// We forego and at length description of each of the fields below.
		// Intuitively, in order to put features on [0,1] we must know the
		// maximum value each can attain. This cannot be strongly bounded,
		// but here we record the maximum value for each feature observed
		// over all Wikipedia edits prior to 2009/11/03. We note that
		// features normalized slightly > 1.0 is not of terrible consequence.
	private static double MAX_REP_USER = 1709.61;
	private static double MAX_REP_ARTICLE = 69.18;
	private static double MAX_TOD = 24.0;
	private static double MAX_DOW = 7.0;
	private static double MAX_TS_R = 277740616.0;
	private static double MAX_TS_LP = 242347117.0;
	private static double MAX_TS_RBU = 181347999.0;
	private static double MAX_COMM_LENGTH = 750.0;
	
		// The following features were added after the authorship of the
		// EUROSEC'10 paper, and their impact and normalization is not
		// nearly as rigorous as those listed above.
	private static double MAX_BYTE_CHANGE = 1000.0;
	
		// The following are 'integer caps' -- which define the saturation of
		// a feature. (i.e., if n=100, and an edit has 105 bad words, it scores
		// as if it had 100). This permits integer-feature normalizaton.
	private static int CAP_DIRTY = 25;
	private static int CAP_CHAR_REP = 10;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Given a feature set, normalize and correctly adjust-polarity of all
	 * features, and output them in a line suitable for SVM use.
	 * @param features Feature set whose normalization is desired
	 * @return String containing normalize features, in SVM format
	 */
	public static String normalize_and_output(feature_set feats){
		
		String line;
		if(feats.LABEL)
			line = "+1";
		else line =  "-1";
		
		line += 
		" 1:"  + normalize_boolean(feats.IS_IP) +
		" 2:"  + normalize_feature(feats.REP_USER, MAX_REP_USER, false) + 
		" 3:"  + normalize_feature(feats.REP_ARTICLE, MAX_REP_ARTICLE, false) +  
		" 4:"  + normalize_feature(feats.TOD, MAX_TOD, false) + 
		" 5:"  + normalize_feature(feats.DOW, MAX_DOW, false) + 
		" 6:"  + normalize_feature(feats.TS_R, MAX_TS_R, true) + 
		" 7:"  + normalize_feature(feats.TS_LP, MAX_TS_LP, true) + 
		" 8:"  + normalize_feature(feats.TS_RBU, MAX_TS_RBU, true) + 
		" 9:"  + normalize_feature(feats.COMM_LENGTH, MAX_COMM_LENGTH, true) + 
		" 10:"  + normalize_about_zero(feats.BYTE_CHANGE, MAX_BYTE_CHANGE) +
		" 11:" + feats.REP_COUNTRY + // Calculation guarantees [0,1]
		" 12:" + normalize_int_cap(feats.NLP_DIRTY, CAP_DIRTY) + 
		" 13:" + normalize_int_cap(feats.NLP_CHAR_REP, CAP_CHAR_REP) + 
		" 14:" + feats.NLP_UCASE + // Calculation guarantees [0,1]
		" 15:" + feats.NLP_ALPHA + // Calculation guarantees [0,1]
		" # "  + feats.R_ID;
			
		return (line);
	}
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Normalize a feature, and if necessary, flip its polarity.
	 * @param value Feature value to be normalized
	 * @param normalizer Normalizer, the greatest value observed for 'value'
	 * @param flip If TRUE, flip the polarity of the feature. If FALSE, do not
	 * flip the polarity of the feature.
	 * @return Normalized, and possible 'flipped', value of 'feature'. If
	 * (value == -1), always return 0 (zero) as output.
	 */
	private static double normalize_feature(double value, 
			double normalizer, boolean flip){
		
		if(value == -1)
			return 0; // Unknown data tends towards innocence (origin)
		else if(!flip)
			return(value/normalizer);
		else // (flip)
			return((normalizer-value)/normalizer);
	}
	
	/**
	 * Normalize a feature whose value is sign-variable. Normalization assumes
	 * that feature values on [-normal,normal] are possible, and maps these
	 * to [0,1], respectively. Thus a feature value of zero maps to 0.5
	 * @param value Feature value which is being normalized (may be neg.)
	 * @param normal Normalizer (positive), which when made negative, is
	 * also applicable for negative values.
	 * @return Normalized value of 'value', per method description
	 */
	private static double normalize_about_zero(double value, double normal){
		double norm_val = 0.5 + ((value / normal) / 2.0);
		return(norm_val);
	}
	
	/**
	 * Normalize an integer-feature, using a cap-and-divide strategy.
	 * @param value Feature value which is being normalized
	 * @param cap Integer at which 'value' should be interpreted as saturated
	 * @return If 'value' is < 'cap', then (value/cap) is returned. 
	 * Else, 1.0 will be returned.
	 */
	private static double normalize_int_cap(int value, int cap){
		if(value < cap)
			return((value * 1.0) / (cap * 1.0));
		else return(1.0);
	}
	
	/**
	 * Return a boolean as a double value.
	 * @param val Boolean being converted
	 * @return 1.0 if (val=TRUE), Return 0.0; otherwise
	 */
	private static double normalize_boolean(boolean val){
		if(val)
			return(1.0);
		else return (0.0);
	}
	
}
