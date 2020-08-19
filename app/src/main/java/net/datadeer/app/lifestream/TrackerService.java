package net.datadeer.app.lifestream;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.datadeer.app.NetworkService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import static net.datadeer.app.NetworkService.getPreferences;

public class TrackerService extends Service {

    public static boolean getTrackingEnabled(TrackerMethod m, Context c) {
        return getPreferences(c).getBoolean(m.getName(),false);
    }
    public static void setTrackingEnabled(TrackerOption m, Context c, boolean checked) {
        NetworkService.getPreferences(c).edit().putBoolean(m.id,checked).apply();
    }

    public final static String TAG = "net.datadeer.app";

    Handler handler;

    private boolean isRunning = false;

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent pIntent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startTheUpdates();
        }
        Log.v(TAG,"OnStartCommand network service");
        return super.onStartCommand(pIntent, flags, startId);
    }

    //main This is used as a separate-thread task of networking so
    static class TrackerTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<TrackerService> activityReference;
        private WeakReference<Handler> handlerReference;
        TrackerTask(TrackerService trackerService,Handler handler) {
            this.activityReference = new WeakReference<>(trackerService);
            this.handlerReference = new WeakReference<>(handler);
        }
        @Override
        protected Void doInBackground(Void... integers) {
            Looper.prepare();
            Log.v(TAG,"FROM BACKGROUNDDD");
            for (TrackerOption t : TrackerManager.trackers) {
                boolean enabled = getTrackingEnabled(t.tracker, activityReference.get());
                if (!enabled) continue;
                for(String perm : t.perms) {
                    int access = ContextCompat.checkSelfPermission(activityReference.get(),perm);
                    if (access== PackageManager.PERMISSION_DENIED) enabled=false;
                }
                if (!enabled) continue;
                t.tracker.setContext(activityReference.get());
                t.tracker.setHandler(handlerReference.get());
                t.tracker.run();
            }
            Looper.loop();
            return Void.TYPE.cast(null);
        }
        @Override
        protected void onPostExecute(Void status) {

        }
    }

    public static void publishSpyResults(TrackerMethod from, JSONObject spyResultsRaw) {
        Log.v(TAG,"publishSpyResults called! Publishing results!");
        if (from==null || spyResultsRaw == null || !TrackerService.getTrackingEnabled(from,from.getContext())) return;
        JSONObject spyResults;
        try {
            spyResults = new JSONObject();
            spyResults.put(from.getName(),spyResultsRaw);
        } catch (JSONException e) {
            return;
        }
        Log.v(TAG,"SPY RESULTS: "+spyResults.toString());
        new Thread(() -> {
            try {
                URL url = new URL("https://datadeer.net/lifestream/upload.php");
                HttpsURLConnection http = (HttpsURLConnection) url.openConnection();
                http.setDoInput(true);
                http.setDoOutput(false);
                http.setRequestMethod("POST");
                http.setReadTimeout(8000);
                http.setConnectTimeout(8000);
                http.setDefaultUseCaches(false);
                http.setUseCaches(false);
                String cookie = NetworkService.getCookie(from.getContext());
                if (cookie==null) {
                    Log.e(TAG,"NO COOKIE");
                    return;
                }
                http.setRequestProperty("Cookie",cookie);
                BufferedOutputStream out = new BufferedOutputStream(http.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                writer.write(spyResults.toString());
                writer.flush();
                writer.close();
                out.close();
                http.getInputStream();
                Log.v(TAG,"NETWORK THING GOING");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    //This is the NetworkService function that runs once in order to make the asynchronous task
    void startTheUpdates() {
        Log.v(TAG,"TRACKER SERVICE RUNNING");
        handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    try {
                        TrackerTask nt = new TrackerTask(TrackerService.this,handler);
                        nt.execute();
                    } catch (Exception ignored) {
                    }
                });
            }
        };
//        timer.schedule(doAsynchronousTask,0);
        //main maybe repeat?
        timer.schedule(doAsynchronousTask, 0, 60_000); //execute in every x ms
    }
}
