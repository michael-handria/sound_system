from socket import*
import socket
import netifaces
import sys
import os

# os.system('fuser -k 14123/tcp') #close the port if it happens to be open
os.system('cls')

HOST = "192.168.1.103"
PORT = 14123
BUFSIZ = 1024
ADDR = (HOST, PORT)

print("Hosting Server on {0}:{1}...".format(HOST,PORT))

#create a TCP socket (SOCK_STREAM)

# Socket Family (family=)
# AF_INET - 90% of sockets use this idk why

# Socket type (type=)
# SOCK_STREAM - TCP
# SOCK_DGRAM - UDP

# Protocol (proto=)
# Usually left as zero - specifies variation of protocol within the socket family
print('Creating socket...')

server_sock = socket.socket(family=AF_INET, type=SOCK_STREAM)
server_sock.bind(ADDR)
server_sock.listen(5)
server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

print('Socket created.\n')

while True:
    print('Server waiting for connection...')
    client_sock, addr = server_sock.accept()
    print('Client connected from: ', addr)

    try:
        client_connected = 1
        while True:
            data = client_sock.recv(BUFSIZ) #get data from client

            data = data.decode('utf-8')
            data = data.rstrip()
            
            print("Client: %s" %data)
            payload = input("Server: ")

            client_sock.send(payload.encode('utf-8'))
        #endwhile1
        client_sock.close()
        print('Client socket closed')
    except ConnectionResetError:
        client_sock.close()
        print("Client disconnected")
    except KeyboardInterrupt:
        client_sock.close()
        server_sock.close()
        print('\n Keyboard interrupt detected. Closing server...')
        break
    except BrokenPipeError:
        client_sock.close()
        print("Client timed out")
    #endexcept
#endwhile0
server_sock.close()
