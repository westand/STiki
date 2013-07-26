/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_feedback_insert.sql - This is a stored         */ 
/* procedure which records STiki client "feedback." That is, this         */
/* persistently records uses of the "innocent" and "vandalism" button.    */
/*                                                                        */
/* In an attempt at modularity, this method also dequeues the classifed   */
/* RID from all edit queues.                                              */ 
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_feedback_insert

(IN rid INTEGER UNSIGNED,  /* IN: Revision ID of edit being classifed */
 IN plabel INTEGER,        /* IN: Integer code of classification */
 IN user VARCHAR(256))     /* IN: Username of one making classification */

BEGIN
	
	    /* Check for merged account */
  SET user = client_func_usermap(user);

    /* Action duplicated across all queues */
  INSERT INTO feedback 
    VALUES (rid,plabel,UNIX_TIMESTAMP(),user)
    ON DUPLICATE KEY
    UPDATE LABEL=plabel,TS_FB=UNIX_TIMESTAMP(),USER_FB=user; 

    /* Any feedback action should dequeue edits */
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
    VALUES (SUBSTRING(USER(), 1, 128),'feedback_insert',UNIX_TIMESTAMP());
 
END

/********************************** END ***********************************/
