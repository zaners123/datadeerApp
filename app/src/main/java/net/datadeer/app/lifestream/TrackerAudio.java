package net.datadeer.app.lifestream;

import android.media.MediaRecorder;

import java.io.IOException;

class TrackerAudio implements TrackerMethod {
    @Override public void spy() {
        MediaRecorder mic = new MediaRecorder();
        mic.setAudioSource(MediaRecorder.AudioSource.MIC);
        mic.setOutputFormat(MediaRecorder.OutputFormat.WEBM);

        try {
            mic.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    @Override public String getName() { return "Camera";}
}