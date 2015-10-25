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

import json

class Error(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return self.value
    def __repr__(self):
        return str(self)

def warning(w):
    print 'WARNING: %s' % w

def verbose(v):
    print v

def send_json(socket, data):
    """Send a dict as JSON to a socket"""
    s = json.dumps(data)
    length = bytearray(4)
    length[0] = (len(s) >> 24) & 0xff
    length[1] = (len(s) >> 16) & 0xff
    length[2] = (len(s) >>  8) & 0xff
    length[3] = (len(s) >>  0) & 0xff
    socket.sendall(length)
    socket.sendall(s)
    verbose('-> %s' % data)

def get_data(sock, length):
    """Get some data from a socket"""
    all = ""
    got = 0
    while got < length:
        d = sock.recv(length - got)
        if not d:
            break
        all += d
        got += len(d)

    return all

def receive_json(socket):
    """Receive some JSON from a socket"""
    s = get_data(socket, 4)
    if len(s) < 4:
        return None

    size = (ord(s[0]) << 24) | (ord(s[1]) << 16) | (ord(s[2]) << 8) | ord(s[3])
    s = get_data(socket, size)
    if len(s) != size:
        raise Error('could not get data from socket (got %d instead of %d)' % (len(s), size))

    verbose('<- %s' % s)
    return json.loads(s);
