/*
    Copyright (C) 2014 Carl Hetherington <cth@carlh.net>

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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class State {

    /** Whether or not heating is `enabled' (i.e. switched on) */
    public static final int HEATING_ENABLED = 0;
    /** Whether or not the heating in each zone is enabled */
    public static final int ZONE_HEATING_ENABLED = 1;
    /** Target for each zone */
    public static final int TARGET = 2;
    /** Whether or not the boiler is on */
    public static final int BOILER_ON = 3;
    /** Temperatures (current and historic) in each zone */
    public static final int TEMPERATURES = 4;
    /** Humidities (current and historic) in each zone */
    public static final int HUMIDITIES = 5;
    /** Rules (programmed targets at particular times) */
    public static final int RULES = 6;

    /** Our context */
    private Context context;
    /** Handlers that will be notified when there is a change in state */
    private ArrayList<Handler> handlers;

    private boolean heatingEnabled = false;
    private HashMap<String, Boolean> zoneHeatingEnabled = new HashMap<String, Boolean>();
    private HashMap<String, Double> target = new HashMap<String, Double>();
    private boolean boilerOn = false;
    private HashMap<String, ArrayList<Datum> > temperatures = new HashMap<>();
    private HashMap<String, ArrayList<Datum> > humidities = new HashMap<>();
    private ArrayList<Rule> rules = new ArrayList<Rule>();
    private String explanation;

    public State(Context context) {
        this.context = context;
        this.handlers = new ArrayList<Handler>();
    }

    public void addHandler(Handler h) {
        handlers.add(h);
    }

    private void changed(int p) {
        for (Handler h: handlers) {
            Message m = Message.obtain();
            Bundle b = new Bundle();
            b.putInt("property", p);
            m.setData(b);
            h.sendMessage(m);
        }
    }

    /* Get */

    public synchronized boolean getHeatingEnabled() {
        return heatingEnabled;
    }

    public synchronized HashMap<String, Boolean> getZoneHeatingEnabled() {
        return zoneHeatingEnabled;
    }

    public synchronized HashMap<String, Double> getTarget() {
        return target;
    }

    public synchronized boolean getBoilerOn() {
        return boilerOn;
    }

    public synchronized HashMap<String, ArrayList<Datum> > getTemperatures() {
        return temperatures;
    }

    public synchronized HashMap<String, ArrayList<Datum> > getHumidities() {
        return humidities;
    }

    public synchronized ArrayList<Rule> getRules() {
        return rules;
    }

    public synchronized String getExplanation() {
        return explanation;
    }

    /** Add that state which goes from client to server as JSON */
    public synchronized void addAsJSON(JSONObject json, int id) {
        try {
            switch (id) {
                case HEATING_ENABLED:
                    json.put("heating_enabled", heatingEnabled);
                    break;
                case ZONE_HEATING_ENABLED: {
                    JSONArray a = new JSONArray();
                    for (Map.Entry<String, Boolean> i : zoneHeatingEnabled.entrySet()) {
                        JSONObject o = new JSONObject();
                        o.put("zone", i.getKey());
                        o.put("zone_heating_enabled", i.getValue());
                        a.put(o);
                    }
                    json.put("zone_heating_enabled", a);
                    break;
                }
                case TARGET: {
                    JSONArray a = new JSONArray();
                    for (Map.Entry<String, Double> i : target.entrySet()) {
                        JSONObject o = new JSONObject();
                        o.put("zone", i.getKey());
                        o.put("target", i.getValue());
                        a.put(o);
                    }
                    json.put("target", a);
                    break;
                }
                case BOILER_ON:
                    json.put("boiler_on", boilerOn);
                    break;
                case TEMPERATURES:
                {
                    JSONArray a = new JSONArray();
                    for (Map.Entry<String, ArrayList<Datum>> i : temperatures.entrySet()) {
                        JSONObject o = new JSONObject();
                        o.put("zone", i.getKey());
                        JSONArray t = new JSONArray();
                        for (Datum j : i.getValue()) {
                            t.put(j.asJSONArray());
                        }
                        o.put("temperatures", t);
                        a.put(o);
                    }
                    json.put("temperatures", a);
                    break;
                }
                case HUMIDITIES:
                {
                    JSONArray a = new JSONArray();
                    for (Map.Entry<String, ArrayList<Datum>> i : humidities.entrySet()) {
                        JSONObject o = new JSONObject();
                        o.put("zone", i.getKey());
                        JSONArray t = new JSONArray();
                        for (Datum j : i.getValue()) {
                            t.put(j.asJSONArray());
                        }
                        o.put("humidities", t);
                        a.put(o);
                    }
                    json.put("humidities", a);
                    break;
                }
                case RULES: {
                    JSONArray a = new JSONArray();
                    for (Rule r : rules) {
                        a.put(r.asJSON());
                    }
                    json.put("rules", a);
                    break;
                }
            }
        } catch (JSONException e) {
        }
    }


    /* Set */

    public synchronized void setHeatingEnabled(boolean e) {
        if (heatingEnabled != e) {
            heatingEnabled = e;
            changed(HEATING_ENABLED);
        }
    }

    public synchronized void setZoneHeatingEnabled(String zone, boolean e) {
        zoneHeatingEnabled.put(zone, e);
        changed(ZONE_HEATING_ENABLED);
    }

    public synchronized void setTarget(String zone, double t) {
        if (!target.containsKey(zone) || Math.abs(target.get(zone) - t) > 1e-6) {
            target.put(zone, t);
            changed(TARGET);
        }
    }

    public synchronized void colder(String zone) {
        target.put(zone, target.get(zone) - 0.5);
        changed(TARGET);
    }

    public synchronized void warmer(String zone) {
        target.put(zone, target.get(zone) + 0.5);
        changed(TARGET);
    }

    public synchronized void setBoilerOn(boolean o) {
        if (boilerOn != o) {
            boilerOn = o;
            changed(BOILER_ON);
        }
    }

    public synchronized void setTemperatures(String zone, ArrayList<Datum> t) {
        temperatures.put(zone, t);
        changed(TEMPERATURES);
    }

    public synchronized void setHumidities(String zone, ArrayList<Datum> t) {
        humidities.put(zone, t);
        changed(HUMIDITIES);
    }

    public synchronized void addOrReplace(Rule rule) {
        boolean done = false;
        for (Rule r : rules) {
            if (r.getId() == rule.getId()) {
                r.copyFrom(rule);
                done = true;
            }
        }

        if (!done) {
            rules.add(rule);
        }

        changed(RULES);
    }

    public synchronized void remove(Rule rule) {
        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            if (r.getId() == rule.getId()) {
                i.remove();
            }
        }

        changed(RULES);
    }

    public synchronized void setRules(ArrayList<Rule> r) {
        if (rules != r) {
            rules = r;
            changed(RULES);
        }
    }

    public synchronized void setFromJSON(JSONObject json) {
        try {
            if (json.has("heating_enabled")) {
                setHeatingEnabled(json.getBoolean("heating_enabled"));
            }

            if (json.has("zone_heating_enabled")) {
                JSONArray zones = json.getJSONArray("zone_heating_enabled");
                for (int i = 0; i < zones.length(); i++) {
                    JSONObject o = zones.getJSONObject(i);
                    setZoneHeatingEnabled(o.getString("zone"), o.getBoolean("zone_heating_enabled"));
                }
            }

            if (json.has("target")) {
                JSONArray j = json.getJSONArray("target");
                for (int i = 0; i < j.length(); i++) {
                    setTarget(j.getJSONObject(i).getString("zone"), j.getJSONObject(i).getDouble("target"));
                }
            }

            if (json.has("boiler_on")) {
                setBoilerOn(json.getBoolean("boiler_on"));
            }

            if (json.has("temperatures")) {
                JSONArray zones = json.getJSONArray("temperatures");
                for (int i = 0; i < zones.length(); i++) {
                    ArrayList<Datum> t = new ArrayList<>();
                    JSONArray k = zones.getJSONObject(i).getJSONArray("temperatures");
                    for (int j = 0; j < k.length(); j++) {
                        t.add(new Datum(k.getJSONArray(j)));
                    }
                    setTemperatures(zones.getJSONObject(i).getString("zone"), t);
                }
            }

            if (json.has("humidities")) {
                JSONArray zones = json.getJSONArray("humidities");
                for (int i = 0; i < zones.length(); i++) {
                    ArrayList<Datum> t = new ArrayList<>();
                    JSONArray k = zones.getJSONObject(i).getJSONArray("humidities");
                    for (int j = 0; j < k.length(); j++) {
                        t.add(new Datum(k.getJSONArray(j)));
                    }
                    setHumidities(zones.getJSONObject(i).getString("zone"), t);
                }
            }

            if (json.has("rules")) {
                ArrayList<Rule> r = new ArrayList<Rule>();
                JSONArray j = json.getJSONArray("rules");
                for (int i = 0; i < j.length(); i++) {
                    r.add(new Rule(j.getJSONObject(i)));
                }
                setRules(r);
            }

            if (json.has("explanation")) {
                explanation = json.getString("explanation");
            }

        } catch (JSONException e) {
            Log.e("toast", "JSON exception was thrown.");
        } catch (ParseException e) {
            Log.e("toast", "Date parse exception was thrown: " + e);
        }
    }
}
