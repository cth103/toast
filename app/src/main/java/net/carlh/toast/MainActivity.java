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

import java.io.IOException;
import java.util.List;

    public class MainActivity extends FragmentActivity {

    private State state;
    private Client client;

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

        /* State: this must update the UI and the server
           when it changes.
        */

        state = new State(this);
        state.addHandler(new Handler() {
            public void handleMessage(Message message) {

                /* Send the change to the server (unless it's something
                   that only goes server -> client).
                */
                int property = message.getData().getInt("property");
                if (property != State.TEMPERATURES && property != State.HUMIDITIES && client != null) {
                    client.send(state.getBinary(property));
                }

                /* Update the whole UI */
                update();
            }
        });

        startClient();
    }

    /* Must be called from UI thread */
    private void stopClient() {
        if (client == null) {
            return;
        }

        client.stop();
        client = null;
    }

    /* Must be called from UI thread */
    private void startClient() {
        if (client != null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /* Client: this must update the State when it receives new data from the server.
           We also need to know when the client connects or disconnects.
        */

        client = new Client(
                new Handler() {
                    public void handleMessage(Message message) {
                        Bundle data = message.getData();

                        if (data != null && data.getByteArray("data") != null) {
                            /* Some state changed */
                            state.setFromBinary(data.getByteArray("data"));
                        } else {
                            /* Connected or disconnected */
                            if (getConnected()) {
                                /* Newly connected: ask the server to tell us the basics
                                   and then the full temperature history.
                                   XXX this was to cope with the fact that the whole history
                                   takes a long time to parse: may not be a problem with
                                   binary transfer.
                                */
                                client.send(new byte[]{State.OP_SEND_BASIC});
                                client.send(new byte[]{State.OP_SEND_ALL});
                            }
                        }
                    }
                });

        client.start(prefs.getString("hostname", "192.168.1.1"), Integer.parseInt(prefs.getString("port", "80")));
    }

    public static class Adapter extends FragmentPagerAdapter {

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public int getCount() {
            return 3;
        }

        public Fragment getItem(int position) {
            switch (position) {
            case 0:
                return new ControlFragment();
            case 1:
                return new TimerFragment();
            case 2:
                return new GraphFragment();
            }

            return null;
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

    @Override
    protected void onResume() {
        super.onResume();
        startClient();
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

    private void update() {
        if (menuTimer != null && state != null) {
            menuTimer.setEnabled(getConnected());
        }

        FragmentManager manager = getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                net.carlh.toast.Fragment tf = (net.carlh.toast.Fragment) f;
                if (tf != null && tf.isVisible()) {
                    tf.update();
                }
            }
        }
    }

    /* Must be called from the UI thread */
    public boolean getConnected() {
        if (client != null) {
            return client.getConnected();
        } else {
            return false;
        }
    }
}
