package net.carlh.toast;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class ControlFragment extends Fragment {

    private TextView temperature;
    private TextView target;
    private ToggleButton enabled;
    private Button warmer;
    private Button colder;
    private TextView on;
    private TextView explanation;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        temperature = (TextView) view.findViewById(R.id.temperature);
        target = (TextView) view.findViewById(R.id.target);
        enabled = (ToggleButton) view.findViewById(R.id.enabled);
        colder = (Button) view.findViewById(R.id.colder);
        warmer = (Button) view.findViewById(R.id.warmer);
        on = (TextView) view.findViewById(R.id.on);
        explanation = (TextView) view.findViewById(R.id.explanation);

        /* Any changes to these widgets are reflected instantly
           in the UI by update().  State handles pushing of the
           new values to the server and suspending UI updates
           until the corresponding pull comes back.
        */

        final State state = getState();

        colder.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state.colder();
                update();
            }
        });

        warmer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state.warmer();
                update();
            }
        });

        enabled.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                state.setEnabled(!getState().getEnabled());
                update();
            }
        });

        update();
        return view;
    }

    /** Update the UI from state */
    public void update() {
        if (temperature == null) {
            /* temperature is the first variable to be set in onCreateView
               so if it's null we'll assume onCreateView hasn't been called
               yet.
             */
            return;
        }

        State state = getState();
        if (state == null) {
            return;
        }

        enabled.setEnabled(state.getConnected());
        temperature.setEnabled(state.getConnected());
        target.setEnabled(state.getConnected());
        target.setEnabled(state.getConnected() && state.getEnabled());
        warmer.setEnabled(state.getConnected() && state.getEnabled());
        colder.setEnabled(state.getConnected() && state.getEnabled());

        if (state.getConnected()) {
            ArrayList<Double> temperatures = state.getTemperatures();
            if (temperatures != null && temperatures.size() > 0) {
                temperature.setText(String.format("%.1f°", temperatures.get(temperatures.size() - 1)));
            }
            target.setText(String.format("%.1f°", state.getTarget()));
            enabled.setChecked(state.getEnabled());
        } else {
            temperature.setText("...");
            target.setText("...");
            enabled.setText("");
        }
        
        if (state.getConnected() && state.getOn()) {
            on.setText("Boiler is on");
            explanation.setText("");
        } else if (state.getConnected() && !state.getOn()) {
            on.setText("Boiler is off");
            if (state.getEnabled()) {
                explanation.setText("Target temperature reached");
            } else {
                explanation.setText("Heating is switched off");
            }
        } else {
            on.setText("Not connected");
            explanation.setText("Check that you have a WiFi connection");
        }
    }
};
