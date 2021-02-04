package com.ofek.dev.broadcastmanager;

public class MetaBroadcast extends Broadcast {
    public final Broadcast src;

    public MetaBroadcast(Broadcast broadcast) {
        this.src = broadcast;
    }
}
