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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class State {

    public static final int  OP_SEND_BASIC = 0x00;
    public static final int  OP_SEND_ALL   = 0x01;
    private static final int OP_CHANGE     = 0x80;
    /** Temperatures (current and historic) in each zone */
    public static final int  TEMPERATURES  = 0x02;
    /** Humidities (current and historic) in each zone */
    public static final int  HUMIDITIES    = 0x04;
    /** Rules (programmed targets at particular times) */
    public static final int  RULES         = 0x08;
    public static final int  ACTUATORS     = 0x10;
    public static final int  PERIODS       = 0x20;
    public static final int  ALL           = 0x3C;

    private Context context;
    /** Handler that will be notified when there is a change in state */
    private Handler handler;

    private Map<String, ArrayList<Datum>> temperatures = new HashMap<>();
    private Map<String, ArrayList<Datum>> humidities = new HashMap<>();
    /** zone name -> map of actuator name to actuator state */
    private HashMap<String, Map<String, Boolean>> actuators = new HashMap<>();
    private List<Rule> rules = new ArrayList<>();
    private List<String> zones = new ArrayList<>();
    private List<Period> periods = new ArrayList<>();

    public State(Context context) {
        this.context = context;
    }

    public void setHandler(Handler h) {
        handler = h;
    }

    private void changed(int p) {
        Message m = Message.obtain();
        Bundle b = new Bundle();
        b.putInt("property", p);
        m.setData(b);
        handler.sendMessage(m);
    }

    /* Get */

    public synchronized List<Period> getPeriods() { return periods; }

    public synchronized Map<String, ArrayList<Datum>> getTemperatures() {
        return temperatures;
    }

    public synchronized Map<String, ArrayList<Datum>> getHumidities() {
        return humidities;
    }

    public synchronized List<Rule> getRules() {
        return rules;
    }

    public synchronized byte[] getBinary(int id) {
        /* XXX: check this is long enough */
        byte[] data = new byte[32*1024];
        int off = 0;

        data[off++] = (byte) (OP_CHANGE | id);

        switch (id) {
        case PERIODS:
            data[off++] = (byte) periods.size();
            for (Period p: periods) {
                off = p.getBinary(data, off);
            }
            break;
        case RULES:
            /* XXX */
            break;
        }

        return data;
    }

    public synchronized Map<String, Map<String, Boolean>> getActuators () { return actuators; }

    public synchronized List<String> getZones() { return zones; }


    /* Set */

    public synchronized void setPeriods(List<Period> p) {
        periods = p;
        changed(PERIODS);
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
        int changes = 0;

        int numZones = data[o++];
        zones.clear();
        for (int i = 0; i < numZones; ++i) {
            String name = Util.getString(data, o);
            o += name.length() + 1;
            zones.add(name);
        }

        for (String name : zones) {
             if (all || op == (OP_CHANGE | TEMPERATURES)) {
                ArrayList<Datum> t = new ArrayList<>();
                o = getDatumArray(data, o, t);
                temperatures.put(name, t);
                changes |= TEMPERATURES;
            }
            if (all || op == (OP_CHANGE | HUMIDITIES)) {
                ArrayList<Datum> t = new ArrayList<>();
                o = getDatumArray(data, o, t);
                if (t.size() > 0) {
                    humidities.put(name, t);
                    changes |= HUMIDITIES;
                }
            }
            if (all || op == (OP_CHANGE | ACTUATORS)) {
                HashMap<String, Boolean> act = new HashMap<>();
                int N = data[o++];
                for (int i = 0; i < N; ++i) {
                    String actName = Util.getString(data, o);
                    o += actName.length() + 1;
                    act.put(actName, data[o++] > 0);
                }
                actuators.put(name, act);
                changes |= ACTUATORS;
            }
        }

        if (all || op == (OP_CHANGE | PERIODS)) {
            int num = data[o++];
            periods.clear();
            for (int i = 0; i < num; ++i) {
                Period p = new Period(data, o);
                periods.add(p);
                o += p.binaryLength();
            }
            Log.e("test", "got " + periods.size() + " periods.");
            changes |= PERIODS;
        }

        if (all || op == (OP_CHANGE | RULES)) {
            int num = data[o++];
            rules.clear();
            for (int i = 0; i < num; ++i) {
                Rule r = new Rule(data, o);
                rules.add(r);
                o += r.binaryLength();
            }
            changes |= RULES;
        }

        changed(changes);
    }
}
