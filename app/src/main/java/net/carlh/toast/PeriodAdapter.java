package net.carlh.toast;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;
import java.util.Random;

/**
 * Created by carl on 26/01/18.
 */

public class PeriodAdapter extends ArrayAdapter<Period> {
    private final Context context;
    private final Period[] values;

    public PeriodAdapter(Context context, Period[] values) {
        super(context, R.layout.period_layout, values);
        this.context = context;
        this.values = values;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.period_layout, parent, false);
        TextView target = (TextView) row.findViewById(R.id.target);
        TextView time = (TextView) row.findViewById(R.id.time);
        ProgressBar progress = (ProgressBar) row.findViewById(R.id.progress);

        final Period period = values[position];

        target.setText(String.format("%s to %.1f°C", period.zone, period.target));

        Date now = new Date();
        if (period.from.after(now)) {
            time.setText("Coming up");
        } else {
            long remainingMins = (period.to.getTime() - now.getTime()) / (1000 * 60);
            int hours = (int) (remainingMins / 60);
            int mins = (int) (remainingMins - hours * 60);
            time.setText(String.format("%d:%02d left", hours, mins));
        }

        progress.setRotation(180);
        progress.setMax(100);
        progress.setProgress((int) (100 * (Math.max(0L, now.getTime() - period.from.getTime())) / (period.to.getTime() / period.from.getTime())));

        return row;
    }

}
