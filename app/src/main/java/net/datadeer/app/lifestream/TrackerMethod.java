package net.datadeer.app.lifestream;

import android.content.Context;
import android.os.Handler;

public abstract class TrackerMethod {

    private boolean running;
    private String name;
    private Context context;
    private Handler handler;

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
    final boolean run() {
        if (running) return false;
        running = true;
        spy();
        return true;
    }

    protected abstract void spy();

    public final String getName() {return name;}
}
