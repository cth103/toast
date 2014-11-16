package net.carlh.toast;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class Rule implements Serializable {

    private int id;
    /* Days as a bitfield; 1 is Monday, 2 is Tuesday etc. */
    private int days;
    private int onHour;
    private int onMinute;
    private int offHour;
    private int offMinute;
    private double target;

    public Rule(JSONObject json) {
        try {
            id = json.getInt("id");
            days = json.getInt("days");
            onHour = json.getInt("on_hour");
            onMinute = json.getInt("on_minute");
            offHour = json.getInt("off_hour");
            offMinute = json.getInt("off_minute");
            target = json.getDouble("target");
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
    }

    JSONObject json() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("days", days);
            json.put("on_hour", onHour);
            json.put("on_minute", onMinute);
            json.put("off_hour", offHour);
            json.put("off_minute", offMinute);
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
        if (h > 12) {
            return Integer.toString(h - 12) + ":" + Integer.toString(m) + "pm";
        } else {
            return Integer.toString(h) + ":" + Integer.toString(m) + "am";
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

    void setOnTime(int h, int m) {
        onHour = h;
        onMinute = m;
    }

    void setOffTime(int h, int m) {
        offHour = h;
        offMinute = m;
    }

    void setDay(int day, boolean active) {
        Log.e("Toast", "Set " + day + " " + active);
        if (active) {
            days |= (1 << day);
        } else {
            days &= ~(1 << day);
        }
    }
};
