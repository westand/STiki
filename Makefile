#############################################################################
#
# Andrew G. West - Makefile for STiki source distribution
# 
# This Makefile is extremely over-simplified. In particular, if any *.java
# file in the project has been altered, all executables are (re)-compiled.
#
# Further, this Makefile assumes all dependencies (see README) lie on the
# default compilation class-path. If this is not the case, this file should
# be altered to provide pointers to the location of those libraries.
#
############################################################################

	# Compilers
JC = javac

	# Optimization or other option flags
OPT = -target 1.5	-source 1.5		# Everything should be Java 1.5 compatible?

	# Cleanup macro
UNMAKE = rm *.class */*.class

	# We simply group all Java files together, regardless of the 
	# specific-dependency any executable(s) hold over them.
ALL_FILES = */*.java


#######################################

all: backend frontend utilities audit

backend: $(ALL_FILES)
	@echo [Compiling STiki back-end]
	$(JC) $(OPT) executables/stiki_backend_driver.java
	@echo [STiki back-end compilation successful!]
	
frontend: $(ALL_FILES)
	@echo [Compiling STiki front-end]
	$(JC) $(OPT) executables/stiki_frontend_driver.java
	@echo [STiki front-end compilation successful!]
	
utilities: $(ALL_FILES)
	@echo [Compiling STiki utilities]
	$(JC) $(OPT) utilities/*.java
	@echo [STiki utility compilation successful!]

audit: $(ALL_FILES)
	@echo [Compiling audit tool]
	$(JC) $(OPT) audit_tool/audit.java
	@echo [Audit tool compilation successful!]

clean:
	$(UNMAKE)


#################################### END ####################################