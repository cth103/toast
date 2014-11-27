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

public class GraphFragment extends Fragment {

    int minutes = 60;
    int dataLength = 0;
    Spinner period;
    GraphView graphView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        period = (Spinner) view.findViewById(R.id.graphPeriod);
        String periods[] = {"Last hour", "Last day", "Last week"};
        ArrayAdapter adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        period.setAdapter(adapter);
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

    void update() {
        Log.e("Toast", "GraphFragment.update()");
        if (period == null) {
            Log.e("Toast", "not doing it as period is null");
            return;
        }

        State state = getState();

        if (state != null) {
            ArrayList<Double> temperatures = state.getTemperatures();
            Log.e("Toast", temperatures.size() + " temperatures in state.");
            if (temperatures.size() > 0) {
                period.setEnabled(true);
                graphView.setVisibility(View.VISIBLE);
                graphView.removeAllSeries();
                
                dataLength = Math.min(temperatures.size(), minutes);
                GraphView.GraphViewData[] d = new GraphView.GraphViewData[dataLength];
                for (int i = 0; i < dataLength; i++) {
                    d[i] = new GraphView.GraphViewData(i, temperatures.get(temperatures.size() - dataLength + i));
                }
                
                graphView.addSeries(new GraphViewSeries(d));
            } else {
                period.setEnabled(false);
                graphView.setVisibility(View.INVISIBLE);
            }
        } else {
            Log.e("Toast", "state is null");
            period.setEnabled(false);
            graphView.setVisibility(View.INVISIBLE);
        }
    }
}
