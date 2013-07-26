/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_users_explicit.sql - This is a stored proc.    */
/* which determines if some username is in the "explicit users" table     */
/* which exempts them from any requirements needed to use the STIki tool. */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_users_explicit

(IN user_in VARCHAR(256), /* IN:  Name of user fetching edits */
 OUT approved INTEGER)    /* OUT: Whether they have explicit approval 0/1 */

BEGIN
	
	    /* Initialize variables and set constants */
  SET approved = 0;
  
      /* Issue query, store into output */
  SELECT COUNT(*)
    FROM users_explicit
    WHERE USER=user_in 
    INTO approved;

END

/********************************** END ***********************************/
