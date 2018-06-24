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
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
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
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Fragment to plot graphs of temperature */
public class GraphFragment extends Fragment {

    private Date startTime;
    private Date endTime = new Date();
    /** Zone spinner */
    private Spinner zone;
    /** Period spinner */
    private Spinner period;
    /** The graph */
    private GraphView graphView;
    private LineGraphSeries<DataPoint> temperatureSeries;
    private LineGraphSeries<DataPoint> humiditySeries;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        startTime = new Date(endTime.getTime() - 60 * 60 * 1000);

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
        String periods[] = {"Last hour", "Today", "This week"};
        ArrayAdapter periodAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, periods);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        period.setAdapter(periodAdapter);
        period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Calendar cal = new GregorianCalendar();
                switch (position) {
                    case 0:
                        endTime = new Date();
                        startTime = new Date(endTime.getTime() - 60 * 60 * 1000);
                        break;
                    case 1:
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        startTime = cal.getTime();
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        endTime = cal.getTime();
                        break;
                    case 2:
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.DAY_OF_WEEK, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        startTime = cal.getTime();
                        cal.add(Calendar.DAY_OF_MONTH, 7);
                        endTime = cal.getTime();
                        break;
                }
                update();
            }

            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        graphView = new GraphView(getActivity());
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(5);
        graphView.getViewport().setMaxY(25);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        temperatureSeries = null;
        humiditySeries = null;
    }

    private DataPoint[] getDataPoints(ArrayList<Datum> data) {
        /* Count how many data points are less than minutes old */
        int dataLength = 0;
        for (Datum i: data) {
            if (startTime.compareTo(i.time) <= 0 && i.time.compareTo(endTime) < 0) {
                ++dataLength;
            }
        }

        DataPoint[] graphData = new DataPoint[dataLength];
        ArrayList<Double> maf = new ArrayList<>();
        final int mafLength = 5;

        int j = 0;
        String s = "";
        for (Datum i: data) {
            if (startTime.compareTo(i.time) <= 0 && i.time.compareTo(endTime) < 0) {
                double v = i.value;
                maf.add(v);
                if (maf.size() > mafLength) {
                    maf.remove(0);
                    double total = 0;
                    for (Double d: maf) {
                        total += d;
                    }
                    v = total / mafLength;
                }
                graphData[j] = new DataPoint(TimeUnit.MILLISECONDS.toSeconds(i.time.getTime() - startTime.getTime()), v);
                ++j;
            }
        }

        Log.e("toast", s);

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
        if (temperatureData != null && temperatureData.size() > 0) {
            DataPoint[] temperatureDataPoints = getDataPoints(temperatureData);
            if (temperatureSeries != null) {
                temperatureSeries.resetData(temperatureDataPoints);
            } else {
                temperatureSeries = new LineGraphSeries<>(temperatureDataPoints);
                graphView.addSeries(temperatureSeries);
            }
        }

        ArrayList<Datum> humidityData = state.getHumidities().get(zone.getSelectedItem());
        if (humidityData != null && humidityData.size() > 0) {
            DataPoint[] humidityDataPoints = getDataPoints(humidityData);
            if (humiditySeries != null) {
                humiditySeries.resetData(humidityDataPoints);
            } else {
                humiditySeries = new LineGraphSeries<>(humidityDataPoints);
                humiditySeries.setColor(Color.GREEN);
                graphView.getSecondScale().addSeries(humiditySeries);
            }
        } else {
            graphView.getSecondScale().removeAllSeries();
            humiditySeries = null;
        }

        graphView.getSecondScale().setMinY(0);
        graphView.getSecondScale().setMaxY(100);

        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(TimeUnit.MILLISECONDS.toSeconds(endTime.getTime() - startTime.getTime()) - 1);
    }
}
