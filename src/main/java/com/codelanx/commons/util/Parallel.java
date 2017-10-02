package com.codelanx.commons.util;

import java.util.concurrent.locks.Lock;
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
}
