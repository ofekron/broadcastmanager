package com.ofek.dev.broadcastmanager.os;

import android.os.Looper;

public class CurrentThreadExecutor extends AndroidExecutor {

    @Override
    protected Looper getLooper() {
        return Looper.myLooper();
    }

    @Override
    public void execSync(Runnable o) {
        o.run();
    }
}
