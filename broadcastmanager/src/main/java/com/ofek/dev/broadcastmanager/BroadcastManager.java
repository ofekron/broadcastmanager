package com.ofek.dev.broadcastmanager;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ofek.dev.broadcastmanager.os.OsDefaults;
import com.ofek.dev.broadcastmanager.requests.BroadcastRequest;
import com.ofek.dev.broadcastmanager.requests.RegisterRequest;
import com.ofek.dev.broadcastmanager.utils.ErrorHandler;
import com.ofek.dev.broadcastmanager.utils.Executor;
import com.ofek.dev.broadcastmanager.utils.MapUtils;
import com.ofek.dev.broadcastmanager.utils.CrossThreadExecutionLock;
import com.ofek.dev.broadcastmanager.utils.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;


public class BroadcastManager {

    public enum State {
        STARTING,
        RUNNING,
        STOPPING,
        IDLE
    }
    private State state = State.IDLE;
    private final String name;
    private Map<String, List<BRWrapper>> broadcastReceivers;
    private Map<String, Set<Broadcast>> stickyBroadcasts;
    private Map<BroadcastReceiver, BRWrapper> brToWrap;
    private CopyOnWriteArrayList<StateListener> stateListeners = new CopyOnWriteArrayList<>();
    private ErrorHandler errorHandler = null;
    private ExecutorFactory executorFactory;
    private CrossThreadExecutionLock lock = new CrossThreadExecutionLock();
    private Executor executor;

