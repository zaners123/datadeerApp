package net.datadeer.app.lifestream;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public abstract class TrackerMethod {

    private boolean running;
    private String name;
    private Context context;
    private Handler handler;
    private Looper looper;

    public TrackerMethod(String name) {
        this.name = name;
    }

    public Context getContext() {return context;}
    public void setContext(Context context) {this.context = context;}
    public void setHandler(Handler handler) {
        this.handler = handler;
    }
    public Handler getHandler() {
        return handler;
    }

    public boolean isRunning(){return running;}
    final void run() {
        if (running) return;
        running = true;
        spy();
    }

    protected abstract void spy();

    public final String getName() {return name;}

    public void setLooper(Looper looper) {
        this.looper = looper;
    }

    public Looper getLooper() {
        return looper;
    }

    protected void doneSpying() {
        running = false;
    }
}
