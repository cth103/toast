package net.carlh.toast;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class TemperatureFetcher {

    private GraphView.GraphViewData[] data;
    private HttpClient client;
    private final Lock lock = new ReentrantLock();
    private final Condition fetchCondition = lock.newCondition();
    private boolean fetch = true;
    /** Handlers that will be notified when there is a change in state */
    private ArrayList<Handler> handlers = new ArrayList<Handler>();

    TemperatureFetcher(final Context context, GraphView graphView) {

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 3000);
        HttpConnectionParams.setSoTimeout(params, 3000);
        client = new DefaultHttpClient(params);

        Thread thread = new Thread(new Runnable() {
            public void run() {

                try {

                    try {
                        lock.lock();
                        while (fetch == false) {
                            fetchCondition.await(10, TimeUnit.SECONDS);
                        }
                    } catch (InterruptedException e) {

                    } finally {
                        lock.unlock();
                    }

                    HttpResponse response = client.execute(new HttpGet(Util.url(context, "temperatures?minutes=60")));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        out.close();

                        JSONArray json = new JSONArray(out.toString());
                        try {
                            lock.lock();
                            data = new GraphView.GraphViewData[json.length()];
                            for (int i = 0; i < json.length(); i++) {
                                data[i] = new GraphView.GraphViewData(i, json.getDouble(i));
                            }
                        } finally {
                            lock.unlock();
                        }

                    } else {
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (HttpHostConnectException e) {
                    Log.e("Toast", "HttpHostConnectException in get()");
                } catch (ConnectTimeoutException e) {
                    Log.e("Toast", "ConnectTimeoutException in get()");
                } catch (SocketException e) {
                    Log.e("Toast", "SocketException in get()");
                } catch (SocketTimeoutException e) {
                    Log.e("Toast", "SocketTimeoutException in get()");
                } catch (IOException e) {
                    Log.e("Toast", "Exception", e);
                } catch (JSONException e) {
                    Log.e("Toast", "Exception", e);
                } finally {
                    Log.e("Fetch", "Done it");
                    for (Handler h : handlers) {
                        h.sendEmptyMessage(0);
                    }
                }
            }
        });

        thread.start();
    }
            
    public void fetch(boolean f) {
        try {
            lock.lock();
            fetch = f;
            if (fetch) {
                fetchCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }
            
    public void addHandler(Handler h) {
        handlers.add(h);
    }
    
    public GraphView.GraphViewData[] getData() {
        GraphView.GraphViewData[] r;
        try {
            lock.lock();
            r = data;
        } finally {
            lock.unlock();
        }
        return r;
    }
}
