
##############################################################################
#
# Andrew G. West - stiki_api_readme.txt - A README file describing the
# simple HTTP/web interface into the STiki database, providing developers 
# access to both the STiki vandalism scores ("magic numbers"), and the raw 
# feature data which was used to arrive at these scores.
#
##############################################################################

 The URL format for the API is: 
 http://armstrong.cis.upenn.edu/stiki_api.php?style=score&rid=346523549

 For which there are two required parameters:

 (1) "style": Must be either [score] or [raw]. If [score], only a "magic
     number" will be output. If [raw], then an entire feature set will
     be returned to the client in JSON format.

 (2) "rid": Here, a Wikipedia revision-ID must be provided. See
     Wikipedia for additional documentation to this affect.

#######################################

 The API may return errors or empty results under certain criteria:

 (*) First, Internet connectivity must be present and the STiki
     database must be operational. If either is not the case, then the
     API will fail silently or with a MySQL error.

 (*) The provided RID must be valid. Further, STiki must have data for
     that RID. STiki only processes Wikipedia edits (RIDs) in 
     namespace-zero (NS0). Beyond that, there are additional criteria:
     
       (*) STiki became operational around Jan. 2010. The database contains
           RIDs >= 346523549 for requests of type [score], and
           RIDs >= 315000002 for requests of type [raw].
       
       (*) Prior to RID=379406568, only anonymous/IP edits were processed.
           Since then, the edits of registered users are also considered.
   
       (*) The STiki server has experience occassional down-time, and no
           attempt is made to process edits made during this time.
           Therefore, they are missing from the database.
           
       (*) There is a slight delay between when an edit is made to Wikipedia,
           and STiki is done processing it. Under normal conditions, this
           is less than 5 seconds -- but this may increase during periods
           of heavy server load.

#######################################

 Assuming a result is returned, it should be interpreted as follows:

 For requests of "style"=[score]: 

   A single real number will be returned in plain text. This "vandalism score" 
   speaks to the probability that the provided RID is vandalism. Higher numbers
   indicate a stronger probability of vandalism. Values lie on [0,1].
   
   Generally speaking, the values should be interpreted relative to each 
   other. However, as new fields have been added to the feature set, the
   machine-learning model changes -- and a set of edits that spans such a 
   change may not be strictly comparable.

###################

 For requests of "style"=[raw]: 

   A 15-tuple in JSON format is returned. Each feature is briefly described
   below. Please investigate the STiki source code for more information.
   Also note that new features have been introduced, and others deprecated
   during the STiki development process -- leaving default values:

     (01) IS_IP: Boolean in {0,1}. If 0 then the edit in question was made
          by a registered user. Else, the editor was anonymous.
     (02) REP_USER: Real on [0,INF] speaking to the reputation
          of the user who made the edit. High values are poor.
     (03) REP_ARTICLE: Real on [0,INF] speaking to the reputation
          of the article the edit was made on. High values are poor.
     (04) TOD: Real on [0,24) OR -1. The local time-of-day the edit was made.
          Negative one (-1) is returned if not able to calculate.
     (05) DOW: Integer on [1,7] OR -1. The local day-of-week the edit was made.
          Sunday=1. Negative one (-1) is returned if not able to calculate.
     (06) TS_R: Integer on [0,INF]. The "time-since registration", in
          seconds for the editing user. For IP editors, this is the time
          since that IP first made an edit.
     (07) TS_LP: Integer on [-1,INF]. The "time-since page last edited",
          in seconds, for the page edited. Negative one (-1) is returned if
          the edit in question was the first on that page.
     (08) TS_RBU: Integer on [-1,INF]. The "time-since user rolled-back",
          in seconds. That is, "how many seconds ago was the editing user
          last caught vandalizing?". Negative one is returned if the 
          editing user has never been caught in a vandalism instance.
     (09) COMM_LENGTH: Integer on [0,INF]. Number of characters in the 
          edit summary, as provided by the editing user.
     (10) BYTE_CHANGE: Integer on [-INF,INF]. The change in size of the
          edited article, in bytes, relative to the previous version.
     (11) REP_COUNTRY: Real on [0,1] OR -1, speaking to the reputation of the
          country in which the editing user resides. High values are poor.
          Negative one (-1) is returned if not able to calculate.
     (12) NLP_DIRTY: Integer on [0,INF]. The number of "dirty words"
          added to the article by the revision under investigation.
     (13) NLP_CHAR_REP: Integer on [0,INF]. The maximum number of times a
          single character is repeated in any additions made by the eidt.
     (14) NLP_UCASE: Real on [0,1]. The percentage of alpha-characters
          added to the article which are upper-case (capitalized).
     (15) NLP_ALPHA: Real on [0,1]. The percentage of all characters added
          to the article which are alpha (letters), not numeric/symbolic.
          

#################################### END #####################################
