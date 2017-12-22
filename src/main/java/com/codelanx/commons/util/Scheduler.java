/*
 * Copyright (C) 2016 Codelanx, All Rights Reserved
 *
 * This work is licensed under a Creative Commons
 * Attribution-NonCommercial-NoDerivs 3.0 Unported License.
 *
 * This program is protected software: You are free to distrubute your
 * own use of this software under the terms of the Creative Commons BY-NC-ND
 * license as published by Creative Commons in the year 2015 or as published
 * by a later date. You may not provide the source files or provide a means
 * of running the software outside of those licensed to use it.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the Creative Commons BY-NC-ND license
 * long with this program. If not, see <https://creativecommons.org/licenses/>.
 */
package com.codelanx.commons.util;

import com.codelanx.commons.logging.Debugger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Façade utility class for simplifying scheduling tasks
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public final class Scheduler {

    private static final List<Future<?>> executives = new ArrayList<>(); //TODO implement a cache pattern
    private static final ReadWriteLock execLock = new ReentrantReadWriteLock();
    private static Supplier<? extends ScheduledExecutorService> supplier = () -> Executors.newScheduledThreadPool(10); //Going to find an expanding solution to this soon
    private static ScheduledExecutorService es;

    private Scheduler() {
    }

    /**
     * Runs a repeating asynchronous task
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param r The runnable to execute
     * @param startAfter Time (in milliseconds) to wait before execution
     * @param delay Time (in milliseconds) between execution to wait
     * @return The scheduled Task
     */
    public static ScheduledFuture<?> runAsyncTaskRepeat(Runnable r, long startAfter, long delay) {
        ScheduledFuture<?> sch = Scheduler.getService().scheduleAtFixedRate(r, startAfter, delay, TimeUnit.MILLISECONDS);
        Scheduler.addTask(sch);
        return sch;
    }

    private static void addTask(Future<?> task) {
        Reflections.operateLock(Scheduler.execLock.writeLock(), () -> {
            Scheduler.executives.removeIf(f -> f.isDone() || f.isCancelled());
            Scheduler.executives.add(task);
        });
    }

    /**
     * Runs a single asynchronous task
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param r The runnable to execute
     * @param delay Time (in milliseconds) to wait before execution
     * @return The scheduled Task
     */
    public static ScheduledFuture<?> runAsyncTask(Runnable r, long delay) {
        ScheduledFuture<?> sch = Scheduler.getService().schedule(r, delay, TimeUnit.MILLISECONDS);
        Scheduler.addTask(sch);
        return sch;
    }

    /**
     * Immediately runs a single asynchronous task
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param r The runnable to execute
     * @return The scheduled Task
     */
    public static ScheduledFuture<?> runAsyncTask(Runnable r) {
        return Scheduler.runAsyncTask(r, 0);
    }
    
    /**
     * Runs a Callable
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param <T> The return type of the {@link Callable}
     * @param c The callable to execute
     * @param delay Time (in milliseconds) to wait before execution
     * @return The scheduled Task
     */
    public static <T> ScheduledFuture<T> runCallable(Callable<T> c, long delay) {
        ScheduledFuture<T> sch = Scheduler.getService().schedule(c, delay, TimeUnit.MILLISECONDS);
        Scheduler.addTask(sch);
        return sch;
    }

    public static <R> CompletableFuture<R> complete(Supplier<R> supplier) {
        CompletableFuture<R> back = CompletableFuture.supplyAsync(supplier, Scheduler.getService());
        Scheduler.addTask(back);
        return back;
    }
    
    /**
     * Cancels all running tasks/threads and clears the cached queue.
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    public static void cancelAllTasks() {
        Reflections.operateLock(Scheduler.execLock.writeLock(), () -> {
            List<? extends Future<?>> back = new ArrayList<>(Scheduler.executives);
            Scheduler.executives.clear();
            return back;
        }).forEach(s -> s.cancel(false));
    }

    public static void cancelAndShutdown() {
        Scheduler.cancelAllTasks();
        try {
            Scheduler.getService().awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Scheduler.getService().shutdownNow();
            Debugger.error(ex, "Error halting scheduler service");
        }
        Scheduler.getService().shutdown();
    }

    /**
     * Returns the underlying {@link ScheduledExecutorService} used for this
     * utility class
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The underlying {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService getService() {
        if (Scheduler.es == null || Scheduler.es.isShutdown()) {
            Scheduler.es = Scheduler.supplier.get();
        }
        return Scheduler.es;
    }

    public static void setProvider(Supplier<? extends ScheduledExecutorService> serviceProvider) {
        if (serviceProvider == null) {
            throw new IllegalArgumentException("Cannot register a null service provider");
        }
        Scheduler.supplier = serviceProvider;
    }

    public static int getTaskCount() {
        return Reflections.operateLock(Scheduler.execLock.readLock(), Scheduler.executives::size);
    }

}
