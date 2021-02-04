package com.ofek.dev.broadcastmanager.utils;

public class Utils {
    public static final void safeRun(Runnable runnable, ErrorHandler eh) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            eh.onError(e);
        }
    }

    public static <T> T requireNonNull(T obj, String ifNull) {
        if (obj==null) {
            throw new NullPointerException(ifNull);
        }
        return obj;
    }
}
