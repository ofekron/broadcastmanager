package com.ofek.dev.broadcastmanager.requests;

import com.ofek.dev.broadcastmanager.BroadcastManager;

public interface RequestBuilder<T> {
    T build();
    void exec(BroadcastManager bm);
}
