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
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

class RuleSender extends AsyncTask<Rule, Void, Void> {

    private Context context;
    
    public RuleSender(Context context) {
        this.context = context;
    }

    protected Void doInBackground(Rule... rules) {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpPut put = new HttpPut(Util.url(context, "rule"));
            StringEntity entity = new StringEntity(rules[0].json().toString());
            entity.setContentType("text/json");
            put.setEntity(entity);
            client.execute(put);
        } catch (HttpHostConnectException e) {
            Log.e("Toast", "Exception", e);
        } catch (SocketException e) {
            Log.e("Toast", "Exception", e);
        } catch (IOException e) {
            Log.e("Toast", "Exception", e);
        }

        return null;
    }
}
