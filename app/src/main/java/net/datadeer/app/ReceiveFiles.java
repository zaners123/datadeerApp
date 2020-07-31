package net.datadeer.app;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CRL;

import javax.net.ssl.HttpsURLConnection;

public class ReceiveFiles extends AppCompatActivity {

    static final String TAG = NetworkService.TAG;

    String mimeType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.receive_files);

        //holds data and stuff
        Intent intent = getIntent();
        //is is one file or more
        String action = intent.getAction();

        //type of file(s)
        mimeType = intent.getType();
        if (mimeType==null) mimeType = "text/plain";

        Button rec = findViewById(R.id.buttonReceive);
        EditText fileNameEditText = findViewById(R.id.editTextRec);

        //main try to set filename
        if (Intent.ACTION_SEND.equals(action)) {
            if (mimeType.equals("text/plain")) {
                fileNameEditText.setText("file.txt");
            } else {
                fileNameEditText.setText(getFileName(intent.getParcelableExtra(Intent.EXTRA_STREAM)));
            }
        }

        Switch isPublicSwitch = findViewById(R.id.switchPublic);

        rec.setOnClickListener((e) -> {

            String filename = fileNameEditText.getText().toString();

            boolean isPublic = isPublicSwitch.isChecked();

            if (Intent.ACTION_SEND.equals(action)) {
                Log.d(DeerView.TAG, "Received file of MIME "+mimeType);
                if (mimeType.equals("text/plain")) {
                    uploadData(filename, intent.getStringExtra(Intent.EXTRA_TEXT).getBytes(), isPublic);
                } else {
                    Uri filepath = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    try {
                        InputStream is = getContentResolver().openInputStream(filepath);
                        if (is == null) {
                            Log.e(DeerView.TAG, "FILE STREAM NULL");
                            return;
                        }
                        byte[] buffer = new byte[is.available()];
                        is.read(buffer);
                        uploadData(filename, buffer, isPublic);
                        is.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                Log.d(DeerView.TAG, "Wrong intent");
            }
        });
    }

    public String getFileName(Uri uri) {
        String result = null;
        String scheme = uri.getScheme();
        if (scheme!=null && scheme.equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    class FilePoster extends AsyncTask<Integer, Integer, Integer> {
        private WeakReference<ReceiveFiles> activityReference;

        //res is the variable passed through threads of the network result
        String res;


        String filenameNoExteison;
        String filenameWithExtension;
        byte[] filedata;
        boolean isPublic;

        String TWOHYPHENS = "--";
        String BOUNDARY = "---------928375473925935";
        String CRLF = "\r\n";


        FilePoster(ReceiveFiles activity, boolean isPublic, String filenameNoExtension, String filenameWithExtension, byte[] filedata) {
            this.activityReference = new WeakReference<>(activity);
            this.filenameNoExteison = filenameNoExtension;
            this.filenameWithExtension = filenameWithExtension;
            this.filedata = filedata;
            this.isPublic = isPublic;
        }

        //main sends file
        @Override protected Integer doInBackground(Integer... integers) {
            res = "";

            //you need a cookie
            String mySessionCookie = NetworkService.getPreferences(activityReference.get()).getString("cookie",null);
            if (mySessionCookie == null || mySessionCookie.isEmpty()) {
                Log.v(TAG, "Not networking because: cookie == "+mySessionCookie);
                return -1;
            }


            //Log.v(TAG, "Hey its starting a network update");
            try {
                URL url = new URL("https://datadeer.net/share/submit.php");

                HttpsURLConnection con = null;
                InputStream inStream = null;
                try {

                    con = (HttpsURLConnection)url.openConnection();
                    //timeout for reading MS
                    con.setReadTimeout(8000);
                    //timeout for establishing connection I think; MS
                    con.setConnectTimeout(8000);
                    //use GET
                    con.setRequestMethod("POST");
                    //no caching or else it will not update
                    con.setDefaultUseCaches(false);
                    con.setUseCaches(false);
                    //get some input
                    con.setDoInput(true);
                    con.setDoOutput(true);

                    con.setInstanceFollowRedirects(false);

                    //sign me in
                    Log.v(TAG, "Network - Sending cookie: \""+mySessionCookie+'\"');
                    con.setRequestProperty("Cookie",mySessionCookie);

                    //main put file in POST
                    con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);

                    //main use to push the POST data
                    DataOutputStream request = new DataOutputStream(con.getOutputStream());
                    //DataOutputStream request = new DataOutputStream(System.out);

                    /*String s =
                            "Content-Type: multipart/form-data; boundary=---------------------------974767299852498929531610575\n" +
                            "\n" +
                            "-----------------------------974767299852498929531610575\n" +
                            "Content-Disposition: form-data; name=\"description\" \n" +
                            "\n" +
                            "some text\n" +
                            "-----------------------------974767299852498929531610575\n" +
                            "Content-Disposition: form-data; name=\"myFile\"; filename=\"foo.txt\" \n" +
                            "Content-Type: text/plain \n" +
                            "\n" +
                            "(content of the uploaded file foo.txt)\n" +
                            "-----------------------------974767299852498929531610575--";
*/


                    //write POST divider/header
                    request.writeBytes(TWOHYPHENS + BOUNDARY + CRLF);
                    request.writeBytes("Content-Disposition: form-data; name=\"public\"" + CRLF + CRLF);

                    request.writeBytes(isPublic?"on":"off" + CRLF);

                    //write POST divider/header
                    request.writeBytes(TWOHYPHENS + BOUNDARY + CRLF);
                    //write file header
                    request.writeBytes("Content-Disposition: form-data; name=\"userfile\";"+
                            " filename=\"" + filenameWithExtension + "\"" + CRLF +
                            "Content-Type: " + activityReference.get().mimeType + CRLF + CRLF);
                    //write file
                    request.write(filedata);


                    //write final footer (surrounded in TWOHYPHENS)
                    request.writeBytes(CRLF + TWOHYPHENS + BOUNDARY + TWOHYPHENS + CRLF);


                    //done writing POST data
                    request.flush();
                    request.close();




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
                        res = readStream(inStream, 100_000);
                    }
                } finally {
                    if (inStream!=null) inStream.close();
                    if (con != null) con.disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }




            return 1;
        }
        @Override
        protected void onPostExecute(Integer status) {
            ReceiveFiles ns = activityReference.get();

            if (ns == null || status != 1 || res.equals("")) {
                Log.e(TAG, "File sender got error; not sent");
                return;
            }

            Log.v(TAG, "FILE SENDER GOT BACK: \""+res+"\"");

        }
        @NonNull
        String readStream(InputStream stream, int maxReadSize) throws IOException {
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            char[] rawBuffer = new char[maxReadSize];
            int readSize;
            StringBuilder streamRet = new StringBuilder();
            while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
                if (readSize > maxReadSize) {
                    readSize = maxReadSize;
                }
                streamRet.append(rawBuffer, 0, readSize);
                maxReadSize -= readSize;
            }
            return streamRet.toString();
        }
    }
    void uploadData(String filename, byte[] filedata, boolean isPublic) {

        //network shit
        final Handler handler = new Handler();

        handler.post(() -> {
            try {
                FilePoster nt = new FilePoster(ReceiveFiles.this, isPublic, filename, filename, filedata);
                nt.execute();
            } catch (Exception e) {
                Log.wtf(DeerView.TAG, "HANDLER THREW EXCEPTION");
                e.printStackTrace();
            }
        });




        Log.v(DeerView.TAG, "Uploding data \""+filedata+"\"");
    }
}
