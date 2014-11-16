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
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class State {

    private Context context;
    private ArrayList<Handler> handlers;
    private final Lock lock = new ReentrantLock();
    private final Condition putCondition = lock.newCondition();
    private AtomicInteger pendingPuts = new AtomicInteger(0);
    private HttpClient client = new DefaultHttpClient();
    private AtomicBoolean connected = new AtomicBoolean(false);

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
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                
                /* Initial get to start things off */
                get();
                
                while (true) {

                    try {
                        
                        /* PUT if there is already a need, or if we are
                           woken with one during a short sleep.
                        */
                        if (pendingPuts.get() == 0) {
                            lock.lock();
                            try {
                                putCondition.await(10, TimeUnit.SECONDS);
                            } finally {
                                lock.unlock();
                            }
                        }

                        if (pendingPuts.get() > 0) {
                            HttpPut put = new HttpPut(Util.url(State.this.context, "state"));
                            StringEntity entity = new StringEntity(json().toString());
                            entity.setContentType("text/json");
                            put.setEntity(entity);
                            client.execute(put);
                            pendingPuts.decrementAndGet();
                        }

                        /* GET current state from the server, but ignore it if there
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
                
                if (pendingPuts.get() == 0) {
                    readJSON(new JSONObject(out.toString()));
                }
            } else {
                connected.set(false);
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (HttpHostConnectException e) {
            connected.set(false);
            Log.e("Toast", "Exception", e);
        } catch (SocketException e) {
            connected.set(false);
            Log.e("Toast", "Exception", e);
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
        pendingPuts.incrementAndGet();
    }

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
