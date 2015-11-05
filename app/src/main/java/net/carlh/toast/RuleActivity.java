/*
    Copyright (C) 2014 Carl Hetherington <cth@carlh.net>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package net.carlh.toast;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.util.Set;

/** Activity to edit a single `rule', i.e. a specification of a time period
 *  on one or more days when a zone should be at a target temperature.
 */
public class RuleActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule);

        final Rule rule = (Rule) getIntent().getSerializableExtra("rule");
        final String[] zones = getIntent().getStringArrayExtra("zones");

        Spinner zone = (Spinner) findViewById(R.id.zone);
        ArrayAdapter zoneAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, zones);
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zone.setAdapter(zoneAdapter);
        zone.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                rule.setZone(parentView.getItemAtPosition(position).toString());
            }

            public void onNothingSelected(AdapterView<?> parentView) {}
        });
        for (int i = 0; i < zones.length; ++i) {
            if (zones[i].equals(rule.getZone())) {
                zone.setSelection(i);
            }
        }

        Spinner target = (Spinner) findViewById(R.id.target);
        String[] targets = { "16", "17", "18", "19", "20", "21", "22", "23", "24", "25" };
        ArrayAdapter targetAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, targets);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        target.setAdapter(targetAdapter);
        target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                rule.setTarget(Integer.parseInt(parentView.getItemAtPosition(position).toString()));
            }

            public void onNothingSelected(AdapterView<?> parentView) {}
        });
        target.setSelection((int) (rule.getTarget() - 16));

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
                Bundle b = new Bundle();
                b.putSerializable("rule", rule);
                Intent i = new Intent();
                i.putExtras(b);
                setResult(RESULT_OK, i);
                RuleActivity.this.finish();
            }
        });
    }
}
