package net.carlh.toast;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

public class GraphFragment extends Fragment {

    TemperatureFetcher fetcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        final GraphView graphView = new LineGraphView(getActivity(), "Temperature");
        graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    if (value > 45) {
                        return "An hour ago";
                    } else if (value > 15) {
                        return "Half an hour ago";
                    } else {
                        return "Now";
                    }
                }
                return null;
            }
        });

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.graphLayout);
        layout.addView(graphView);

        fetcher = new TemperatureFetcher(getActivity(), graphView);
        fetcher.addHandler(new Handler() {
            public void handleMessage(Message message) {
                graphView.removeAllSeries();
                graphView.addSeries(new GraphViewSeries(fetcher.getData()));
                graphView.setHorizontalLabels(new String[]{"An hour ago", "half an hour ago", "Now"});
            }
        });

        return view;
    }
}
