package net.datadeer.app.lifestream;

import android.Manifest;

/**
 * @deprecated TrackerManager has a new and improved list
 * */
class TrackerFactory {
    static TrackerMethod getSpyMethod(String manifestName) {
        switch (manifestName) {
            case Manifest.permission.READ_CONTACTS: return new TrackerContacts();
            case Manifest.permission.ACCESS_COARSE_LOCATION:return new TrackerLocation();
            case Manifest.permission.READ_PHONE_NUMBERS: return new TrackerPhoneNumbers();
            case Manifest.permission.CAMERA:return new TrackerCamera();
            case Manifest.permission.RECORD_AUDIO:return new TrackerAudio();
            case Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS:
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_NETWORK_STATE:
            case Manifest.permission.ACCESS_NOTIFICATION_POLICY:
            case Manifest.permission.ACCESS_WIFI_STATE:
            case Manifest.permission.READ_CALENDAR:
            case Manifest.permission.READ_CALL_LOG:
            case Manifest.permission.READ_PHONE_STATE:
            case Manifest.permission.READ_SMS:
            case Manifest.permission.READ_SYNC_SETTINGS:
            case Manifest.permission.READ_SYNC_STATS:
            case Manifest.permission.READ_VOICEMAIL:
            case Manifest.permission.USE_BIOMETRIC:
            case Manifest.permission.BODY_SENSORS:
            case Manifest.permission.GET_PACKAGE_SIZE:
            case Manifest.permission.GET_ACCOUNTS:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.INTERNET:
            default:return null;

        }
    }
}
