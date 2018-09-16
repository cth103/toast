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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class State {

    public static final int OP_SEND_BASIC = 0x0;
    public static final int OP_SEND_ALL = 0x1;
    private static final int OP_CHANGE = 0x10;

    /** Whether or not heating is `enabled' (i.e. switched on) */
    public static final int HEATING_ENABLED = 0;
    /** Whether or not the heating in each zone is enabled */
    public static final int ZONE_HEATING_ENABLED = 1;
    /** Target for each zone */
    public static final int TARGET = 2;
    /** Whether or not the boiler is on */
    public static final int BOILER_ON = 3;
    /* Temperatures (current and historic) in each zone */
    public static final int TEMPERATURES = 4;
    /** Humidities (current and historic) in each zone */
    public static final int HUMIDITIES = 5;
    /** Rules (programmed targets at particular times) */
    public static final int RULES = 6;
    public static final int OUTSIDE_HUMIDITIES = 7;
    public static final int OUTSIDE_TEMPERATURES = 8;
    public static final int ACTUATORS = 9;
    public static final int ALL = 0xf;

    private Context context;
    /** Handlers that will be notified when there is a change in state */
    private ArrayList<Handler> handlers;

    private boolean heatingEnabled = false;
    private HashMap<String, Boolean> zoneHeatingEnabled = new HashMap<String, Boolean>();
    private HashMap<String, Double> target = new HashMap<String, Double>();
    private boolean boilerOn = false;
    private HashMap<String, ArrayList<Datum>> temperatures = new HashMap<>();
    private HashMap<String, ArrayList<Datum>> humidities = new HashMap<>();
    private ArrayList<Datum> outsideHumidities = new ArrayList<>();
    private ArrayList<Datum> outsideTemperatures = new ArrayList<>();
    private HashMap<String, HashMap<String, Boolean>> actuators = new HashMap<>();
    private ArrayList<Rule> rules = new ArrayList<>();
    private String explanation;
    private ArrayList<String> zones = new ArrayList<>();

    public State(Context context) {
        this.context = context;
        this.handlers = new ArrayList<Handler>();
    }

    public void addHandler(Handler h) {
        handlers.add(h);
    }

    private void changed(int p) {
        for (Handler h : handlers) {
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

    public synchronized HashMap<String, ArrayList<Datum>> getTemperatures() {
        return temperatures;
    }

    public synchronized HashMap<String, ArrayList<Datum>> getHumidities() {
        return humidities;
    }

    public synchronized ArrayList<Datum> getOutsideHumidities() { return outsideHumidities; }

    public synchronized ArrayList<Datum> getOutsideTemperatures() { return outsideTemperatures; }

    public synchronized ArrayList<Rule> getRules() {
        return rules;
    }

    public synchronized String getExplanation() {
        return explanation;
    }

    public synchronized byte[] getBinary(int id) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();

        if (id == HEATING_ENABLED) {
            s.write(OP_CHANGE | HEATING_ENABLED);
            s.write(heatingEnabled ? 1 : 0);
        } else if (id == ZONE_HEATING_ENABLED) {
            s.write(OP_CHANGE | ZONE_HEATING_ENABLED);
        } else if (id == TARGET) {
            s.write(OP_CHANGE | TARGET);
        } else if (id == BOILER_ON) {
            s.write(OP_CHANGE | BOILER_ON);
            s.write(boilerOn ? 1 : 0);
        }

        s.write(zones.size());
        for (String name: zones) {
            Util.putString(s, name);
        }

        if (id == ZONE_HEATING_ENABLED) {
            for (int i = 0; i < zones.size(); ++i) {
                if (zoneHeatingEnabled.containsKey(zones.get(i))) {
                    s.write(zoneHeatingEnabled.get(zones.get(i)) ? 1 : 0);
                } else {
                    s.write(0);
                }
            }
        } else if (id == TARGET) {
            for (int i = 0; i < zones.size(); ++i) {
                if (target.containsKey(zones.get(i))) {
                    Util.putFloat(s, target.get(zones.get(i)));
                } else {
                    Util.putFloat(s, 0);
                }
            }
        } else if (id == RULES) {
            /* XXX */
        }

        return s.toByteArray();
    }

    public HashMap<String, HashMap<String, Boolean>> getActuators () { return actuators; }


    /* Set */

    public synchronized void setHeatingEnabled(boolean e) {
        if (heatingEnabled != e) {
            heatingEnabled = e;
            changed(HEATING_ENABLED);
        }
    }

    public synchronized void setZoneHeatingEnabled(String zone, boolean e) {
        if (!zoneHeatingEnabled.containsKey(zone) || zoneHeatingEnabled.get(zone) != e) {
            zoneHeatingEnabled.put(zone, e);
            changed(ZONE_HEATING_ENABLED);
        }
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

    public synchronized void setOutsideHumidities(ArrayList<Datum> h) {
        outsideHumidities = h;
        changed(OUTSIDE_HUMIDITIES);
    }

    public synchronized void setOutsideTemperatures(ArrayList<Datum> t) {
        outsideTemperatures = t;
        changed(OUTSIDE_TEMPERATURES);
    }

    public synchronized void setActuators(String zone, HashMap<String, Boolean> v) {
        actuators.put(zone, v);
        changed(ACTUATORS);
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

    private int getDatumArray(byte[] data, int offset, ArrayList<Datum> d) {
        int num = Util.getInt16(data, offset);
        offset += 2;
        for (int j = 0; j < num; ++j) {
            d.add(new Datum(data, offset));
            offset += Datum.BINARY_LENGTH;
        }
        return offset;
    }

    public synchronized void setFromBinary(byte[] data) {
        int o = 0;
        final int op = data[o++];

        boolean all = op == (OP_CHANGE | ALL);

        if (all || op == (OP_CHANGE | HEATING_ENABLED)) {
            setHeatingEnabled(data[o++] == 1);
        }

        if (all || op == (OP_CHANGE | BOILER_ON)) {
            setBoilerOn(data[o++] == 1);
        }

        if (all || op == (OP_CHANGE | OUTSIDE_HUMIDITIES)) {
            ArrayList<Datum> t = new ArrayList<>();
            o = getDatumArray(data, o, t);
            setOutsideHumidities(t);
        }

        if (all || op == (OP_CHANGE | OUTSIDE_TEMPERATURES)) {
            ArrayList<Datum> t = new ArrayList<>();
            o = getDatumArray(data, o, t);
            setOutsideTemperatures(t);
        }

        int numZones = data[o++];
        zones.clear();
        for (int i = 0; i < numZones; ++i) {
            String name = Util.getString(data, o);
            o += name.length() + 1;
            zones.add(name);
        }

        for (String name : zones) {
            if (all || op == (OP_CHANGE | ZONE_HEATING_ENABLED)) {
                setZoneHeatingEnabled(name, data[o++] == 1);
            }
            if (all || op == (OP_CHANGE | TARGET)) {
                setTarget(name, Util.getFloat(data, o));
                o += 2;
            }
            if (all || op == (OP_CHANGE | TEMPERATURES)) {
                ArrayList<Datum> t = new ArrayList<>();
                o = getDatumArray(data, o, t);
                setTemperatures(name, t);
            }
            if (all || op == (OP_CHANGE | HUMIDITIES)) {
                ArrayList<Datum> t = new ArrayList<>();
                o = getDatumArray(data, o, t);
                setHumidities(name, t);
            }
            if (all || op == (OP_CHANGE | ACTUATORS)) {
                HashMap<String, Boolean> act = new HashMap<>();
                int N = data[o++];
                for (int i = 0; i < N; ++i) {
                    String actName = Util.getString(data, o);
                    o += actName.length() + 1;
                    act.put(actName, data[o++] > 0);
                }
                setActuators(name, act);
            }
        }

        if (all || op == (OP_CHANGE | RULES)) {
            int num = data[o++];
            /* XXX
            ArrayList<Rule> r = new ArrayList<>();
            for (int i = 0; i < num; ++i) {
                r.add(new Rule(data, o));
                o += Rule.binary_length;
            }
            */
        }

        if (all) {
            explanation = Util.getString(data, o);
            o += explanation.length() + 1;
        }
    }
}


