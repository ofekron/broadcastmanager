package com.ofek.dev.broadcastmanager.utils;

public interface Executor {

    boolean isSame(Thread thread);
    void init();
    void destroy();

    void execAsync(Runnable o);

    boolean isCurrentThread();

    void execFirstAsync(Runnable o);

    void execSync(Runnable o);

    long getId();
}
