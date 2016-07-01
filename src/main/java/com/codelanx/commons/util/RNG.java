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

import com.codelanx.commons.util.exception.Exceptions;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang3.Validate;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * A class of random number generators that can be used instead of instantiating
 * new {@link Random} classes all about the program
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public final class RNG {

    static {
        Scheduler.runAsyncTaskRepeat(() -> {
            //nobody fuckin' with my secure random
            RNG.SECURE_RAND.setSeed(RNG.SECURE_RAND.generateSeed(RNG.THREAD_LOCAL().nextInt(30)));
        }, 0, TimeUnit.MINUTES.toSeconds(10));
    }

    private RNG() {}

    /**
     * A simple {@link Random} instance, should only be used on the program's main thread
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    public static final Random RAND = new Random();

    /**
     * Represents a {@link SecureRandom} with a new, randomly generated seed of
     * a pseudo-random bitlength every 10 minutes.
     * 
     * @since 0.1.0
     * @version 0.1.0
     */
    public static final SecureRandom SECURE_RAND = new SecureRandom();

    /**
     * Returns the {@link ThreadLocalRandom} specific to the calling thread
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The {@link ThreadLocalRandom} for the current thread context
     */
    public static final ThreadLocalRandom THREAD_LOCAL() {
        return ThreadLocalRandom.current();
    }

    /**
     * Returns a random element from a collection
     *
     * @since 0.2.0
     * @version 0.2.0
     *
     * @param collection The {@link Collection} to get an element from
     * @param <T> The type of the collection and returned element
     * @return A random element
     */
    public static <T> T get(Collection<T> collection) {
        if (collection.isEmpty()) {
            return null;
        }
        int rand = RNG.THREAD_LOCAL().nextInt(collection.size());
        if (collection instanceof List) {
            return ((List<T>) collection).get(rand);
        } else {
            Iterator<T> itr = collection.iterator();
            return IntStream.range(0, collection.size()).boxed().map(i -> {
                T var = itr.next();
                return i == rand ? var : null;
            }).filter(Lambdas::notNull).findFirst().orElse(null);
        }
    }

    /**
     * Returns a list of random, unique elements from a collection
     *
     * @since 0.2.0
     * @version 0.2.0
     *
     * @param collection The {@link Collection} to get elements from
     * @param amount The amount of elements to retrieve
     * @param <T> The type of the elements in the collection, and what's returned
     * @return A {@link List} of unique, random elements
     * @throws IllegalArgumentException If {@code amount > collection#size}
     */
    public static <T> List<T> get(Collection<T> collection, int amount) {
        Validate.isTrue(amount <= collection.size(), "Amount cannot be greater than the collection size");
        List<T> back = new ArrayList<>();
        if (amount <= 0 || collection.size() == 0) {
            return back;
        }
        Random r = RNG.THREAD_LOCAL();
        if (collection instanceof List) {
            List<T> lis = (List<T>) collection;
            for (int i = 0; i < amount; i++) {
                back.add(lis.get(r.nextInt(collection.size())));
            }
        } else {
            int[] indexes = new int[amount];
            for (int i = 0; i < amount; i++) {
                int next = r.nextInt(collection.size());
                if (Arrays.stream(indexes).noneMatch(c -> c == next)) {
                    indexes[i] = next;
                }
            }
            Iterator<T> itr = collection.iterator();
            Arrays.sort(indexes);
            for (int i = 0, w = 0; i < amount && w < indexes.length; i++) {
                T val = itr.next();
                if (indexes[w] == i) {
                    back.add(val);
                    w++;
                }
            }
        }
        return back;
    }

    public static <T> T getFromWeightedMap(Map<T, Double> weights) {
        if (weights.isEmpty()) {
            return null;
        }
        double chance = THREAD_LOCAL().nextDouble() * weights.values().stream().reduce(0D, Double::sum);
        AtomicDouble needle = new AtomicDouble();
        return weights.entrySet().stream().filter((ent) -> {
            return needle.addAndGet(ent.getValue()) >= chance;
        }).findFirst().map(Map.Entry::getKey).orElse(null);
    }


}
