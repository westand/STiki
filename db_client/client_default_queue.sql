/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_default_queue.sql - This is a stored procedure */
/* which, returns the queue "integer code" for the queue which should be  */
/* selected by default when the GUI loads.                                */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_default_queue

(OUT qcode INTEGER)            /* OUT: Integer code for queue */

BEGIN

    /* Initialize variables and set constants */
  SET qcode = 0;
  
    /* Issue query, store into output */
  SELECT SYS_CODE
    FROM default_queue
    WHERE DEF=1
    INTO qcode;
   
END

/********************************** END ***********************************/

