package net.datadeer.app.lifestream;

import android.Manifest;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;


public class TrackerLocation extends TrackerMethod {

    private TrackerManager that;

    private static final long REFRESH_MS = 60_000;
    private static final float REFRESH_MIN_METERS = 10;

    void publishLoc(Location loc) {
        if (loc==null) return;
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
            that.publishSpyResults(this,container);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override public void spy() {


        JobScheduler js;

        this.that = TrackerManager.get();
        if (that==null) return;
        if (ActivityCompat.checkSelfPermission(that, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(that, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;
        LocationManager mLocationManager;
        LocationListener mLocationListener = new LocationListener() {
            @Override public void onLocationChanged(final Location location) {
                publishLoc(location);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };
        mLocationManager = (LocationManager) that.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REFRESH_MS, REFRESH_MIN_METERS, mLocationListener);

        Location loc = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        publishLoc(loc);

    }

    @Override
    public String getName() {
        return "Location";
    }
}
