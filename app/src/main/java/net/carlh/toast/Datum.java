package net.carlh.toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Datum {
    public Date time;
    public double value;

    public static final int TYPE_TEMPERATURE = 0;
    public static final int TYPE_HUMIDITY = 1;
    public static final int TYPE_COUNT = 2;

    //private final static DateFormat format = new SimpleDateFormat("'wallclock('yyyy-MM-dd'T'HH:mm:ssZ')'");
    private final static DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public Datum(JSONArray array) throws JSONException, ParseException {
        time = format.parse(array.getString(0).replaceAll("Z$", "+0000"));
        value = array.getDouble(1);
    }

    public Datum(Date time, double value) {
        this.time = time;
        this.value = value;
    }

    public JSONArray asJSONArray() {
        StringBuffer sb = new StringBuffer();
        JSONArray a = new JSONArray();
        a.put(format.format(time));
        a.put(Double.toString(value));
        return a;
    }
}
