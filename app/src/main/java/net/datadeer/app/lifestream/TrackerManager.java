package net.datadeer.app.lifestream;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import net.datadeer.app.DeerView;
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
import java.util.Arrays;
import java.util.TreeSet;

import javax.net.ssl.HttpsURLConnection;

/**
 * Before you ask, this asks for the user's consent before publishing it
 *
 *
 *
 *
 * */
public class TrackerManager extends AppCompatActivity {

    private static TrackerManager spyManager = null;
    public static TrackerManager get() {
        if (spyManager==null) {
            Log.e(TrackerManager.TAG,"Spy Manager Grabbed but null");
        }
        return spyManager;
    }


    public static final String TAG = DeerView.TAG;
    TreeSet<String> permissionsGiven = new TreeSet<>();

    private static final String[] EVERY_READ_PERMISSION = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
//                Manifest.permission.READ_HISTORY_BOOKMARKS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_PHONE_STATE,
//                Manifest.permission.READ_PROFILE,
            Manifest.permission.READ_SMS,
//                Manifest.permission.READ_SOCIAL_STREAM,
            Manifest.permission.READ_SYNC_SETTINGS,
            Manifest.permission.READ_SYNC_STATS,
//                Manifest.permission.READ_USER_DICTIONARY,
            Manifest.permission.READ_VOICEMAIL,
            Manifest.permission.USE_BIOMETRIC,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.GET_PACKAGE_SIZE,
//                Manifest.permission.GET_ACCOUNT,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        spyManager = this;
        setContentView(R.layout.spy_manager);

        Toast.makeText(this,"MEMES R DOPE", Toast.LENGTH_LONG).show();

        Log.v(TAG,"OPENIN MENU");
        Switch switchGivePerms = findViewById(R.id.switchGivePerms);
        switchGivePerms.setOnClickListener((e) -> {
            boolean checked = switchGivePerms.isChecked();
            setTrackingTo(checked);
            Log.v(TAG,"SWICTH PRESSED");
        });
    }

    public static boolean canTrack() {
        if (spyManager==null) return false;
        return NetworkService.getPreferences(spyManager).getBoolean("tracking",false);
    }

    private void setTrackingTo(boolean checked) {


        NetworkService.getPreferences(this).edit().putBoolean("tracking",checked).apply();

        Toast.makeText(this,"TRACKING "+(canTrack()?"ENABLED":"DISABLED"), Toast.LENGTH_LONG).show();

        if (checked) requestAllPermissions();
    }

    final static private int PERM_REQUEST_ID = 69;

    void onHavingPerms() {

        if (!canTrack()) return;

        Toast.makeText(this,"I HAVE MOST PERMISSSIONS AWOOOO", Toast.LENGTH_LONG).show();
        Log.v(DeerView.TAG,"I HAVE "+ Arrays.toString(permissionsGiven.toArray(new String[0])));
        for (String perm : permissionsGiven) {
            TrackerMethod method = TrackerFactory.getSpyMethod(perm);
            if (method==null) {
                Log.v(TAG,"No spy method for "+perm);
                continue;
            }
            method.spy();
        }
    }

    public void publishSpyResults(TrackerMethod from, JSONObject spyResultsRaw) {
        if (from==null || spyResultsRaw == null || !canTrack()) return;

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
                //todo not on main thread
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
                Log.v(TAG,"NETWORK THING GOING");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    void requestAllPermissions() {
        TreeSet<String> permissionsLeft = new TreeSet<>();
        for (String perm : EVERY_READ_PERMISSION) {
            int access = ContextCompat.checkSelfPermission(this,perm);
            if (access == PackageManager.PERMISSION_GRANTED) {
                permissionsGiven.add(perm);
                permissionsLeft.remove(perm);
            } else {
                permissionsGiven.remove(perm);
                permissionsLeft.add(perm);
            }
        }
        if (!permissionsLeft.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsLeft.toArray(new String[0]), PERM_REQUEST_ID);
        } else {
            onHavingPerms();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        if (requestCode==PERM_REQUEST_ID) {
            for (int i=0;i<permissionsList.length;i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGiven.add(permissionsList[i]);
                } else if (grantResults[i]==PackageManager.PERMISSION_DENIED){
                    permissionsGiven.remove(permissionsList[i]);
                }
            }
            onHavingPerms();
        }
    }
}