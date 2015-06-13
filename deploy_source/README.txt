#############################################################################
#
# Andrew G. West - README - This is the README file that should accompany
# the distribution of STiki source code.
#
############################################################################

############ INTRODUCTION #############

  First off, thank you for downloading STiki! STiki is an anti-vandalism tool
  for use on Wikipedia. STiki is a tool which facilitates anti-vandalism 
  efforts on Wikipedia, by scoring edits to prioritize their display to 
  end-users. A number of different strategies ("queues") are integrated by 
  the code. Several of these are accessed via third-party APIs. However, 
  this distribution also contains the complete code of one such queue, 
  based on scoring of edit metadata. 
  
  This README is intended to be distributed with STiki source code. The
  source-code package contains the Java and shell-scripts that implement
  (in combination with neccesary dependencies) both the user-facing STiki
  client, as well as the back-end edit processing engine.
  
  
########## SOURCE CODE USAGE ##########

  The STiki client and server communicate via a JDBC (database connector). 
  For security reasons, we do not disclose the back-end database location
  or connection string in this source (see [core_objects.stiki_db_con.java]).
  Thus, THIS SOURCE WILL NOT COMPILE TO A USABLE EXECUTABLE. In order to
  make use of this source-code, researchers/developers should:
  
    (1) Use this source-code to run their own STiki-server. This package
        provides all the necessary functionality, and the connection
        parameters simply need written in [core_objects.stiki_db_con.java]. 
        The basic database schema is provided in the [misc] package, and 
        STiki authors can provide historical table-data in order to help
        a new server get started (see "contact" below). 
    
    (2) Obtain connection parameters from the STiki authors (see "contact"
        below). This is only appropriate if one is modifying solely the 
        STiki-frontend, or wants only READ access over the database.
    
    (3) Alternatively, the server-side can be ignored, and a developer could
        use the GUI to display edits of his/her own choosing. If this is the
        case, see [gui_support.gui_rid_queue.java], as it is the interface 
        between the front-end and back-end.
    
    
############# COMPILATION #############

  Although STiki code will not compile to a use-able executable 
  out-of-the-box, it will still successfully compile. Further, a Makefile
  is provided as a service to developers who may find it useful.
  
  In order to compile, simply navigate to the project-root directory (that
  containing the Makefile, and run: 
  
    > make all
  
  Assuming the dependencies described below are met, the code should
  successfully compile.


############ DEPENDENCIES #############
  
  STiki is dependent on several external JAVA libraries. These libraries
  must be obtained and placed on the compilation classpath. This can be done
  by (1) editing the makefile, so that compilation is aware of the libraries
  location, or more simply, (2) placing the library files in/on the default
  Java classpath, so explicit references are not necessary.  
  
  The required code/libraries are:
  
    (1) Java-JRE 1.5, or higher -- Due to use of Generic types and
        Collections, the run-time environment should be fairly recent.
    
    (2) mysql-connector-java-3.1.14, or higher -- This JDBC provides
        client-server connectivity. Freely available under the GPL.
    
    (3) irclib-1.10, or higher -- Wikipedia publishes its 'recent changes'
        on an IRC channel, on which the back-end engine listens. Also
        freely available under the GPL. 
      

############# CODE NOTES ##############

  We encourage researchers/developers to explore the code. Feel free to 
  contact the authors (see below), if any questions arise. Along these lines,
  there are a few broad notes to make:
  
    * The files compromising the STiki-frontend have been developed to be
      platform independent, as much as possible. However, ...
    * ... The back-end engine was designed to operate on a UNIX machine. 
      Slight modification may be required to ensure correct operation under 
      your operating system of choice. For example, shell scripts are
      written for BASH, and commands are assumed to reside in /bin.


############## LICENSING ##############

  All code written for the STiki project is released under the GPL license.
  Developers should take care that all criteria of the licenses of external 
  libraries (dependencies) are also met when making derivative works.
  
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