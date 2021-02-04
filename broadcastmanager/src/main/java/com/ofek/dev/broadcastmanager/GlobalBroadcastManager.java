package com.ofek.dev.broadcastmanager;

public class GlobalBroadcastManager extends BroadcastManager {
    private GlobalBroadcastManager(String name){ super(name); }
    public final static GlobalBroadcastManager instance = new GlobalBroadcastManager("GlobalBroadcastManager");


}
