package com.ofek.dev.broadcastmanager;

public interface BroadcastReceiver<T extends Broadcast> {
    void onReceive(T broadcast);
}
