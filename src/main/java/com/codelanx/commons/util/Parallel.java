package com.codelanx.commons.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility class for simplifying paralell/threaded operations, and their mechanics.
 * For example, {@link Lock} utilization and SE concepts like double-locking mechanisms
 *
 * @since 0.3.3
 * @author 1Rogue
 * @version 0.3.3
 */
public final class Parallel {

    private Parallel() {
    }

    /**
     * Performs an operation with a lock, saving room by not requiring a lot of
     * {@code try-finally} blocks
     *
     * @since 0.2.0
     * @version 0.2.0
     *
     * @param lock The {@link Lock} to utilize
     * @param operation The code to be run
     */
    public static void operateLock(Lock lock, Runnable operation) {
        lock.lock();
        try {
            operation.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Performs an operation with a lock, saving room by not requiring a lot of
     * {@code try-finally} blocks
     *
     * @since 0.2.0
     * @version 0.2.0
     *
     * @param <R> The return type of the {@link Supplier}
     * @param lock      The {@link Lock} to utilize
     * @param operation The code to be run
     * @return A value returned from the inner {@link Supplier}
     */
    public static <R> R operateLock(Lock lock, Supplier<R> operation) {
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    public static <T> T doubleLockInit(Supplier<T> currentValue, Supplier<T> initValue) {
        T curr = currentValue.get();
        if (curr == null) {
            T back = initValue.get();
            curr = currentValue.get();
            return curr == null ? back : curr;
        }
        return curr;
    }

    public static <T> T doubleLockInit(AtomicReference<T> ref, Supplier<T> initValue) {
        T curr = ref.get();
        if (curr == null) {
            ref.compareAndSet(null, initValue.get());
            return ref.get();
        }
        return curr;
    }

    public static class StampLocks {

        public static <R> R operate(StampedLock lock, Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Function<Long, R> operation) {
            R back;
            long stamp = type.apply(lock);
            try {
                back = operation.apply(stamp);
            } finally {
                release.accept(lock, stamp);
            }
            return back;
        }

        public static <R> R operate(StampedLock lock, Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Supplier<R> operation) {
            return StampLocks.operate(lock, type, release, i -> operation.get());
        }

        public static void operate(StampedLock lock, Function<StampedLock, Long> type, BiConsumer<StampedLock, Long> release, Runnable operation) {
            StampLocks.operate(lock, type, release, i -> {
                operation.run();
                return null;
            });
        }

        public static <R> R optimisticRead(StampedLock lock, Supplier<R> operation) {
            return StampLocks.optimisticRead(lock, i -> operation.get());
        }

        public static <R> R optimisticRead(StampedLock lock, Function<Long, R> operation) {
            return StampLocks.operate(lock, StampedLock::tryOptimisticRead, (l, i) -> {}, i -> {
                R back = i == 0 ? null : operation.apply(i);
                if (i == 0 || !lock.validate(i)) {
                    try {
                        i = lock.readLock();
                        back = operation.apply(i);
                    } finally {
                        lock.unlockRead(i);
                    }
                }
                return back;
            });
        }

        public static <R> R read(StampedLock lock, Supplier<R> operation) {
            return StampLocks.operate(lock, StampedLock::readLock, StampedLock::unlockRead, operation);
        }

        public static <R> R read(StampedLock lock, Function<Long, R> operation) {
            return StampLocks.operate(lock, StampedLock::readLock, StampedLock::unlockRead, operation);
        }

        public static void write(StampedLock lock, Runnable operation) {
            StampLocks.operate(lock, StampedLock::writeLock, StampedLock::unlockWrite, operation);
        }

        public static <R> R write(StampedLock lock, Supplier<R> operation) {
            return StampLocks.operate(lock, StampedLock::writeLock, StampedLock::unlockWrite, operation);
        }

        public static <R> R write(StampedLock lock, Function<Long, R> operation) {
            return StampLocks.operate(lock, StampedLock::writeLock, StampedLock::unlockWrite, operation);
        }

        private static long writeUnsafe(StampedLock lock, long stamp, Runnable operation) {
            long w = lock.tryConvertToWriteLock(stamp);
            if (w == 0) {
                w = lock.writeLock();
            }
            operation.run();
            return w;
        }

        public static <R> void readThenWrite(StampedLock lock, Supplier<R> read, Predicate<R> writeIf, Consumer<R> write) {
            StampLocks.operate(lock, StampedLock::tryOptimisticRead, (l, i) -> {}, i -> {
                R val = i == 0 ? null : read.get();
                if (i == 0 || !lock.validate(i)) {
                    try {
                        i = lock.readLock();
                        R fval = read.get();
                        if (writeIf.test(fval)) {
                            //do write operation
                            i = StampLocks.writeUnsafe(lock, i, () -> write.accept(fval));
                        }
                    } finally {
                        lock.unlock(i);
                    }
                } else {
                    //do write operation
                    try {
                        i = StampLocks.writeUnsafe(lock, i, () -> write.accept(val));
                    } finally {
                        lock.unlockWrite(i);
                    }
                }
                return null;
            });
        }

        public static void writeIf(StampedLock lock, Supplier<Boolean> read, Runnable write) {
            StampLocks.readThenWrite(lock, read, Boolean.TRUE::equals, b -> write.run());
        }

        public static <R> void readThenWrite(StampedLock lock, Supplier<R> read, Consumer<R> write) {
            StampLocks.readThenWrite(lock, read, r -> true, write);
        }

    }
}
