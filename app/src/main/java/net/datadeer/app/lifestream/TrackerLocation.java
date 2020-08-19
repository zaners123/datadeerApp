package net.datadeer.app.lifestream;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import static net.datadeer.app.DeerView.TAG;

public class TrackerLocation extends TrackerMethod {

    private static final long REFRESH_MS = 2 * 60 * 1000;// 5 minutes recommended, we'll go for 2
    private static final float REFRESH_MIN_METERS = 10;
//    private static final long REFRESH_MS = 2_000;
//    private static final float REFRESH_MIN_METERS = 0;

    TrackerLocation(){this("Location");}
    public TrackerLocation(String name) {
        super(name);
    }

    long lastLocTimeSent = 0;

    void publishLoc(Location loc) {
        if (loc==null) return;
        if (loc.getTime()== lastLocTimeSent) return;
        lastLocTimeSent = loc.getTime();
        try {
            JSONObject container = new JSONObject();
            JSONObject now = new JSONObject();
            now.put("longitude",loc.getLongitude());
            now.put("latitude",loc.getLatitude());
            now.put("altitude",loc.getAltitude());
            now.put("accuracy",loc.getAccuracy());
            now.put("speed",loc.getSpeed());
            now.put("bearing",loc.getBearing());
            now.put("time",loc.getTime());
            if (loc.getExtras() != null) {
                Set<String> keyset = loc.getExtras().keySet();
                for (String key : keyset) {
                    if (key==null) continue;
                    Object val = loc.getExtras().get(key);
                    if (val==null) continue;
                    now.put("extra"+key,val.toString());
                }
            }

            String lastRec = ""+loc.getTime();
            container.put(lastRec,now);
            container.put("last-recieved",lastRec);
            TrackerService.publishSpyResults(this,container);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override public void spy() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;

        final LocationListener mLocationListener = new LocationListener() {
            @Override public void onLocationChanged(final Location location) {
                Log.v(TAG,"Sent location update");
                publishLoc(location);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        LocationManager mLocationManager;
        mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        Log.v(TAG,"Requesting to send locationupdate");
        mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, REFRESH_MS, REFRESH_MIN_METERS, mLocationListener,getHandler().getLooper());
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, REFRESH_MS, REFRESH_MIN_METERS, mLocationListener,getHandler().getLooper());
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REFRESH_MS, REFRESH_MIN_METERS, mLocationListener,getHandler().getLooper());

        publishLoc(mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
    }
}