package net.carlh.toast;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TimerActivity extends Activity {

    private ListView rulesList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        rulesList = (ListView) findViewById(R.id.rulesList);
        new RulesFetcher(this, rulesList).execute();

        rulesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Rule rule = (Rule) parent.getItemAtPosition(position);
                Intent intent = new Intent(TimerActivity.this, RuleActivity.class);
                intent.putExtra("rule", rule);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("Toast", "RESULT");
        new RulesFetcher(this, rulesList).execute();
    }
}
