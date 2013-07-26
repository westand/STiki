#!/bin/sh

#############################################################################
#
# Andrew G. West - create_jar.sh - This shell script builds the JAR which
# allows the audit tool to be easily run by an end user. It packages
# this JAR along-side a README file in a ZIP archive
#
# In order to only include files that audit-tool relevant (and not
# other STiki stuff) we use the Makefile, and only compile that aspect.
#
############################################################################

    # Path relating build-directory to project root, move there
PATH_TO_PROJECT_ROOT=..
SCRIPT_DIR=audit_tool/
cd $PATH_TO_PROJECT_ROOT

    # Clean the compilation so no *.class files exist, and then compile
    # only the audit tool, so no non-relevant code is included.
		# Manually remove some un-used dependencies.
make clean
make audit
rm db_server/*.class
rm db_client/*.class
rm edit_processing/*.class
rm irc_work/*.class

    # The output JAR name should include the build-date
CUR_DATE=`date -u +%Y_%m_%d`
JAR_NAME='WikiAudit_'$CUR_DATE'.jar'
ZIP_NAME='WikiAudit_'$CUR_DATE'.zip'

    # This is the class with the main() method that should be executed
ENTRY_POINT='audit_tool.audit'

    # Specify the files the JAR should include, build it
ALL_FILES=*/*.class				# Java class files
jar vcfe $JAR_NAME $ENTRY_POINT $ALL_FILES

    # Pack the JAR file into a ZIP, including a README & CHANGELOG
cp $SCRIPT_DIR''README.txt README.txt
cp $SCRIPT_DIR''CHANGELOG.txt CHANGELOG.txt
cp $SCRIPT_DIR''example_report.html example_report.html
zip -r $ZIP_NAME $JAR_NAME README.txt CHANGELOG.txt example_report.html
mv $ZIP_NAME $SCRIPT_DIR

    # Clean-up the project-space
rm $JAR_NAME
rm README.txt
rm CHANGELOG.txt
rm example_report.html

    # Finally, re-make the project locally
make all

exit 0


#################################### END ####################################
