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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

/** Fragment to plot graphs of temperature */
public class GraphFragment extends Fragment {

    private Date startTime;
    private Date endTime = new Date();
    /** Zone spinner */
    private Spinner zone;
    /** Period spinner */
    private Spinner period;
    private Graph graph;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        startTime = new Date(endTime.getTime() - 60 * 60 * 1000);

        zone = (Spinner) view.findViewById(R.id.graphZone);
        checkZones();
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
                String[] xLabels;
                switch (position) {
                    case 0:
                        endTime = new Date();
                        startTime = new Date(endTime.getTime() - 60 * 60 * 1000);
                        xLabels = new String[5];
                        xLabels[0] = "1h ago";
                        xLabels[1] = "45m ago";
                        xLabels[2] = "30m ago";
                        xLabels[3] = "15m ago";
                        xLabels[4] = "Now";
                        graph.setXLabels(xLabels);
                        graph.setXDivisions(4);
                        break;
                    case 1:
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        startTime = cal.getTime();
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        endTime = cal.getTime();
                        xLabels = new String[] {
                                "Midnight",
                                "6am",
                                "Noon",
                                "6pm",
                                "Midnight"
                        };
                        graph.setXLabels(xLabels);
                        graph.setXDivisions(4);
                        break;
                    case 2:
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                        startTime = cal.getTime();
                        cal.add(Calendar.DAY_OF_MONTH, 7);
                        endTime = cal.getTime();
                        xLabels = new String[] {
                                "Mo",
                                "Tu",
                                "Wed",
                                "Th",
                                "Fr",
                                "Sat",
                                "Su",
                                ""
                        };
                        graph.setXLabels(xLabels);
                        graph.setXDivisions(7);
                        break;
                }
                update();
            }

            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        graph = (Graph) view.findViewById(R.id.graph);
        update();

        return view;
    }

    private void checkZones() {
        if (zone.getAdapter() != null && zone.getAdapter().getCount() > 0) {
            /* Already set up */
            return;
        }

        Map<String, ArrayList<Datum>> temps = getState().getTemperatures();
        ArrayAdapter zoneAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, temps.keySet().toArray(new String[0]));
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zone.setAdapter(zoneAdapter);
    }


    private ArrayList<Graph.Point> getSmoothedGraphData(ArrayList<Datum> data) {
        if (data == null) {
            return null;
        }

        ArrayList<Graph.Point> graphData = new ArrayList<>();
        ArrayList<Double> maf = new ArrayList<>();
        final int mafLength = 5;

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
                graphData.add(new Graph.Point(i.time.getTime() - startTime.getTime(), (float) v));
            }
        }

        return graphData;
    }

    private ArrayList<Graph.Point> getSparseGraphData(ArrayList<Datum> data) {
        if (data == null) {
            return null;
        }

        ArrayList<Graph.Point> graphData = new ArrayList<>();
        double lastBefore = 0;
        double firstAfter = 0;
        for (Datum i: data) {
            if (i.time.compareTo(startTime) < 0) {
                /* before range */
                lastBefore = i.value;
                firstAfter = i.value;
            } else if (startTime.compareTo(i.time) <= 0 && i.time.compareTo(endTime) < 0) {
                /* in range */
                graphData.add(new Graph.Point(i.time.getTime() - startTime.getTime(), (float) i.value));
                if (lastBefore == 0) {
                    lastBefore = i.value;
                }
                firstAfter = i.value;
            } else if (endTime.compareTo(i.time) < 0) {
                /* after range */
                if (lastBefore == 0) {
                    lastBefore = i.value;
                }
                firstAfter = i.value;
            }
        }

        graphData.add(0, new Graph.Point(0, (float) lastBefore));
        graphData.add(new Graph.Point(endTime.getTime() - startTime.getTime(), (float) firstAfter));

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
            return;
        }

        checkZones();

        period.setEnabled(true);

        ArrayList<Datum> temps = state.getTemperatures().get(zone.getSelectedItem());
        graph.setData(Datum.TYPE_TEMPERATURE, getSmoothedGraphData(temps));
        ArrayList<Datum> hums = state.getHumidities().get(zone.getSelectedItem());
        graph.setData(Datum.TYPE_HUMIDITY, getSmoothedGraphData(hums));

        graph.setData(Datum.TYPE_OUTSIDE_TEMPERATURE, getSparseGraphData(state.getOutsideTemperatures()));
        graph.setData(Datum.TYPE_OUTSIDE_HUMIDITY, getSparseGraphData(state.getOutsideHumidities()));

        graph.setTimeRange(endTime.getTime() - startTime.getTime());
    }
}

