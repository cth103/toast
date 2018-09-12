#!/usr/bin/python
#
#    Copyright (C) 2014-2015 Carl Hetherington <cth@carlh.net>
#
#    This program is free software; you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation; either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program; if not, write to the Free Software
#    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#

import threading
import socket
import traceback
import sys
import datetime
import os

import util

class Server:
    def __init__(self, port):
        # Port number to listen on
        self.port = port

    # Start the server
    def start(self):
        for res in socket.getaddrinfo(None, self.port, socket.AF_UNSPEC, socket.SOCK_STREAM, 0, socket.AI_PASSIVE):
            af, socktype, proto, canonname, sa = res
            try:
                s = socket.socket(af, socktype, proto)
            except socket.error as msg:
                s = None
                continue

            try:
                s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                s.bind(sa)
                s.listen(1)
            except socket.error as msg:
                s.close()
                s = None
                continue

            break

        if s is None:
            raise util.Error('could not open socket (%s)' % msg)

        self.accept_thread = threading.Thread(target=self.run, args=(s,))
        self.accept_thread.start()

    # Thread which listens for incoming connections
    def run(self, sock):
        while True:
            conn, addr = sock.accept()
            conn.settimeout(10.0)
            threading.Thread(target=self.client, args=(conn,)).start()

    # Thread to handle one client session
    def client(self, conn):
        try:
            while True:
                data = util.get_bytearray(conn)
                if len(data) == 0:
                    break
                reply = self.handler(data)
                if reply is not None:
                    util.send_bytearray(conn, reply)
        except Exception as e:
            util.warning('Server handler threw "%s"' % e)
            traceback.print_exc(file=sys.stdout)
            pass

        conn.close()
