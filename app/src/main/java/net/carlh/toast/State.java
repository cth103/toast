package net.carlh.toast;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class State {

    private double temperature;
    private double target;
    private boolean on;
    private boolean enabled;

    public State() {
        temperature = 0;
        target = 0;
        on = false;
        enabled = false;
    }

    public synchronized JSONObject json() {
        JSONObject json = new JSONObject();
        try {
            json.put("temperature", temperature);
            json.put("target", target);
            json.put("on", on);
            json.put("enabled", enabled);
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
        return json;
    }

    public synchronized void readJSON(JSONObject json) {
        try {
            temperature = json.getDouble("temperature");
            target = json.getDouble("target");
            on = json.getBoolean("on");
            enabled = json.getBoolean("enabled");
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
    }

    public synchronized double getTemperature() {
        return temperature;
    }

    public synchronized double getTarget() {
        return target;
    }

    public synchronized void colder() {
        target -= 0.5;
    }

    public synchronized void warmer() {
        target += 0.5;
    }

    public synchronized boolean getEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean e) {
        enabled = e;
    }

    public synchronized boolean getOn() {
        return on;
    }
}
