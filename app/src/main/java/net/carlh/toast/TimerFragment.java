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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TableLayout;

import java.util.ArrayList;

/** Fragment to see all `timer' rules, edit them and add new ones */
public class TimerFragment extends Fragment {

    private ListView rulesList;
    private Button addRule;

    private ArrayAdapter<Rule> adapter;
    private ArrayList<Rule> rules = new ArrayList<Rule>();
    /** Possibly unnecessary hack so that we know what rule we are
        talking about when handling context menus opened by long-click.
    */
    private Rule lastClickRule = null;

    public static final int ADD_OR_UPDATE_RULE = 0;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        rulesList = (ListView) view.findViewById(R.id.rulesList);
        adapter = new ArrayAdapter<Rule>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, rules);
        rulesList.setAdapter(adapter);

        registerForContextMenu(rulesList);

        /* Edit rules on click */
        rulesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Rule rule = (Rule) parent.getItemAtPosition(position);
                Intent intent = new Intent(getActivity(), RuleActivity.class);
                intent.putExtra("rule", rule);
                getActivity().startActivityForResult(intent, ADD_OR_UPDATE_RULE);
            }
        });

        /* See lastClickRule... */
        rulesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                lastClickRule = (Rule) parent.getItemAtPosition(position);
                return false;
            }
        });
        
        addRule = (Button) view.findViewById(R.id.addRule);
        addRule.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Rule rule = new Rule(0, 8, 0, 18, 0);
                Intent intent = new Intent(getActivity(), RuleActivity.class);
                intent.putExtra("rule", rule);
                getActivity().startActivityForResult(intent, ADD_OR_UPDATE_RULE);
            }
        });

        update();
        return view;
    }

    public void update() {
        if (addRule == null) {
            /* addRule is the first variable to be set in onCreateView
               so if it's null we'll assume onCreateView hasn't been called
               yet
             */
            return;
        }

        addRule.setEnabled(getConnected());
        rulesList.setEnabled(getConnected());
            
        /* State.rules is modified by other threads, so we can't use it in an ArrayAdapter */
        rules.clear();
        if (getState() != null) {
            ArrayList<Rule> stateRules = getState().getRules();
            for (Rule r: stateRules) {
                rules.add(new Rule(r));
            }
        }
        
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() != R.id.rulesList) {
            return;
        }
        
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(lastClickRule.toString());
        menu.add(Menu.NONE, 0, 0, "Remove");
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case 0:
            getState().remove(lastClickRule);
            break;
        }
        return true;
    }
}
