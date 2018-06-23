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

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Fragment to plot graphs of temperature */
public class GraphFragment extends Fragment {

    /**
     * Number of minutes to plot
     */
    private int minutes = 60;
    /**
     * Number of minutes that we have data for
     */
    private int dataLength = 0;
    /**
     * Zone spinner
     */
    private Spinner zone;
    /**
     * Period spinner
     */
    private Spinner period;
    /**
     * The graph
     */
    private GraphView graphView;
    private LineGraphSeries<DataPoint> temperatureSeries;
    private LineGraphSeries<DataPoint> humiditySeries;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        zone = (Spinner) view.findViewById(R.id.graphZone);
        Map<String, ArrayList<Datum>> temps = getState().getTemperatures();
        ArrayAdapter zoneAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, temps.keySet().toArray(new String[0]));
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
        ArrayAdapter periodAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, periods);
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

        graphView = new GraphView(getActivity());

        /* Format the x axis with times or dates */
        graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (!isValueX) {
                    return super.formatLabel(value, isValueX);
                }

                return "";
            }
        });

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.graph);
        layout.addView(graphView);

        update();
        return view;
    }

    private DataPoint[] getDataPoints(ArrayList<Datum> data) {
        dataLength = Math.min(data.size(), minutes);
        DataPoint[] graphData = new DataPoint[dataLength];
        ArrayList<Double> maf = new ArrayList<Double>();
        final int mafLength = 5;
        for (int i = 0; i < dataLength; i++) {
            double v = data.get(data.size() - dataLength + i).value;
            maf.add(v);
            if (maf.size() > mafLength) {
                maf.remove(0);
                double total = 0;
                for (Double d: maf) {
                    total += d;
                }
                v = total / mafLength;
            }
            graphData[i] = new DataPoint(i, v);
        }
        return graphData;
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

        period.setEnabled(true);
        graphView.setVisibility(View.VISIBLE);

        ArrayList<Datum> temperatureData = state.getTemperatures().get(zone.getSelectedItem());
        if (temperatureData != null) {
            DataPoint[] temperatureDataPoints = getDataPoints(temperatureData);
            if (temperatureSeries != null) {
                temperatureSeries.resetData(temperatureDataPoints);
            } else {
                temperatureSeries = new LineGraphSeries<>(temperatureDataPoints);
                graphView.addSeries(temperatureSeries);
            }
        }

        ArrayList<Datum> humidityData = state.getHumidities().get(zone.getSelectedItem());
        if (humidityData != null) {
            DataPoint[] humidityDataPoints = getDataPoints(humidityData);
            if (humiditySeries != null) {
                humiditySeries.resetData(humidityDataPoints);
            } else {
                humiditySeries = new LineGraphSeries<>(humidityDataPoints);
                humiditySeries.setColor(Color.GREEN);
                graphView.getSecondScale().addSeries(humiditySeries);
            }
            double minVal = Double.MAX_VALUE;
            double maxVal = Double.MIN_VALUE;
            for (DataPoint i: humidityDataPoints) {
                minVal = Math.min(i.getY(), minVal);
                maxVal = Math.max(i.getY(), maxVal);
            }
            graphView.getSecondScale().setMinY(minVal);
            graphView.getSecondScale().setMaxY(maxVal);
        }
    }
}
