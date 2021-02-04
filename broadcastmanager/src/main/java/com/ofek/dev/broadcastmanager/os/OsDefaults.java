package com.ofek.dev.broadcastmanager.os;


import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.ofek.dev.broadcastmanager.BroadcastManager;
import com.ofek.dev.broadcastmanager.utils.Executor;

public class OsDefaults {


    public static final Executor mainThreadExecutor=new MainThreadExecutor();
    static {
        mainThreadExecutor.init();
    }
    public static final Executor currentThreadExecutor() {
        CurrentThreadExecutor curr = new CurrentThreadExecutor();
        curr.init();
        return curr;
    }
    public static final Executor newThreadExecutor(String name) {
        NewThreadExecutor t = new NewThreadExecutor(name);
        t.init();
        return t;
    }

    public static BroadcastManager.ExecutorFactory createDefaultExecutorFactory() {
        return new BroadcastManager.ExecutorFactory() {
            @Override
            public Executor createExecutor(String name) {
                return new NewThreadExecutor(name);
            }
        };

    }

    public static String toString(Object o) {
        return Integer.toHexString(o.hashCode());
    }

    public static BroadcastManager.StateListenerCleaner cleanOnDestroy(LifecycleOwner lifecycleOwner) {
        return new BroadcastManager.StateListenerCleaner() {

            @Override
            public void setCleanUpJob(Runnable cleanUpJob) {
                lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    public void onDestroy() {
                        cleanUpJob.run();
                        lifecycleOwner.getLifecycle().removeObserver(this);
                    }
                });
            }

        };

    }

}
