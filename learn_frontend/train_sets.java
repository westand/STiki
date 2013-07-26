package learn_frontend;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import core_objects.feature_set;

import db_server.db_features;
import db_server.stiki_con_server;

/**
 * Andrew G. West - train_sets.java - Wrapping multiple strategies for
 * generating training sets (of RIDS).
 */
public class train_sets{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * An extremely rough estimate of the number of NS0/Anonymous-User edits
	 * per day on Wikipedia. Useful in sizing historical windows
	 */
	private static final long IP_EDITS_PER_DAY = 36000;
	
	
	// **************************** PUBLIC METHODS ***************************

	/**
	 * Return a set of feature sets, bound between two RIDs.
	 * @param con_server Connection to the STiki database (full privs.)
	 * @param start_rid RID at which training set should begin
	 * @param end_rid RID at which training set should conclude
	 * @return A list of all 'feature_sets' between 'start_rid' and
	 * 'end_rid' for which the STiki DB has data
	 */
	public static List<feature_set> get_seq_set(stiki_con_server con_server, 
			long start_rid, long end_rid) throws Exception{
		
		db_features db_feat = new db_features(con_server);
		List<feature_set> train_set = db_feat.get_features_in_interval(
				start_rid, end_rid);
		db_feat.shutdown();
		return(train_set);
	}
	
	/**
	 * A sequential training-set generator [train_sequential] is most simple,
	 * but insufficient. If the most recent say, 10k edits are used to form a
	 * training set, than it will not span time-of-day and day-of-week metrics 
	 * in use. Increasing the number will produce training sets not 
	 * process-able in a reasonable amount of time.
	 * 
	 * The solution is this class. We examine a larger time-period of edits,
	 * selecting a random subset for inclusion into the training set, whose
	 * size can be fixed a priori.
	 * 
	 * @param con_server Connection to the Presta-DB (fully privileged).
	 * @param last_rid_proc Last RID processed. Upper bound of training set
	 * @param hist_window How many RIDs to consider back into the past
	 * @param set_size Approximation of how large returned set shoudl be
	 * @return List of [feature_set] objects, encoding training examples
	 */
	public static List<feature_set> get_smart_set(stiki_con_server con_server, 
			long last_rid_proc, long hist_window, long set_size)
			throws Exception{
		
			// Get a list of eligible RIDs in interval
		db_features db_feat = new db_features(con_server);
		List<Long> rid_list = db_feat.get_rids_in_interval(
				(last_rid_proc - hist_window), last_rid_proc);
		
			// We calculate the probability that any one potential RID
			// should be included, based on required set size
		double prob = ((set_size * 1.0) / (rid_list.size() * 1.0));
		Random rand = new Random();
		List<feature_set> train_set = new ArrayList<feature_set>();
		
		long rid;
		Iterator<Long> iter = rid_list.iterator();
		while(iter.hasNext()){
			rid = iter.next();
			if(rand.nextDouble() <= prob)
				train_set.add(db_feat.feature_set_by_rid(rid));
		} // Based on probability, build the training set

			// Clean-up and return
		db_feat.shutdown();
		return(train_set);
	}
	
	/**
	 * This method is identical to its longer version. This one, however,
	 * provides reasonable default values for the missing parameters.
	 */
	public static List<feature_set> get_smart_set(stiki_con_server con_server, 
			long last_rid_proc) throws Exception{
		long DEF_WINDOW = (IP_EDITS_PER_DAY * 14);
		long DEF_SIZE = 25000;
		return(get_smart_set(con_server, last_rid_proc, DEF_WINDOW , DEF_SIZE));
	}
		
}
