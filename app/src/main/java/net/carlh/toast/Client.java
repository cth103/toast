/*
    Copyright (C) 2014 Carl Hetherington <cth@carlh.net>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package net.carlh.toast;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    /** True if all threads should stop */
    AtomicBoolean stop = new AtomicBoolean(false);
    /** True if we're fairly sure we're connected */
    AtomicBoolean connected = new AtomicBoolean(false);
    /** Mutex to protect changes to the value of socket */
    private final Object mutex = new Object();
    /** Socket that we are talking to the server with */
    private Socket socket;
    /** Thread to read data from socket */
    private Thread readThread;
    /** Thread to write data to socket */
    private Thread writeThread;
    /** Lock and condition to tell the write thread that there is something to do */
    private final Lock lock = new ReentrantLock();
    private final Condition writeCondition = lock.newCondition();
    /** Things that need writing */
    private ArrayList<String> toWrite = new ArrayList<String>();
    /** Thread to ping the server */
    private Thread pingThread;
    AtomicBoolean ping = new AtomicBoolean(false);
    /** True if we have received a pong reply to our last ping */
    AtomicBoolean pong = new AtomicBoolean(false);
    /** Handlers that will be told about incoming commands */
    ArrayList<Handler> handlers = new ArrayList<Handler>();

    /** Ping interval in ms (must be less than timeout) */
    private int pingInterval = 1000;
    /** Socket timeout in ms */
    private int timeout = 5000;

    public void start(final String hostName, final int port) throws java.net.UnknownHostException, java.io.IOException {

	/* Thread to read stuff from the server */
        readThread = new Thread(new Runnable() {

            private byte[] getData(Socket socket, int length) {
                byte[] d = new byte[length];
                int offset = 0;
                while (offset < length) {
                    try {
                        int t = socket.getInputStream().read(d, offset, length - offset);
                        if (t == -1) {
                            break;
                        }
                        offset += t;
                    } catch (SocketException e) {
                        /* This is probably because the socket has been closed in order to make
                           this thread terminate.
                        */
                        Log.e("Toast", "SocketException in client.getData()");
                    } catch (IOException e) {
                        Log.e("Toast", "IOException in Client.getData()", e);
                        break;
                    }
                }

                return java.util.Arrays.copyOf(d, offset);
            }

            public void run() {
                while (!stop.get()) {
                    try {
                        synchronized (mutex) {
                            /* Connect */
                            socket = new Socket(hostName, port);
                            socket.setSoTimeout(timeout);
                        }

                        /* Keep going until there is a problem on read */

                        while (true) {
                            byte[] b = getData(socket, 4);
                            if (b.length != 4) {
                                break;
                            }

                            int length = ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
                            if (length < 0 || length > (64*1024)) {
                                /* Don't like the sound of that */
                                Log.e("Toast", "Strange length " + length);
                                break;
                            }

                            byte[] d = getData(socket, length);
                            
                            if (d.length == length) {
                                try {
                                    handler(new JSONObject(new String(d)));
                                } catch (JSONException e) {
                                    Log.e("Toast", "Exception " + e.toString());
                                }
                            }
                        }

                        synchronized (mutex) {
                            /* Close the socket and go back round to connect again */
                            socket.close();
                            socket = null;
                        }

                    } catch (ConnectException e) {
                        Log.e("Toast", "ConnectException");
                        try {
                            /* Sleep a little until we try again */
                            Thread.sleep(timeout);
                        } catch (java.lang.InterruptedException f) {

                        }
                    } catch (UnknownHostException e) {
                        Log.e("Toast", "UnknownHostException");
                        try {
                            /* Sleep a little until we try again */
                            Thread.sleep(timeout);
                        } catch (java.lang.InterruptedException f) {

                        }
                    } catch (IOException e) {
                        Log.e("Client", "IOException");
                    }
                }
            }
        });

        readThread.start();

        /* Thread to send stuff to the server */
        writeThread = new Thread(new Runnable() {

            public void run() {

                while (!stop.get()) {

                    lock.lock();
                    try {
                        while (toWrite.size() == 0 && !stop.get()) {
                            writeCondition.await();
                        }
                    } catch (InterruptedException e) {

                    } finally {
                        lock.unlock();
                    }

                    String s = null;
                    lock.lock();
                    if (toWrite.size() > 0) {
                        s = toWrite.get(0);
                        toWrite.remove(0);
                    }
                    lock.unlock();

                    synchronized (mutex) {
                        try {
                            if (socket != null && s != null) {
                                socket.getOutputStream().write((s.length() >> 24) & 0xff);
                                socket.getOutputStream().write((s.length() >> 16) & 0xff);
                                socket.getOutputStream().write((s.length() >> 8) & 0xff);
                                socket.getOutputStream().write((s.length() >> 0) & 0xff);
                                socket.getOutputStream().write(s.getBytes());
                            }
                        } catch (IOException e) {
                            Log.e("Toast", "IOException in write");
                        }
                    }
                }
            }
        });

        writeThread.start();

        /* Thread to send pings every so often */
        pingThread = new Thread(new Runnable() {
            public void run() {
                while (!stop.get()) {
                    if (ping.get() == true && pong.get() == false) {
                        for (Handler h : handlers) {
                            h.sendEmptyMessage(0);
                        }
                        setConnected(false);
                    }
                    pong.set(false);
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", "ping");
                        send(json);
                        ping.set(true);
                        Thread.sleep(pingInterval);
                    } catch (JSONException e) {
                    } catch (InterruptedException e) {
                    }

                }
            }
        });

        pingThread.start();
    }

    private void handler(JSONObject json) {
        try {
            if (json.has("type") && json.get("type").equals("pong")) {
                setConnected(true);
                pong.set(true);
            } else {
                for (Handler h : handlers) {
                    Message m = Message.obtain();
                    Bundle b = new Bundle();
                    b.putString("json", json.toString());
                    m.setData(b);
                    h.sendMessage(m);
                }
            }
        } catch (JSONException e) {
        }
    }

    /** Send to server */
    public void send(JSONObject json) {
        lock.lock();
        try {
            toWrite.add(json.toString());
            writeCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        stop.set(true);

        /* Wake the write thread so it notices that we want to stop */
        lock.lock();
        try {
            writeCondition.signal();
        } finally {
            lock.unlock();
        }

        /* This interrupts blocking reads in the read thread */
        try {
            synchronized (mutex) {
                socket.close();
            }
        } catch (IOException e) {
        }

        /* Interrupt the ping thread */
        pingThread.interrupt();
    }

    /** Add a handler which will be called with an empty message
     *  when connection state changes, or a message containing
     *  a JSON block (with key "json").
     */
    void addHandler(Handler handler) {
        handlers.add(handler);
    }

    private void setConnected(boolean c) {
        if (c == connected.get()) {
            return;
        }

        connected.set(c);

        for (Handler h : handlers) {
            h.sendEmptyMessage(0);
        }
    }

    public boolean getConnected() {
        return connected.get();
    }
}

