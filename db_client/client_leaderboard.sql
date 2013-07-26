/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_leaderboard.sql - This is a stored proc        */
/* which generates a simplified version of the "leaderboard" to be        */
/* presented inside the STiki GUI. It is complicated somewhat by the      */
/* need to output all leaderboard data as a single String.                */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_leaderboard

(OUT d VARCHAR(64000))     /* OUT: CSV String of USER,COUNT(*),VAND,AGF */

BEGIN
  
  DECLARE user VARCHAR(256);
  DECLARE quant INTEGER UNSIGNED;
  DECLARE vand INTEGER UNSIGNED;
  DECLARE agf INTEGER UNSIGNED;
  
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR SELECT USER_FB,COUNT(*),
    SUM(IF(LABEL>0 && LABEL<5,1,0)) as VANDALISM,
    SUM(IF(LABEL>=5,1,0)) as AGF 
    FROM feedback GROUP BY USER_FB ORDER BY COUNT(*) DESC;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  
  SET d = '';
   
    /* Compile results into a CSV for simplified output */
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO user,quant,vand,agf;
    IF done THEN
      LEAVE read_loop;
    END IF;
    SELECT CONCAT(d,user,',',quant,',',vand,',',agf,',') INTO d;
  END LOOP;
  CLOSE cur;
  
      /* Log call for debugging and security purposes */
  INSERT INTO log_client 
    VALUES (SUBSTRING(USER(), 1, 128),'leaderboard',UNIX_TIMESTAMP());
 
END

/********************************** END ***********************************/
