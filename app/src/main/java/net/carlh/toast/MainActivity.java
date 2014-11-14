package net.carlh.toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.Runnable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Message;
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

    private Handler handler;

    private HttpClient client = new DefaultHttpClient();

    private TextView temperature;
    private TextView target;
    private Button onOff;
    private Button colder;
    private Button warmer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature = (TextView) findViewById(R.id.temperature);
        target = (TextView) findViewById(R.id.target);
        onOff = (Button) findViewById(R.id.onOff);
        colder = (Button) findViewById(R.id.colder);
        warmer = (Button) findViewById(R.id.warmer);

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

        onOff.setOnClickListener(new View.OnClickListener() {
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
                            HttpPut put = new HttpPut("http://192.168.1.1/state");
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
            HttpResponse response = client.execute(new HttpGet("http://192.168.1.1/state"));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                
                if (pendingPuts.get() == 0) {
                    state.readJSON(new JSONObject(out.toString()));
                    handler.sendEmptyMessage(0);
                }
            } else {
                Log.e("Toast", "Request failed");
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            Log.e("Toast", "Exception", e);
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void update() {
        temperature.setText(String.format("%.1f°", state.getTemperature()));
        target.setText(String.format("%.1f°", state.getTarget()));
        target.setEnabled(state.getEnabled());
        warmer.setEnabled(state.getEnabled());
        colder.setEnabled(state.getEnabled());
        if (state.getEnabled()) {
            onOff.setText("Switch heating off");
        } else {
            onOff.setText("Switch heating on");
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
}
