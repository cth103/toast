package net.carlh.toast;

import java.io.ByteArrayOutputStream;
import java.util.Date;

public class Period {
    public Period(String zone, float target, Date from, Date to) {
        this.zone = zone;
        this.target = target;
        this.from = from;
        this.to = to;
    }

    public Period(byte[] data, int off) {
        zone = Util.getString(data, off);
        off += zone.length() + 1;
        target = Util.getFloat(data, off);
        off += 2;
        from = new Date(Util.getInt64(data, off) * 1000);
        off += 8;
        to = new Date(Util.getInt64(data, off) * 1000);
        off += 8;
    }

    public int getBinary(byte[] data, int off) {
        off = Util.putString(data, off, zone);
        off = Util.putFloat(data, off, target);
        off = Util.putInt64(data, off, from.getTime() / 1000);
        return Util.putInt64(data, off, to.getTime() / 1000);
    }

    public int binaryLength() {
        return zone.length() + 1 + 2 + 8 + 8;
    }

    public void extend(long by) {
        to.setTime(to.getTime() + by);
    }

    public long length() {
        return to.getTime() - from.getTime();
    }

    public boolean equals(Object o) {
        Period other = (Period) o;
        if (o == null) {
            return false;
        }
        return zone.equals(other.zone) && target == other.target && from.equals(other.from) && to.equals(other.to);
    }

    public String zone;
    public double target;
    public Date from;
    public Date to;
}
