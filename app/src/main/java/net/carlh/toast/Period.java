package net.carlh.toast;

import java.util.Date;

public class Period {
    Period(String zone, double target, Date from, Date to) {
        this.zone = zone;
        this.target = target;
        this.from = from;
        this.to = to;
    }

    public String zone;
    public double target;
    public Date from;
    public Date to;
}
