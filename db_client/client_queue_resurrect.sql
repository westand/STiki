/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_queue_resurrect.sql - This is a stored         */
/* procedures which "ressurrects" an RID. This weird case is required     */
/* if the back-button is used to make an "INNOCENT->PASS" change.         */
/*                                                                        */
/* Observe that resurrection must be performed on ALL queues, regardless  */
/* of which queue the RID was fetched from. It is possible that a system  */
/* never classified the edit that it is being asked to resurrect -- thus  */
/* the NULL tests allow such resurrections to be abandoned.               */
/*                                                                        */
/* The [feedback] left (and since invalidated) also needs revoked.        */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_queue_resurrect

(IN rid INTEGER UNSIGNED,  /* IN: Revision ID of edit being resurrected */
 IN pid INTEGER UNSIGNED)  /* IN: Page ID of edit being resurrected */

BEGIN

  DECLARE score DOUBLE;

    /* Revoke the invalidated feedback left */   
  DELETE FROM feedback 
    WHERE R_ID=rid;

  /********** STiki QUEUE ************/

    /* First, determine how the RID was scored */
  SELECT SCORE FROM scores_stiki 
    WHERE R_ID=rid 
    INTO score;
  
  IF score IS NOT NULL THEN
    
      /* Re-insert RID into priority queue. Original feedback should */
      /* have freed space. So ignore on duplicate PRIMARY KEY        */
    INSERT IGNORE INTO queue_stiki
      VALUES (rid,pid,score,0,0,"");
    
  END IF;
  
  /*********** CBNG QUEUE ************/
  
  SELECT SCORE FROM scores_cbng
    WHERE R_ID=rid 
    INTO score;
  
  IF score IS NOT NULL THEN
    
    INSERT IGNORE INTO queue_cbng
      VALUES (rid,pid,score,0,0,"");
    
  END IF;
  
  /******** WIKITRUST QUEUE **********/
  
  SELECT SCORE FROM scores_wt 
    WHERE R_ID=rid 
    INTO score;
  
  IF score IS NOT NULL THEN
    
    INSERT IGNORE INTO queue_wt
      VALUES (rid,pid,score,0,0,"");
  
  END IF;

  /******** LINK-SPAM QUEUE **********/
  
  SELECT SCORE FROM scores_spam
    WHERE R_ID=rid 
    INTO score;
  
  IF score IS NOT NULL THEN
    
    INSERT IGNORE INTO queue_spam
      VALUES (rid,pid,score,0,0,"");
  
  END IF;
  
  /********** OTHER STUFF ************/
  
        /* Log call for debugging and security purposes */
  INSERT INTO log_client 
    VALUES (SUBSTRING(USER(), 1, 128),'queue_resurrect',UNIX_TIMESTAMP());
  
END

/********************************** END ***********************************/
  
