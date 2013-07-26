#############################################################################
#
# Andrew G. West - exec_backend.sh -- This simple script launches the STiki 
# backend (processing engine). Moreso than being a short-hand convenience, this
# script exists to provide root-directory visibility to the executable.
#
# Note: It is likely an end-user will want to wrap the execution of this
# script in a command like 'nohup ... & ' so that execution can occur in
# the background, and will not terminate when the session terminal closes. 
#
#############################################################################

java executables.stiki_backend_driver

#################################### END ####################################