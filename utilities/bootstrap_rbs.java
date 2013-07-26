package utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import core_objects.metadata;
import db_server.db_country;
import db_server.db_edits;
import db_server.db_features;
import db_server.db_geolocation;
import db_server.db_hyperlinks;
import db_server.db_off_edits;
import db_server.stiki_con_server;
import edit_processing.rollback_handler;

import mediawiki_api.api_retrieve;

/**
 * Andrew G. West - rb_bootstrap.java - Rollbacks are capable of contributing
 * statistical significance to reputation for about 6 months after they occur.
 * Thus, our back-end processing requires this interval in order to 'warm-up',
 * 
 * Rather than waiting, we instead back-process rollbacks using this script --
 * which would also be useful if our back-end experiences any downtime.
 */
public class bootstrap_rbs{
	
		// Bootstrapping history -- a log of this method's usage:
		// 
		// BEG_RID: 326831891 (2009-11-20T00:00:00Z)
		// END_RID: 346194229 (and then the backend was started)
	
	
	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Queries sent to the MediaWiki API should return, at most, this number
	 * of results (corresponds to Wiki-imposed limit). 
	 */
	public static final int BATCH_SIZE = 100;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Driver method. Locate and store historical rollbacks/offending-edits.
	 * @param args Two arguments are required by this method. (1) the starting
	 * RID and (2) the ending RID which bounds this rollback search. RIDs
	 * in the interval (in namespace 0) will all be investigated.
	 */
	public static void main(String args[]) throws Exception{
		
			// Parse the command line (bounding) arguments
		long RID_BEG = Long.parseLong(args[0]);
		long RID_END = Long.parseLong(args[1]);
		long current_rid = RID_BEG;
		
			// DB handler for storing OEs, and handlers for triggers.
		stiki_con_server con_server = new stiki_con_server();
		db_off_edits db_oe = new db_off_edits(con_server);
		db_geolocation db_geo = new db_geolocation(con_server);
		db_edits db_edits = new db_edits(con_server);
		db_features db_feat = new db_features(con_server);
		db_country db_country = new db_country(con_server);
		db_hyperlinks db_links = new db_hyperlinks(con_server, null); // TODO
		
		List<Long> rid_list = new ArrayList<Long>(BATCH_SIZE);
		while(current_rid <= RID_END){
			rid_list.add(current_rid);
			if(rid_list.size() == BATCH_SIZE){
				process_rid_list_for_rbs(rid_list, db_oe, db_geo, 
						db_edits, db_feat, db_country, db_links);
				rid_list.clear();
			} // If max size reached, process and then clear list
			current_rid++;
		} // Sequentially add RIDs in the interval
		
		if(rid_list.size() > 0) // Process RIDs that didn't fill buffer
			process_rid_list_for_rbs(rid_list, db_oe, db_geo, db_edits, 
					db_feat, db_country, db_links);
		
			// Shut everything down neatly
		db_oe.shutdown();
		db_geo.shutdown();
		db_edits.shutdown();
		db_feat.shutdown();
		db_country.shutdown();
		db_links.shutdown();
		con_server.con.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Process a list of RIDs for rollbacks. For each RID, fetch the metadata
	 * from the MediaWiki API and for those in NS0, give to RB-handler.
	 * @param rid_list List containing revision-IDs (RIDs)
	 * @prarm db_oe Database-handler for the [rollbacks] table
	 * @param db_geo DB-handler for mapping IP address to geographic locales
	 * @param db_edits DB-handler for trigger into the [all_edits] table
	 * @param db_feat DB-handler for trigger into the [features] table
	 * @param db_country DB-handler for trigger into the [country] table
	 * @param db_links DB-handler for trigger into the [hyperlinks] table
	 */
	private static void process_rid_list_for_rbs(List<Long> rid_list, 
			db_off_edits db_oe, db_geolocation db_geo, db_edits db_edits, 
			db_features db_feat, db_country db_country, db_hyperlinks db_links)
			throws Exception{
			
			// Retrieve the metadata for all RIDs
		List<metadata> md_set;
		md_set = api_retrieve.process_multiple_rids(rid_list, db_geo);
		Iterator<metadata> iter = md_set.iterator();
		
		metadata cur_md;
		while(iter.hasNext()){
			cur_md = iter.next();
			if(cur_md.namespace == 0)
				rollback_handler.new_edit(cur_md, db_oe, db_geo, db_edits, 
						db_feat, db_country, db_links);
		} // Then submit this data to rollback inspection
					
	}
}
