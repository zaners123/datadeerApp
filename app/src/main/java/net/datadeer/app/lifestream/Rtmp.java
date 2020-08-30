package net.datadeer.app.lifestream;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;

import net.datadeer.app.DeerView;
import net.datadeer.app.NetworkService;
import net.datadeer.app.R;
import net.ossrs.rtmp.ConnectCheckerRtmp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Rtmp extends AppCompatActivity {

    public static final String HOST = "192.168.0.251";//todo set to datadeer.net
    public static final int RTMP_PORT = 51818;

    private static final String TAG = DeerView.TAG;

    public static class Pointer<E> {
        E e;
    }

    public static final ConnectCheckerRtmp DEBUG_CCR = new ConnectCheckerRtmp() {
        @Override public void onConnectionSuccessRtmp() {Log.d(TAG,"DEBUG_CCR RTMP ON connection success yay");}
        @Override public void onConnectionFailedRtmp(@NonNull String reason) {Log.e(TAG,"DEBUG_CCR RTMP ON connection failed "+reason);}
        @Override public void onNewBitrateRtmp(long bitrate) {Log.d(TAG,"DEBUG_CCR RTMP ON NEW bitrate "+bitrate );}
        @Override public void onDisconnectRtmp() {Log.d(TAG,"DEBUG_CCR RTMP ON DISCONNECT");}
        @Override public void onAuthErrorRtmp() {Log.e(TAG,"DEBUG_CCR RTMP ON AUTH ERROR");}
        @Override public void onAuthSuccessRtmp() {Log.d(TAG,"DEBUG_CCR RTMP AUTH SUCCESS AWOOOOO");}
    };

    public static Pointer<RtmpCamera1> init(Context c, SurfaceView view) {
        Pointer<RtmpCamera1> ret = new Pointer<>();
        Log.v(TAG,"RTMP rtmp_test created");
        view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                ret.e = new RtmpCamera1(view, DEBUG_CCR);
                startStreaming(c, ret.e);
            }
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
        });
        return ret;
    }

    public static void stop(RtmpCamera1 cam) {
        cam.stopPreview();
        cam.stopStream();
    }

    public static void startStreaming(Context c, RtmpCamera1 cam) {
        boolean hasMic;
        boolean hasCam;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            hasMic = cam.prepareAudio();
            hasCam = cam.prepareVideo();
            if (!hasCam || !hasMic) {
                Log.e(TAG, "RTMP HAS CAM?" +hasCam+" has mic?" +hasMic);
                return;
            }
            Log.v(TAG,"Has Cam and mic, run");
            String appName;
            String username = "deer";
            String password = NetworkService.getCookie(c);
            try {
                appName = "videochat/"+URLEncoder.encode(username,String.valueOf(StandardCharsets.UTF_8));
                password = URLEncoder.encode(password, String.valueOf(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                Log.wtf(TAG,"This device doesn't support UTF-8...");
                e.printStackTrace();
                return;
            }
            String fullUrl = String.format("rtmp://%s:%s/%s?creds=%s", HOST, RTMP_PORT,appName,password);
            Log.d(TAG,"Streaming RTMP to url "+fullUrl);
            cam.setFpsListener(fps -> Log.v(TAG,String.format("FPS: %d",fps)));
            cam.startPreview(CameraHelper.Facing.FRONT);
            Log.d(TAG,"IS ON PREVIEW? "+cam.isOnPreview());
            cam.startStream(fullUrl);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rtmp_test);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.rtmp_test);
    }

    @Override
    protected void onStart() {
        super.onStart();
        init(this, findViewById(R.id.surfaceView));
    }
}
