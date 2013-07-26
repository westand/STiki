#!/bin/sh
#
###############################################################################
#
# Andrew G. West - svm_learn.sh - Straightforward SVM (regression) learning
# over a set of training edits.
#
##############################################################################

############## FILENAMES ##############

        # Base-directory relative to which file handling should occur
    BASE_DIR=/home/westand/PreSTA_STiki/learn_svm

        # These filenames must agree with those in [svm_classify.sh], and
        # those in the Java class responsible for writing training sets.
    TRAIN_FILE=$BASE_DIR/train_set.txt
    MODEL_FILE=$BASE_DIR/model.txt
    
        # This file name is not dependent. Change it to anything!
    LOG_FILE=$BASE_DIR/train_log.txt
    
    
############# PARAMETERS ##############
   
  # Options to the SVM training routine:
  # -z: {c,r} Whether to "classify" or perform "regression" 
  # -t: Type of kernel function (2 = RBF)
  # -c: Trade of between training error and margin
  # -g: Gamma parameter of the RBF kernel
  # -j: Cost factor (FP control)
  # -e: Error acceptable for routine termination
  # -m: Cache size for kernel evaluations (in MB)
  # -v: Verbosity level of output
  
  
############## INDUCTIVE ##############

    TRAIN_OPTS="-z r -t 2 -j 8.0 -c 10 -g 8 -e 0.05 -m 1000"
    svm_learn $TRAIN_OPTS $TRAIN_FILE $MODEL_FILE > $LOG_FILE
    
    
############## CLEAN-UP ###############

        # Impossible to clean-up much:
    # rm $LOG_FILE -- let persist to for debugging purposes


##################################### END ####################################
