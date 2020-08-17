package net.datadeer.app.lifestream;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Camera;
import android.media.MediaRecorder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 *  Life Stream - Stream your life, 24/7! Simply let this run in the background, and become internet-famous on datadeer.net/lifestream!
 *
 *
 * */
public class TrackerStreamingService extends Service {

    public final static String TAG = "net.datadeer.app.";


    PeriodicWorkRequest periodicWorkRequest;

    public class UploadWorker extends Worker {
        MediaRecorder mediaRecorder;

        public UploadWorker(@NonNull Context context,@NonNull WorkerParameters params) {
            super(context, params);
        }

        Camera cam;

        @Override
        public Result doWork() {
            setupMediaRecorder();

            for (int i=0;i<10;i++) {
                Log.v(TAG,"DOING WORK");
            }

            // Indicate whether the work finished successfully with the Result
            return Result.success();
        }

        private void setupMediaRecorder() {
//            mCamera = CameraHelper.getDefaultCameraInstance();
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments");
        thread.start();

        periodicWorkRequest =
                new PeriodicWorkRequest.Builder(UploadWorker.class, 15, TimeUnit.MINUTES)
                        .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        WorkManager
                .getInstance(this)
                .enqueue(periodicWorkRequest);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

}
