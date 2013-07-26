package audit_tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import core_objects.stiki_utils;

/**
 * Andrew G. West - audit.java - This is the driver class for producing
 * audit reports for a user-provided set of IP addresses.
 */
public class audit{
	
	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * Regular expression describing proper IP-address format
	 */
	public final static String IP_REGEX = 
			"\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+";
	
	
	// *************************** PROTECTED FIELDS **************************
	
	// ********* COMMAND-LINE *********
	
	/**
	 * This is a connection string, such that [CON_STRING + "api.php"] is
	 * the path to the Mediawiki API of the wiki whose contributions are
	 * being analyzed. The default is English Wikipedia, but this should
	 * be accessible via a command-line parameter.
	 */
	protected String API_PATH = "en.wikipedia.org/w/";
	
	/**
	 * A date variable, such that only events occuring on the analysis wiki
	 * on-or-after this date will be eligible for output/reporting. The
	 * format of the data is the Mediawiki standard "YYYYMMDD"
	 */
	protected String LIMIT_TIME = "19710101";

	/**
	 * Output path to which report will be written. Should be an HTML file.
	 * Default will be "index.html" in the root project directory. 
	 */
	protected String OUT_PATH = "index.html";
	
	/**
	 * A list of IP addresses ranges that will be included in wiki analysis.
	 * This variable is built from data provided at the command-line.
	 */
	protected List<ip_range> IP_RANGES;
	
	
	// ******** GENERATED VALUES ******
	
	/**
	 * Number of IP addresses (user provided) being processed by this instance.
	 */
	protected long NUM_IPS;
	
	/**
	 * The "LIMIT_TIME" variable expressed as a UNIX timestamp
	 */
	protected long LIMIT_TIME_UNIX;
	
