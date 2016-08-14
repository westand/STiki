package core_objects;

import java.util.List;

import mediawiki_api.api_retrieve;
import db_client.client_interface;
import db_server.db_geolocation;


/**
 * Andrew G. West - metadata.java - This class is the most fundamental 
 * object of the STiki project. It encapsulates the metadata of a single
 * edit/revision into an object for easy passing and handling. It is the
 * equivalent of a single row in the PreSTA-STiki database. 
 */
public class metadata{
	
	// **************************** PUBLIC FIELDS ****************************
	
		// Note that a number of fields are [public] and [final], these are
		// immutable properties of the edit (often provided by Wikipedia).
		// Those we set ourselves are private variables w/accessors
	
	/**
	 * Unique revision ID of the data to be added.
	 */
	public final long rid;
	
	/**
	 * Unique page-identifier (number) mapping to 'title'.
	 */
	public final long pid;
	
	/**
	 * Timestamp (UNIX) at which 'rid' made.
	 */
	public final long timestamp;
	
	/**
	 * Namespace in which the edit took place.
	 */
	public final int namespace;
	
	/**
	 * English title of the article on which 'rid' was made.
	 */
	public final String title;
	
	/**
	 * Registered user-name or IP of editor making 'rid'.
	 */
	public final String user;

	/**
	 * An indication if string 'user' is an IPv4 address. Some older 
	 * functions are unable to handle v6 addresses.
	 */
	public final boolean user_is_ipv4;
	
	/**
	 * An indication if string 'user' is an IP address (v4 or v6).
	 */
	public final boolean user_is_ipv4_or_ipv6;
	
	/**
	 * Revision comment left by 'user'.
	 */
	public final String comment;
		
	/**
	 * Assuming [user_is_ip]. this is the two-letter country code of the
	 * IP address origin, per geo-location data. Where geolocation data is
	 * unavailable or the user is registered, this should be the empty string.
	 */
	public final String country;
	
	/**
	 * List of tags associated with an edit (per [[WP:Tags]]).
	 */
	public final List<String> tags;
	
	/**
	 * Rollback token (for properly permissioned users). Note that this is
	 * NOT a final field, as it may be possible to refresh the edit token
	 * if the session cookie changes (i.e., a new user logs on).
	 */
	public String rb_token;
	
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * Indicator if this edit is a rollback, a.k.a. a "flagging edit".
	 */
	private boolean is_rb = false;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [metadata] object by providing all fields in STRING format.
	 * We forgoe an at length description of the parameters here (see above),
	 * but note that the TIMESTAMP argument should be provided in Wiki-format
	 * and will subsequently be transformed into a UNIX one. Further,
	 * [db_geo] is a DB-handler permitting IP->country mappings. If [db_geo]
	 * is null, this portion of the computation will be abandoned.
	 */
	public metadata(String str_rid, String str_timestamp, 
			String str_title, String str_pid, String str_namespace, 
			String str_user, String str_comment, List<String> tags, 
			String rb_token, db_geolocation db_geo) throws Exception{
		
			// NOTE: For string fields, we have chosen not to escape
			// special characters. Escaping IS done internal to DB handlers,
			// (and thus in the DB) as such formatting can error DB formats.
		
			// Some fields are simple numerical parsings or unmodified
		this.namespace = Integer.parseInt(str_namespace);
		this.rid = Long.parseLong(str_rid);
		this.pid = Long.parseLong(str_pid);
		this.title = str_title;
		this.tags = tags;
		this.rb_token = rb_token;
		
			// If no comment provided, no attribute is set in XML, leading
			// to null-status -- which is corrected here.
		if(str_comment == null) this.comment = "";
		else this.comment = str_comment;
		
			// User parse; determine if registered or anonymous (IP address).
		this.user = str_user;
		if(this.user.matches("(\\d)+\\.(\\d)+\\.(\\d)+\\.(\\d)+"))
			user_is_ipv4 = true;
		else user_is_ipv4 = false;
		user_is_ipv4_or_ipv6 = stiki_utils.is_v4_v6_ip(this.user);
		
			// Determine country of origination for anonymous users
		if(!this.user_is_ipv4 || db_geo == null)
			 country = "";
		else // Only do geolocation-calculation where user is IP address
			this.country = db_geo.get_country_code(
					stiki_utils.ip_to_long(this.user));

			// Only timestamp requires serious transformation
		this.timestamp = stiki_utils.wiki_ts_to_unix(str_timestamp);
	}
	
	/**
	 * This constructor is identical to that previous. The ONLY difference
	 * is that it uses client semantics and stored procedures to populaate
	 * the "country" field, rather than raw SQL calls.
	 */
	public metadata(String str_rid, String str_timestamp, 
			String str_title, String str_pid, String str_namespace, 
			String str_user, String str_comment, List<String> tags, 
			String rb_token, client_interface client) throws Exception{
		
			// NOTE: For string fields, we have chosen not to escape
			// special characters. Escaping IS done internal to DB handlers,
			// (and thus in the DB) as such formatting can error DB formats.
		
			// Some fields are simple numerical parsings or unmodified
		this.namespace = Integer.parseInt(str_namespace);
		this.rid = Long.parseLong(str_rid);
		this.pid = Long.parseLong(str_pid);
		this.title = str_title;
		this.tags = tags;
		this.rb_token = rb_token;
		
			// If no comment provided, no attribute is set in XML, leading
			// to null-status -- which is corrected here.
		if(str_comment == null) this.comment = "";
		else this.comment = str_comment;
		
			// User parse; determine if registered or anonymous (IP address).
		this.user = str_user;
		if(this.user.matches("(\\d)+\\.(\\d)+\\.(\\d)+\\.(\\d)+"))
			user_is_ipv4 = true;
		else user_is_ipv4 = false;
		user_is_ipv4_or_ipv6 = stiki_utils.is_v4_v6_ip(this.user);
		
			// Determine country of origination for anonymous users
		if(!this.user_is_ipv4 || client == null)
			 country = "";
		else // Only do geolocation-calculation where user is IP address
			this.country = client.geo_country(
					stiki_utils.ip_to_long(this.user));
		
			// Only timestamp requires serious transformation
		this.timestamp = stiki_utils.wiki_ts_to_unix(str_timestamp);
	}
	
	/**
	 * Construct an empty metadata object. All fields are set to zero, null,
	 * or un-interesting values. This constructor is useful only where a 
	 * metadata object is REQUIRED but not NEEDED for any practical purpose
	 */
	public metadata(){
		this.rid = 0;
		this.pid = 0;
		this.timestamp = 0;
		this.namespace = 0;
		this.title = "";
		this.user  = "";
		this.user_is_ipv4 = false;
		this.user_is_ipv4_or_ipv6 = false;
		this.comment = "";
		this.rb_token = "";
		this.country = "";
		this.is_rb = false;
		this.tags = null;
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Refresh the RB token associated with an edit (for example, if the
	 * logged in user changed since the previous fetch).
	 */
	public void refresh_rb_token() throws Exception{
		this.rb_token = api_retrieve.process_basic_rid(this.rid).rb_token;
	}
	
	/**
	 * Set the [is_rb] field (private) of this metadata object.
	 * @param is_rb_passed RB status of the edit encoded by this object
	 */
	public void set_is_rb(boolean is_rb){
		this.is_rb = is_rb;
	}
	
	/**
	 * Return the value of the [is_rb] (private) field of this metadata object.
	 * @return Value of the [is_rb] (private) field
	 */
	public boolean get_is_rb(){
		return (this.is_rb);
	}

}