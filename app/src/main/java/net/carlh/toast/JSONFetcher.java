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

abstract class JSONFetcher extends AsyncTask<Void, Void, String> {

    protected Context context;
    private String request;
    protected String json;

    public JSONFetcher(Context context, String request) {
        this.context = context;
        this.request = request;
    }
    
    protected String doInBackground(Void... unused) {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(new HttpGet(Util.url(context, request)));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                json = out.toString();
                out.close();
            } else {
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (HttpHostConnectException e) {
            Log.e("Toast", "Exception", e);
        } catch (SocketException e) {
            Log.e("Toast", "Exception", e);
        } catch (IOException e) {
            Log.e("Toast", "Exception", e);
        }

        return json;
    }
}
                   


