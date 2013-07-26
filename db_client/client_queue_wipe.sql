/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_queue_wipe.sql - This is a stored procedure    */
/* which "wipes" a reservation, or "unreserves" it -- this is useful      */
/* when the operating user changes or the user shuts down -- so that      */ 
/* they can return their unclassified edits to the active queue(s)        */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_queue_wipe

(IN resid INTEGER UNSIGNED)  /* IN:  Randomly generated ID known to client */

BEGIN

    /* Simple update across all queues */
  UPDATE queue_stiki
    SET RES_EXP=UNIX_TIMESTAMP() 
    WHERE RES_ID=resid;
    
  UPDATE queue_cbng
    SET RES_EXP=UNIX_TIMESTAMP() 
    WHERE RES_ID=resid;
    
  UPDATE queue_wt
    SET RES_EXP=UNIX_TIMESTAMP() 
    WHERE RES_ID=resid;

  UPDATE queue_spam
    SET RES_EXP=UNIX_TIMESTAMP() 
    WHERE RES_ID=resid;

    /* Log call for debugging and security purposes */
  INSERT INTO log_client 
    VALUES (SUBSTRING(USER(), 1, 128),'queue_wipe',UNIX_TIMESTAMP());
 
END

/********************************** END ***********************************/
