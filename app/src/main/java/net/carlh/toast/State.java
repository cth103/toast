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

import java.util.ArrayList;
import java.util.Iterator;

public class State {

    public static final int TARGET = 0;
    public static final int ON = 1;
    public static final int ENABLED = 2;
    public static final int RULES = 3;
    public static final int TEMPERATURES = 4;

    /** Our context */
    private Context context;
    /** Handlers that will be notified when there is a change in state */
    private ArrayList<Handler> handlers;

    private double target = 0;
    private boolean on = false;
    private boolean enabled = false;
    private ArrayList<Rule> rules = new ArrayList<Rule>();
    private ArrayList<Double> temperatures = new ArrayList<Double>();

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

    public synchronized double getTarget() {
        return target;
    }

    public synchronized boolean getOn() {
        return on;
    }

    public synchronized boolean getEnabled() {
        return enabled;
    }

    public synchronized ArrayList<Rule> getRules() {
        return rules;
    }

    public synchronized ArrayList<Double> getTemperatures() {
        return temperatures;
    }

    public synchronized void addAsJSON(JSONObject json, int id) {
        try {
            switch (id) {
                case TARGET:
                    json.put("target", target);
                    break;
                case ON:
                    json.put("on", on);
                    break;
                case ENABLED:
                    json.put("enabled", enabled);
                    break;
                case RULES:
                    JSONArray a = new JSONArray();
                    for (Rule r : rules) {
                        a.put(r.asJSON());
                    }
                    json.put("rules", a);
                    break;
                case TEMPERATURES:
                    a = new JSONArray();
                    for (Double t : temperatures) {
                        a.put(t);
                    }
                    json.put("temperatures", a);
                    break;
            }
        } catch (JSONException e) {
        }
    }


    /* Set */

    public synchronized void colder() {
        target -= 0.5;
        changed(TARGET);
    }

    public synchronized void warmer() {
        target += 0.5;
        changed(TARGET);
    }

    public synchronized void setTarget(double t) {
        if (Math.abs(target - t) > 1e-6) {
            target = t;
            changed(TARGET);
        }
    }

    public synchronized void setOn(boolean o) {
        if (on != o) {
            on = o;
            changed(ON);
        }
    }

    public synchronized void setEnabled(boolean e) {
        if (enabled != e) {
            enabled = e;
            changed(ENABLED);
        }
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

    public synchronized void setTemperatures(ArrayList<Double> t) {
        temperatures = t;
        changed(TEMPERATURES);
    }

    public synchronized void setFromJSON(JSONObject json) {
        try {
            if (json.has("target")) {
                setTarget(json.getDouble("target"));
            }
            
            if (json.has("on")) {
                setOn(json.getBoolean("on"));
            }
            
            if (json.has("enabled")) {
                setEnabled(json.getBoolean("enabled"));
            }
            
            if (json.has("rules")) {
                ArrayList<Rule> r = new ArrayList<Rule>();
                JSONArray j = json.getJSONArray("rules");
                for (int i = 0; i < j.length(); i++) {
                    r.add(new Rule(j.getJSONObject(i)));
                }
                setRules(r);
            }
            
            if (json.has("temperatures")) {
                ArrayList<Double> t = new ArrayList<Double>();
                JSONArray j = json.getJSONArray("temperatures");
                for (int i = 0; i < j.length(); i++) {
                    t.add(j.getDouble(i));
                }
                setTemperatures(t);
            }
        } catch (JSONException e) {
        }
    }
}
