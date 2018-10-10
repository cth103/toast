/*
    Copyright (C) 2014-2015 Carl Hetherington <cth@carlh.net>

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
import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ControlFragment extends Fragment {

    private class Zone {

        private TextView tempHum;
        private Spinner target;
        private Button heat;
        private ImageView heatIcon;
        private double temperature;
        private double humidity;

        /** Controls for a zone */
        public Zone(final String name, boolean first) {

            /* Text size */
            int size = 20;

            Activity a = ControlFragment.this.getActivity();
            TableRow r = new TableRow(a);

            /* Zone name */
            TextView l = new TextView(a);
            l.setText(name);
            l.setTextSize(size);

            TableRow.LayoutParams lp = new TableRow.LayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL;
            r.addView(l, lp);

            /* Current temperature and humidity */
            tempHum = new TextView(a);
            tempHum.setTextSize(size);
            tempHum.setGravity(Gravity.BOTTOM);
            tempHum.setPadding(24, 0, 0, 0);

            lp = new TableRow.LayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL;
            r.addView(tempHum, lp);

            /* Heat button */
            heat = new Button(a);
            heat.setBackgroundResource(R.drawable.ic_launcher);

            lp = new TableRow.LayoutParams(48, 96);
            lp.gravity = Gravity.CENTER_VERTICAL;
            r.addView(heat, lp);

            /* Target spinner */
            target = new Spinner(a);
            String[] targets = { "5°C", "17°C", "18°C", "19°C", "20°C", "21°C", "22°C" };
            ArrayAdapter<String> targetAdapter = new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, targets);
            target.setAdapter(targetAdapter);

            lp = new TableRow.LayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL;
            r.addView(target, lp);

            r.setGravity(Gravity.BOTTOM);
            r.setPadding(0, 64, 0, 0);

            ControlFragment.this.table.addView(r);
        }

        /** Set up the sensitivity of our controls */
        public void setSensitivity() {
            boolean c = ControlFragment.this.getConnected();
            tempHum.setEnabled(c);
            target.setEnabled(c);
            heat.setEnabled(c);
        }

        public void setTemperature(double t) {
            temperature = t;
            updateTempHum();
        }

        private void updateTempHum() {
            if (humidity > 0) {
                tempHum.setText(String.format("%.1f°\n%.0f%%", temperature, humidity));
            } else {
                tempHum.setText(String.format("%.1f°", temperature));
            }
        }

        public void clearTempHum() {
            tempHum.setText("");
        }

        public void setHumidity(double h) {
            humidity = h;
            updateTempHum();
        }
    }

    private TableLayout table;
    private HashMap<String, Zone> zones = new HashMap<String, Zone>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        table = (TableLayout) view.findViewById(R.id.mainTable);

        /* On second and subsequent calls we will have stuff in zones
           but no UI, so force the UI to be recreated.
         */
        zones.clear();

        update();
        return view;
    }

    /**
     * Update the UI from state
     */
    public void update() {
        if (table == null) {
            /* If table is null we'll assume onCreateView hasn't been called yet */
            return;
        }

        State state = getState();

        /* Check we have all zones that state.getTarget() mentions */
        boolean first = true;
        for (String i: state.getTarget().keySet()) {
            if (!zones.containsKey(i)) {
                zones.put(i, new Zone(i, first));
                first = false;
            }
        }

        for (Map.Entry<String, Zone> i: zones.entrySet()) {
            Zone z = i.getValue();
            i.getValue().setSensitivity();
        }

        if (getConnected()) {

            for (Map.Entry<String, ArrayList<Datum>> i: state.getTemperatures().entrySet()) {
                Zone z = zones.get(i.getKey());
                ArrayList<Datum> temps = i.getValue();
                if (z != null && temps != null && temps.size() > 0) {
                    z.setTemperature(temps.get(temps.size() - 1).value);
                }
            }

            for (Map.Entry<String, ArrayList<Datum>> i: state.getHumidities().entrySet()) {
                Zone z = zones.get(i.getKey());
                ArrayList<Datum> hums = i.getValue();
                if (z != null && hums != null && hums.size() > 0) {
                    z.setHumidity(hums.get(hums.size() - 1).value);
                }
            }

        } else {
            for (Map.Entry<String, Zone> i : zones.entrySet()) {
                Zone z = i.getValue();
                z.clearTempHum();
            }

            if (getActivity() != null) {
                ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (!cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                    WifiManager wm = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    wm.setWifiEnabled(true);
                }
            }
        }
    }
};
