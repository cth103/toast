package net.carlh.toast;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Util
{
    public static String url(Context context, String request) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return "http://" + prefs.getString("hostname", "192.168.1.1") + "/" + request;
    }
}
