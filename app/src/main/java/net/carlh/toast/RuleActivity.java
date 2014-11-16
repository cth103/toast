package net.carlh.toast;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TimePicker;

public class RuleActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule);

        final Rule rule = (Rule) getIntent().getSerializableExtra("rule");

        for (int i = 0; i < 7; i++) {
            int id = getResources().getIdentifier("day" + Integer.toString(i), "id", getPackageName());
            CheckBox c = (CheckBox) findViewById(id);
            c.setChecked(rule.getDayActive(i));

            final int dayNumber = i;
            c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    rule.setDay(dayNumber, isChecked);
                }
            });
        }

        final Button on = (Button) findViewById(R.id.on);
        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog d = new TimePickerDialog(RuleActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker picker, int hour, int minute) {
                        rule.setOnTime(hour, minute);
                        on.setText(rule.getOnTime());
                    }
                }, rule.getOnHour(), rule.getOnMinute(), false);
                d.setTitle("Select on time");
                d.show();
            }
        });
        on.setText(rule.getOnTime());

        final Button off = (Button) findViewById(R.id.off);
        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog d = new TimePickerDialog(RuleActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker picker, int hour, int minute) {
                        rule.setOffTime(hour, minute);
                        off.setText(rule.getOffTime());
                    }
                }, rule.getOffHour(), rule.getOffMinute(), false);
                d.setTitle("Set off time");
                d.show();
            }
        });

        off.setText(rule.getOffTime());

        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RuleActivity.this.finish();
            }
        });

        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.getState().addOrReplace(rule);
                RuleActivity.this.finish();
            }
        });
    }
}

