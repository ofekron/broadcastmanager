package com.ofek.dev.broadcastmanager;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.ofek.dev.broadcastmanager.os.MainThreadExecutor;
import com.ofek.dev.broadcastmanager.os.OsDefaults;
import com.ofek.dev.broadcastmanager.requests.BroadcastRequest;
import com.ofek.dev.broadcastmanager.requests.RegisterRequest;
import com.ofek.dev.broadcastmanager.utils.ErrorHandler;
import com.ofek.dev.broadcastmanager.utils.Executor;

import org.junit.Test;

import java.util.Random;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class BroadcastManagerUnitTest {

    public static final String BROADCAST_MANAGER_TEST = "BroadcastManagerTest";
    private BroadcastManager bm = new BroadcastManager("Test");

    public BroadcastManagerUnitTest() {
        bm.addStateListener((oldState, newState) -> log("State changed " + oldState + "=>" + newState));
        bm.setErrorHandler(new ErrorHandler() {
            @Override
            public void onError(RuntimeException error) {
                error.printStackTrace();
            }
        });
    }

    public void log(String m) {
        Log.d(BROADCAST_MANAGER_TEST, m);

    }

    @Test
    public void scenario1() {
        BroadcastManager bm = new BroadcastManager("Test");
        bm.setExecutorFactory(name -> new MainThreadExecutor());
        bm.setErrorHandler(error -> {
            error.printStackTrace();
        });
        bm.addStateListener((old, state) -> log("state is " + state.name()));
        bm.start();
        register(Broadcast.class.getName(), (b) -> {
            log("received, stopping");
            bm.stop();
            bm.start();
            bm.stop();
            bm.start();
        });

        send(new Broadcast());
        bm.start();

    }

    @Test
    public void scenario2() {

        bm.setExecutorFactory(new BroadcastManager.ExecutorFactory() {
            @Override
            public Executor createExecutor(String name) {
                return new MainThreadExecutor();
            }
        });
        bm.setErrorHandler(error -> {
            error.printStackTrace();
        });
        bm.addStateListener((old, state) -> log("state is " + state.name()));
        bm.start();
        register("channel1", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 1 " + OsDefaults.toString(b));

        });
        register("channel2", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 2 " + OsDefaults.toString(b));
        });
        send(new Broadcast());
        send("channel1", new Broadcast());
        send("channel2", new Broadcast());
        send("channel3", new Broadcast(), Broadcast.Stickiness.STICKY_UNTIL_CONSUMED);

        BroadcastReceiver br = (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 3 " + OsDefaults.toString(b));
            b.consume();
        };
        register("channel3", br);
        unregister(br);
        send("channel3", new Broadcast(), Broadcast.Stickiness.STICKY_UNTIL_CONSUMED);
        register("channel3", new BroadcastReceiver<Broadcast>() {
            @Override
            public void onReceive(Broadcast b) {
                log("received on " + OsDefaults.toString(this) + " channel 3 one time simulate 1 " + OsDefaults.toString(b));
                b.consume();
                bm.unregister(this);
            }
        });


        register("channel3", new BroadcastReceiver<Broadcast>() {
            @Override
            public void onReceive(Broadcast b) {
                log("received on " + OsDefaults.toString(this) + " channel 3 one time simulate 2" + OsDefaults.toString(b));
                b.consume();
                bm.unregister(this);
            }
        });
        send("channel3", new Broadcast(), Broadcast.Stickiness.STICKY_UNTIL_CONSUMED);

        register("channel3", new BroadcastReceiver<Broadcast>() {
            @Override
            public void onReceive(Broadcast b) {
                log("received on " + OsDefaults.toString(this) + " channel 3 one time simulate 3 " + OsDefaults.toString(b));
                b.consume();
                bm.unregister(this);
            }
        });
        send("channel3", new Broadcast());

        send("channel3", new Broadcast(), Broadcast.Stickiness.STICKY_UNTIL_CONSUMED);
        send("channel3", new Broadcast(), Broadcast.Stickiness.STICKY_UNTIL_CONSUMED);
        send("channel3", new Broadcast());
        registerOneShot("channel3", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 3 one shot 1 " + OsDefaults.toString(b));
        });
        send("channel3", new Broadcast());
        send("channel3", new Broadcast(), Broadcast.Stickiness.STICKY_UNTIL_CONSUMED);
        registerOneShot("channel3", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 3 one shot 2 " + OsDefaults.toString(b));
            b.consume();
        });
        registerOneShot("channel3", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 3 one shot 3 " + OsDefaults.toString(b));
            b.consume();
        });
        register("channel3", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 3 should happen twice " + OsDefaults.toString(b));
        });
        registerOneShot("channel3", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 3 one shot 4 " + OsDefaults.toString(b));
            b.consume();
        });
        send("channel3", new Broadcast());

        registerOneShot("channel3", (b) -> {
            log("received on " + OsDefaults.toString(this) + " channel 3 one shot 5 shouldnt occur " + OsDefaults.toString(b));
        });
        bm.stop();

    }

    @Test
    public void scenario3() {
        bm.start();
        Random random = new Random();
        HandlerThread handlerthread = new HandlerThread("configuration");
        handlerthread.start();
        Handler handler = new Handler(handlerthread.getLooper());
        handler.postDelayed(() -> {
            BroadcastRequest.create()
                    .withChannel("configuration")
                    .withBroadcast(new Broadcast())
                    .withExecutor(OsDefaults.newThreadExecutor("test3"))
                    .withStickiness(Broadcast.Stickiness.STICKY_UNTIL_STOPPED)
                    .exec(bm);
        }, random.nextInt(1000));
        handler.postDelayed(() -> {
            RegisterRequest.create()
                    .withChannel("configuration")
                    .withBroadcastReceiver(broadcast -> log("received on " + Thread.currentThread()))
                    .withExecutor(OsDefaults.currentThreadExecutor())
                    .exec(bm);
        }, random.nextInt(1000));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        bm.stopAndWait();

    }

    private void send(Broadcast broadcast) {
        bm.send(BroadcastRequest.create().withBroadcast(broadcast).build());
    }

    private void send(String channel3, Broadcast broadcast, Broadcast.Stickiness stickyUntilConsumed) {
        bm.send(BroadcastRequest.create().withBroadcast(broadcast).withStickiness(stickyUntilConsumed).withChannel(channel3).build());
    }

    private void send(String channel3, Broadcast broadcast) {
        bm.send(BroadcastRequest.create().withBroadcast(broadcast).withChannel(channel3).build());
    }

    private void registerOneShot(String channel3, BroadcastReceiver o) {

        bm.register(RegisterRequest.create().withChannel(channel3).
                withBroadcastReceiver(o).withOneShot(true).
                build());
    }

    private void register(String channel3, BroadcastReceiver<Broadcast> broadcastBroadcastReceiver) {
        bm.register(RegisterRequest.create().withChannel(channel3).
                withBroadcastReceiver(broadcastBroadcastReceiver).
                build());
    }

    private void unregister(BroadcastReceiver br) {
        bm.unregister(br);
    }

}