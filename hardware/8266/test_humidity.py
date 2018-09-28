#!/usr/bin/python

import socket
import time

while True:
	sock = socket.socket()
	sock.connect(('192.168.1.67', 9142))
        sock.send('humidity\r\n')
        print sock.recv(64)
	time.sleep(3)


