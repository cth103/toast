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
import android.widget.ToggleButton;

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

    /* This variable's reference should not be changed (i.e. do not do
       "state = new State());"
    */
    private static State state;

    private TextView temperature;
    private TextView target;
    private ToggleButton enabled;
    private Button warmer;
    private Button colder;
    private TextView on;
    private TextView explanation;

    static State getState() {
        return state;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        state = new State(this);

        temperature = (TextView) findViewById(R.id.temperature);
        target = (TextView) findViewById(R.id.target);
        enabled = (ToggleButton) findViewById(R.id.enabled);
        colder = (Button) findViewById(R.id.colder);
        warmer = (Button) findViewById(R.id.warmer);
        on = (TextView) findViewById(R.id.on);
        explanation = (TextView) findViewById(R.id.explanation);

        colder.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state.colder();
                update();
            }
        });

        warmer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state.warmer();
                update();
            }
        });

        enabled.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state.setEnabled(!state.getEnabled());
                update();
            }
        });

        state.addHandler(new Handler() {
            public void handleMessage(Message message) {
                update();
            }
        });
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

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_timer) {
            startActivity(new Intent(this, TimerActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void update() {
        temperature.setEnabled(state.getConnected());
        target.setEnabled(state.getConnected());
        target.setEnabled(state.getConnected() && state.getEnabled());
        warmer.setEnabled(state.getConnected() && state.getEnabled());
        colder.setEnabled(state.getConnected() && state.getEnabled());

        if (state.getConnected()) {
            temperature.setText(String.format("%.1f°", state.getTemperature()));
            target.setText(String.format("%.1f°", state.getTarget()));
            enabled.setChecked(state.getEnabled());
        } else {
            temperature.setText("...");
            target.setText("...");
            enabled.setText("");
        }
        
        if (state.getConnected() && state.getOn()) {
            on.setText("Boiler is on");
            explanation.setText("");
        } else if (state.getConnected() && !state.getOn()) {
            on.setText("Boiler is off");
            if (state.getEnabled()) {
                explanation.setText("Target temperature reached");
            } else {
                explanation.setText("Heating is switched off");
            }
        } else {
            on.setText("Not connected");
            explanation.setText("Check that you have a WiFi connection");
        }
    }

}
