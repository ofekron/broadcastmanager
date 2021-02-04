package com.ofek.dev.broadcastmanager;

import com.ofek.dev.broadcastmanager.requests.RegisterRequest;
import com.ofek.dev.broadcastmanager.utils.Executor;

import java.util.ArrayList;

import static com.ofek.dev.broadcastmanager.Broadcast.Stickiness.NEVER;

public class Broadcast {
    final String consumptionChannel;
    boolean isBeingDelivered;
    private boolean stalled;
    private ArrayList<Runnable> proceedJobs = new ArrayList<>();

    public Executor getExecutor() {
        return executor;
    }

    public boolean isStalled() {
        return stalled;
    }

    void addProceedJob(Runnable proceedJob) {
        this.proceedJobs.add(proceedJob);
    }

    public enum Stickiness {
        NEVER,
        STICKY_UNTIL_CONSUMED,
        STICKY_UNTIL_STOPPED
    }
    private Stickiness stickiness = NEVER;
    private BroadcastManager bm;
    private boolean consumed;

    public final String getChannel() {
        return channel;
    }
    public final boolean isConsumed() {
        return consumed;
    }

    public final boolean isActive() {
        return active;
    }
    private String channel;
    private Executor executor=null;
    private boolean active = false;

    public Broadcast() {
        String broadcastMetaChannel ="Broadcast@"+System.identityHashCode(Broadcast.this);
        consumptionChannel=broadcastMetaChannel+"_consumed";
    }

    public final Stickiness getStickiness() {
        return stickiness;
    }


    public void consume() {
        consumed = true;
        bm.cleanUp(this, channel);
    }

    public void addConsumedListener(BroadcastReceiver<MetaBroadcast> b) {
        run(() -> {
            bm.register(RegisterRequest.create().withChannel(consumptionChannel).withBroadcastReceiver(b).build());
        });
    }

    public void removeConsumedListener(BroadcastReceiver<MetaBroadcast> b) {
        run(() -> {
            bm.unregister(consumptionChannel,b);
        });
    }

    public void run(Runnable r) {
        ensureActive();
        bm.run(r);
    }

    private void ensureActive() {
        if (bm==null) throw new RuntimeException("Broadcast was not broadcasted to any BroadcastManager");
        if (!active) throw new RuntimeException("broadcast is not active");
    }



    void activate(Stickiness stickiness, BroadcastManager broadcastManager, String channel, Executor executor) {
        if (bm!=null) throw new RuntimeException("Broadcast was already broadcasted to another BroadcastManager");
        this.stickiness = stickiness;
        this.bm=broadcastManager;
        this.channel=channel;
        this.executor=executor;
        active = true;
    }
    void deactivate() {
        bm.cleanUpBroadcast(this);
        bm=null;
        active = false;
    }

    public void stall() {
        if (!isBeingDelivered)
            throw new IllegalStateException("Stalling a broadcast is allowed only from onReceive code of some BroadcastReceiver");
        if (stalled) return;
        this.proceedJobs.clear();
        stalled = true;
    }
    public void proceed() {
        stalled = false;
        for (Runnable job : proceedJobs)
            run(()->job.run());
    }




}
