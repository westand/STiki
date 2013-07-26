/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_queue_fetch_spam.sql - This is a stored proc   */
/* which fetches RIDs for a user to inspect. A "reservation block" is     */
/* checked-out, which has a time-to-live before expiration. Fetches are   */
/* user-specific given the ability to ignore certain edits.               */
/*                                                                        */
/* While generated from a single queue, a reservation cascades across     */
/* ALL queues. Since it isn't a great idea to permit a table as an open   */
/* procedure variable -- each queue has its own fetch procedure.          */
/*                                                                        */
/* This file handles the "Link Spam" queue (and the [queue_spam] table)   */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_queue_fetch_spam

(IN user VARCHAR(256),      /* IN:  Name of user fetching edits*/
 IN resid INTEGER UNSIGNED, /* IN:  Randomly generated ID known to client */
 OUT d VARCHAR(512))        /* OUT: CSV String of R_ID,P_ID pairs to enqueue */

BEGIN
  
  DECLARE ignore_sep CHAR(1);
  DECLARE expiry INTEGER UNSIGNED;
  DECLARE ttl INTEGER UNSIGNED;
  
  DECLARE rev INTEGER UNSIGNED;
  DECLARE page INTEGER UNSIGNED;
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR SELECT R_ID, P_ID FROM queue_spam WHERE RES_ID=resid;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    
      /* Check for merged account */
  SET user = client_func_usermap(user);
  
    /* Initialize variables and set constants */
  SET d = '';
  SET ignore_sep = '|';
  SET ttl = 1200;
  SELECT (UNIX_TIMESTAMP() + ttl) INTO expiry;

    /* First make the reservation with an UPDATE                */
    /* Note that empty string users should not use "PASS" field */
    /* We can also use this block to notify blocked users       */
  IF user = 'blocked_user_xx' THEN /* Blocked users */
    SELECT '412188633,30751023,' INTO d;
    SELECT SLEEP(5);
  ELSEIF user = '' THEN
    UPDATE queue_spam 
      SET RES_ID=resid,RES_EXP=expiry 
      WHERE RES_EXP<=UNIX_TIMESTAMP()
      ORDER BY SCORE DESC
      LIMIT 10;
  ELSE
    UPDATE queue_spam
      SET RES_ID=resid,RES_EXP=expiry 
      WHERE RES_EXP<=UNIX_TIMESTAMP() AND 
        PASS NOT LIKE CONCAT('%',ignore_sep,user,ignore_sep,'%')
      ORDER BY SCORE DESC
      LIMIT 10;
  END IF;  
   
    /* Then go back and see what was reserved, compiling pairs into a */
    /* CSV file (note that there is a trailing comma on this CSV      */
    /* In this cursor loop, also transfer reservation to other queues */
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO rev,page;
    IF done THEN
      LEAVE read_loop;
    END IF;
    UPDATE queue_stiki SET RES_ID=resid,RES_EXP=expiry WHERE R_ID=rev;
    UPDATE queue_cbng SET RES_ID=resid,RES_EXP=expiry WHERE R_ID=rev;
    UPDATE queue_wt SET RES_ID=resid,RES_EXP=expiry WHERE R_ID=rev;
    SELECT CONCAT(d,rev,',',page,',') INTO d;
  END LOOP;
  CLOSE cur;
  
      /* Log call for debugging and security purposes */
  INSERT INTO log_client 
    VALUES (SUBSTRING(USER(), 1, 128),'queue_fetch_spam',UNIX_TIMESTAMP());
 
END

/********************************** END ***********************************/
