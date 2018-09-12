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
import java.io.OutputStream;
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

    private Thread commsThread;
    private Thread pingThread;
    /** Socket timeout in ms */
    private int timeout = 5000;
    private int pingInterval = 1000;
    private final Lock reconnectLock = new ReentrantLock();
    private final Condition reconnectCondition = reconnectLock.newCondition();
    /** true if all threads should stop */
    private AtomicBoolean shouldStop = new AtomicBoolean(false);
    /** true if we're fairly sure we're connected */
    private AtomicBoolean connected = new AtomicBoolean(false);
    /** Lock and condition to tell the write thread that there is something to do */
    private final Lock writeLock = new ReentrantLock();
    private final Condition writeCondition = writeLock.newCondition();
    /** Things that need writing */
    private ArrayList<byte[]> toWrite = new ArrayList<>();
    /** Handler that will be told about incoming messages */
    Handler handler;

    public Client(Handler handler) {
        this.handler = handler;
    }

    public void start(final String hostName, final int port) {
        commsThread = new Thread(new Runnable() {

            Socket socket = null;

            public void run() {
                while (!shouldStop.get()) {
                    try {
                        /* Connect */
                        try {
                            socket = new Socket(hostName, port);
                            socket.setSoTimeout(timeout);
                        } catch (IOException e) {
                            connected.set(false);
                            throw e;
                        }

                        connected.set(true);

                        /* Wait for something to send */
                        writeLock.lock();
                        try {
                            while (toWrite.size() == 0 && !shouldStop.get()) {
                                writeCondition.await();
                            }
                        } finally {
                            writeLock.unlock();
                        }

                        /* Send it */
                        writeLock.lock();
                        if (toWrite.isEmpty()) {
                            writeLock.unlock();
                            continue;
                        }
                        byte[] s = toWrite.get(0);
                        writeLock.unlock();
                        OutputStream os = socket.getOutputStream();
                        Log.e("Client", "Sending msg of len " + s.length + " first byte " + ((int) s[0]));
                        os.write((s.length >> 24) & 0xff);
                        os.write((s.length >> 16) & 0xff);
                        os.write((s.length >> 8) & 0xff);
                        os.write((s.length >> 0) & 0xff);
                        os.write(s);
                        os.flush();
                        writeLock.lock();
                        toWrite.remove(0);
                        writeLock.unlock();

                        /* Read the reply */
                        byte[] b = Util.getData(socket, 4);
                        int length = ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
                        if (length < 0 || length > (1024 * 1024)) {
                            throw new Error("Strange block length " + length);
                        }

                        Message message = Message.obtain();
                        Bundle bundle = new Bundle();
                        bundle.putByteArray("data", Util.getData(socket, length));
                        message.setData(bundle);
                        handler.sendMessage(message);
                    } catch(InterruptedException e){

                    } catch(IOException e){

                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {

                        }
                    }
                }
            }
        });

        /* XXX this was to cope with the fact that the whole history
           takes a long time to parse: may not be a problem with
           binary transfer. */
        send(new byte[]{State.OP_SEND_BASIC});
        send(new byte[]{State.OP_SEND_ALL});
        commsThread.start();

        /* Start a thread to request updates every so often */
        pingThread = new Thread(new Runnable() {
           public void run() {
               while (!shouldStop.get()) {
                   try {
                       Thread.sleep(pingInterval);
                   } catch (InterruptedException e) {

                   }
                   sendIfNotPending(State.OP_SEND_ALL);
               }
           }
        });

        pingThread.start();
    }

    public void sendImmediate(byte[] data) {
        writeLock.lock();
        try {
            toWrite.add(0, data);
            writeCondition.signal();
        } finally {
            writeLock.unlock();
        }
    }

    public void send(byte[] data) {
        writeLock.lock();
        try {
            Log.e("Client", "add a message type " + data[0] + "; " + toWrite.size() + " to send.");
            toWrite.add(data);
            writeCondition.signal();
        } finally {
            writeLock.unlock();
        }
    }

    public void sendIfNotPending(int request) {
        writeLock.lock();
        for (byte[] b: toWrite) {
            if (b[0] == request) {
                writeLock.unlock();
                return;
            }
        }
        writeLock.unlock();
        send(new byte[]{(byte) request});
    }

    public void stop() {
        shouldStop.set(true);
        if (commsThread != null) {
            commsThread.interrupt();
            try {
                commsThread.join();
            } catch (InterruptedException e) {

            }
        }
        if (pingThread != null) {
            pingThread.interrupt();
            try {
                pingThread.join();
            } catch (InterruptedException e) {

            }
        }
    }

    public boolean getConnected() {
        return connected.get();
    }
}
