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
import com.codelanx.commons.logging.Logging;
import com.codelanx.commons.util.exception.Exceptions;
import com.google.common.primitives.Primitives;
import org.apache.commons.lang3.Validate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Represents utility functions that utilize either java's reflection api,
 * analysis of the current Stack in use, low-level operations, primitives, or
 * other methods that deal with operations outside the norm of Java or Bukkit's
 * own system
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
            //We shouldn't be able to get this high on the stack at theoritical offset 0
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
     * Checks whether or not there is a plugin on the server with the name of
     * the passed {@code name} paramater. This method achieves this by scanning
     * the plugins folder and reading the {@code plugin.yml} files of any
     * respective jarfiles in the directory.
     * 
     * @since 0.1.0
     * @version 0.2.0
     * 
     * @param name The name of the plugin as specified in the {@code plugin.yml}
     * @return The {@link File} for the plugin jarfile, or {@code null} if not
     *         found
     */
    public static File findPluginJarfile(String name) {
        File plugins = new File("plugins");
        Exceptions.illegalState(plugins.isDirectory(), "'plugins' isn't a directory! (wat)");
        for (File f : plugins.listFiles((File pathname) -> {
            return pathname.getPath().endsWith(".jar");
        })) {
            try (InputStream is = new FileInputStream(f); ZipInputStream zi = new ZipInputStream(is)) {
                ZipEntry ent = null;
                while ((ent = zi.getNextEntry()) != null) {
                    if (ent.getName().equalsIgnoreCase("plugin.yml")) {
                        break;
                    }
                }
                if (ent == null) {
                    continue; //no plugin.yml found
                }
                ZipFile z = new ZipFile(f);
                try (InputStream fis = z.getInputStream(ent);
                        InputStreamReader fisr = new InputStreamReader(fis);
                        BufferedReader scan = new BufferedReader(fisr)) {
                    String in;
                    while ((in = scan.readLine()) != null) {
                        if (in.startsWith("name: ")) {
                            if (in.substring(6).equalsIgnoreCase(name)) {
                                return f;
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                Debugger.error(ex, "Error reading plugin jarfiles");
            }
        }
        return null;
    }

    public static <K, V> Map<K, V> difference(Map<K, V> initial, Map<K, V> replacer) {
        Map<K, V> back = new HashMap<>();
        back.putAll(initial);
        back.putAll(replacer);
        back.entrySet().removeAll(initial.entrySet());
        return back;
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
     * @param lock      The {@link Lock} to utilize
     * @param operation The code to be run
     * @version 0.2.0
     * @since 0.2.0
     */
    public static <R> R operateLock(Lock lock, Supplier<R> operation) {
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
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

    public static UUID parseUUID(String uuid) {
        Validate.isTrue(uuid.length() == 32 || uuid.length() == 36, "Invalid UUID format supplied");
        if (uuid.length() == 36) {
            return UUID.fromString(uuid);
        } else {
            return UUID.fromString(uuid.substring(0, 8)
                    + "-" + uuid.substring(8, 12)
                    + "-" + uuid.substring(12, 16)
                    + "-" + uuid.substring(16, 20)
                    + "-" + uuid.substring(20, 32));
        }
    }

    public static String objectString(Object in) {
        if (in == null) {
            return "null";
        }
        return in.getClass().getName() + "@" + Integer.toHexString(in.hashCode());
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

    public static Optional<Integer> parseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Double> parseDouble(String s) {
        try {
            return Optional.of(Double.parseDouble(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Float> parseFloat(String s) {
        try {
            return Optional.of(Float.parseFloat(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Short> parseShort(String s) {
        try {
            return Optional.of(Short.parseShort(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Long> parseLong(String s) {
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Byte> parseByte(String s) {
        try {
            return Optional.of(Byte.parseByte(s));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static String properEnumName(Enum<?> val) {
        String s = val.name().toLowerCase();
        char[] ch = s.toCharArray();
        boolean skip = false;
        for (int i = 0; i < ch.length; i++) {
            if (skip) {
                skip = false;
                continue;
            }
            if (i == 0) {
                ch[i] = Character.toUpperCase(ch[i]);
                continue;
            }
            if (ch[i] == '_') {
                ch[i] = ' ';
                if (i < ch.length - 1) {
                    ch[i + 1] = Character.toUpperCase(ch[i + 1]);
                }
                skip = true;
            }
        }
        return new String(ch).intern();
    }

    /**
     * Façade method for streaming {@link Iterable<T>}, so that Iterable can be accepted as a general parameter
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param itr The {@link Iterable<T>} to turn into a stream
     * @param <T> The type of the stream
     * @return A {@link Stream<T>} of the iterable elements
     */
    public static <T> Stream<T> stream(Iterable<T> itr) {
        if (itr instanceof Collection) {
            return ((Collection<T>) itr).stream();
        } else {
            return StreamSupport.stream(itr.spliterator(), false);
        }
    }

    /**
     * Façade method for arrays, to simplify accepting general parameters even further
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param obj The object(s) to stream
     * @param <T> The type of the objects and stream
     * @return A {@link Stream<T>} of the iterable elements
     */
    public static <T> Stream<T> stream(T... obj) {
        return Arrays.stream(obj);
    }

}
