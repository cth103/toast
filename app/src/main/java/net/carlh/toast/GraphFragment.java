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
import java.util.Calendar;

public class GraphFragment extends Fragment {

    TemperatureFetcher fetcher;
    int minutes = 60;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        Spinner period = (Spinner) view.findViewById(R.id.graphPeriod);
        String periods[] = {"Last hour", "Last day", "Last week"};
        period.setAdapter(new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, periods));
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

                Log.e("Test", "Set period " + minutes);
                fetcher.setPeriod(minutes);
                fetcher.fetchNow();
            }

            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        final GraphView graphView = new LineGraphView(getActivity(), "Temperature");
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

                c.add(Calendar.MINUTE, (int) (- fetcher.getData().length + value));
                return f.format(c.getTime());
            }
        });

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.graph);
        layout.addView(graphView);

        fetcher = new TemperatureFetcher(getActivity(), graphView);
        fetcher.addHandler(new Handler() {
            public void handleMessage(Message message) {
                graphView.removeAllSeries();
                graphView.addSeries(new GraphViewSeries(fetcher.getData()));
            }
        });

        return view;
    }
}
