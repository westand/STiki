#############################################################################
#
# Andrew G. West - exec_scrub_passes.sh -- A simple script that dequeues
# all edits/RIDs that have [x]+ pass actions associated with them. Pass
# accumulation is inevitable on certain edits, but given STiki's small user
# base, a large breadth/coverage is preferrable. Thus, this script, 
# presumably run on some 'cron' interval, will clear out edits with high
# multiplicity from all queues.
#
#############################################################################

java utilities.scrub_passes

#################################### END ####################################
