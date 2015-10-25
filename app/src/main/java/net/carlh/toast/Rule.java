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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class Rule implements Serializable {

    private int id = -1;
    /* Days as a bitfield; 1 is Monday, 2 is Tuesday etc. */
    private int days;
    private int onHour;
    private int onMinute;
    private int offHour;
    private int offMinute;
    private String zone;
    private double target;

    public Rule(int days, int onHour, int onMinute, int offHour, int offMinute, String zone, double target) {
        this.days = days;
        this.onHour = onHour;
        this.onMinute = onMinute;
        this.offHour = offHour;
        this.offMinute = offMinute;
        this.zone = zone;
        this.target = target;
    }

    public Rule(JSONObject json) {
        try {
            id = json.getInt("id");
            days = json.getInt("days");
            onHour = json.getInt("on_hour");
            onMinute = json.getInt("on_minute");
            offHour = json.getInt("off_hour");
            offMinute = json.getInt("off_minute");
            zone = json.getString("zone");
            target = json.getDouble("target");
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
    }

    public Rule(Rule r) {
        copyFrom(r);
    }

    public void copyFrom(Rule r) {
        id = r.id;
        days = r.days;
        onHour = r.onHour;
        onMinute = r.onMinute;
        offHour = r.offHour;
        offMinute = r.offMinute;
        zone = r.zone;
        target = r.target;
    }

    JSONObject asJSON() {
        JSONObject json = new JSONObject();
        try {
            if (id != -1) {
                json.put("id", id);
            }
            json.put("days", days);
            json.put("on_hour", onHour);
            json.put("on_minute", onMinute);
            json.put("off_hour", offHour);
            json.put("off_minute", offMinute);
            json.put("zone", zone);
            json.put("target", target);
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
        return json;
    }

    public static ArrayList<Rule> readJSON(JSONArray json) {
        ArrayList<Rule> out = new ArrayList<Rule>();
        try {
            for (int i = 0; i < json.length(); i++) {
                out.add(new Rule(json.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }

        return out;
    }

    private static String period(int from, int to) {
        if (from == to) {
            return dayName(from);
        } else {
            return dayName(from) + "-" + dayName(to);
        }
    }

    private static String twelveHour(int h, int m) {
        if (h > 13) {
            return String.format("%d:%02dpm", h - 12, m);
        } else if (h == 12) {
            return String.format("%d:%02dpm", h, m);
        } else {
            return String.format("%d:%02dam", h, m);
        }
    }

    private static String dayName(int d) {
        String[] names = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        return names[d];
    }

    public String toString() {
        String s = "";

        /* Days */

        int periodStart = -1;
        for (int i = 0; i < 7; i++) {
            if ((days & (1 << i)) != 0) {
                if (periodStart == -1) {
                    periodStart = i;
                }
            } else {
                if (periodStart != -1) {
                    s += period(periodStart, i - 1) + ", ";
                    periodStart = -1;
                }
            }
        }

        if (periodStart != -1) {
            s += period(periodStart, 6) + ", ";
        }

        /* Remove last ", " */
        if (s.length() >= 2) {
            s = s.substring(0, s.length() - 2);
        }

        /* Time */
        s += " " + twelveHour(onHour, onMinute) + "-" + twelveHour(offHour, offMinute);

        s += " " + zone + " to " + target + "°C";

        return s;
    }

    int getId() {
        return id;
    }

    boolean getDayActive(int d) {
        return (days & (1 << d)) != 0;
    }

    int getOnHour() {
        return onHour;
    }

    int getOnMinute() {
        return onMinute;
    }

    int getOffHour() {
        return offHour;
    }

    int getOffMinute() {
        return offMinute;
    }

    String getOnTime() {
        return twelveHour(onHour, onMinute);
    }

    String getOffTime() {
        return twelveHour(offHour, offMinute);
    }

    String getZone() {
        return zone;
    }

    double getTarget() {
        return target;
    }

    void setOnTime(int h, int m) {
        onHour = h;
        onMinute = m;
    }

    void setOffTime(int h, int m) {
        offHour = h;
        offMinute = m;
    }

    void setDay(int day, boolean active) {
        if (active) {
            days |= (1 << day);
        } else {
            days &= ~(1 << day);
        }
    }

    void setZone(String z) {
        zone = z;
    }

    void setTarget(double t) {
        target = t;
    }
};
