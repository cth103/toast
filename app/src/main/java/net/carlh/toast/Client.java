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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {

    private Thread connectThread;
    /** Socket timeout in ms */
    private int timeout = 5000;
    private final Lock reconnectLock = new ReentrantLock();
    private final Condition reconnectCondition = reconnectLock.newCondition();
    /** true if all threads should stop */
    private AtomicBoolean shouldStop = new AtomicBoolean(false);
    /** true if we're fairly sure we're connected */
    private AtomicBoolean connected = new AtomicBoolean(false);
    private Thread controlThread;
    /** Lock and condition to tell the write thread that there is something to do */
    private final Lock writeLock = new ReentrantLock();
    private final Condition writeCondition = writeLock.newCondition();
    /** Things that need writing */
    private ArrayList<byte[]> toWrite = new ArrayList<>();
    /** Handler that will be told about incoming commands */
    Handler handler;

    /** Ping interval in ms (must be less than timeout) */
    private int pingInterval = 4000;

    public Client(Handler handler) {
        this.handler = handler;
    }

    public void start(final String hostName, final int port) {
        connectThread = new Thread(new Runnable() {
            Socket socket = null;
            public void run() {
                while (!shouldStop.get()) {
                    Log.e("Client", "New socket.");
                    try {
                        socket = new Socket(hostName, port);
                        socket.setSoTimeout(timeout);
                        setConnected(true);
                    } catch (IOException e) {
                        reconnectLock.lock();
                        reconnectCondition.signal();
                        reconnectLock.unlock();
                    }

                    Thread readThread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                while (!shouldStop.get()) {
                                    byte[] b = Util.getData(socket, 4);
                                    int length = ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
                                    if (length < 0 || length > (1024 * 1024)) {
                                        throw new Error("Strange block length " + length);
                                    }

                                    Message message = Message.obtain();
                                    Bundle bundle = new Bundle();
                                    bundle.putByteArray("data", Util.getData(socket, length));
                                    message.setData(bundle);
                                    Log.e("Client", "Passing data to UI");
                                    handler.sendMessage(message);
                                }
                            } catch (IOException e) {
                                reconnectLock.lock();
                                reconnectCondition.signal();
                                reconnectLock.unlock();
                            }
                        }
                    });

                    readThread.start();

                    Thread writeThread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                while (!shouldStop.get()) {
                                    writeLock.lock();
                                    try {
                                        while (toWrite.size() == 0 && !shouldStop.get()) {
                                            writeCondition.await();
                                        }
                                    } finally {
                                        writeLock.unlock();
                                    }

                                    byte[] s = null;
                                    writeLock.lock();
                                    if (toWrite.size() > 0) {
                                        s = toWrite.get(0);
                                    }
                                    writeLock.unlock();
                                    if (s != null) {
                                        socket.getOutputStream().write((s.length >> 24) & 0xff);
                                        socket.getOutputStream().write((s.length >> 16) & 0xff);
                                        socket.getOutputStream().write((s.length >> 8) & 0xff);
                                        socket.getOutputStream().write((s.length >> 0) & 0xff);
                                        socket.getOutputStream().write(s);
                                        writeLock.lock();
                                        toWrite.remove(0);
                                        writeLock.unlock();
                                    }
                                }
                            } catch (IOException e) {
                                reconnectLock.lock();
                                reconnectCondition.signal();
                                reconnectLock.unlock();
                            } catch (InterruptedException e) {
                                reconnectLock.lock();
                                reconnectCondition.signal();
                                reconnectLock.unlock();
                            }
                        }
                    });

                    writeThread.start();

                    try {
                        reconnectLock.lock();
                        reconnectCondition.await();
                        reconnectLock.unlock();
                    } catch (InterruptedException e) {
                    }

                    try {
                        readThread.interrupt();
                        readThread.join();
                        writeThread.interrupt();
                        writeThread.join();
                    } catch (InterruptedException e) {

                    }

                    try {
                        socket.close();
                    } catch (IOException e) {

                    }

                    setConnected(false);
                }
            }
        });

        connectThread.start();
    }

    public void send(byte[] data) {
        writeLock.lock();
        try {
            toWrite.add(data);
            writeCondition.signal();
        } finally {
            writeLock.unlock();
        }
    }

    public void stop() {
        shouldStop.set(true);
        if (controlThread != null   ) {
            controlThread.interrupt();
            try {
                controlThread.join();
            } catch (InterruptedException e) {

            }
        }
    }

    private void setConnected(boolean c) {
        if (c == connected.get()) {
            return;
        }

        connected.set(c);
        handler.sendEmptyMessage(0);
    }

    public boolean getConnected() {
        return connected.get();
    }
}
