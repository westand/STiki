/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_queue_ignore.sql - This is a stored procedure  */
/* which marks an enqueued RID "ignored" by a STiki user. Morover, it     */
/* releases the reservation on that edit, so some else can review it.     */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_queue_ignore

(IN rev INTEGER UNSIGNED,  /* IN:  Revision ID of edit being ignored */
 IN user VARCHAR(256))     /* IN:  Name of user fetching edits */

BEGIN

  DECLARE ignore_sep CHAR(1);
  SET ignore_sep = '|';
  
      /* Check for merged account */
  SET user = client_func_usermap(user);
  
    /* Straightforward update, across all queues */
  UPDATE queue_stiki
    SET PASS=CONCAT(PASS,ignore_sep,user,ignore_sep),RES_EXP=UNIX_TIMESTAMP()
    WHERE R_ID=rev;

  UPDATE queue_cbng
    SET PASS=CONCAT(PASS,ignore_sep,user,ignore_sep),RES_EXP=UNIX_TIMESTAMP()
    WHERE R_ID=rev; 
    
  UPDATE queue_wt
    SET PASS=CONCAT(PASS,ignore_sep,user,ignore_sep),RES_EXP=UNIX_TIMESTAMP()
    WHERE R_ID=rev; 

  UPDATE queue_spam
    SET PASS=CONCAT(PASS,ignore_sep,user,ignore_sep),RES_EXP=UNIX_TIMESTAMP()
    WHERE R_ID=rev; 
    
    /* Log call for debugging and security purposes */
  INSERT INTO log_client 
    VALUES (SUBSTRING(USER(), 1, 128),'queue_ignore',UNIX_TIMESTAMP());
    
END

/********************************** END ***********************************/
