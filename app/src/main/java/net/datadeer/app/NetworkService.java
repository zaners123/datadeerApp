package net.datadeer.app;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class NetworkService extends Service {

    //TODO save notificationShown through restart
    static ArrayList<Long> notificationsShown = new ArrayList<>();

    public final static String TAG = "net.datadeer.app";

    //used so if it is already running, it doesn't run again
    boolean isRunning = false;

    //the cookie used to get your data
    public static final String PREF_FILE = "DeerPref";

    //the last state the app was in

    static void setCookie(Context c, String value) {
        getPreferences(c).edit().putString("cookie",value).apply();
    }
    public static SharedPreferences getPreferences(Context c) {
        return c.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    //This is ran when you give it a request to start
    @Override
    public int onStartCommand(Intent pIntent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startTheUpdates();
        }
        Log.v(TAG,"OnStartCommand network service");

        return super.onStartCommand(pIntent, flags, startId);
    }

    static class Msg {
        String from;
        String body;
        long timeSent;
        Long msgNum;
        Msg(String from, String body, long timeSent, long msgNum) {
            this.from = from;
            this.body = body;
            this.timeSent = timeSent;
            this.msgNum = msgNum;
        }
    }

    //main This is used as a separate-thread task of networking so
    class NetworkTask extends AsyncTask<Integer, Integer, Integer> {
        private WeakReference<NetworkService> activityReference;

        //res is the variable passed through threads of the network result
        String res;

        //The server file is the file it loads, such as "chatGet.php","chatCount.php", etc.
        // It is also used for context of results
        String serverFile;

        //used when getting updated chat
//        String gettingChatWithUser;
//        long gettingChatNum;

        NetworkTask(NetworkService ns, String phoneGrabContext) {
            this.activityReference = new WeakReference<>(ns);
            this.serverFile = phoneGrabContext;
        }
        /*FilePoster(NetworkService ns, String phoneGrabContext, String gettingChatForUser, long gettingChatNum) {
            this.activityReference = new WeakReference<>(ns);
            this.serverFile = phoneGrabContext;
//            this.gettingChatWithUser = gettingChatForUser;
//            this.gettingChatNum = gettingChatNum;
        }*/

        /*This does the frequent network updating to see if it should get anything from the phone JSON file*/
        @Override protected Integer doInBackground(Integer... integers) {
            res = "";

            //you need a cookie
            String mySessionCookie = getPreferences(activityReference.get()).getString("cookie",null);
            if (mySessionCookie == null || mySessionCookie.isEmpty()) {
//                Log.v(TAG, "Not networking because: cookie == "+mySessionCookie);
                return -1;
            }


            //Log.v(TAG, "Hey its starting a network update");
            try {
                //if you are getting a chat for a user, add the GET to it
//                if (gettingChatWithUser != null) {
//                    res = downloadURL(mySessionCookie,new URL("https://datadeer.net/phone/"+serverFile+"?user="+ gettingChatWithUser+"&chatNum="+ gettingChatNum));
//                } else {
                    res = downloadURL(mySessionCookie,new URL("https://datadeer.net/phone/"+serverFile));
//                }
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }

            //get the status (explained in phoneStatus.php)
            String serverStatus = res.substring(0, res.indexOf('\n'));
            //after getting the server status, trim it from res
            res = res.substring(1+res.indexOf('\n'));
            switch (serverStatus) {
                case "SIGNEDIN":
                    //Log.v(TAG, "I am networking; I am signed in");
                    return 1;
                case "NEEDTOSIGNIN":
//                    Log.v(TAG, "I tried networking, but need a signed in session");
                    return 2;
                case "NEEDCOOKIE":
//                    Log.v(TAG, "I tried networking, but never had a cookie");
                    return 3;
                default:
//                    Log.v(TAG, "Unknown status");
                    return 4;
            }
        }
        String downloadURL(String mySessionCookie, URL url) throws IOException {
            HttpsURLConnection con = null;
            InputStream inStream = null;
            String ret = "";
            try {
                con = (HttpsURLConnection)url.openConnection();
                //timeout for reading MS
                con.setReadTimeout(8000);
                //timeout for establishing connection I think; MS
                con.setConnectTimeout(8000);
                //use GET
                con.setRequestMethod("GET");
                //no caching or else it will not update
                con.setDefaultUseCaches(false);
                con.setUseCaches(false);
                //get some input
                con.setDoInput(true);

                //sign me in
//                Log.v(TAG, "Network - Sending cookie: \""+mySessionCookie+'\"');
                con.setRequestProperty("Cookie",mySessionCookie);


                //open it and start the networking
                con.connect();


                //respond to errors
                int responseCode = con.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    //error response
                    throw new IOException("HTTP error code " +responseCode);
                }

                //get the response body
                inStream = con.getInputStream();

                //read the stream and output it
                if (inStream != null) {
                    ret = readStream(inStream, 100_000);
                }
            } finally {
                if (inStream!=null) inStream.close();
                if (con != null) con.disconnect();
            }
            return ret;
        }
        @Override
        protected void onPostExecute(Integer status) {
            NetworkService ns = activityReference.get();

            if (ns == null || status != 1) return;

            switch (serverFile) {
                case "sendNotRead.php":
                    /*main every time this is called you need to:

                    *  parse the JSON onto a messagesToSend ArrayList
                    *
                    *  If the currently showing messages are not still showing, remove them
                    *
                    *  Add all the other messages
                    */
//                    Log.v(TAG, "got sendNotRead");

                    NotificationManagerCompat nmc = NotificationManagerCompat.from(ns);
                    ArrayList<Msg> messagesToSend = new ArrayList<>();

                    //main load all the messages
                    try {
                        //read the chat JSON and see if we should load some chats
                        JSONArray counts = new JSONArray(res);
                        for (int i = 0; i < counts.length(); i++) {
                            //load the JSON object from the array {user:"deer",count:3}
                            JSONObject o = (JSONObject) counts.get(i);
                            //main add a message into the send list
                            messagesToSend.add(new Msg(
                                    o.getString("msgfrom"),
                                    o.getString("msg"),
                                    Long.parseLong(o.getString("msgtime")) * 1000,
                                    Long.parseLong(o.getString("msgnum"))
                            ));
//                            Log.v(TAG, "Message to notify in queue: "+messagesToSend.get(messagesToSend.size()-1));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //main add new notifications (for every message, if its not already shown...
                    for (Msg m : messagesToSend) {
                        if (!notificationsShown.contains(m.msgNum)) {
                            //main add new notifications
//                            Log.v(TAG, "Adding notification "+m.msgNum);
                            showMessageNotification(nmc, ns, m);
                            //main add it to notificationsShown
                            notificationsShown.add(m.msgNum);
                        }
                    }

                    //main remove old notifications (for every notification, if its no longer there...
                    for (int notifyNum = 0; notifyNum < notificationsShown.size(); notifyNum++) {
                        Long notify = notificationsShown.get(notifyNum);
                        boolean stillNeeded = false;

                        for (Msg m : messagesToSend) {
                            if (m.msgNum.equals(notify)) stillNeeded=true;
                        }

                        if (!stillNeeded) {
//                            Log.v(TAG, "Removing notification msg number "+notify);
                            //main remove that notification, it has been read
                            nmc.cancel(notify.intValue());
                            //remove it from the notification table
                            notificationsShown.remove(notifyNum);
                            notifyNum--;
                        } else {
//                            Log.v(TAG, "Not removing notification msg number"+notify);
                        }
                    }
                    break;
                default:
//                    Log.e(TAG, "Unknown serverFile");
            }
        }
        @NonNull
        String readStream(InputStream stream, int maxReadSize) throws IOException {
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            char[] rawBuffer = new char[maxReadSize];
            int readSize;
            StringBuffer buffer = new StringBuffer();
            while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
                if (readSize > maxReadSize) {
                    readSize = maxReadSize;
                }
                buffer.append(rawBuffer, 0, readSize);
                maxReadSize -= readSize;
            }
            return buffer.toString();
        }
        /*void considerMessaging() {
            long nextMessage = gettingChatNum+1;
            if (nextMessage < messagesNeeded.get(gettingChatWithUser)) {
                Log.v(TAG, "From stack, getting message ("+nextMessage+"/"+messagesNeeded.get(gettingChatWithUser)+")");
                //get the next message
                FilePoster nt = new FilePoster(activityReference.get(), "chatGet.php",gettingChatWithUser, nextMessage);
                nt.execute();
            }
        }*/
    }
    //This is the NetworkService function that runs once in order to make the asynchronous task
    void startTheUpdates() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    try {
                        NetworkTask nt = new NetworkTask(NetworkService.this, "sendNotRead.php");
                        nt.execute();
                    } catch (Exception ignored) {
                    }
                });
            }
        };
        //TODO make a setting for notification check frequency
        timer.schedule(doAsynchronousTask, 0, 10_000); //execute in every x ms
    }

    private static void showMessageNotification(NotificationManagerCompat nmc, @NonNull NetworkService service, Msg msg) {
        //give it an intent (where you go after clicking the notification)
        Intent intent = new Intent(service, DeerView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("msgfrom",msg.from);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //make the notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(service.getApplicationContext(), "usr"+msg.from)
                .setSmallIcon(R.drawable.deer_notify) // notification icon
                .setContentTitle(msg.from+" sent you") // title for notification
                .setContentText(msg.body)// message for notification

                .setWhen(msg.timeSent)//set the time to when the message was sent to the user

                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // clear notification after click
                .setContentIntent(pendingIntent);

        //notify with a unique number for removing or changing (or just the time)
        nmc.notify(msg.msgNum.intValue(), mBuilder.build());
    }
}
