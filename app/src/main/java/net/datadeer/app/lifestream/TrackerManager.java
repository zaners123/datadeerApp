package net.datadeer.app.lifestream;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
    private static final String TAG = DeerView.TAG;
    private TreeSet<String> permissionsGiven = new TreeSet<>();
    /*private static final String[] EVERY_READ_PERMISSION = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_SYNC_SETTINGS,
            Manifest.permission.READ_SYNC_STATS,
            Manifest.permission.READ_VOICEMAIL,
            Manifest.permission.USE_BIOMETRIC,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.GET_PACKAGE_SIZE,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            //                Manifest.permission.READ_HISTORY_BOOKMARKS,
            //                Manifest.permission.READ_PROFILE,
            //                Manifest.permission.READ_SOCIAL_STREAM,
            //                Manifest.permission.READ_USER_DICTIONARY,
            //                Manifest.permission.GET_ACCOUNT,
    };*/

    private LinearLayout switches;

    public static final TrackerOption[] trackers = {
        new TrackerOption("Device Location", new TrackerLocation(), Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
        new TrackerOption("Your Phone Number", new TrackerPhoneNumbers(), Manifest.permission.READ_PHONE_NUMBERS),
        new TrackerOption("Contacts", new TrackerContacts(), Manifest.permission.READ_CONTACTS),
    };

    void addSwitch(Switch s) {
        s.setGravity(Gravity.CENTER_HORIZONTAL);
        s.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,20,0,0);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        switches.addView(s,-1, lp);
    }

    void addEverythingSwitch() {
        Switch s = new Switch(this);
        s.setText(R.string.everything_switch);
        s.setOnClickListener((e) -> {
            for (TrackerOption opt : trackers) {
                setTrackerTo(opt, s.isChecked());
            }
        });
        addSwitch(s);
    }
    void addSwitches() {
        addEverythingSwitch();
        for (TrackerOption opt : trackers) {
            opt.s = new Switch(this);
            opt.s.setChecked(TrackerService.getTrackingEnabled(opt.tracker, this));
            opt.s.setText(opt.desc);
            opt.s.setOnClickListener((e) -> setTrackerTo(opt,opt.s.isChecked()));
            addSwitch(opt.s);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracker_manager);
        switches = findViewById(R.id.check_scroll);
        addSwitches();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void setTrackerTo(TrackerOption opt, boolean checked) {
        TrackerService.setTrackingEnabled(opt,this,true);
        if (checked) requestPermissions(opt.perms);
        opt.s.setChecked(checked);
    }

    final static private int PERM_REQUEST_ID = 69;

    void onHavingPerms() {
        Toast.makeText(this,"Perms updated", Toast.LENGTH_LONG).show();
        Log.v(DeerView.TAG,"I HAVE "+ Arrays.toString(permissionsGiven.toArray(new String[0])));
        for(TrackerOption opt : trackers) {
            boolean hasPerms = opt.hasPerms(permissionsGiven);
            if (!hasPerms) opt.s.setChecked(false);
            //todo update TrackerService
        }
    }

    void requestPermissions(String... perms) {
        TreeSet<String> permissionsLeft = new TreeSet<>();
        for (String perm : perms) {
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
    public void onRequestPermissionsResult(int requestCode, String[] permissionsList, int[] grantResults) {
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