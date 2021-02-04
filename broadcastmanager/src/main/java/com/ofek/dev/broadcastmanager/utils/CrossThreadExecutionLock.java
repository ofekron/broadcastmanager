package com.ofek.dev.broadcastmanager.utils;

public class CrossThreadExecutionLock {
    int i = 0;
    private Integer owner = null;
    private long owningThreadId;
    private int idProvider=0;
    public synchronized int newExecutionId() {
        return idProvider++;
    }
    public synchronized void lock(int executionId) {
        if (owner !=null && isOwnedByCurrThread() && owner!=executionId) {
            throw new RuntimeException("DeadLock prevented, thread already owning this lock using a different id");
        }
        while (owner !=null && owner!=executionId) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        owner=executionId;
        owningThreadId=Thread.currentThread().getId();
        i++;
    }

    private boolean isOwnedByCurrThread() {
        return owningThreadId==Thread.currentThread().getId();
    }


    public synchronized void unlock(int id) {
        if (owner!=id)
            new IllegalMonitorStateException("id "+ id + "is not the owner, owner is "+ owner);
        if (i==0)
            new IllegalMonitorStateException("unlock was called too many times");
        i--;
        if (i==0) {
            owner = null;
        }

        notify();
    }

    public synchronized void delegateTo(long newThreadId) {
        if (owner ==null)
            throw new RuntimeException("This lock is not locked");
        if (!isOwnedByCurrThread())
            throw new RuntimeException("Only owning thread can delegate execution to another thread");
        owningThreadId=newThreadId;

    }


}
