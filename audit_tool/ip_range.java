package audit_tool;

import core_objects.stiki_utils;

/**
 * Andrew G. West - ip_range.java - Object wrapping a range of IP addresses.
 */
public class ip_range implements Comparable<ip_range>{

	// ***************************** PUBLIC FIELDS ***************************
	
	/**
	 * First IP address in the range, in String format ("127.0.0.1").
	 */
	public final String IP_BEG;
	
	/**
	 * Last IP address in the range, in String format ("127.0.0.1").
	 */
	public final String IP_END;
	
	/**
	 * First IP address in the range, in int/bit-wise format
	 */	
	public final long IP_BEG_INT;
	
	/**
	 * Last IP address in the range, in int/bit-wise format
	 */
	public final long IP_END_INT;

	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [ip_range] by providing boundary addresses. 
	 * @param ip_beg First IP address, in String format ("127.0.0.1")
	 * @param ip_end Last IP address, in String format ("127.0.0.1")
	 */
	public ip_range(String ip_beg, String ip_end){
		this.IP_BEG = ip_beg;
		this.IP_END = ip_end;
		this.IP_BEG_INT = stiki_utils.ip_to_long(ip_beg);
		this.IP_END_INT = stiki_utils.ip_to_long(ip_end);
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * IP ranges are compared/sorted based solely on the start address.
	 */
	@Override
	public int compareTo(ip_range other){
		if(this.IP_BEG_INT < other.IP_BEG_INT)
			return(-1);
		else if(this.IP_BEG_INT > other.IP_BEG_INT)
			return(1);
		else return(0);
	}
	
	/**
	 * Human-readable representation of the range
	 */
	public String toString(){
		if(this.breadth() != 1){
			return(IP_BEG + " to " + IP_END + 
					" (" + this.breadth() + " IP addresses)");
		} else{
			return(IP_BEG + " (1 IP address)");
		} // branch on quantity
		
	}
	
	/**
	 * Return the number of IP addresses this range represents.
	 * @return the number of IP addresses this range represents
	 */
	public long breadth(){
		return(IP_END_INT - IP_BEG_INT + 1);
	}
	
	
	
}
