/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_oe_insert.sql - This is a stored procedure     */ 
/* inserting an entry into the "offending edits" table. This class may    */
/* also handle all triggers on that table in an in-line fasion.           */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_oe_insert

(IN rid INTEGER UNSIGNED,       /* IN: Poor rev-ID being marked as OE */
 IN pid INTEGER UNSIGNED,       /* IN: Page ID on which 'rid' resides */
 IN oe_ts INTEGER UNSIGNED,     /* IN: Timestamp when edit 'rid was made */ 
 IN ns INTEGER,                 /* IN: Namespace in wich 'rid' resides */
 IN user VARCHAR(256),          /* IN: User who committed edit 'rid' */
 IN flag_rid INTEGER UNSIGNED,  /* IN: RID which flagged 'rid' as offending */
 IN country_code CHAR(2),       /* IN: Country code of 'rid' origination */
 IN rb_code INTEGER UNSIGNED)   /* IN: Code speaking to bot/human RB */

BEGIN

    /* Calculate UNIX day when offending edit made*/
  DECLARE p_unix_day INTEGER UNSIGNED;
  SELECT FLOOR(oe_ts/(60*60*24)) INTO p_unix_day;

    /* Determine if the R_ID has been flagged, this will happen, when */
    /* an R_ID is displayed in STiki, but gets beaten to flagging. If */
    /* already flagged, no need to redo flag, or all the triggers.    */ 
  SELECT R_ID FROM offending_edits WHERE R_ID=rid;
  IF FOUND_ROWS() = 0 THEN

      /* First, simple insertion into [OE] table */
    INSERT INTO offending_edits 
      VALUES (rid,pid,oe_ts,ns,user,flag_rid,-1);
      
      /* Then, handle simple triggers on associated tables */
    UPDATE all_edits SET OE=1 WHERE R_ID=rid;
    UPDATE features SET LABEL=1 WHERE R_ID=rid;
    UPDATE hyperlinks SET RBED=rb_code WHERE R_ID=rid;
      
      /* Trigger into [country] table requires some logic */
    SELECT UNIX_DAY FROM country 
      WHERE UNIX_DAY=p_unix_day AND 
      COUNTRY=country_code;
    IF FOUND_ROWS() = 0 THEN
      INSERT INTO country VALUES (p_unix_day,country_code,0,0);
    END IF;
    UPDATE country 
      SET BAD_EDITS=(BAD_EDITS+1) 
      WHERE UNIX_DAY=p_unix_day AND COUNTRY=country_code;

  END IF;
  
      /* Log call for debugging and security purposes */
  INSERT INTO log_client 
    VALUES (SUBSTRING(USER(), 1, 128),'oe_insert',UNIX_TIMESTAMP());
    
END

/********************************** END ***********************************/
