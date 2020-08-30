package net.datadeer.app.lifestream;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.datadeer.app.DeerView;
import net.datadeer.app.R;

import java.util.TreeSet;

/**
 * Before you ask, this asks for the user's consent before publishing it
 *
 * */
public class TrackerManager extends AppCompatActivity {
    private static final String TAG = DeerView.TAG;
    private LinearLayout switches;

    void addSwitch(SwitchCompat s) {
        s.setGravity(Gravity.CENTER_HORIZONTAL);
        s.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,20,0,0);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        switches.addView(s,-1, lp);
    }

    void addSwitches() {
        for (TrackerOption opt : TrackerService.trackers) {
            opt.s = new SwitchCompat(this);
            opt.s.setChecked(TrackerService.getTrackingEnabled(this, opt.tracker));
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
        TrackerService.setTrackingEnabled(opt,this,checked);
        opt.s.setChecked(checked);
        if (checked) requestPermissions(opt.perms);
    }

    void verifyTrackersHaveNecessaryPerms() {
        for(TrackerOption opt : TrackerService.trackers) {
            boolean hasPerms = TrackerService.hasPerms(this, opt.perms);
            if (!hasPerms) setTrackerTo(opt,false);
        }
    }

    /**Requests given list of permissions*/
    void requestPermissions(String... perms) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())),
                    TrackerService.PERM_OVERLAY_ID
                );
            }
        }
        TreeSet<String> permissionsLeft = new TreeSet<>();
        for (String perm : perms) {
            int access = ContextCompat.checkSelfPermission(this,perm);
            if (access == PackageManager.PERMISSION_GRANTED) {
                permissionsLeft.remove(perm);
            } else {
                permissionsLeft.add(perm);
            }
        }
        if (!permissionsLeft.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsLeft.toArray(new String[0]), TrackerService.PERMISSION_TO_OVERLAY_REQUEST_ID);
        } else {
            verifyTrackersHaveNecessaryPerms();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissionsList, int[] grantResults) {
        verifyTrackersHaveNecessaryPerms();
    }
}