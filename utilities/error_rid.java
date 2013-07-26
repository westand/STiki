package utilities;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import core_objects.stiki_utils;

/**
 * Andrew G. West - error_rid.java - Custom log processor for determining
 * problematic RIDs that might have tripped infinite loop scenarios.
 */
public class error_rid{

	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Report any RIDs who were started; but never complemeted. This
	 * is per a custom log-file format
	 * @param args File pointer to log file
	 */
	public static void main(String[] args) throws Exception{
		
		// Terribly simple. Any RID that does not show up twice in the
		// file is one that was never completed.
		
		BufferedReader in = stiki_utils.create_reader(args[0]);
		Set<String> open_set = new TreeSet<String>();
		String rid, line = in.readLine();
		while(line != null){
			rid = line.split(" ")[0];
			if(open_set.contains(rid))
				open_set.remove(rid);
			else open_set.add(rid);
			line = in.readLine();
		} // iterate over log, line-by-line
		in.close();
		
		System.out.println("Problematic RIDs follow:");
		Iterator<String> iter = open_set.iterator();
		while(iter.hasNext())
			System.out.println(iter.next());
	}

}
