package net.carlh.toast;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

class RulesFetcher extends JSONFetcher {

    private ListView view;

    public RulesFetcher(Context context, ListView view) {
        super(context, "rule");
        this.view = view;
    }

    protected void onPostExecute(String json) {
        try {
            view.setAdapter(new ArrayAdapter<Rule>(context, android.R.layout.simple_list_item_1, android.R.id.text1, Rule.readJSON(new JSONArray(json))));
        } catch (JSONException e) {
            Log.e("Toast", "Exception", e);
        }
    }
}
                   
