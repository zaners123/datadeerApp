package net.datadeer.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.datadeer.app.lifestream.TrackerService;

public class ReceiveBoot extends BroadcastReceiver {
    final static String TAG = "net.datadeer.app";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.v(TAG,"onReceive with action "+intent.getAction());

        if (
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MAIN.equals(intent.getAction())
                )
        context.startService(new Intent(context, NetworkService.class));
        context.startService(new Intent(context, TrackerService.class));
    }
}