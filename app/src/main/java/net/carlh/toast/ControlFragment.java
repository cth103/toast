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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ControlFragment extends Fragment {

    private class Zone {

        private TextView temperature;
        private TextView humidity;
        private CheckBox target;
        private Button warmer;
        private Button colder;

        /** Controls for a zone */
        public Zone(final String name, boolean first) {

            /* Text size */
            int size = 20;

            Activity a = ControlFragment.this.getActivity();

            /* Zone name and current temperature / humidity */
            {
                TableRow r = new TableRow(a);

                TextView l = new TextView(a);
                l.setText(name);
                l.setTextSize(size);
                l.setPadding(32, first ? 32 : 0, 0, 0);
                l.setTypeface(null, Typeface.BOLD);

                ImageView iv = new ImageView(a);
                iv.setImageResource(R.drawable.ic_fan);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

                LinearLayout state = new LinearLayout(a);
                state.addView(l);
                state.addView(iv);
                state.setGravity(Gravity.BOTTOM);
                //iv.setVisibility(View.GONE);

                LinearLayout.LayoutParams linear_params = (LinearLayout.LayoutParams) iv.getLayoutParams();
                linear_params.height = 64;
                iv.setLayoutParams(linear_params);

                r.addView(state);

                TableRow.LayoutParams table_params = (TableRow.LayoutParams) state.getLayoutParams();
                table_params.span = 2;
                state.setLayoutParams(table_params);

                temperature = new TextView(a);
                temperature.setTextSize(size);
                temperature.setTypeface(null, Typeface.BOLD);
                r.addView(temperature);

                humidity = new TextView(a);
                humidity.setTextSize(size - 4);
                r.addView(humidity);

                r.setGravity(Gravity.BOTTOM);

                ControlFragment.this.table.addView(r);
            }

            /* Target (with enable) / warmer / colder */
            {
                TableRow r = new TableRow(a);

                target = new CheckBox(a);
                r.addView(target);

                warmer = new Button(a);
                warmer.setText("Warmer");
                r.addView(warmer);

                colder = new Button(a);
                colder.setText("Colder");
                r.addView(colder);

                TableRow.LayoutParams params = (TableRow.LayoutParams) colder.getLayoutParams();
                params.span = 2;
                colder.setLayoutParams(params);

                ControlFragment.this.table.addView(r);
            }

            target.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ControlFragment.this.getState().setZoneHeatingEnabled(name, target.isChecked());
                }
            });

            colder.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ControlFragment.this.getState().colder(name);
                }
            });

            warmer.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ControlFragment.this.getState().warmer(name);
                }
            });
        }

        /** Set up the sensitivity of our controls */
        public void setSensitivity() {
            boolean c = ControlFragment.this.getConnected();
            boolean e = ControlFragment.this.getHeatingEnabled();
            temperature.setEnabled(c);
            humidity.setEnabled(c);
            target.setEnabled(c && e);
            warmer.setEnabled(c && e && target.isChecked());
            colder.setEnabled(c && e && target.isChecked());
        }

        public void setZoneEnabled(boolean e) {
            target.setChecked(e);
        }

        public void setTarget(double t) {
            target.setText(String.format("%.1f°", t));
        }

        public void clearTarget() {
            target.setText("");
        }

        public void setTemperature(double t) {
            temperature.setText(String.format("%.1f°", t));
        }

        public void clearTemperature() {
            temperature.setText("");
        }

        public void setHumidity(double h) {
            humidity.setText(String.format("%.0f%%", h));
        }

        public void clearHumidity() {
            humidity.setText("");
        }
    }

    private ToggleButton heatingEnabled;
    private TextView boilerOn;
    private TextView explanation;
    private TableLayout table;
    private HashMap<String, Zone> zones = new HashMap<String, Zone>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        heatingEnabled = (ToggleButton) view.findViewById(R.id.enabled);
        boilerOn = (TextView) view.findViewById(R.id.on);
        explanation = (TextView) view.findViewById(R.id.explanation);
        table = (TableLayout) view.findViewById(R.id.mainTable);

        final State state = getState();

        heatingEnabled.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state.setHeatingEnabled(!getState().getHeatingEnabled());
            }
        });

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
        if (heatingEnabled == null) {
            /* heatingEnabled is the first variable to be set in onCreateView
               so if it's null we'll assume onCreateView hasn't been called
               yet.
             */
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

        heatingEnabled.setEnabled(getConnected());
        for (Map.Entry<String, Zone> i: zones.entrySet()) {
            Zone z = i.getValue();
            i.getValue().setSensitivity();
        }

        if (getConnected()) {

            heatingEnabled.setChecked(getHeatingEnabled());

            for (Map.Entry<String, Boolean> i: state.getZoneHeatingEnabled().entrySet()) {
                Log.e("test", "getting zone called " + i.getKey());
                zones.get(i.getKey()).setZoneEnabled(i.getValue());
            }

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

            for (Map.Entry<String, Double> i: state.getTarget().entrySet()) {
                Zone z = zones.get(i.getKey());
                if (z != null) {
                    z.setTarget(i.getValue());
                }
            }

            if (state.getBoilerOn()) {
                boilerOn.setText("Boiler is on");
            } else {
                boilerOn.setText("Boiler is off");
            }

            explanation.setText(state.getExplanation());
        } else {
            for (Map.Entry<String, Zone> i : zones.entrySet()) {
                Zone z = i.getValue();
                z.clearTemperature();
                z.clearHumidity();
                z.clearTarget();
            }
            heatingEnabled.setText("");

            if (getActivity() != null) {
                ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (!cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                    WifiManager wm = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    wm.setWifiEnabled(true);
                }
            }

            boilerOn.setText("Connecting...");
            explanation.setText("");
        }
    }

    private boolean getHeatingEnabled() {
        return getState() != null && getState().getHeatingEnabled();
    }
};
