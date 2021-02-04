package com.ofek.dev.broadcastmanager.os;

import android.os.Looper;

public class MainThreadExecutor extends AndroidExecutor {


    @Override
    protected Looper getLooper() {
        return Looper.getMainLooper();
    }


}
