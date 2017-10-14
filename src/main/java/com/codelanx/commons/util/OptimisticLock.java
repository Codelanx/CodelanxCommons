package com.codelanx.commons.util;

import com.codelanx.commons.util.Parallel.StampLocks;

import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class OptimisticLock extends StampedLock {

    public <R> R operate(Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Function<Long, R> operation) {
        return StampLocks.operate(this, type, release, operation);
    }

    public <R> R operate(Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Supplier<R> operation) {
        return StampLocks.operate(this, type, release, operation);
    }

    public void operate(Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Runnable operation) {
        StampLocks.operate(this, type, release, operation);
    }

    public void optimisticRead(Runnable operation) {
        StampLocks.optimisticRead(this, operation);
    }

    public <R> R optimisticRead(Supplier<R> operation) {
        return StampLocks.optimisticRead(this, operation);
    }

    public <R> R optimisticRead(Function<Long, R> operation) {
        return StampLocks.optimisticRead(this, operation);
    }

    public <R> R read(Supplier<R> operation) {
        return StampLocks.read(this, operation);
    }

    public <R> R read(Function<Long, R> operation) {
        return StampLocks.read(this, operation);
    }

    public void write(Runnable operation) {
        StampLocks.write(this, operation);
    }

    public <R> R write(Supplier<R> operation) {
        return StampLocks.write(this, operation);
    }

    public <R> R write(Function<Long, R> operation) {
        return StampLocks.write(this, operation);
    }

    public <R> void readThenWrite(Supplier<R> read, Predicate<R> writeIf, Consumer<R> write) {
        StampLocks.readThenWrite(this, read, writeIf, write);
    }

    public void writeIf(Supplier<Boolean> read, Runnable write) {
        StampLocks.writeIf(this, read, write);
    }

    private <R> void readThenWrite(Supplier<R> read, Consumer<R> write) {
        StampLocks.readThenWrite(this, read, write);
    }
}
