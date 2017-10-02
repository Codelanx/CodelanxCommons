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

import com.codelanx.commons.logging.Logging;
import com.google.common.primitives.Primitives;
import org.apache.commons.lang3.Validate;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Represents utility functions that utilize either java's reflection api,
 * analysis of the current Stack in use, low-level operations, primitives, or
 * other methods that deal with operations outside the normal (read: non-reflecting)
 * Java usage.
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.2.0
 */
public final class Reflections {

    private Reflections() {
    }

    /**
     * Returns {@code true} if the specified target has the passed
     * {@link Annotation}
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param target The relevant element to check for an {@link Annotation}
     * @param check  The {@link Annotation} class type to check for
     * @return {@code true} if the {@link Annotation} is present
     */
    public static boolean hasAnnotation(AnnotatedElement target, Class<? extends Annotation> check) {
        return target.getAnnotation(check) != null;
    }

    /**
     * Returns whether or not the current context was called from a class
     * (instance or otherwise) that is passed to this method. This method can
     * match a regex pattern for multiple classes. Note anonymous classes have
     * an empty name.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param regex The regex to check against the calling class
     * @return {@code true} if accessed from a class that matches the regex
     */
    public static boolean accessedFrom(String regex) {
        return Reflections.getCaller(1).getClassName().matches(regex);
    }

    /**
     * Returns whether or not the current context was called from a class
     * (instance or otherwise) that is passed to this method. Note anonymous
     * classes have an empty name.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param clazz The class to check
     * @return {@code true} if accessed from this class
     */
    public static boolean accessedFrom(Class<?> clazz) {
        return Reflections.getCaller(1).getClassName().equals(clazz.getName());
    }

    /**
     * Returns a {@link StackTraceElement} of the direct caller of the current
     * method's context.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param offset The number of additional methods to look back
     * @return A {@link StackTraceElement} representing where the current
     *         context was called from
     */
    public static StackTraceElement getCaller(int offset) {
        Validate.isTrue(offset >= 0, "Offset must be a positive number");
        StackTraceElement[] elems = Thread.currentThread().getStackTrace();
        if (elems.length < 4 + offset) {
            //We shouldn't be able to get this high on the stack at theoretical offset 0
            throw new IndexOutOfBoundsException("Offset too large for current stack");
        }
        return elems[3 + offset];
    }

    /**
     * Returns a {@link StackTraceElement} of the direct caller of the current
     * method's context. This method is equivalent to calling
     * {@code Reflections.getCaller(0)}
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @return A {@link StackTraceElement} representing where the current
     *         context was called from
     */
    public static StackTraceElement getCaller() {
        return Reflections.getCaller(1);
    }

    /**
     * Returns a "default value" of -1 or {@code false} for a default type's
     * class or autoboxing class. Will return {@code null} if not relevant to
     * primitives
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param <T> The type of the primitive
     * @param c The primitive class
     * @return The default value, or {@code null} if not a primitive
     */
    public static <T> T defaultPrimitiveValue(Class<T> c) {
        if (c.isPrimitive() || Primitives.isWrapperType(c)) {
            c = Primitives.unwrap(c);
            T back = null;
            if (c == boolean.class) {
                back = c.cast(false);
            } else if (c == char.class) { //god help me
                back = c.cast((char) -1);
            } else if (c == float.class) {
                back = c.cast(-1F);
            } else if (c == long.class) {
                back = c.cast(-1L);
            } else if (c == double.class) {
                back = c.cast(-1D);
            } else if (c == int.class) {
                back = c.cast(-1); //ha
            } else if (c == short.class) {
                back = c.cast((short) -1);
            } else if (c == byte.class) {
                back = c.cast((byte) -1);
            }
            return back;
        }
        return null;
    }

    /**
     * Prints out the stack history 10 calls back
     *
     * @version 0.2.0
     * @since 0.2.0
     */
    public static void trace() {
        trace(10);
    }

