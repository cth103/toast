package net.carlh.toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.Runnable;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

public class MainActivity extends ActionBarActivity {

    /* State of the system as we see it.  This variable's
       reference should not be changed (i.e. do not
       do "state = new State();"
    */
    private State state = new State();

    /* Lock and condition to tell the comms thread when
       it needs to PUT our state to the server.
    */
    private final Lock lock = new ReentrantLock();
    private final Condition putCondition = lock.newCondition();
    private AtomicInteger pendingPuts = new AtomicInteger(0);

    private AtomicBoolean connected = new AtomicBoolean(false);

    private Handler handler;

    private HttpClient client = new DefaultHttpClient();

    private TextView temperature;
    private TextView target;
    private Button enabled;
    private Button warmer;
    private Button colder;
    private TextView on;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature = (TextView) findViewById(R.id.temperature);
        target = (TextView) findViewById(R.id.target);
        enabled = (Button) findViewById(R.id.enabled);
        colder = (Button) findViewById(R.id.colder);
        warmer = (Button) findViewById(R.id.warmer);
        on = (TextView) findViewById(R.id.on);

        colder.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pendingPuts.incrementAndGet();
                state.colder();
                update();
                requestPut();

            }
        });

        warmer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pendingPuts.incrementAndGet();
                state.warmer();
                update();
                requestPut();
            }
        });

        enabled.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pendingPuts.incrementAndGet();
                state.setEnabled(!state.getEnabled());
                update();
                requestPut();
            }
        });

        /* Handler to update the UI when the comms thread has
           done a GET.
        */
        handler = new Handler() {
            public void handleMessage(Message message) {
                update();
            }
        };

        Thread thread = new Thread(new Runnable() {
            public void run() {



                /* Initial get to start things off */
                get();

                while (true) {

                    try {

			            /* PUT if there is already a need, or if we are
                           woken during a short sleep.
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
                            HttpPut put = new HttpPut(url("state"));
                            StringEntity entity = new StringEntity(state.json().toString());
                            entity.setContentType("text/json");
                            put.setEntity(entity);
                            client.execute(put);
                            pendingPuts.decrementAndGet();
                        }

                        /* GET current state from the server, but ignore it if there
                           are any pending PUTs so that we can "safely" update our
                           local thread ahead of hearing about it from the server.
                        */
                        get();

                    } catch (IOException e) {
                        Log.e("Toast", "Exception", e);
                    } catch (InterruptedException e) {

                    }
                }
            }

        }

        );

        thread.start();
    }

    private void get()
    {
        try {
            HttpResponse response = client.execute(new HttpGet(url("state")));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                connected.set(true);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                
                if (pendingPuts.get() == 0) {
                    state.readJSON(new JSONObject(out.toString()));
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
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void update() {

        temperature.setEnabled(connected.get());
        target.setEnabled(connected.get());
        target.setEnabled(connected.get() && state.getEnabled());
        warmer.setEnabled(connected.get() && state.getEnabled());
        colder.setEnabled(connected.get() && state.getEnabled());

        if (connected.get()) {
            temperature.setText(String.format("%.1f°", state.getTemperature()));
            target.setText(String.format("%.1f°", state.getTarget()));
            if (state.getEnabled()) {
                enabled.setText("ON");
            } else {
                enabled.setText("OFF");
            }
        } else {
            temperature.setText("...");
            target.setText("...");
            enabled.setText("");
        }
        
        if (connected.get() && state.getOn()) {
            on.setText("Boiler is on");
        } else if (connected.get() && !state.getOn()) {
            on.setText("Boiler is off");
        } else {
            on.setText("Not connected");
        }
    }

    private void requestPut() {
        try {
            lock.lock();
            putCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    private String url(String request) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return "http://" + prefs.getString("hostname", "192.168.1.1") + "/" + request;
    }
}
