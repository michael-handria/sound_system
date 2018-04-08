#!/usr/bin/env python3

#file storing global variable names to be used by different threads
aliveSpeakers      = {} # {speakerNumber : isAlive}
speakerAddresses   = {} # {'##.##.##.##' : speakerNumber}
speakerWDTs        = {} # {speakerNumber : LastAliveTime}
isSendingSong = False
songToSend = ' '
songFileIndex = 44

# starting values of global variables
def init():
    speakerWDTs      = {}
    aliveSpeakers    = {}
    speakerAddresses = {}
    isSendingSong = False
    songToSend = ' '
    songFileIndex = 44