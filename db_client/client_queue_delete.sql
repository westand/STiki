/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_queue_delete.sql - This is a stored procedure  */
/* which deletes an edit from the priority queues. It is used when an     */
/* edit is classified, "vandalism" or "innocent", so that it is not       *.
/* reclassified in the future.                                            */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_queue_delete

(IN rid INTEGER UNSIGNED)  /* IN: Revision identifier to be dequeued */

BEGIN

    /* Action duplicated across all queues */
  DELETE FROM queue_stiki
    WHERE R_ID=rid;
  
  DELETE FROM queue_cbng 
    WHERE R_ID=rid;
  
  DELETE FROM queue_wt 
    WHERE R_ID=rid;

  DELETE FROM queue_spam 
    WHERE R_ID=rid;
    
    /* Log call for debugging and security purposes */
  INSERT INTO log_client 
    VALUES (SUBSTRING(USER(), 1, 128),'queue_delete',UNIX_TIMESTAMP());
 
END

/********************************** END ***********************************/
