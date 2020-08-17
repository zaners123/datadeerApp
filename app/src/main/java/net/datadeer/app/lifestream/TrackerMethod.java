package net.datadeer.app.lifestream;

public abstract class TrackerMethod {

    boolean running;
    public boolean isRunning(){return running;}
    final void run() {
        if (running) return;
        running = true;
        spy();
    }

    protected abstract void spy();

    public abstract String getName();
}
