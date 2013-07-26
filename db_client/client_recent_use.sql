/**************************************************************************/
/*                                                                        */
/* Andrew G. West - client_recent_use.sql - This is a stored procedure    */
/* which, will return some "recent usage statistics (relative to a        */
/* provided timestamp), to guide a user on whether it is a good time      */
/* to use the tool, or if they should switch queues.                      */
/*                                                                        */
/**************************************************************************/

CREATE PROCEDURE client_recent_use

(IN time_ago INTEGER UNSIGNED,      /* IN:  User wanting recent statistics */
 OUT ru_stiki INTEGER UNSIGNED,     /* OUT: STiki queue uses in period */
 OUT ru_rv_stiki INTEGER UNSIGNED,  /* OUT: STiki reverts over same period */
 OUT ru_cbng INTEGER UNSIGNED,      /* OUT: CBNG queue uses in period */
 OUT ru_rv_cbng INTEGER UNSIGNED,   /* OUT: CBNG reverts over same period */
 OUT ru_wt INTEGER UNSIGNED,        /* OUT: WT queue uses in period */
 OUT ru_rv_wt INTEGER UNSIGNED,     /* OUT: WT reverts over same period */
 OUT ru_spam INTEGER UNSIGNED,      /* OUT: SPAM queue uses in period */
 OUT ru_rv_spam INTEGER UNSIGNED)   /* OUT: SPAM reverts over same period */

BEGIN
	
	  /* Initialize all output */
	SET ru_stiki = 0;
	SET ru_rv_stiki = 0;
	SET ru_cbng = 0;
	SET ru_rv_cbng = 0;
	SET ru_wt = 0;
	SET ru_rv_wt = 0;
	SET ru_spam = 0;
	SET ru_rv_spam = 0;
  
    /* STiki queries */
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=-1 OR LABEL=1 OR LABEL=5)
    INTO ru_stiki;
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=1 OR LABEL=5)
    INTO ru_rv_stiki;
    
    /* CBNG queries */
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=-2 OR LABEL=2 OR LABEL=10)
    INTO ru_cbng;
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=2 OR LABEL=10)
    INTO ru_rv_cbng;
    
    /* WikiTrust queries */
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=-3 OR LABEL=3 OR LABEL=15)
    INTO ru_wt;
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=3 OR LABEL=15)
    INTO ru_rv_wt;

    /* SPAM queries */
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=-4 OR LABEL=4 OR LABEL=20)
    INTO ru_spam;
  SELECT COUNT(*)
    FROM feedback
    WHERE TS_FB>=time_ago AND (LABEL=4 OR LABEL=20)
    INTO ru_rv_spam;
    
END

/********************************** END ***********************************/

