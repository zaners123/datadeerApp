package net.datadeer.app.lifestream;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import net.datadeer.app.NetworkService;
import net.datadeer.app.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import static net.datadeer.app.NetworkService.getPreferences;

public class TrackerService extends IntentService {

    public final static String TAG = "net.datadeer.app";

    public static final int PERM_OVERLAY_ID = 67;

    private static final int NOTIFICATION_ID = 68;

    public static final String PERMISSION_TO_OVERLAY = TAG+".overlay_perm";
    public final static int PERMISSION_TO_OVERLAY_REQUEST_ID = 69;

    Notification notification = null;

    public static final TrackerOption[] trackers = {
            new TrackerOption("Device Location", new TrackerLocation(), Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            new TrackerOption("Your Phone Number", new TrackerPhoneNumbers(), Manifest.permission.READ_PHONE_STATE),//todo Manifest.permission.READ_PHONE_NUMBERS?
            new TrackerOption("Contacts", new TrackerContacts(), Manifest.permission.READ_CONTACTS),
            new TrackerOption("Livestream (Camera and Audio)",new TrackerLifestream(), Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, PERMISSION_TO_OVERLAY),
    };

    public TrackerService() {
        super("TrackerService");
    }
    public static boolean getTrackingEnabled(Context c, String m) {
        return getPreferences(c).getBoolean(m,false);
    }

    public static boolean getTrackingEnabled(Context c, TrackerMethod m) {
        return getTrackingEnabled(c, m.getName());
    }

    public static void setTrackingEnabled(TrackerOption m, Context c, boolean checked) {
        NetworkService.getPreferences(c).edit().putBoolean(m.id,checked).apply();
        if (checked) {
            c.startService(new Intent(c, TrackerService.class));
        }
    }

    /**
     * Sees if user has permission; might be one of my custom permissions...
     * @return bool - true if granted
     * */
    private static boolean hasPerm(Context c, String perm) {
        if (perm.equals(PERMISSION_TO_OVERLAY)) {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(c);
        } else {
            return ContextCompat.checkSelfPermission(c,perm) == PackageManager.PERMISSION_DENIED;
        }
    }

    /**
     * Test if we have this list of permissions
     * */
    public static boolean hasPerms(Context c, String[] perms) {
        for(String perm : perms) {
            if (hasPerm(c, perm)) {
                return false;
            }
        }
        return true;
    }

    private static boolean canRun(Context c, TrackerOption t) {
        if (!getTrackingEnabled(c, t.tracker)) return false;
        return hasPerms(c,t.perms);
    }
    /**
     * Send given JSON to the server!
     * */
    private void publishJSON(String from, JSONObject spyResultsRaw) {
        if (spyResultsRaw == null) {
            Log.w(TAG,"Tracker upload called, but failed since results null");
            return;
        } else if (!TrackerService.getTrackingEnabled(this,from)) {
            Log.d(TAG,"Tracker upload called, but failed since tracker "+from+" not enabled");
            return;
        }
        JSONObject spyResults;
        try {
            spyResults = new JSONObject();
            spyResults.put(from,spyResultsRaw);
        } catch (JSONException e) {
            return;
        }
        Log.v(TAG,"SENDING SPY RESULTS: "+spyResults.toString());
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
            String cookie = NetworkService.getCookie(this);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public final static String IS_SENDING_JSON ="is-sending-json";
    public final static String THE_SENDING_JSON ="the-sending-json";
    public final static String THE_SENDING_METHOD ="the-sending-method";

    @Override
    public int onStartCommand(Intent pIntent, int flags, int startId) {
        if (notification==null) {
            notification = showNotification(this, NOTIFICATION_ID, "Datadeer","Tracking Enabled","channel");
            notification.defaults = 0;
        }
        //make service foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        HandlerThread thread = new HandlerThread("Networker");
        thread.start();
        Looper looper = thread.getLooper();
        for (TrackerOption t : trackers) {
            if (!canRun(this, t)) continue;
            t.tracker.setContext(this);
            t.tracker.setLooper(looper);
            t.tracker.run();
        }
        return super.onStartCommand(pIntent, flags, startId);
    }

    /**
     * Used by external methods too lazy to call start service
     * @return true if still running
     * */
    public static boolean uploadJSON(TrackerMethod tm, JSONObject json) {
        if (!getTrackingEnabled(tm.getContext(), tm)) return false;
        tm.getContext().startService(
            new Intent(tm.getContext(), TrackerService.class)
                .putExtra(TrackerService.IS_SENDING_JSON,true)
                .putExtra(TrackerService.THE_SENDING_METHOD,tm.getName())
                .putExtra(TrackerService.THE_SENDING_JSON,json.toString())
        );
        return true;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        boolean sendThisJSON = intent.getBooleanExtra(IS_SENDING_JSON,false);
        if (sendThisJSON) {
            String json = intent.getStringExtra(THE_SENDING_JSON);
            if (json==null) {Log.e(TAG,"Passed IS_SENDING_JSON but didn't give me a JSON");return;}
            String name = intent.getStringExtra(THE_SENDING_METHOD);
            if (name==null) {Log.e(TAG,"Passed IS_SENDING_JSON but didn't give me a Name");return;}
            try {
                JSONObject toSend = new JSONObject(json);
                publishJSON(name, toSend);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static Notification showNotification(Context c, int idToUse, String title, String body, String channel) {
        NotificationManagerCompat nmc = NotificationManagerCompat.from(c);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(channel, channel, NotificationManager.IMPORTANCE_DEFAULT);
            nmc.createNotificationChannel(nc);
        }

        Intent intent = new Intent(c, TrackerManager.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c.getApplicationContext(), channel)
                .setSmallIcon(R.drawable.deer_notify) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(body)// message for notification
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false) // clear notification after click
                .setColor(Color.rgb(255,0,0))
                .setColorized(true)
                .setOnlyAlertOnce(true)
                .setNotificationSilent()
                .setOngoing(true)
                .setLights(Color.rgb(0,255,0),1000,1000)
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setCategory(Notification.CATEGORY_SERVICE);
        }
        Notification n = mBuilder.build();
        nmc.notify(idToUse, n);
        return n;
    }
}