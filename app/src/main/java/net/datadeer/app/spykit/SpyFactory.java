package net.datadeer.app.spykit;

import android.Manifest;

class SpyFactory {
    static SpyMethod getSpyMethod(String manifestName) {
        switch (manifestName) {
            case Manifest.permission.READ_CONTACTS: return new SpyContacts();
            default:return null;

        }
    }
}
