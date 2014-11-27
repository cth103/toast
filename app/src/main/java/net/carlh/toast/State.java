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

import java.util.ArrayList;
import java.util.Iterator;

public class State {

    /* State identifiers */

    public final int TARGET = 0;
    public final int ON = 1;
    public final int ENABLED = 2;
    public final int RULES = 3;
    public final int TEMPERATURES = 4;

    /* Stuff to manage the state */

    private Context context;
    /** Handlers that will be notified when there is a change in state */
    private ArrayList<Handler> handlers;

    /* State */

    private double target;
    private boolean on;
    private boolean enabled;
    private ArrayList<Rule> rules;
    private ArrayList<Double> temperatures;

    public State(Context context) {
        this.context = context;
        this.handlers = new ArrayList<Handler>();

        this.target = 0;
        this.on = false;
        this.enabled = false;
        this.rules = new ArrayList<Rule>();
        this.temperatures = new ArrayList<Double>();
    }

    public void addHandler(Handler h) {
        handlers.add(h);
    }

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

    public synchronized void setFromJSON(JSONObject json) {
        if (json.has("target")) {
            if (Math.abs(target - json.getDouble("target")) > 1e-6) {
                target = json.getDouble("target");
                changed(TARGET);
            }
        } else if (json.has("on")) {
            if (json.getBoolean("on") != on) {
                on = json.getBoolean("on");
                changed(ON);
            } 
        } else if (json.has("enabled")) {
            setEnabled(enabled);
        } else if (json.has("rules")) {
            JSONArray array = json.getJSONArray("rules");
            rules.clear();
            for (int i = 0; i < array.length(); i++) {
                rules.add(new Rule(array.getJSONObject(i)));
            }
            changed(RULES);
        } else if (json.has("temperatures")) {
            JSONArray array = json.getJSONArray("temperatures");
            temperatures.clear();
            for (int i = 0; i < array.length(); i++) {
                temperatures.add(array.getDouble(i));
            }
            changed(TEMPERATURES);
        }
    }

    public synchronized void colder() {
        target -= 0.5;
        changed(TARGET);
    }

    public synchronized void warmer() {
        target += 0.5;
        changed(TARGET);
    }

    public synchronized void setEnabled(boolean e) {
        if (e != enabled) {
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

    private void changed(int p) {
        for (Handler h: handlers) {
            Message m = Message.obtain();
            Bundle b = new Bundle();
            b.putInt("property", p);
            m.setData(b);
            h.sendMessage(m);
        }
    }
}
