#############################################################################
#
# Andrew G. West - README.txt - This is the README file that should 
# accompany the production build of the audit tool (WikiAudit).
#
############################################################################

############ INTRODUCTION #############

  First off, thank you for downloading WikiAudit! WikiAudit is a tool, that
  given a set of IP addresses as input, outputs a report summarizing the
  contributions and behavior of those IPs on some wiki. In particular,
  heuristics direct attention to malicious/unconstructive behaviors.
  
  Though optimized for English Wikipedia, WikiAudit can be used on any
  Mediawiki wiki. At current, WikiAudit is only a command-line tool (i.e., 
  there is no graphical user interface).
  
  We envision WikiAudit being useful for:
    
    (1) Institutional/organizational network administrators who want to 
    monitor the contributions coming from their IP space. From the 
    organization's perspective, this can help protect reputation and mis-use
    of organizational resources. Similarly, organizations's who take steps
    to prevent future mis-behavior help benefit the wiki.
    
    (2) Casual readers who use the tool to conduct security investigations
    and reveal organizational bias in authoring. For example, an edit to 
    Wikipedia article [x] from the IP space of organization [x] might be
    inappropriately promotional or scrub factual criticism.
  
  This README should be distributed with, or internal to, a single *.JAR file
  This JAR file contains all code necessary to run WikiAudit (minus 
  the Java runtime environment).
  
  This README covers only "meta" information about WikiAudit and how to
  run it over a set of IP addresses. The actual report it generates will
  include documentation about how that report should be interpreted.
  

######### RUNNING WIKI-AUDIT ##########

  WikiAudit takes one (1) required, and three (3) optional arguments.
  See also the "example usages" at the end of this section:
  
  REQUIRED ARGUMENT
    The program requires a list of IP addresses for analysis. The format 
    of this list should be "IP1,IP2,IP3..." (minus quotes, no spaces).
    These "IP" fields should take one of three forms:
      1. A single IP address, e.g., "127.0.0.0"
      2. A hyphenated IP range, e..g., "127.0.0.0-127.255.255.255"
      3. A CIDR IP range, e.g., "127.0.0.0/8"
    and should be provided using one of two flags:
      1. (-ipc) The IP CSV is provided directly at the command-line
      2. (-ipf) The IP CSV is in a plain-text file, and this argument
                provides the path to that text file.
    - Do not overlap ranges or include an IP address more than once.
    - Behavior is undefined if this field is not well-formed.
  
  OPTIONAL ARGUMENTS
    (-a) A connection string to the wiki over which analysis should
         be performed. If "api.php" is appended to this String, it
         should be the API of that wiki. If this argument is not provided
         "en.wikipedia.org/w/" will be used (English Wikipedia).
         The site must be running Mediawiki software.
    
    (-t) Time variable so that only wiki events occuring on-or-after
         the provided date will be included in reporting. Format should
         be "YYYYMMDD". If this argument is not provided, the
         program will default to the UNIX epoch (i.e., "19700101").
         Realize that many wikis operate in UTC locale.
          
    (-o) Path for output file. This path should have a *.html extension.
         If not provided, the default will be "index.html" in the 
         project directory.
         
  EXAMPLE USAGES:
   > java -jar WikiAudit_[version_#].jar -ipc 71.250.134.0/12
  
   > java -jar WikiAudit_[version_#].jar -c en.wikipedia.org/w/ -t 20100101 
     -ipc 128.91.0.0/16,130.91.0.0/16,158.130.0.0-158.130.255.255
     

############## CAVEATS ################

  A few things to consider when running WikiAudit:
  
  * WikiAudit is very much alpha-version software being provided as a 
    service to the community. It probably has bugs. They can be reported
    and/or discussed at [WP:WikiAudit], or get the source code and
    fix them yourself!
  
  * WikiAudit and its heuristics are optimized for English Wikipedia. 
    Things should work relatively well for any English language running 
    Mediawiki. Foreign language performance is untested, but contact us 
    (see below) if you'd like to help with localization.
  
  * The tool works by making extensive API calls from the user's machine
    to the wiki-hosted API. Program speed is dependent on the network
    connection and the density of editing activity in the IP range. For
    perspective, we can usually process ~65,000 addresses (i.e., a /16 CIDR)
    in about 5 minutes time. Submitting excessively large IP ranges may
    result in API throttling.


######### SOURCE/LICENSING ############

  The WikiAudit source code can be obtained by visiting 
  [http://en.wikipedia.org/wiki/WP:STiki] and downloading the source for
  the STiki tool (as these tools share a code-base). All WikiAudit 
  code is included in the [audit_tool/] directory. Just as with STiki, 
  all code is released under the GPL license.
  
  We also ask that anyone using WikiAudit in their own work cite the homepage
  for WikiAudit as:
  
      [*] West, A.G. (2012). WikiAudit: Examining Organizational 
      Contribitions to Wiki Environments (software). Available from 
      http://en.wikipedia.org/wiki/WP:WikiAudit
  
  Hopefully there will also be WikiAudit-derived academic writing/
  demonstrations/posters that can be cited in the future. 


###### CONTACT/MORE INFORMATION #######

  WikiAudit was written by Andrew G. West and supported in part by 
  ONR-MURI-N00014-07-1-0907. He may be reached at 
  [last_name]+and@cis.upenn.edu. His website is:
  [http://www.cis.upenn.edu/~westand]
  
  If you would like more information about WikiAudit, like to check for
  version updates, or anything else; visit WikiAudit's online presence:
  
  http://en.wikipedia.org/wiki/Wikipedia:WikiAudit
  

#################################### END ####################################
