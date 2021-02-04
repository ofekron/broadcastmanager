package com.ofek.dev.broadcastmanager.requests;

import androidx.annotation.NonNull;

import com.ofek.dev.broadcastmanager.Broadcast;
import com.ofek.dev.broadcastmanager.BroadcastManager;
import com.ofek.dev.broadcastmanager.BroadcastReceiver;
import com.ofek.dev.broadcastmanager.utils.Executor;
import com.ofek.dev.broadcastmanager.utils.Utils;


public class RegisterRequest {
    private RegisterRequest() {
    }

    public static RegisterRequestBuilder create() {
        return new RegisterRequestBuilder();
    }

    public boolean isOneShot() {
        return oneShot;
    }

    private String channel;
    private BroadcastReceiver broadcastReceiver;
    private boolean oneShot=false;
    private int priority=0;
    private Executor executor = null;
    public String getChannel() {
        return channel;
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    public int getPriority() {
        return priority;
    }

    public Executor getExecutor() {
        return executor;
    }


    public static final class RegisterRequestBuilder<T extends Broadcast> implements RequestBuilder<RegisterRequest> {

        private final RegisterRequest registerRequest;

        public RegisterRequestBuilder() {
            registerRequest = new RegisterRequest();
        }


        public RegisterRequestBuilder<T> withChannel(@NonNull String channel) {
            registerRequest.channel = channel;
            return this;
        }
        public RegisterRequestBuilder<T> withChannel(@NonNull Class<T> channel) {
            registerRequest.channel = channel.getName();
            return this;
        }
        public RegisterRequestBuilder<T> withBroadcastReceiver(@NonNull BroadcastReceiver<T> broadcastReceiver) {
            registerRequest.broadcastReceiver = broadcastReceiver;
            return this;
        }

        public RegisterRequestBuilder<T> withOneShot(boolean oneShot) {
            registerRequest.oneShot = oneShot;
            return this;
        }

        public RegisterRequestBuilder<T> withPriority(int priority) {
            registerRequest.priority = priority;
            return this;
        }
        public RegisterRequestBuilder<T> withExecutor(Executor e) {
            registerRequest.executor = e;
            return this;
        }
        public RegisterRequest build() {
            Utils.requireNonNull(registerRequest.channel,"Channel cant be null");
            Utils.requireNonNull(registerRequest.broadcastReceiver,"BroadcastReciever cant be null");
            return registerRequest;
        }

        public void exec(BroadcastManager bm) {
            bm.register(build());
        }
    }
}
