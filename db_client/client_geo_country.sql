/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_geo_country.sql - This is a stored procedure   */
/* which, given an IP address, returns the two-character country-code     */
/* to which that IP address geo-locates (or an empty String)              */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_geo_country

(IN p_int_ip INTEGER UNSIGNED, /* IN:  IP address, in integer format */
 OUT ccode CHAR(2))            /* OUT: Country code to which 'in' maps */

BEGIN

    /* Initialize variables and set constants */
  SET ccode = '';
  
    /* Issue query, store into output */
  SELECT country_code 
    FROM geo_country 
    WHERE ip_start<=p_int_ip
    ORDER BY ip_start DESC LIMIT 1
    INTO ccode;
   
END

/********************************** END ***********************************/
