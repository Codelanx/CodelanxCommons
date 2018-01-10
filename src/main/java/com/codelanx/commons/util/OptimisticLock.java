package com.codelanx.commons.util;

import com.codelanx.commons.util.Parallel.StampLocks;

import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class OptimisticLock extends StampedLock {

    public enum Optimism {
        NONE, //never try an optimistic lock, effectively making this a hard StampedLock implementation     |even RW
        TRY_ONCE, //try optimistic once, and switch to a hardstamped lock on failure                        |unbalanced
        RETRY, //continue optimistic reads until success                                                    |unbal R++,W
        ;
    }

    private final Optimism opt;

    public OptimisticLock() {
        this(Optimism.TRY_ONCE);
    }

    public OptimisticLock(Optimism opt) {
        this.opt = opt;
    }

    public <R> R operate(Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Function<Long, R> operation) {
        return StampLocks.operate(this, type, release, operation);
    }

    public <R> R operate(Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Supplier<R> operation) {
        return StampLocks.operate(this, type, release, operation);
    }

    public void operate(Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Runnable operation) {
        StampLocks.operate(this, type, release, operation);
    }

    public void forcdRead(Runnable operation) {
        StampLocks.read(this, operation);
    }

    public <R> R forcedRead(Supplier<R> operation) {
        return StampLocks.read(this, operation);
    }

    public <R> R forcedRead(Function<Long, R> operation) {
        return StampLocks.read(this, operation);
    }

    //optimistic
    public void read(Runnable operation) {
        StampLocks.optimisticRead(this, operation);
    }

    //optimistic
    public <R> R read(Supplier<R> operation) {
        return StampLocks.optimisticRead(this, operation);
    }

    //optimistic
    public <R> R read(Function<Long, R> operation) {
        return StampLocks.optimisticRead(this, operation);
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

    public <R> R readThenWrite(Supplier<R> read, Predicate<R> writeIf, Consumer<R> write) {
        return StampLocks.readThenWrite(this, read, writeIf, write);
    }

    public void writeIf(Supplier<Boolean> read, Runnable write) {
        StampLocks.writeIf(this, read, write);
    }

    public <T, R> R writeWith(T value, Function<T, R> writer) {
        return this.write(() -> writer.apply(value));
    }

    public <T, R> R readWith(T value, Function<T, R> writer) {
        return this.write(() -> writer.apply(value));
    }

    private <R> R readThenWrite(Supplier<R> read, Consumer<R> write) {
        return StampLocks.readThenWrite(this, read, write);
    }
}
