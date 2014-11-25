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

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class State {

    /* Stuff to manage the state */

    private Context context;
    /** Handlers that will be notified when there is a change in state */
    private ArrayList<Handler> handlers;
    private final Lock lock = new ReentrantLock();
    private final Condition putCondition = lock.newCondition();
    private AtomicBoolean pendingPut = new AtomicBoolean(false);
    private HttpClient client;
    private AtomicBoolean connected = new AtomicBoolean(false);

    /* The actual state */

    private double temperature;
    private double target;
    private boolean on;
    private boolean enabled;
    private ArrayList<Rule> rules;

    public State(Context context) {
        this.context = context;
        this.handlers = new ArrayList<Handler>();

        this.temperature = 0;
        this.target = 0;
        this.on = false;
        this.enabled = false;
        this.rules = new ArrayList<Rule>();

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 3000);
        HttpConnectionParams.setSoTimeout(params, 3000);
        client = new DefaultHttpClient(params);

        /* Thread to handle get/put of our state to the server */
        Thread thread = new Thread(new Runnable() {
            public void run() {
                
                /* Initial get to start things off */
                get();
                
                while (true) {

                    try {

                        /* PUT if there is already a need, or if we are
                           woken with one during a short sleep.
                        */
                        if (!pendingPut.get()) {
                            lock.lock();
                            try {
                                putCondition.await(10, TimeUnit.SECONDS);
                            } finally {
                                lock.unlock();
                            }
                        }

                        if (pendingPut.get()) {
                            HttpPut put = new HttpPut(Util.url(State.this.context, "state"));

                            /* We must take the state from json() and set pendingPut to
                               false atomically (XXX and it isn't atomic!).  Then if
                               pendingPut is set back to true (and a new state set up)
                               we will PUT again.  If we wait until completion to reset
                               pendingPut we might have missed changes to state.
                            */
                            StringEntity entity = new StringEntity(json().toString());
                            pendingPut.set(false);
                            entity.setContentType("text/json");
                            put.setEntity(entity);
                            client.execute(put);
                        }

                        /* GET current state from the server; get() will ignore it if there
                           are any pending PUTs so that we can "safely" update our
                           local data ahead of hearing about the change from the server.
                        */
                        get();

                    } catch (IOException e) {
                        Log.e("Toast", "Exception", e);
                    } catch (InterruptedException e) {

                    }
                }
            }
        });

        thread.start();
    }

    /** Get state from the server and write it to our variables
     *  if pendingPut is false.
     */
    private void get()
    {
        try {
            HttpResponse response = client.execute(new HttpGet(Util.url(context, "state")));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                connected.set(true);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                
                if (!pendingPut.get()) {
                    readJSON(new JSONObject(out.toString()));
                }
            } else {
                connected.set(false);
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (HttpHostConnectException e) {
            connected.set(false);
            Log.e("Toast", "HttpHostConnectException in State.get()");
        } catch (ConnectTimeoutException e) {
            connected.set(false);
            Log.e("Toast", "ConnectTimeoutException in State.get()");
        } catch (SocketException e) {
            connected.set(false);
            Log.e("Toast", "SocketException in State.get()");
        } catch (SocketTimeoutException e) {
            connected.set(false);
            Log.e("Toast", "SocketTimeoutException in State.get()", e);
        } catch (IOException e) {
            Log.e("Toast", "Exception", e);
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        } finally {
            for (Handler h: handlers) {
                h.sendEmptyMessage(0);
            }
        }
    }

    public void addHandler(Handler h) {
        handlers.add(h);
    }

    private void beginUpdate() {
        pendingPut.set(true);
    }

    /** Wake the get/put thread if it is asleep */
    private void endUpdate() {
        try {
            lock.lock();
            putCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public synchronized JSONObject json() {
        JSONObject json = new JSONObject();
        try {
            json.put("temperature", temperature);
            json.put("target", target);
            json.put("on", on);
            json.put("enabled", enabled);
            JSONArray rulesJSON = new JSONArray();
            for (Rule r: rules) {
                rulesJSON.put(r.json());
            }
            json.put("rules", rulesJSON);
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
        return json;
    }

    public synchronized void readJSON(JSONObject json) {
        try {
            temperature = json.getDouble("temperature");
            target = json.getDouble("target");
            on = json.getBoolean("on");
            enabled = json.getBoolean("enabled");
            JSONArray rulesArray = json.getJSONArray("rules");
            rules.clear();
            for (int i = 0; i < rulesArray.length(); i++) {
                rules.add(new Rule(rulesArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
    }

    public boolean getConnected() {
        return connected.get();
    }

    public synchronized double getTemperature() {
        return temperature;
    }

    public synchronized double getTarget() {
        return target;
    }

    public synchronized boolean getEnabled() {
        return enabled;
    }

    public synchronized boolean getOn() {
        return on;
    }

    public synchronized ArrayList<Rule> getRules() {
        return rules;
    }

    public synchronized void colder() {
        beginUpdate();
        target -= 0.5;
        endUpdate();
    }

    public synchronized void warmer() {
        beginUpdate();
        target += 0.5;
        endUpdate();
    }

    public synchronized void setEnabled(boolean e) {
        beginUpdate();
        enabled = e;
        endUpdate();
    }

    public synchronized void addOrReplace(Rule rule) {
        beginUpdate();
        boolean done = false;
        for (Rule r: rules) {
            if (r.getId() == rule.getId()) {
                r.copyFrom(rule);
                done = true;
            }
        }

        if (!done) {
            rules.add(rule);
        }
        endUpdate();
    }

    public synchronized void remove(Rule rule) {
        beginUpdate();
        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            if (r.getId() == rule.getId()) {
                i.remove();
            }
        }
        endUpdate();
    }
}
