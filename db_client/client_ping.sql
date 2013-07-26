/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_ping.sql - This is a stored procedure which    */
/* aims to be the most simplistic possible. It is useful for "pinging"    */
/* the STiki server to keep persistent connections alive                  */ 
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_ping()

BEGIN

  SELECT 1;
 
END

/********************************** END ***********************************/
