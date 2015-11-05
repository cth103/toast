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
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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
        private CheckBox target;
        private Button warmer;
        private Button colder;

        /** Controls for a zone */
        public Zone(final String name, boolean first) {

            /* Text size */
            int size = 20;

            Activity a = ControlFragment.this.getActivity();

            /* Zone name and current temperature */
            {
                TableRow r = new TableRow(a);

                TextView l = new TextView(a);
                l.setText(name);
                l.setTextSize(size);
                l.setPadding(32, first ? 32 : 0, 0, 0);
                l.setTypeface(null, Typeface.BOLD);
                r.addView(l);

                TableRow.LayoutParams params = (TableRow.LayoutParams) l.getLayoutParams();
                params.span = 2;
                l.setLayoutParams(params);

                temperature = new TextView(a);
                temperature.setTextSize(size);
                temperature.setTypeface(null, Typeface.BOLD);
                r.addView(temperature);

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

                ControlFragment.this.table.addView(r);
            }

            target.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ControlFragment.this.getState().setZoneEnabled(name, target.isChecked());
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

            for (Map.Entry<String, Boolean> i: state.getZoneEnabled().entrySet()) {
                zones.get(i.getKey()).setZoneEnabled(i.getValue());
            }

            for (Map.Entry<String, ArrayList<Double>> i: state.getTemperatures().entrySet()) {
                Zone z = zones.get(i.getKey());
                ArrayList<Double> temps = i.getValue();
                if (z != null && temps != null && temps.size() > 0) {
                    z.setTemperature(temps.get(temps.size() - 1));
                }
            }

            for (Map.Entry<String, Double> i: state.getTarget().entrySet()) {
                Zone z = zones.get(i.getKey());
                if (z != null) {
                    z.setTarget(i.getValue());
                }
            }

            boolean zone_enabled = false;
            for (Map.Entry<String, Boolean> i: getState().getZoneEnabled().entrySet()) {
                if (i.getValue()) {
                    zone_enabled = true;
                }
            }

            if (state.getBoilerOn()) {
                boilerOn.setText("Boiler is on");
                if (!zone_enabled || !getHeatingEnabled()) {
                    explanation.setText("Heating to programmed target");
                }
            } else {
                boilerOn.setText("Boiler is off");
                if (getHeatingEnabled()) {
                    if (zone_enabled) {
                        explanation.setText("Target temperatures reached");
                    } else {
                        explanation.setText("All rooms switched off");
                    }
                } else {
                    explanation.setText("Heating is switched off");
                }
            }
        } else {
            for (Map.Entry<String, Zone> i : zones.entrySet()) {
                Zone z = i.getValue();
                z.clearTemperature();
                z.clearTarget();
            }
            heatingEnabled.setText("");
            boilerOn.setText("Not connected");
            explanation.setText("Check that you have a WiFi connection");
        }
    }

    private boolean getHeatingEnabled() {
        return getState() != null && getState().getHeatingEnabled();
    }
};
