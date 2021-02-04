package com.ofek.dev.broadcastmanager.os;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.ofek.dev.broadcastmanager.utils.Executor;

abstract class AndroidExecutor implements Executor {
    private Handler handler;
    private long id;
    private Thread thread;

    AndroidExecutor() {

    }

    protected abstract Looper getLooper();

    @Override
    public boolean isSame(@NonNull Thread thread) {
        return this.thread.equals(thread);
    }
    public void init() {
        if (handler!=null) return;
        handler = new Handler(getLooper());
        thread = handler.getLooper().getThread();
        id=thread.getId();
    }

    @Override
    public void destroy() {
        handler=null;
        thread=null;
    }

    @Override
    public void execAsync(Runnable r) {
        handler.post(r);
    }

    @Override
    public boolean isCurrentThread() {
        return isSame(Thread.currentThread());
    }

    @Override
    public void execSync(Runnable o) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                o.run();
                synchronized (this) {
                    notify();
                }
            }
        };
        synchronized (r) {
            handler.post(r);
            try {
                r.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void execFirstAsync(Runnable o) {
        handler.postAtFrontOfQueue(o);
    }

    @Override
    public long getId() {
        return id;
    }
}
