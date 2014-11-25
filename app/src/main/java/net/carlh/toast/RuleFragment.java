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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TimePicker;

/** Fragment to edit a single `rule', i.e. a specification of a time period
 *  on one or more days when the heating should be on.
 */
public class RuleFragment extends Fragment {

    private Rule rule = null;
    
    public RuleFragment(Rule rule) {
        this.rule = rule;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_rule, container, false);

        for (int i = 0; i < 7; i++) {
            int id = getResources().getIdentifier("day" + Integer.toString(i), "id", getActivity().getPackageName());
            CheckBox c = (CheckBox) view.findViewById(id);
            c.setChecked(rule.getDayActive(i));

            final int dayNumber = i;
            c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                    rule.setDay(dayNumber, isChecked);
                }
            });
        }

        final Button on = (Button) view.findViewById(R.id.on);
        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog d = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
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

        final Button off = (Button) view.findViewById(R.id.off);
        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog d = new TimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
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

        Button cancel = (Button) view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        Button ok = (Button) view.findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                activity.getState().addOrReplace(rule);
                getActivity().finish();
            }
        });

        return view;
    }
}

