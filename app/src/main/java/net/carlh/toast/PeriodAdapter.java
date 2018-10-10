package net.carlh.toast;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;
import java.util.List;
import java.util.Random;

public class PeriodAdapter extends ArrayAdapter<Period> {
    private final Context context;
    private final List<Period>values;

    public PeriodAdapter(Context context, List<Period> values) {
        super(context, R.layout.period_layout, values);
        this.context = context;
        this.values = values;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.period_layout, parent, false);
        TextView target = row.findViewById(R.id.target);
        TextView time = row.findViewById(R.id.time);
        ProgressBar progress = row.findViewById(R.id.progress);

        final Period period = values.get(position);

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
        /* the progress bar shows how much of the last hour is left */
        progress.setMax(60);
        progress.setProgress((int) (Math.max(0L, period.to.getTime() - now.getTime()) / 60000));

        return row;
    }

}
