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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends FragmentActivity {

    private State state;
    /* XXX: needs locking */
    private Client client;
    private boolean connected = false;

    public State getState() {
        return state;
    }
    
    private MenuItem menuTimer;
    private ViewPager pager;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new Adapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        state = new State(this);
        state.addHandler(new Handler() {
            public void handleMessage(Message message) {
                update();
            }
        });

        state.addHandler(new Handler() {
            public void handleMessage(Message message) {
                if (client == null) {
                    return;
                }

                JSONObject json = new JSONObject();
                json.set("type", "change");

                switch (message.getData().getInt("property")) {
                case State.TARGET:
                    json.set("target", state.getTarget());
                    break;
                case State.ON:
                    json.set("on", state.getOn());
                    break;
                case State.ENABLED:
                    json.set("enabled", state.getEnabled());
                    break;
                case State.RULES:
                    JSONArray array = new JSONArray();
                    ArrayList<Rule> rules = state.getRules();
                    for (int i = 0; i < rules.size(); i++) {
                        array.insert(i, rules[i].json());
                    }
                    json.set("rules", array);
                    break;
                case State.TEMPERATURES:
                    JSONArray array = new JSONArray();
                    ArrayList<Double> temperatures = state.getTemperatures();
                    for (int i = 0; i < temperatures.size(); i++) {
                        array.insert(i, temperatures[i]);
                    }
                    json.set("temperatures", array);
                    break;
                }

                client.send(json);
            }
        });

        startClient();
    }

    private void stopClient() {
        if (client != null) {
            client.stop();
            client = null;
        }
    }

    private void startClient() {
        stopClient();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            client = new Client(prefs.getString("hostname", "192.168.1.1"), Integer.parseInt(prefs.getString("port", "80")));
            client.addHandler(new Handler() {
                public void handleMessage(Message message) {
                    Bundle data = message.getData();
                    if (data != null && data.getString("json") != null) {
                        /* We have received some JSON from the server */
                        try {
                            Log.e("Toast", "Received " + data.getString("json"));
                            state.setFromJSON(new JSONObject(data.getString("json"));
                        } catch (JSONException e) {
                        }
                    } else {
                        /* Empty messages mean that the connection state has changed */
                        update();
                    }
                }
            });
        } catch (IOException e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClient();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopClient();
        startClient();
    }

    public static class Adapter extends FragmentPagerAdapter {

        private ControlFragment controlFragment = new ControlFragment();
        private TimerFragment timerFragment = new TimerFragment();
        private GraphFragment graphFragment = new GraphFragment();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public int getCount() {
            return 3;
        }

        public Fragment getItem(int position) {
            if (position == 0) {
                return controlFragment;
            } else if (position == 1) {
                return timerFragment;
            } else {
                return graphFragment;
            }
        }

        public ControlFragment getControlFragment() {
            return controlFragment;
        }

        public TimerFragment getTimerFragment() {
            return timerFragment;
        }

        public GraphFragment getGraphFragment() {
            return graphFragment;
        }
    }
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menuTimer = menu.getItem(1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_timer) {
            pager.setCurrentItem(1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TimerFragment.ADD_OR_UPDATE_RULE && data != null) {
            state.addOrReplace((Rule) data.getExtras().getSerializable("rule"));
        }
    }

    private void update() {
        if (menuTimer != null && state != null) {
            menuTimer.setEnabled(getConnected());
        }
        adapter.getControlFragment().update();
        adapter.getTimerFragment().update();
        adapter.getGraphFragment().update();
    }

    public boolean getConnected() {
        if (client != null) {
            return client.getConnected();
        } else {
            return false;
        }
    }
}