    /**
     * Prints out the stack history {@code length} calls back
     *
     * @since 0.2.0
     * @version 0.2.0
     *
     * @param length The number of calls back on the stack to print out
     */
    public static void trace(int length) {
        StackTraceElement[] elems = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder("Callback history:");
        for (int i = 2; i < elems.length && i < length + 2; i++) {
            sb.append(String.format("\n\tCalled from:\t%s#%s:%d\t\tFile: %s", elems[i].getClassName(), elems[i].getMethodName(), elems[i].getLineNumber(), elems[i].getFileName()));
        }
        Logging.info(sb.toString());
    }

    public static Class<?> getArrayClass(Class<?> componentType) throws ClassNotFoundException {
        ClassLoader classLoader = componentType.getClassLoader();
        String name;
        if (componentType.isArray()) {
            // just add a leading "["
            name = "[" + componentType.getName();
        } else if (componentType == boolean.class) {
            name = "[Z";
        } else if (componentType == byte.class) {
            name = "[B";
        } else if (componentType == char.class) {
            name = "[C";
        } else if (componentType == double.class) {
            name = "[D";
        } else if (componentType == float.class) {
            name = "[F";
        } else if (componentType == int.class) {
            name = "[I";
        } else if (componentType == long.class) {
            name = "[J";
        } else if (componentType == short.class) {
            name = "[S";
        } else {
            // must be an object non-array class
            name = "[L" + componentType.getName() + ";";
        }
        return classLoader != null ? classLoader.loadClass(name) : Class.forName(name);
    }

    //TODO: Remove as of 0.4.0

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Retrieves a {@link Map Map&lt;K, V&gt;} which represents all entries in the
     * {@code replacer} map parameter which do not exist in the {@code initial} parameter
     *
     * @since 0.3.2
     * @version 0.3.3
     *
     * @deprecated
     * @see Clump#difference(Map, Map)
     * @param initial The initial map to filter by
     * @param replacer The map with entries to validate for differences
     * @param <K> The key type of the maps
     * @param <V> The value type of the maps
     * @return A {@link Map Map&lt;K, V&gt;} with entries solely existing in {@code replacer}
     */
    @Deprecated
    public static <K, V> Map<K, V> difference(Map<K, V> initial, Map<K, V> replacer) {
        return Clump.difference(initial, replacer);
    }

    @Deprecated
    public static UUID parseUUID(String uuid) {
        return Readable.parseUUID(uuid);
    }

    @Deprecated
    public static String objectString(Object in) {
        return Readable.objectString(in);
    }

    //guaranteed safety, so no class cast if wrong (just converts)
    //CCE occurs for bad Class<T> parameter
    @Deprecated
    public static <T extends Number> T convertNumber(Number in, Class<T> out) {
        return Readable.convertNumber(in ,out);
    }

    @Deprecated
    public static Optional<Integer> parseInt(String s) {
        return Readable.parseInt(s);
    }

    @Deprecated
    public static Optional<Double> parseDouble(String s) {
        return Readable.parseDouble(s);
    }

    @Deprecated
    public static Optional<Float> parseFloat(String s) {
        return Readable.parseFloat(s);
    }

    @Deprecated
    public static Optional<Short> parseShort(String s) {
        return Readable.parseShort(s);
    }

    @Deprecated
    public static Optional<Long> parseLong(String s) {
        return Readable.parseLong(s);
    }

    @Deprecated
    public static Optional<Byte> parseByte(String s) {
        return Readable.parseByte(s);
    }

