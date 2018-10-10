package carlh.net.toasttest;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private TableLayout table;
    private ListView periods;
    private PeriodAdapter periodAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        periods = (ListView) findViewById(R.id.periods);
        table = (TableLayout) findViewById(R.id.zones);

        Calendar to1 = Calendar.getInstance();
        to1.add(Calendar.HOUR, 1);
        Calendar to2 = Calendar.getInstance();
        to2.add(Calendar.HOUR, 2);
        Period[] values = new Period[] {
            new Period("Sitting room", 17, Calendar.getInstance().getTime(), to1.getTime()),
            new Period("Bathroom", 16.5, Calendar.getInstance().getTime(), to2.getTime())
        };

        periodAdapter = new PeriodAdapter(this, values);
        periods.setAdapter(periodAdapter);

        /* Zone name and current temperature / humidity */
        {
            TableRow r = new TableRow(this);

            TextView l = new TextView(this);
            l.setText("Sitting room");
            l.setTextSize(16);
            l.setPadding(24, 64, 0, 0);
            l.setTypeface(null, Typeface.BOLD);

            r.addView(l);

            TextView temperature = new TextView(this);
            temperature.setTextSize(16);
            temperature.setText("20°C (68%)");
            r.addView(temperature);

            Button heat = new Button(this);
            heat.setText("H");
            r.addView(heat);

            Spinner target = new Spinner(this);
            String[] targets = { "5°C", "17°C", "18°C", "19°C", "20°C", "21°C", "22°C" };
            ArrayAdapter targetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, targets);
            targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            target.setAdapter(targetAdapter);
            r.addView(target);

            r.setGravity(Gravity.BOTTOM);

            table.addView(r);
        }

        {
        TableRow r = new TableRow(this);

        TextView l = new TextView(this);
        l.setText("Bathroom");
        l.setTextSize(16);
        l.setPadding(24, 64, 0, 0);
        l.setTypeface(null, Typeface.BOLD);

        r.addView(l);

        TextView temperature = new TextView(this);
        temperature.setTextSize(16);
        temperature.setText("16°C (45%)");
        r.addView(temperature);

        Button heat = new Button(this);
        heat.setText("H");
        heat.setPadding(0, 0, 0, 0);
        r.addView(heat);

        Spinner target = new Spinner(this);
        String[] targets = { "5°C", "17°C", "18°C", "19°C", "20°C", "21°C", "22°C" };
        ArrayAdapter targetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, targets);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        target.setAdapter(targetAdapter);
        r.addView(target);

        r.setGravity(Gravity.BOTTOM);

        table.addView(r);
    }
    }
}
