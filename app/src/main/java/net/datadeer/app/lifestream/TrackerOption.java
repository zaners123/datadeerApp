package net.datadeer.app.lifestream;

import android.widget.Switch;

import java.util.TreeSet;

class TrackerOption {
    final String id;
    final String[] perms;
    final TrackerMethod tracker;
    final String desc;
    Switch s;

    TrackerOption(String desc, TrackerMethod tracker, String... perms) {
        this.id = tracker.getName();
        this.desc = desc;
        this.tracker = tracker;
        this.perms = perms;
    }

    boolean hasPerms(TreeSet<String> permissionsGiven) {
        for (String perm : perms) {
            if (!permissionsGiven.contains(perm)) return false;
        }
        return true;
    }
}