    @Deprecated
    public static String properEnumName(Enum<?> val) {
        return Readable.properEnumName(val);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Converts a stack trace into a String for ease-of-use in network-level debugging
     *
     * @since 0.3.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Readable#stackTraceToString(Throwable)
     * @param t The {@link Throwable}
     * @return A String form of the exception
     */
    @Deprecated
    public static String stackTraceToString(Throwable t) {
        return Readable.stackTraceToString(t);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Performs an operation with a lock, saving room by not requiring a lot of
     * {@code try-finally} blocks
     *
     * @since 0.2.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Parallel#operateLock(Lock, Runnable)
     * @param lock The {@link Lock} to utilize
     * @param operation The code to be run
     */
    @Deprecated
    public static void operateLock(Lock lock, Runnable operation) {
        Parallel.operateLock(lock, operation);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Performs an operation with a lock, saving room by not requiring a lot of
     * {@code try-finally} blocks
     *
     * @since 0.2.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Parallel#operateLock(Lock, Supplier)
     * @param <R> The return type of the {@link Supplier}
     * @param lock      The {@link Lock} to utilize
     * @param operation The code to be run
     * @return A value returned from the inner {@link Supplier}
     */
    @Deprecated
    public static <R> R operateLock(Lock lock, Supplier<R> operation) {
        return Parallel.operateLock(lock, operation);
    }


    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Returns a {@link Set} of keys that closest match the passed in string
     * value
     *
     * @since 0.1.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Clump#matchClosestKeys(Map, String)
     * @param map The {@link Map} with a string key to look through
     * @param search The string to use as a base to search for
     * @return A list of the closest matching keys, will be empty if no keys
     *         begin with the search phrase
     */
    @Deprecated
    public static Set<String> matchClosestKeys(Map<String, ?> map, String search) {
        return Clump.matchClosestKeys(map, search);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Returns a {@link List} of values mapped from keys that closest match the
     * passed in string value
     *
     * @since 0.1.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Clump#matchClosestValues(Map, String)
     * @param <T> The type of the values
     * @param map The {@link Map} to look through
     * @param search The string to use as a base to search for
     * @return A list of the closest matching values, will be empty if no keys
     *         begin with the search phrase
     */
    @Deprecated
    public static <T> List<T> matchClosestValues(Map<String, T> map, String search) {
        return Clump.matchClosestValues(map, search);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Returns a new {@link ArrayList} of the passed parameters that is not
     * fixed-size, akin to what {@link Arrays#asList(Object...)} returns
     *
     * @since 0.1.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Clump#nonFixedList(Object[])
     * @param <T> The type of the objects being passed in
     * @param items The parameters to add to a list
     * @return A new {@link List} of the items, or an empty list if no params
     */
    @Deprecated
    public static <T> List<T> nonFixedList(T... items) {
        return Clump.nonFixedList(items);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Provides a way of mutating an object (into a new result) without worrying
     * about null safety. This is particularly useful in constructor
     * overloading, where null safety cannot be determined before having
     * to call upon the variable
     *
     * @since 0.2.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Clump#nullSafeMutation(Object, Function)
     * @param <I> The potentially null variable to be mutated
     * @param <R> The expected result type
     * @param in The variable to evaluate for a mutation
     * @param act The action to take on the variable
     * @return The expected result
     * @throws IllegalArgumentException if the passed parameter is null
     */
    @Deprecated
    public static <I, R> R nullSafeMutation(I in, Function<I, R> act) {
        return Clump.nullSafeMutation(in, act);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Façade method for streaming {@link Iterable}, so that Iterable can be accepted as a general parameter
     *
     * @since 0.3.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Clump#stream(Iterable)
     * @param itr The {@link Iterable} to turn into a stream
     * @param <T> The type of the stream
     * @return A {@link Stream} of the iterable elements
     */
    @Deprecated
    public static <T> Stream<T> stream(Iterable<T> itr) {
        return Clump.stream(itr);
    }

    /**
     * <b>DUE FOR REMOVAL IN 0.4.0</b>
     *
     * Façade method for arrays, to simplify accepting general parameters even further
     *
     * @since 0.3.0
     * @version 0.3.3
     *
     * @deprecated
     * @see Stream#of(Object[])
     * @see Arrays#stream(Object[])
     * @param obj The object(s) to stream
     * @param <T> The type of the objects and stream
     * @return A {@link Stream} of the iterable elements
     */
    @Deprecated
    public static <T> Stream<T> stream(T... obj) {
        return Arrays.stream(obj);
    }
}
