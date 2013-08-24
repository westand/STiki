/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_req_version.sql - This is a stored procedure   */
/* which, returns the latest version of STiki a client is REQUIRED to     */
/* have. This is useful in forcing client upgrades when there are API     */
/* or other dramatic changes forced on us from above.                     */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_req_version

(OUT req INTEGER)          /* OUT: Integer wrapping required version */

BEGIN

    /* Initialize variables and set constants */
  SET req = 0;
  
    /* Issue query, store into output */
  SELECT RVERSION
    FROM req_version
    INTO req;
   
END

/********************************** END ***********************************/

