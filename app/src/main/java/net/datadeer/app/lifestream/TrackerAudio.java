package net.datadeer.app.lifestream;

import android.media.MediaRecorder;

import java.io.IOException;

class TrackerAudio extends TrackerMethod {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public TrackerAudio(String name) {
        super(name);
    }

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
}