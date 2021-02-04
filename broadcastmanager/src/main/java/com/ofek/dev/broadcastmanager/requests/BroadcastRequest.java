package com.ofek.dev.broadcastmanager.requests;

import androidx.annotation.NonNull;

import com.ofek.dev.broadcastmanager.Broadcast;
import com.ofek.dev.broadcastmanager.BroadcastManager;
import com.ofek.dev.broadcastmanager.utils.Executor;
import com.ofek.dev.broadcastmanager.utils.Utils;

public class BroadcastRequest {

    public static BroadcastRequestBuilder create() {
        return new BroadcastRequestBuilder();
    }
    public String getChannel() {
        return channel;
    }

    public Broadcast.Stickiness getStickiness() {
        return stickiness;
    }

    public Broadcast getBroadcast() {
        return broadcast;
    }

    private String channel;
    private Broadcast.Stickiness stickiness = Broadcast.Stickiness.NEVER;
    private Broadcast broadcast;
    private Executor executor = null;
    public Executor getExecutor() {
        return executor;
    }
    private BroadcastRequest() {
    }

    public static final class BroadcastRequestBuilder<T extends Broadcast> implements RequestBuilder<BroadcastRequest> {

        private final BroadcastRequest broadcastRequest;


        public BroadcastRequestBuilder() {
            broadcastRequest = new BroadcastRequest();
        }

        public BroadcastRequestBuilder withChannel(@NonNull String channel) {
            broadcastRequest.channel = channel;
            return this;
        }

        public BroadcastRequestBuilder withChannel(@NonNull Class<T> channel) {
            broadcastRequest.channel = channel.getName();
            return this;
        }

        public BroadcastRequestBuilder withStickiness(@NonNull Broadcast.Stickiness stickiness) {
            broadcastRequest.stickiness = stickiness;
            return this;
        }


        public BroadcastRequestBuilder withBroadcast(T broadcast) {
            broadcastRequest.broadcast = broadcast;
            if (broadcastRequest.channel == null) {
                broadcastRequest.channel = broadcast.getClass().getName();
            }
            return this;
        }
        public BroadcastRequestBuilder withExecutor(Executor e) {
            broadcastRequest.executor = e;
            return this;
        }
        public BroadcastRequest build() {
            Utils.requireNonNull(broadcastRequest.channel,"Channel cant be null");
            Utils.requireNonNull(broadcastRequest.stickiness,"Stickiness cant be null");
            Utils.requireNonNull(broadcastRequest.broadcast,"Broadcast cant be null");
            return broadcastRequest;
        }

        @Override
        public void exec(BroadcastManager bm) {
            bm.send(build());
        }
    }
}
