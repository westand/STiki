#############################################################################
#
# Andrew G. West - README - This is the README file that should accompany
# the STiki front-end production build (no source provided).
#
############################################################################

############ INTRODUCTION #############

  First off, thank you for downloading STiki! STiki is an anti-vandalism tool
  for use on Wikipedia. STiki is a GUI which facilitates crowd-sourced
  solutions for the vandalism detection issue. A number of scoring systems
  ("queues") determine which edits an end-user will see. 

  This README should be distributed with, or internal to, a single *.JAR file
  This JAR file contains all code necessary to run the STiki client (minus 
  the Java runtime environment). This package contains only the user-facing 
  GUI -- STiki also has a server-side component which complements the 
  client (see "source code" below).


########### RUNNING STiki #############

  Starting STiki is straightforward:
  
  > java -jar [STiki_ver].jar
  
  Where [STiki_ver] is the JAR file which was distributed along-side, or 
  contains this README file. Windows should be able to simply double-click
  the JAR file in order to launch the STiki-GUI.
  
  The client machine must be able to connect to the STiki server in order
  for STiki to start (i.e., Internet connectivity must be present). Once 
  the GUI is successfully running, the 'Help' menu should address any
  questions/problems about usage of the application.
  

############ DEPENDENCIES #############

  STiki was developed and compiled using Java 1.6. Java Runtime Environmentt
  (JRE) 1.5 or higher is required on the client-machine in order 
  for STiki to run without error.
  
  STiki was is also dependent on external library(ies) whose necessary
  contributions have been compiled into STiki:
    
    (1) mysql-connector-java-5.0.8 - Released under the GPL, created by the 
    MySQL development team. This library facilitates communication between
    STiki backend processing and GUI clients.
    
  We thank the authors of these library(ies) for their contribution.


############## LICENSING ##############

  All code written for the STiki project is released under the GPL license.
  (The STiki source code should be available adjacent to wherever this
  JAR file was obtained, else see "more information" below).
  
  We ask that anyone using STiki in their own work cite the academic work
  which inspired the STiki project:
  
    [*] West, A.G., Kannan, S., & Lee, I. (2010). Detecting Wikipedia Vandalism
    via Spatio-Temporal Analysis of Revision Metadata. In EUROSEC '10: 
    Proceedings of the Third European Workshop on System Security, Paris,
    France. (A preliminary version was published as UPENN-MS-CIS-10-05).
  
  and the homepage for the STiki tool:
  
    [*] West, A.G. (2011). STiki: An Anti-Vandalism Tool for Wikipedia
    (software). Available from https://en.wikipedia.org/wiki/WP:STiki
    
  those interested in the link-spam queue in particular should see:
  
    [*] West, A.G., Agarwal, A., Baker, P., Exline, B., and Lee, I. (2011).
    Autonomous Detection of Link Spam in Purely Collaborative Environments.
    In Wikisym '11: The Seventh International Symposium on Wikis and Open
    Collaboration, Mountain View, CA, USA.
  

###### CONTACT/MORE INFORMATION #######

  STiki was written by Andrew G. West and supported in part by 
  ONR-MURI-N00014-07-1-0907. He may be reached at 
  [last_name]+and@cis.upenn.edu. His website is:
  [http://www.andrew-g-west.com]
  
  If you would like more information about STiki, like to check for
  version updates, or anything else; visit STiki's online presence:
  
  https://en.wikipedia.org/wiki/Wikipedia:STiki
  

#################################### END ####################################
