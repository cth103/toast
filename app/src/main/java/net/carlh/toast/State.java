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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class State {

    /** Our context */
    private Context context;
    /** Handlers that will be notified when there is a change in state */
    private ArrayList<Handler> handlers;

    private ArrayList<PropertyBase> properties = new ArrayList<PropertyBase>();
    private DoubleProperty target;
    private BooleanProperty on;
    private BooleanProperty enabled;
    private ListProperty<Rule> rules;
    private ListProperty<Double> temperatures;

    public State(Context context) {
        this.context = context;
        this.handlers = new ArrayList<Handler>();

        target = new DoubleProperty("target", 0.0);
        on = new BooleanProperty("on", false);
        enabled = new BooleanProperty("enabled", false);
        rules = new ListProperty<Rule>("rules");
        temperatures = new ListProperty<Double>("temperatures");

        properties.add(target);
        properties.add(on);
        properties.add(enabled);
        properties.add(rules);
        properties.add(temperatures);
    }

    public void addHandler(Handler h) {
        handlers.add(h);
    }

    public synchronized double getTarget() {
        return target.get();
    }

    public synchronized boolean getOn() {
        return on.get();
    }

    public synchronized boolean getEnabled() {
        return enabled.get();
    }

    public synchronized ArrayList<Rule> getRules() {
        return rules.get();
    }

    public synchronized ArrayList<Double> getTemperatures() {
        return temperatures.get();
    }

    public synchronized void setFromJSON(JSONObject json) {
        for (PropertyBase p: properties) {
            if (p.set(json)) {
                changed(p.getId());
            }
        }
    }

    public synchronized void colder() {
        target.set(target.get() - 0.5);
        changed(target.getId());
    }

    public synchronized void warmer() {
        target.set(target.get() + 0.5);
        changed(target.getId());
    }

    public synchronized void setEnabled(boolean e) {
        if (enabled.set(e)) {
            changed(enabled.getId());
        }
    }

    public synchronized void addOrReplace(Rule rule) {
        boolean done = false;
        for (Rule r : rules.get()) {
            if (r.getId() == rule.getId()) {
                r.copyFrom(rule);
                done = true;
            }
        }

        if (!done) {
            rules.add(rule);
        }

        changed(rules.getId());
    }

    public synchronized void remove(Rule rule) {

        ArrayList<Rule> rulesCopy = rules.get();
        for (Iterator<Rule> i = rulesCopy.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            if (r.getId() == rule.getId()) {
                i.remove();
            }
        }

        rules.set(rulesCopy);
        changed(rules.getId());
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

    public void addAsJSON(JSONObject json, int id) {
        for (PropertyBase p: properties) {
            if (p.getId() == id) {
                p.addAsJSON(json);
            }
        }
    }
}
