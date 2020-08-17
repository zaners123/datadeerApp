package net.datadeer.app.lifestream;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pedro.rtplibrary.rtmp.RtmpCamera1;

import net.datadeer.app.DeerView;
import net.ossrs.rtmp.ConnectCheckerRtmp;

public class RTMPViewer extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        RtmpCamera1 rtmp = new RtmpCamera1(this, new ConnectCheckerRtmp() {
            @Override
            public void onConnectionSuccessRtmp() {
                System.out.println("CONNECTION SUCCESSSSSS");
            }

            @Override
            public void onConnectionFailedRtmp(@NonNull String reason) {

            }

            @Override
            public void onNewBitrateRtmp(long bitrate) {

            }

            @Override
            public void onDisconnectRtmp() {

            }

            @Override
            public void onAuthErrorRtmp() {

            }

            @Override
            public void onAuthSuccessRtmp() {

            }
        });
        if (rtmp.prepareAudio() && rtmp.prepareVideo()) {
            Log.v(DeerView.TAG,"PREPARED");
            rtmp.startStream("rtmps://live-api-s.facebook.com:443/rtmp/958004054622478?s_bl=1&s_ps=1&s_sw=0&s_vt=api-s&a=Abza8VMWsgxP8qE_");
        } else {

        }
        //https://github.com/pedroSG94/rtmp-rtsp-stream-client-java
    }
}
