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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

/** Fragment to plot graphs of temperature */
public class GraphFragment extends Fragment {

    /** Number of minutes to plot */
    private int minutes = 60;
    /** Number of minutes that we have data for */
    private int dataLength = 0;
    /** Zone spinner */
    private Spinner zone;
    /** Period spinner */
    private Spinner period;
    /** The graph */
    private GraphView graphView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        zone = (Spinner) view.findViewById(R.id.graphZone);
        Map<String, ArrayList<Double> > temps = getState().getTemperatures();
        ArrayAdapter zoneAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, temps.keySet().toArray());
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zone.setAdapter(zoneAdapter);
        zone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                update();
            }

            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        period = (Spinner) view.findViewById(R.id.graphPeriod);
        String periods[] = {"Last hour", "Last day", "Last week"};
        ArrayAdapter periodAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        period.setAdapter(periodAdapter);
        period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                switch (position) {
                    case 0:
                        minutes = 60;
                        break;
                    case 1:
                        minutes = 24 * 60;
                        break;
                    case 2:
                        minutes = 7 * 24 * 60;
                        break;
                }
                update();
            }

            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        graphView = new LineGraphView(getActivity(), "Temperature");
        graphView.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.abc_text_size_small_material));
        graphView.setScalable(true);

        /* Format the x axis with times or dates */
        graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (!isValueX) {
                    return null;
                }

                Calendar c = Calendar.getInstance();

                SimpleDateFormat f = null;
                if (minutes <= (60 * 24)) {
                    f = new SimpleDateFormat("K:mm a");
                } else {
                    f = new SimpleDateFormat("E K:mm a");
                }

                c.add(Calendar.MINUTE, (int) (value - dataLength));
                return f.format(c.getTime());
            }
        });

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.graph);
        layout.addView(graphView);

        update();
        return view;
    }

    public void update() {
        if (period == null) {
            /* The UI hasn't been created yet */
            return;
        }

        State state = getState();

        if (state == null || state.getTemperatures().size() == 0) {
            period.setEnabled(false);
            graphView.setVisibility(View.INVISIBLE);
            return;
        }

        ArrayList<Double> temperatures = state.getTemperatures().get(zone.getSelectedItem());
        period.setEnabled(true);
        graphView.setVisibility(View.VISIBLE);
        graphView.removeAllSeries();

        dataLength = Math.min(temperatures.size(), minutes);
        GraphView.GraphViewData[] data = new GraphView.GraphViewData[dataLength];
        ArrayList<Double> maf = new ArrayList<Double>();
        final int mafLength = 5;
        for (int i = 0; i < dataLength; i++) {
            double v = temperatures.get(temperatures.size() - dataLength + i);
            maf.add(v);
            if (maf.size() > mafLength) {
                maf.remove(0);
                double total = 0;
                for (Double d: maf) {
                    total += d;
                }
                v = total / mafLength;
            }
            data[i] = new GraphView.GraphViewData(i, v);
        }

        graphView.addSeries(new GraphViewSeries(data));
    }
}
