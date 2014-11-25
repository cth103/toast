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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {

    private State state;

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
    }
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menuTimer = menu.getItem(1);
        update();
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
        if (requestCode == TimerFragment.ADD_OR_UPDATE_RULE) {
            state.addOrReplace((Rule) data.getExtras().getSerializable("rule"));
        }
        update();
    }

    private void update() {
        if (menuTimer != null) {
            menuTimer.setEnabled(state.getConnected());
        }
        adapter.getControlFragment().update();
        adapter.getTimerFragment().update();
    }
}
