#!/usr/bin/env python3
# Thread that listens for commands from the speaker embeded system

import socket
import sharedMem
import os
import time
from Phone_Client_Thread import pauseSong
from Phone_Client_Thread import resumeSong

# global variables

def returnMessage(payload, spkn, client):
    print('Speaker - TCP Client #{0} Response: {1}'.format(spkn,payload))
    client.send(payload.encode('utf-8'))
#end returnMessage

def Speaker_Client(client, spkn, addr):
    global isSendingSong
    global timeOfLastPause
    # initialize variables for this file

    print('')
    print("Speaker - TCP Client #{0} connected from {1}...".format(spkn,addr))

    client.setblocking(0)

    wasPaused = True
    # when a speakers is connected, clear the buffers so that they are synced #
    if(sharedMem.isSendingSong):
        wasPaused = False
        sharedMem.isSendingSong = False
    #endif

    #enter loop for handling client
    try:
        while(sharedMem.aliveSpeakers[spkn]):
            # get speaker payload data

            try:
                data = client.recv(1)
                data = data.decode('utf-8')
                data = data.rstrip()
            except Exception as e:
                data = ' '
            #endexcept
            
            #interpret data and set return payload
            if(data == '?'):
                print('Speaker - TCP Client #{0} Payload:  {1}'.format(spkn,data))
                returnMessage('y', spkn, client)
                time.sleep(1)
                sharedMem.timeOfLastPause = time.time()
                if not wasPaused:
                    sharedMem.syncIndexes()
                    sharedMem.isSendingSong = True
                #endif
            #endif
            
            # repeat forever
        #endwhile
    except Exception as e:
        print('Speaker - TCP Client #{0} ERROR:'.format(spkn))
        print(e)
    #endexcept

    # While loop breaks out only if there is a connection error
    client.close()

    # Remove client from global lists
    sharedMem.aliveSpeakers.pop(spkn)
    sharedMem.speakerWDTs.pop(spkn)
    sharedMem.songFileIndexes.pop(spkn)

    print('Speaker - TCP Client #{0} thread closed'.format(spkn))
#endSpeaker_Client