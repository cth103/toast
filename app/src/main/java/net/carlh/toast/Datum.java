package net.carlh.toast;

import android.util.Log;

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

    public static final int BINARY_LENGTH = 10;

    public Datum(byte[] data, int offset) {
        long epoch =
                (data[offset] & 0xff) | ((data[offset+1] & 0xff) << 8) |
                ((data[offset+2] & 0xff) << 16) | ((data[offset+3] & 0xff) << 24) | ((data[offset+4] & 0xff) << 32) |
                ((data[offset+5] & 0xff) << 40) | ((data[offset+6] & 0xff) << 48) | ((data[offset+7] & 0xff) << 56);
        time = new Date(epoch * 1000);
        value = Util.getFloat(data, offset + 8);
    }

    public Datum(Date time, double value) {
        this.time = time;
        this.value = value;
    }

    public byte[] asBinary() {
        byte[] b = new byte[10];
        long seconds = time.getTime();
        b[0] = (byte) (seconds & 0xff);
        b[1] = (byte) ((seconds & 0xff00) >> 8);
        b[2] = (byte) ((seconds & 0xff0000) >> 16);
        b[3] = (byte) ((seconds & 0xff000000) >> 24);
        b[4] = (byte) ((seconds & 0xff00000000L) >> 32);
        b[5] = (byte) ((seconds & 0xff0000000000L) >> 40);
        b[6] = (byte) ((seconds & 0xff000000000000L) >> 48);
        b[7] = (byte) ((seconds & 0xff00000000000000L) >> 56);
        Util.putFloat(b, 8, value);
        return b;
    }

    public String toString() {
        return time.toString() + ": " + value;
    }
}
