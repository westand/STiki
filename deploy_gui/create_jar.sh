#!/bin/sh

#############################################################################
#
# Andrew G. West - create_jar.sh - This shell script builds the JAR which
# allows the front-end STiki GUI to be easily run by an end user. It packages
# this JAR along-side a README file in a ZIP archive
#
# The dependencies of dealing with external libraries is non-trivial. Thus,
# we have created a single external JAR [libraries_combined.jar] which takes
# all the dependencies and nicely packages them into a single one which
# may be added to the class-path when the STiki-JAR is built. The licensing
# of these external's is handled in the included README.
#
# In order to only include files that are front-end relevant (and not
# back-end stuff) we use the Makefile, and only compile that aspect.
#
# Otherwise, the process is straightforward. This script performs all
# actions relative to the project root directory -- and copies the resulting
# JAR back to the directory where this script is located.
#
############################################################################

    # Path relating build-directory to project root, move there
PATH_TO_PROJECT_ROOT=..
SCRIPT_DIR=deploy_gui/
cd $PATH_TO_PROJECT_ROOT

    # Clean the compilation so no *.class files exist, and then compile
    # only the frontend-aspect, so no non-relevant code is included
make clean
make frontend

    # Due to an unused dependency, it seems SERVER version DB connectivity
    # is included in the GUI jar, this should be stripped out explicitly.
rm db_server/*.class

    # The output JAR name should include the build-date
CUR_DATE=`date -u +%Y_%m_%d`
JAR_NAME='STiki_'$CUR_DATE'.jar'
ZIP_NAME='STiki_exec_'$CUR_DATE'.zip'

    # This is the class with the main() method that should be executed
ENTRY_POINT='executables.stiki_frontend_driver'

    # Add the external JAR classes onto STiki project-space
EXTERNAL_LIBS=$SCRIPT_DIR''combined_libraries.jar
echo $EXTERNAL_LIBS
jar xf $EXTERNAL_LIBS

    # Specify the files the STiki-JAR should include:
JAVA_FILES=*/*.class                    # Java class files
ICON_FILES=gui_support/icons/*.png      # STiki icons and logos
HELP_FILES=gui_menus/stiki_help.html    # Help file
DEPENDS=com/*' 'org/*                   # External library contributions
    
    # Wrap these up into a single variables, build the JAR
ALL_FILES=$JAVA_FILES' '$ICON_FILES' '$HELP_FILES' '$DEPENDS
jar vcfe $JAR_NAME $ENTRY_POINT $ALL_FILES

    # Pack the JAR file into a ZIP, including a README & CHANGELOG
cp $SCRIPT_DIR''README.txt README.txt
cp misc/CHANGELOG.txt CHANGELOG.txt
zip -r $ZIP_NAME $JAR_NAME README.txt CHANGELOG.txt
mv $ZIP_NAME $SCRIPT_DIR

    # Clean-up the project-space
rm -rf com/ org/ META-INF/
rm $JAR_NAME
rm README.txt
rm CHANGELOG.txt

    # Finally, re-make the project locally
make all

exit 0


#################################### END ####################################
