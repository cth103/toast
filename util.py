#!/usr/bin/python
#
#    Copyright (C) 2014-2018 Carl Hetherington <cth@carlh.net>
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

import sys
import json
import datetime

class Error(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return self.value
    def __repr__(self):
        return str(self)

def warning(w):
    print '%s WARNING: %s' % (datetime.datetime.now(), w)
    sys.stdout.flush()

def verbose(v):
    print '%s: %s' % (datetime.datetime.now(), v)
    sys.stdout.flush()

def send_bytearray(socket, data):
    """Send a bytearray to a socket"""
    length = bytearray(4)
    length[0] = (len(data) >> 24) & 0xff
    length[1] = (len(data) >> 16) & 0xff
    length[2] = (len(data) >>  8) & 0xff
    length[3] = (len(data) >>  0) & 0xff
    socket.sendall(length)
    socket.sendall(data)

def send_json(socket, data, verbose=False):
    """Send a dict as JSON to a socket"""
    send_bytearray(socket, json.dumps(data))
    if verbose:
        print '-> %s' % data

def get_data(sock, length):
    """recv() from a socket into a Python bytearray"""
    all = bytearray()
    remaining = length
    while remaining > 0:
        chunk = sock.recv(remaining)
        all.extend(chunk)
        remaining -= len(chunk)

    return all

def get_json(socket, verbose=False):
    """Receive some JSON from a socket"""
    s = get_bytearray(socket)
    if verbose:
        print '<- %s' % s
    return json.loads(s.decode('UTF-8'))

def get_bytearray(sock):
    """Receive a bytearray from a socket"""
    s = get_data(sock, 4)
    if len(s) < 4:
        raise Error('could not get data length from socket')

    size = (s[0] << 24) | (s[1] << 16) | (s[2] << 8) | s[3]
    s = get_data(sock, size)
    if len(s) != size:
        raise Error('could not get data from socket (got %d instead of %d)' % (len(s), size))

    return s

def receive_json(socket, verbose=False):
    """Receive some JSON from a socket"""
    s = get_data(socket, 4)
    if len(s) < 4:
        return None

    size = (s[0] << 24) | (s[1] << 16) | (s[2] << 8) | s[3]
    s = get_data(socket, size)
    if len(s) != size:
        raise Error('could not get data from socket (got %d instead of %d)' % (len(s), size))

    if verbose:
        print '<- %s' % s
    return json.loads(s.decode('UTF-8'))