    public BroadcastManager(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setExecutorFactory(ExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }




    private class Channel<T extends Broadcast> {
        public final String name;

        public Channel(String name) {
            this.name = name;
        }

        public void register(RegisterRequest r) {
            BroadcastManager.this.register(r);
        }

        public void unregister(BroadcastReceiver<T> br) {
            BroadcastManager.this.unregister(name, br);
        }

        public void send(BroadcastRequest r) {
            BroadcastManager.this.send(r);
        }

    }

    public Channel<Broadcast> getChannel(String name) {
        return new Channel<>(name);
    }

    public <T extends Broadcast> Channel<T> getChannel(Class<T> tClass) {
        return new Channel<T>(tClass.getName());
    }



    private void lock(int id) {
        try {
            lock.lock(id);
        } catch (RuntimeException t) {
            throw new RuntimeException("Called start or stop while start or stop is on going on the same thread, change your design. This call was ommited quietly", t);
        }
    }

    public void start() {
        int id = lock.newExecutionId();
        lock(id);
        try {
            if (state == State.RUNNING || state == State.STARTING) return;
            setState(State.STARTING);
            lock(id);
            init();
            lock.delegateTo(executor.getId());
            executor.execAsync(() -> {
                try {
                    setState(State.RUNNING);
                } finally {
                    lock.unlock(id);
                }
            });
        } finally {
            lock.unlock(id);
        }
    }

    public void stop() {
        log("stop was called");
        int id = lock.newExecutionId();
        lock(id);
        try {
            if (state == State.IDLE || state == State.STOPPING) return;
            setState(State.STOPPING);
            lock(id);
            lock.delegateTo(executor.getId());
            executor.execAsync(() -> {
                try {
                    cleanUp();
                    setState(State.IDLE);
                } finally {
                    lock.unlock(id);
                }
            });
        } finally {
            lock.unlock(id);
        }
    }
    public void stopAndWait() {
        stop();
        int id = lock.newExecutionId();
        lock.lock(id);
        lock.unlock(id);
    }



    private void init() {
        broadcastReceivers = new HashMap<>();
        stickyBroadcasts = new HashMap<>();
        brToWrap = new HashMap<>();
        if (executorFactory == null)
            executorFactory = OsDefaults.createDefaultExecutorFactory();
        executor = executorFactory.createExecutor(name);
        executor.init();
    }
    void cleanUpBroadcast(Broadcast b) {
        List<BroadcastManager.BRWrapper> removed = broadcastReceivers.remove(b.consumptionChannel);
        if (removed != null) {
            for (BroadcastManager.BRWrapper brWrapper : removed) {
                brToWrap.remove(brWrapper.br);
            }
        }
    }


    private void cleanUp() {
        broadcastReceivers = null;
        stickyBroadcasts = null;
        brToWrap = null;
        errorHandler = null;
        executor.destroy();
        executor = null;

    }

    private void setState(State newState) {
        State oldState = this.state;
        this.state = newState;
        for (StateListener l : stateListeners) {
            try {
                l.onStateChanged(oldState, this.state);
            } catch (Throwable t) {
                onError(new RuntimeException(t));
            }
        }
    }


    public void addStateListener(@NonNull StateListener listener) {
        stateListeners.add(listener);
        try {
            listener.onStateChanged(null, state);
        } catch (Throwable t) {
            onError(new RuntimeException(t));
        }
    }

    public abstract static class StateListenerCleaner {
        public abstract void setCleanUpJob(Runnable cleanUpJob);
    }

    public void addStateListener(@NonNull StateListener listener, StateListenerCleaner cleaner) {
        stateListeners.add(listener);
        if (cleaner != null)
            cleaner.setCleanUpJob(() -> removeStateListener(listener));
        try {
            listener.onStateChanged(null, state);
        } catch (Throwable t) {
            onError(new RuntimeException(t));
        }
    }

    public void removeStateListener(@NonNull StateListener s) {
        stateListeners.remove(s);
    }


    public void register(RegisterRequest r) {
        BroadcastReceiver broadcastReceiver = r.getBroadcastReceiver();
        String channel = r.getChannel();
        int priority = r.getPriority();
        boolean oneShot = r.isOneShot();
        run(() -> {
            log("registering " + OsDefaults.toString(broadcastReceiver) + " to channel " + channel);
            BRWrapper brWrapper;
            if (oneShot) {
                if (brToWrap.containsKey(broadcastReceiver))
                    throw new RuntimeException("This BroadcastReciever is already registered and therefore cant be registered as one shot");
                brWrapper = new BRWrapper(System.currentTimeMillis(), priority, broadcastReceiver, r.getExecutor()) {

                    @Override
                    public void onReceive(Broadcast broadcast) {
                        super.onReceive(broadcast);
                        unregister(broadcastReceiver);
                    }
                };
            } else {
                brWrapper = MapUtils.getOrPut(brToWrap, broadcastReceiver, () -> new BRWrapper(System.currentTimeMillis(), priority, broadcastReceiver, r.getExecutor()));
                if (brWrapper.channels.contains(channel))
                    throw new RuntimeException("This BroadcastReciever is already registered to this channel");
            }

            List<BRWrapper> receivers = MapUtils.putInList(broadcastReceivers, channel, brWrapper, () -> new CopyOnWriteArrayList<>());
            Collections.sort(receivers);
            brWrapper.channels.add(channel);
            brToWrap.put(brWrapper.br, brWrapper);
            MapUtils.ifExists(stickyBroadcasts, channel, (stickyBroadcasts) -> {
                doStickyBroadcasting(channel, oneShot, brWrapper, stickyBroadcasts.iterator());
            });

        });
    }
    public void registerSync(RegisterRequest r){
        register(r);
        sync();
    }
    public <T extends Broadcast>  void unregisterSync(BroadcastReceiver<T> br){
        unregister(br);
        sync();
    }

    private void doStickyBroadcasting(String channel, boolean oneShot, BRWrapper brWrapper, Iterator<Broadcast> iterator) {
        Broadcast broadcast = iterator.next();
        if (broadcast.isStalled()) {
            broadcast.addProceedJob(() -> {
                stickyBroadcastIteration(channel, oneShot, brWrapper, broadcast, iterator);
            });
        } else {
            stickyBroadcastIteration(channel, oneShot, brWrapper, broadcast, iterator);
        }
    }

    private void stickyBroadcastIteration(String channel, boolean oneShot, BRWrapper brWrapper, Broadcast broadcast, Iterator<Broadcast> iterator) {
        if (!broadcast.isConsumed() || broadcast.getStickiness().equals(Broadcast.Stickiness.STICKY_UNTIL_STOPPED))
            execOnReceive(brWrapper, broadcast);
        if (oneShot || !brWrapper.channels.contains(channel))
            return;
        doStickyBroadcasting(channel, oneShot, brWrapper, iterator);
    }


    public <T extends Broadcast> void unregister(BroadcastReceiver<T> br) {
        run(() -> {
            MapUtils.ifExists(brToWrap, br, (wrapper) -> {
                log("unregistering " + OsDefaults.toString(br));
                for (String channel : wrapper.channels)
                    MapUtils.removeIfInList(broadcastReceivers, channel, wrapper);
                wrapper.channels.clear();
                brToWrap.remove(br);
            });
        });
    }

    public <T extends Broadcast> void unregister(String channel, BroadcastReceiver<T> br) {
        run(() -> {
            MapUtils.ifExists(brToWrap, br, (wrapper) -> {
                log("unregistering " + OsDefaults.toString(br));
                MapUtils.removeIfInList(broadcastReceivers, channel, wrapper);
                wrapper.channels.remove(channel);
                if (wrapper.channels.isEmpty())
                    brToWrap.remove(br);
            });
        });
    }


    public void send(BroadcastRequest request) {

        Broadcast broadcast = request.getBroadcast();

        String channel = request.getChannel();
        Broadcast.Stickiness stickiness = broadcast.getStickiness();

        if (broadcast.getChannel() != null && !channel.equals(broadcast.getChannel()))
            throw new RuntimeException("Cant send broadcast in more then one channel");

        run(() -> {
            log("sending " + stickiness + " " + OsDefaults.toString(broadcast) + " on channel " + channel);
            broadcast.activate(stickiness, BroadcastManager.this, channel, request.getExecutor());
            if (!stickiness.equals(Broadcast.Stickiness.NEVER)) {
                MapUtils.putInSet(stickyBroadcasts, channel, broadcast, () -> new CopyOnWriteArraySet<>());
                log(stickyBroadcasts.get(channel).size() + " sticky");
            }
            MapUtils.ifExists(broadcastReceivers, channel, (list) -> {
                doBroadcasting(list.iterator(), broadcast);
            });

        });
    }
    public void sendSync(BroadcastRequest request) {
        send(request);
        sync();
    }

    /**
     * blocks untill all previous internal jobs has been finished
     */
    public void sync() {
        Object o = new Object();
        synchronized (o) {
            run(()-> {synchronized (o) { o.notify(); }});
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void doBroadcasting(Iterator<BRWrapper> iterator, Broadcast broadcast) {
        BRWrapper brWrapper;
        for (; iterator.hasNext(); ) {
            brWrapper = iterator.next();
            execOnReceive(brWrapper, broadcast);
            if (broadcast.isConsumed() && !broadcast.getStickiness().equals(Broadcast.Stickiness.STICKY_UNTIL_STOPPED))
                break;
            if (broadcast.isStalled()) {
                broadcast.addProceedJob(() -> doBroadcasting(iterator, broadcast));
                return;
            }
        }
        if (!broadcast.isConsumed() && broadcast.getStickiness().equals(Broadcast.Stickiness.NEVER))
            broadcast.consume();
    }

    private void execOnReceive(BRWrapper br, Broadcast broadcast) {
        try {
            broadcast.isBeingDelivered = true;
            br.onReceive(broadcast);
        } catch (RuntimeException e) {
            onError(e);
        } finally {
            broadcast.isBeingDelivered = false;
        }
    }

    void cleanUp(Broadcast b, String channel) {
        run(() -> {
            switch (b.getStickiness()) {
                case STICKY_UNTIL_CONSUMED:
                    MapUtils.removeIfInSet(stickyBroadcasts, channel, b);
                    b.deactivate();
                    break;
                case NEVER:
                    b.deactivate();
                    break;
            }
            send(BroadcastRequest.create().withChannel(b.consumptionChannel).withBroadcast(new MetaBroadcast(b)).build());
        }, true);
    }

    void run(Runnable r) {
        run(r, false);
    }

    void run(Runnable r, boolean asap) {

        if (state == State.IDLE) {
            onError(new RuntimeException("BroadcastManager is not started. make sure you call start and didnt call stop unintentionally."));
            return;
        }

        if (executor.isCurrentThread()) {
            //Internal logic thats being run by current thread always run in place, that make sense
            // since its rational to expect something that being requested to run in the logic executor of this BroadcastManager
            // will run immediately and in place
            Utils.safeRun(r, this::onError);
        } else {
            if (asap)
                executor.execFirstAsync(() -> Utils.safeRun(r, this::onError));
            else
                executor.execAsync(() -> Utils.safeRun(r, this::onError));
        }

    }


    private void onError(RuntimeException e) {
        if (errorHandler != null)
            errorHandler.onError(e);
    }

    public void setErrorHandler(ErrorHandler e) {
        errorHandler = e;
    }



    public interface ExecutorFactory {
        Executor createExecutor(String name);
    }

    public interface StateListener {
        void onStateChanged(State oldState, State newState);
    }


    public Executor getExecutor() {
        return executor;
    }

    protected static class BRWrapper implements Comparable<BRWrapper>, BroadcastReceiver {
        private final Executor executor;
        private long timestamp;
        private int priority;
        BroadcastReceiver br;
        private Set<String> channels = new HashSet<>();

        private BRWrapper(long timestamp, int priority, BroadcastReceiver br, Executor executor) {
            this.timestamp = timestamp;
            this.priority = priority;
            this.br = br;
            this.executor = executor;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || br.equals(obj);
        }

        @Override
        public int compareTo(BRWrapper o) {
            int compare = Integer.compare(priority, o.priority);
            return compare == 0 ? Long.compare(timestamp, o.timestamp) : compare;
        }


        @Override
        public void onReceive(Broadcast broadcast) {
            if (executor != null)
                executor.execSync(() -> br.onReceive(broadcast));
            else {
                Executor executor = broadcast.getExecutor();
                if (executor != null)
                    executor.execSync(() -> br.onReceive(broadcast));
                else
                    br.onReceive(broadcast);

            }
        }
    }

    public static final String BROADCAST_MANAGER_TEST = "BroadcastManagerTest";

    public void log(String m) {
        Log.d(BROADCAST_MANAGER_TEST, m);
    }

}
