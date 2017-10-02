package com.codelanx.commons.util;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for collection, map, and array helper methods
 *
 * @since 0.3.3
 * @author 1Rogue
 * @version 0.3.3
 */
public final class Clump {

    private Clump() {
    }

    /**
     * Returns a {@link Set} of keys that closest match the passed in string
     * value
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param map The {@link Map} with a string key to look through
     * @param search The string to use as a base to search for
     * @return A list of the closest matching keys, will be empty if no keys
     *         begin with the search phrase
     */
    public static Set<String> matchClosestKeys(Map<String, ?> map, String search) {
        return map.keySet().stream().filter(k -> k.startsWith(search)).collect(Collectors.toSet());
    }

    /**
     * Returns a {@link List} of values mapped from keys that closest match the
     * passed in string value
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param <T> The type of the values
     * @param map The {@link Map} to look through
     * @param search The string to use as a base to search for
     * @return A list of the closest matching values, will be empty if no keys
     *         begin with the search phrase
     */
    public static <T> List<T> matchClosestValues(Map<String, T> map, String search) {
        return Reflections.matchClosestKeys(map, search).stream().map(map::get).collect(Collectors.toList());
    }

    /**
     * Returns a new {@link ArrayList} of the passed parameters that is not
     * fixed-size, akin to what {@link Arrays#asList(Object...)} returns
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param <T> The type of the objects being passed in
     * @param items The parameters to add to a list
     * @return A new {@link List} of the items, or an empty list if no params
     */
    public static <T> List<T> nonFixedList(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    /**
     * Provides a way of mutating an object (into a new result) without worrying
     * about null safety. This is particularly useful in constructor
     * overloading, where null safety cannot be determined before having
     * to call upon the variable
     *
     * @since 0.2.0
     * @version 0.2.0
     *
     * @param <I> The potentially null variable to be mutated
     * @param <R> The expected result type
     * @param in The variable to evaluate for a mutation
     * @param act The action to take on the variable
     * @return The expected result
     * @throws IllegalArgumentException if the passed parameter is null
     */
    public static <I, R> R nullSafeMutation(I in, Function<I, R> act) {
        Validate.notNull(in);
        return act.apply(in);
    }

    /**
     * Façade method for streaming {@link Iterable}, so that Iterable can be accepted as a general parameter
     *
     * @since 0.3.0
     * @version 0.3.0
     *
     * @param itr The {@link Iterable} to turn into a stream
     * @param <T> The type of the stream
     * @return A {@link Stream} of the iterable elements
     */
    public static <T> Stream<T> stream(Iterable<T> itr) {
        if (itr instanceof Collection) {
            return ((Collection<T>) itr).stream();
        } else {
            return StreamSupport.stream(itr.spliterator(), false);
        }
    }

    /**
     * Retrieves a {@link Map Map&lt;K, V&gt;} which represents all entries in the
     * {@code replacer} map parameter which do not exist in the {@code initial} parameter
     *
     * @since 0.3.2
     * @version 0.3.3
     *
     * @param initial The initial map to filter by
     * @param replacer The map with entries to validate for differences
     * @param <K> The key type of the maps
     * @param <V> The value type of the maps
     * @return A {@link Map Map&lt;K, V&gt;} with entries solely existing in {@code replacer}
     */
    public static <K, V> Map<K, V> difference(Map<K, V> initial, Map<K, V> replacer) {
        Map<K, V> back = new HashMap<>();
        back.putAll(initial);
        back.putAll(replacer);
        back.entrySet().removeAll(initial.entrySet());
        return back;
    }
}
