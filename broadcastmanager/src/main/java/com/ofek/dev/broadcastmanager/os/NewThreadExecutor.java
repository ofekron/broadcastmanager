package com.ofek.dev.broadcastmanager.os;

import android.os.HandlerThread;
import android.os.Looper;

public class NewThreadExecutor extends AndroidExecutor {


    private final String name;
    private HandlerThread handlerThread;

    NewThreadExecutor(String name) {
        super();
        this.name=name;
    }

    @Override
    public void init() {
        handlerThread = new HandlerThread(name);
        handlerThread.start();
        super.init();
    }

    @Override
    protected Looper getLooper() {
        return handlerThread.getLooper();
    }

}
