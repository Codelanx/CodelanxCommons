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

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang3.Validate;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
            RNG.SECURE_RAND.setSeed(RNG.SECURE_RAND.generateSeed(RNG.THREAD_LOCAL.current().nextInt(30)));
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
    public static final ThreadLocalRandomWrapper THREAD_LOCAL = ThreadLocalRandom::current;
    
    @FunctionalInterface
    private static interface ThreadLocalRandomWrapper {
        public ThreadLocalRandom current();
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
        int rand = RNG.THREAD_LOCAL.current().nextInt(collection.size());
        if (collection instanceof List) {
            return ((List<T>) collection).get(rand);
        } else {
            Iterator<T> itr = collection.iterator();
            return IntStream.range(0, collection.size()).boxed().map(i -> {
                T var = itr.next();
                return i == rand ? var : null;
            }).filter(Objects::nonNull).findFirst().orElse(null);
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
     * @throws IllegalArgumentException If {@code collection} is null
     */
    public static <T> List<T> get(Collection<T> collection, int amount) {
        Validate.notNull(collection, "Cannot retrieve a value from a null collection");
        Validate.isTrue(amount <= collection.size(), "Amount cannot be greater than the collection size");
        List<T> back = new ArrayList<>();
        if (amount <= 0 || collection.size() == 0) {
            return back;
        }
        if (amount == 1) {
            if (collection.size() == 1) {
                back.add(collection.iterator().next());
                return back;
            }
            back.add(RNG.get(collection));
            return back;
        }
        Random r = RNG.THREAD_LOCAL.current();
        if (collection instanceof List) {
            List<T> lis = (List<T>) collection;
            for (int i = 0; i < amount; i++) {
                back.add(lis.get(r.nextInt(collection.size())));
            }
        } else {
            List<Integer> indexes = IntStream.range(0, collection.size()).boxed().collect(Collectors.toList());
            Collections.shuffle(indexes);
            indexes = indexes.subList(0, amount);
            Collections.sort(indexes);
            Iterator<T> itr = collection.iterator();
            for (int i = 0, w = 0; i < amount && w < indexes.size(); i++) {
                T val = itr.next();
                if (indexes.get(w) == i) {
                    back.add(val);
                    w++;
                }
            }
        }
        return back;
    }

    /**
     * Returns a random variable from a weighted map, which is a map of objects to their respective
     * probabilities. An example would be a map of 5 objects, all mapped to 1, 2, 3, 4 and 5. Their
     * respective probabilities would be {@code weight/map weight}, where {@code map weight} is the
     * sum of all the weights in the map. The weights do not have to add up to 1 or 100 or any
     * arbitrary number. Any numbers are supported
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param weights A {@link Map Map&lt;T, Number&gt;} of weighted objects
     * @param <T> The type of the objects being weighted in the {@link Map}
     * @return A randomly selected variable, based on the probabilities from the provided {@link Map}
     */
    public static <T> T getFromWeightedMap(Map<T, ? extends Number> weights) {
        if (weights == null || weights.isEmpty()) {
            return null;
        }
        double chance = THREAD_LOCAL.current().nextDouble() * weights.values().stream().map(Number::doubleValue).reduce(0D, Double::sum);
        AtomicDouble needle = new AtomicDouble();
        return weights.entrySet().stream().filter((ent) -> {
            return needle.addAndGet(ent.getValue().doubleValue()) >= chance;
        }).findFirst().map(Map.Entry::getKey).orElse(null);
    }

}