	/**
	 * This is the URL path prefix for all wiki URLs. This is related to 
	 * [API_PATH]. However, some Mediawiki wikis store the API and content
	 * on slightly altered paths; thus this variable, which we try
	 * to arrive at algorithmically instead of on the command-line.
	 */
	protected String LINK_PREFIX;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Produce a report of unregistered activity from some IP range 
	 * that occured on some wiki with the Mediawiki API. Input arguments
	 * are described below. Output will be a *.HTML file in whatever
	 * directory from which this program was run.
	 * 
	 * @param args Two optional, and one required argument are taken:
	 * 
	 * REQUIRED ARGUMENT
	 *   The program requires a list of IP addresses for analysis. The format 
	 *   of this list should be "IP1,IP2,IP3..." (minus quotes, no spaces).
	 *   These "IP" fields should take one of three forms:
	 *     1. A single IP address, e.g., "127.0.0.0"
	 *     2. A hyphenated IP range, e..g., "127.0.0.0-127.255.255.255"
	 *     3. A CIDR IP range, e.g., "127.0.0.0/8"
	 *   and should be provided using one of two flags:
	 *     1. (-ipc) The IP CSV is provided directly at the command-line
	 *     2. (-ipf) The IP CSV is in a plain-text file, and this argument
	 *               provides the path to that text file.
	 *   - Do not overlap ranges or include an IP address more than once.
	 *   - Behavior is undefined if this field is not well-formed.
	 * 
	 * OPTIONAL ARGUMENTS
	 *   (-a) A connection string to the wiki over which analysis should
	 *        be performed. If "api.php" is appended to this String, it
	 *        should be the API of that wiki. If this argument is not provided
	 *        "en.wikipedia.org/w/" will be used (English Wikipedia).
	 *   
	 *   (-t) Time variable so that only wiki events occuring on-or-after
	 *        the provided date will be included in reporting. Format should
	 *        be "YYYYMMDD". If this argument is not provided, the
	 *        program will default to the UNIX epoch (i.e., "19700101").
	 *        Realize that many wikis operate in UTC locale.
	 *        
	 *   (-o) Path for output file. This path should have a *.html extension.
	 *        If not provided, the default will be "index.html" in the 
	 *        project directory.
	 *        
	 * EXAMPLE USAGES:
	 *  > java audit_tool.audit -ipc 71.250.134.0/12
	 *  
	 *  > java audit_tool.audit -c en.wikinews.org/w/ -t 20100101 
	 *    -ipc 128.91.0.0/16,130.91.0.0/16,158.130.0.0-158.130.255.255
	 *   
	 */
	public static void main(String[] args) throws Exception{
		audit our_audit = new audit();
		our_audit.parse_arguments(args);
		our_audit.run();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Main execution loop of the program. The [parse_arguments()] method
	 * must be run prior to this execution.
	 */
	public void run() throws Exception{
		output_arguments();
		BufferedWriter out = stiki_utils.create_writer(OUT_PATH, false); 
		if(out == null)
			System.exit(1);
		
			// Hacky way to modify what is normally a static connection
			// String (in the context of larger STiki use).
		String api_query_str = API_PATH;
		if(!API_PATH.startsWith("http://"))
			api_query_str = "http://" + api_query_str;
		if(!API_PATH.endsWith("/"))
			api_query_str += "/";
		API_PATH = api_query_str;
		LINK_PREFIX = api_query_str;
		if(LINK_PREFIX.endsWith("/w/")) // MW wikis have diff. API/content paths 
			LINK_PREFIX = LINK_PREFIX.replace("/w/", "/wiki/");
		api_query_str += "api.php?action=query";
		mediawiki_api.api_retrieve.BASE_URL_OVERRIDE = api_query_str;
		
			// Here the report is generated
		List<user_history> HIST = new query_wiki(this).create_history();
		new write_html(this, HIST, out).write();
		
			// Tear-down
		out.close();		
		concluding_output();
	}
	
	/**
	 * Parse the command-line arguments provided to the main() method. 
	 * Arguments are parsed to private variables of this class.
	 * @param args See the arguments of the main() method
	 */
	public void parse_arguments(String[] args){
		
		if(args.length % 2 == 1){
			System.out.print("\nInvalid # of arguments. Aborting.\n\n");
			System.exit(1);
		} // Every flag must have argument, and vice-versa
		
		boolean req_set = false;
		for(int i=1; i < args.length; i+=2){
			if(args[i-1].equalsIgnoreCase("-ipc")){
				req_set = true;
				IP_RANGES = parse_ips_cl(args[i]);
			} else if(args[i-1].equalsIgnoreCase("-ipf")){
				req_set = true;
				IP_RANGES = parse_ips_file(args[i]);
			} else if(args[i-1].equalsIgnoreCase("-c")){
				API_PATH = args[i];
			} else if(args[i-1].equalsIgnoreCase("-t")){
				if(!args[i].matches("\\d{8}?")){
					System.out.println("\nTime limitation argument does not " +
							"appear valid. Please check documentation. " +
							"Aborting.\n\n");
					System.exit(1);
				} // Check for 8 numbers if this arg is provided
				LIMIT_TIME = args[i];
			} else if(args[i-1].equalsIgnoreCase("-o")){	
				OUT_PATH = args[i];
			} else{
				System.out.print("\nInvalid argument(s). Aborting.\n\n");
				System.exit(1);
			} // Catch any unsupported arguments		
		} // Parse all arguments
		
		if(!req_set){
			System.out.print("\nRequired argument(s) not provided. " +
					"Please see README documentation. " +
					"Aborting.\n\n");
			System.exit(1);
		} // Check that required argument was provided
		
		if(IP_RANGES == null || IP_RANGES.size() == 0){
			System.out.print("\nRequired IP list argument was provided. " +
					"However, there was an error in parsing these IP " +
					"addresses. Please check file path and/or format. " +
					"Aborting.\n\n");
			System.exit(1);
		} // If parsing of IP ranges failed in any way; hard fail
		
			// With everything sane; we can do some final aggregation
		NUM_IPS = this.total_ips();
		LIMIT_TIME_UNIX = stiki_utils.arg_unix_time(
				Integer.parseInt(LIMIT_TIME.substring(0, 4)), 
				Integer.parseInt(LIMIT_TIME.substring(4, 6)), 
				Integer.parseInt(LIMIT_TIME.substring(6, 8)), 0, 0, 0);
		
	}
	
	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Parse a list of IP addresses from a plain-text file.
	 * @param file Path to a file which contains, on a single (first) line,
	 * a CSV enumerating the IP addresses to be analyzed. See the arguments
	 * to the main() method for formatting documentation.
	 * @return List of IP ranges enumerated in file. A zero-length or NULL
	 * list will be returned if there were parsing errors
	 */
	private List<ip_range> parse_ips_file(String file){
		try{  // Just pass off to String parse/handler
			BufferedReader in = stiki_utils.create_reader(file);
			String ip_line = in.readLine();
			in.close();
			if(ip_line == null)
				return(null);
			else return(parse_ips_cl(ip_line));
		} catch(Exception e){ return(null); }
	}
	
	/**
	 * Parse a list of IP addresses from a String.
	 * @param ips String, which is a CSV enumerating the IP addresses to be 
	 * analyzed. See the arguments to the main() method for formatting
	 * @return  List of IP ranges enumerated in file. A zero-length or NULL
	 * list will be returned if there were parsing errors
	 */
	private List<ip_range> parse_ips_cl(String ips){
		
		String[] elements = ips.split(",");
		IP_RANGES = new ArrayList<ip_range>(elements.length);
		
		for(int i=0; i < elements.length; i++){
			
			if(elements[i].contains("-")){ // Hyphenated-range 
				String ip1 = elements[i].split("-")[0];
				String ip2 = elements[i].split("-")[1];
				if(!ip1.matches(IP_REGEX)){
					System.out.println("\nError: Expected IP address " + 
							ip1 + " does not appear to be in valid format\n\n");
					return(null);
				} // Basic format checking on start IP
				if(!ip2.matches(IP_REGEX)){
					System.out.println("\nError: Expected IP address " + 
							ip1 + " does not appear to be in valid format\n\n");
					return(null);
				} // Basic format checking on end IP
				if(stiki_utils.ip_to_long(ip2) < stiki_utils.ip_to_long(ip1)){
					System.out.println("\nError: Hyphenated range " +
							elements[i] + " was provided but start address " +
							"is higher than end address\n\n");			
					return(null);
				} // Check hyphenation ordering 
				IP_RANGES.add(new ip_range(ip1,ip2));
				
			} else if(elements[i].contains("/")){ // CIDR
				
				String ip1 = elements[i].split("/")[0];
				int cidr = Integer.parseInt(elements[i].split("/")[1]);
				if(!ip1.matches(IP_REGEX)){
					System.out.println("\nError: Expected IP address " + 
							ip1 + " does not appear to be in valid format\n\n");
					return(null);
				} // Basic format checking on start IP
				if(cidr < 0 || cidr > 32){
					System.out.println("\nError: The CIDR exponent provided: " + 
							elements[i] + " is not valid\n\n");
					return(null);
				} // Sanity check the CIDR exponent
				long ip2 = stiki_utils.ip_to_long(ip1) + 
						stiki_utils.power_of_2(cidr) - 1;
				IP_RANGES.add(new ip_range(ip1, stiki_utils.ip_to_string(ip2)));
				
			} else{ // Single IP address
				String ip = elements[i];
				if(!ip.matches(IP_REGEX)){
					System.out.println("\nError: Expected IP address " + 
							ip + " does not appear to be in valid format\n\n");
					return(null);
				} // Check formatting
				IP_RANGES.add(new ip_range(ip, ip));
			} // Determine input type, parse
		} // Map input format onto Java one (IP range lists)

		Collections.sort(IP_RANGES); // Just to beautify output
		return(IP_RANGES);
	}
	
	/**
	 * Print input arguments to STDOUT as a means of verifying parsing
	 */
	private void output_arguments(){
		System.out.println("\n" + "================== " +
				"WELCOME TO WIKI-AUDIT ==================\n");
		System.out.println("Input arguments for verification:");
		System.out.println("Conn. string:   " + API_PATH);
		System.out.println("Time bound:     " + LIMIT_TIME);
		System.out.println("Output file:    " + OUT_PATH);
		System.out.println("IP ranges (one per line):");
		for(int i=0; i < IP_RANGES.size(); i++)
			System.out.println("  * " + IP_RANGES.get(i).toString());
		System.out.println("Total # of IPs: " + NUM_IPS + "\n");
		System.out.println("============================" +
				"===============================\n");
	}
	
	/**
	 * Final message to be executed upon completion
	 */
	private void concluding_output(){
		System.out.println("\n============================" +
				"===============================\n");
		System.out.println("Audit complete. Output written to: " + 
				OUT_PATH + "\n\n");
	}
	
	/**
	 * Calculate the total number of IPs being analyzed.
	 * @return Total number of IPs contained in IP_RANGES object. This
	 * method cannot determine any overlap/multiplicity in those lists
	 */
	private int total_ips(){
		int running_sum = 0;
		for(int i=0; i < IP_RANGES.size(); i++)
			running_sum += IP_RANGES.get(i).breadth();
		return(running_sum);
	}
	
}
