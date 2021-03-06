package net.datadeer.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.datadeer.app.lifestream.TrackerManager;
import net.datadeer.app.lifestream.TrackerService;

public class DeerView extends AppCompatActivity {
    public final static String TAG = "net.datadeer.app";

    WebView wv;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deer_view);

        //start necessary services
        startService(new Intent(DeerView.this, NetworkService.class));
        startService(new Intent(DeerView.this, TrackerService.class));

        wv = findViewById(R.id.my_deer_webview);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.getSettings().setAppCacheEnabled(false);
        wv.clearCache(true);
        wv.getSettings().setSupportZoom(true);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebChromeClient(new WebChromeClient() {
            //needed for javascript
        });
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.e(TAG,"URL is "+url);
                if (url.contains("appredirectyep.php")) {
                    startActivity(new Intent(DeerView.this, TrackerManager.class));
                }
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null && cookies.contains("PHPSESSID")) {
                    NetworkService.setCookie(DeerView.this,cookies);
                    Log.v(TAG, "Hey have my cookies: "+cookies);
                }
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        setState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        setState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setState(null);
    }

    boolean loadingMessages = false;
    void setState(Bundle state) {
        //where it is going to load to
        String loadTo = "https://datadeer.net?rem=true";

        if (loadingMessages) {
            loadingMessages = false;
            return;
        }
        //the user the message is from (if it loads there)
        String from = null;

        Log.v(TAG, "DeerView getting desired state");
        Bundle extras = getIntent().getExtras();
        if (extras!=null) {
            from = extras.getString("msgfrom", null);
            Log.v(TAG, "DeerView Got "+from+" from extras");
            getIntent().removeExtra("msgfrom");
            loadingMessages = true;
        } else if (state != null) {
            from = (String)state.getSerializable("msgfrom");
            Log.v(TAG, "DeerView Got "+from+" from state");
        }
        //if from is actually a thing, go to that
        if (from != null && from.length() >= 4) {
            loadTo = "https://datadeer.net/chats/pchat/chatroom.php?user=" + from;
        }

        Log.v(TAG, "DeerView going to "+loadTo);
        wv.loadUrl(loadTo);
    }

    @Override
    public void onBackPressed() {
        if (wv.canGoBack()) {
            wv.goBack();
        } else {
            super.onBackPressed();
        }
    }
}