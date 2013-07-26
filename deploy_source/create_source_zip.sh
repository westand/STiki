#!/bin/sh

#############################################################################
#
# Andrew G. West - create_source_zip.sh - This script builds the ZIP file which
# contains the STiki source code. No external libraries or # dependencies are 
# shipped -- it is the responsibility of the compiling user to obtain them.
#
# For the most part, the source-distribution should include all files that
# are part of the project, with some notable exceptions (which may or may not
# apply to a derivative user packaging their own source):
#
#       [1]: Class [stiki_con_client.java] of the [db_client] package
#		[2]: Class [stiki_con.server] of the [db_server] package
#		[3]: Class [irc_output.java] of the [irc_work] package 
#		[4]: All files in the [api] package
#		[5]: All files in the [backup] package
#
# These files are all removed/redacted because they contain authentication 
# items which should not be in the public domain. In the case of the *.java
# files, the actual files have been overwitten with a compile-able,
# but non-functional version thereof.
#
# Otherwise, the process is straightforward. There are an unfortunate number
# of hard-coded paths in this script, of which users should be cautious
#
############################################################################

    # Path relating build-directory to project root, move there
PATH_TO_PROJECT_ROOT=..
SCRIPT_DIR=deploy_source/
cd $PATH_TO_PROJECT_ROOT

    # Clear out any compiled *.class files, move README & CHANGELOG to root
make clean
cp $SCRIPT_DIR''README.txt README.txt
cp misc/CHANGELOG.txt CHANGELOG.txt

    # The output ZIP name should include the build-date
CUR_DATE=`date -u +%Y_%m_%d`
ZIP_NAME='STiki_source_'$CUR_DATE'.zip'

    # Hide connection-string classes from ZIP, by replacing
    # them with compile-able, but non-functional replacements
mv db_client/stiki_con_client.java db_client/stiki_con_client.hide
mv db_server/stiki_con_server.java db_server/stiki_con_server.hide
mv irc_work/irc_output.java irc_work/irc_output.hide
cp $SCRIPT_DIR''stiki_con_client.null db_client/stiki_con_client.java
cp $SCRIPT_DIR''stiki_con_server.null db_server/stiki_con_server.java
cp $SCRIPT_DIR''irc_output.null irc_work/irc_output.java

    # Add all files to the ZIP, then handle exlusions
zip -r $ZIP_NAME *
zip -d $ZIP_NAME api/*     # delete the api bits
zip -d $ZIP_NAME api/      # (and the api folder itself)
zip -d $ZIP_NAME backup/*  # delete the backup package content
zip -d $ZIP_NAME backup/   # (and the backup folder itself)
zip -d $ZIP_NAME *.hide    # remove hidden files

    # Move the operational DB-connection-string back into place
rm db_client/stiki_con_client.java
rm db_server/stiki_con_server.java
rm irc_work/irc_output.java
mv db_client/stiki_con_client.hide db_client/stiki_con_client.java
mv db_server/stiki_con_server.hide db_server/stiki_con_server.java
mv irc_work/irc_output.hide irc_work/irc_output.java

    # Move the resulting ZIP to script directory, clean-up, rebuild
mv $ZIP_NAME $SCRIPT_DIR
rm README.txt
rm CHANGELOG.txt
make all

exit 0


#################################### END ####################################