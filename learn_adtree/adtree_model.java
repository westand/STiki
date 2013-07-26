package learn_adtree;

/**
 * Andrew G. West - adtree_mdodel.java - Given the feature set of an edit,
 * this class facilitates the comparison of these features against a 
 * previously calculated machine-learning model, producing a classification.
 * 
 * More specifically, models are built off-line using using machine-learning
 * software called "Weka".  Weka outputs a model. We then use 
 * [adtree_builder.java] to covert that model to Java source -- which
 * we copy into this file.
 * 
 * The model provided herein was trained over the vandalism corpus of Potthast.
 */
public class adtree_model{

	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * A horrifically long list of (self-explanatory) parameters go in. Note
	 * that if a "MISSING" parameter is true -- it does not matter the
	 * value passed to the associated field.
	 * @return Model score for feature-vector passed
	 */
	public static double score(String IS_IP, double REP_USER, 
			double REP_ARTICLE, float TOD, boolean TOD_MISSING, String DOW, 
			boolean DOW_MISSING, long TS_R, long TS_LP, boolean TS_LP_MISSING,
			long TS_RBU, boolean TS_RBU_MISSING, int COMM_LENGTH, 
			int BYTE_CHANGE, double REP_COUNTRY, boolean REP_COUNTRY_MISSING,
			int NLP_DIRTY, int NLP_CHAR_REP, double NLP_UCASE, 
			double NLP_ALPHA){
		
		double value = -1.262;
		if(IS_IP.equals("true")){
			value += 0.533;
		} else if(IS_IP.equals("false")){
			value += -0.839;
			if(TS_R < 257116.5){
				value += 1.168;
			} else if(TS_R >= 257116.5){
				value += -0.537;
				if(TS_R < 62842110){
					value += 0.457;
				} else if(TS_R >= 62842110){
					value += -0.364;
					if(!TS_LP_MISSING && TS_LP < 187.5){
						value += 0.531;
					} else if(!TS_LP_MISSING && TS_LP >= 187.5){
						value += -0.231;
						if(COMM_LENGTH < 0.5){
							value += 0.759;
							if(NLP_ALPHA < 0.808){
								value += -1.942;
							} else if(NLP_ALPHA >= 0.808){
								value += 0.602;
							}
						} else if(COMM_LENGTH >= 0.5){
							value += -0.326;
							if(!TS_LP_MISSING && TS_LP < 170610){
								value += 0.341;
							} else if(!TS_LP_MISSING && TS_LP >= 170610){
								value += -2.243;
							}
						}
					}
				}
			}
		}
		if(NLP_ALPHA < 0.724){
			value += -0.478;
		} else if(NLP_ALPHA >= 0.724){
			value += 0.257;
			if(NLP_UCASE < 0.011){
				value += 0.352;
			} else if(NLP_UCASE >= 0.011){
				value += -0.13;
				if(NLP_ALPHA < 0.781){
					value += -0.238;
				} else if(NLP_ALPHA >= 0.781){
					value += 0.15;
				}
			}
			if(COMM_LENGTH < 18.5){
				value += 0.146;
			} else if(COMM_LENGTH >= 18.5){
				value += -0.217;
			}
		}
		if(REP_ARTICLE < 0.008){
			value += -0.262;
			if(NLP_UCASE < 0.043){
				value += 0.208;
			} else if(NLP_UCASE >= 0.043){
				value += -0.13;
			}
		} else if(REP_ARTICLE >= 0.008){
			value += 0.339;
		}
		if(NLP_DIRTY < 4.5){
			value += -0.055;
			if(BYTE_CHANGE < -736){
				value += 1.081;
			} else if(BYTE_CHANGE >= -736){
				value += -0.051;
				if(BYTE_CHANGE < 2.5){
					value += -0.32;
					if(NLP_CHAR_REP < 1.5){
						value += -0.244;
					} else if(NLP_CHAR_REP >= 1.5){
						value += 0.28;
					}
				} else if(BYTE_CHANGE >= 2.5){
					value += 0.163;
				}
			}
			if(NLP_CHAR_REP < 3.5){
				value += -0.068;
			} else if(NLP_CHAR_REP >= 3.5){
				value += 0.863;
			}
		} else if(NLP_DIRTY >= 4.5){
			value += 1.151;
		}
		if(NLP_UCASE < 0.373){
			value += -0.051;
			if(!TS_LP_MISSING && TS_LP < 13221){
				value += -0.178;
			} else if(!TS_LP_MISSING && TS_LP >= 13221){
				value += 0.052;
			}
		} else if(NLP_UCASE >= 0.373){
			value += 0.778;
		}
		if(!REP_COUNTRY_MISSING && REP_COUNTRY < 0.068){
			value += -0.628;
		} else if(!REP_COUNTRY_MISSING && REP_COUNTRY >= 0.068){
			value += 0.056;
			if(TS_R < 591){
				value += 0.151;
			} else if(TS_R >= 591){
				value += -0.214;
			}
		}
		if(BYTE_CHANGE < 847.5){
			value += 0.029;
		} else if(BYTE_CHANGE >= 847.5){
			value += -1.384;
		}
		if(!TS_RBU_MISSING && TS_RBU < 741.5){
			value += 0.866;
		} else if(!TS_RBU_MISSING && TS_RBU >= 741.5){
			value += 0.066;
			if(!TOD_MISSING && TOD < 15.309){
				value += 0.462;
			} else if(!TOD_MISSING && TOD >= 15.309){
				value += -0.054;
			}
		}
		return(value);
	}

}