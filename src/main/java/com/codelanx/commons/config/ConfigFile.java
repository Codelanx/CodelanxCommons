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
package com.codelanx.commons.config;

import com.codelanx.commons.data.FileDataType;
import com.codelanx.commons.data.FileSerializable;
import com.codelanx.commons.util.Reflections;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.Validate;

/**
 * Represents a single value that is dynamically retrieved from a
 * {@link FileDataType}. This value can be of any type, and the class should
 * typically be implemented through an enum
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public interface ConfigFile extends InfoFile {

    /**
     * Attempts to return the {@link ConfigFile} value as a casted type. If the
     * value cannot be casted it will attempt to return the default value. If
     * the default value is inappropriate for the class, the method will
     * throw a {@link ClassCastException}.
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param <T> The type of the casting class
     * @param c The class type to cast to
     * @return A casted value, or {@code null} if unable to cast. If the passed
     *         class parameter is of a primitive type or autoboxed primitive,
     *         then a casted value of -1 is returned, or {@code false} for
     *         booleans. If the passed class parameter is for {@link String},
     *         then {@link Object#toString()} is called on the value instead
     */
    default public <T> T as(Class<T> c) {
        Validate.notNull(c, "Cannot cast to null");
        Validate.isTrue(Primitives.unwrap(c) != void.class, "Cannot cast to a void type");
        boolean primitive = Primitives.isWrapperType(c) || Primitives.isWrapperType(Primitives.wrap(c));
        Object o = this.get();
        if (primitive) {
            T back;
            if (o == null) {
                return Reflections.defaultPrimitiveValue(c);
            } else {
                back = Primitives.wrap(c).cast(o);
            }
            return back;
        }
        if (o == null) {
            return null;
        }
        if (o instanceof Map && FileSerializable.class.isAssignableFrom(c)) {
            Map<String, Object> map = (Map<String, Object>) o;
            Class<?> type = map.getClass();
            try {
                //TODO: no raw reflection here
                Constructor<T> obj = c.getConstructor(type);
                obj.setAccessible(true);
                return obj.newInstance(map);
            } catch (NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (c == String.class) {
            return (T) String.valueOf(o);
        }
        if (c.isInstance(o)) {
            return c.cast(o);
        }
        if (c.isInstance(this.getDefault())) {
            return c.cast(this.getDefault());
        }
        throw new ClassCastException("Unable to cast config value");
    }

    /**
     * Attempts to return the {@link ConfigFile} value as a casted type. If the
     * value cannot be casted it will attempt to return the default value. If
     * the default value is inappropriate for the class, the method will
     * throw a {@link ClassCastException}. This exception is also throwing if
     * the type used for the members contained within the collection are
     * incorrect
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param <T> The type of the casting class
     * @param <G> The type of the collection's contents
     * @param collection The collection type to cast to
     * @param type The generic bounding for the collection
     * @return A casted value, or {@code null} if unable to cast. If the passed
     *         class parameter is of a primitive type or autoboxed primitive,
     *         then a casted value of -1 is returned, or {@code false} for
     *         booleans. If the passed class parameter is for {@link String},
     *         then {@link Object#toString()} is called on the value instead
     */
    @SuppressWarnings("rawtypes")
    //TODO: FileSerializable null edge cases
    default public <G, T extends Collection<G>> T as(Class<T> collection, Class<G> type) {
        Collection<?> col = this.as(collection);
        if (FileSerializable.class.isAssignableFrom(type)) {
            //do a conversion
            Object nonMatch = col.stream().filter(o -> !(o instanceof Map) || !type.isAssignableFrom(o.getClass())).findAny().orElse(null);
            if (nonMatch != null) {
                throw new IllegalArgumentException("Cannot deserialize non-map object: " + nonMatch);
            }
            try {
                //TODO: no raw reflection here
                T back = collection.newInstance();
                Constructor<G> obj = type.getConstructor(Map.class);
                obj.setAccessible(true);
                AtomicBoolean broken = new AtomicBoolean();
                col.stream().filter(o -> broken.compareAndSet(false, o != null && !(o instanceof Map) && !type.isAssignableFrom(o.getClass()))).forEach(o -> {
                    if (broken.get()) {
                        return;
                    }
                    if (o == null) {
                        back.add(null);
                        return;
                    }
                    if (type.isAssignableFrom(o.getClass())) {
                        back.add((G) o);
                        return;
                    }
                    try {
                        G val = obj.newInstance(o);
                        back.add(val);
                    } catch (InstantiationException
                            | IllegalAccessException
                            | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });
                if (back.size() != col.size()) { //TODO: Maybe not calculate to end? soft-fuse atm on that boolean
                    throw new IllegalStateException("Mismatched serialization of collection");
                }
                return back;
            } catch (NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        for (Object o : col) {
            if (!type.isInstance(o)) {
                throw new ClassCastException("Inappropriate generic type for collection (Found: " + o.getClass().toString() + ", Attempted: " + type.toString() + ")");
            }
        }
        return (T) col;
    }

    /**
     * Attempts to return the {@link ConfigFile} value as a casted type. If the
     * value cannot be casted it will attempt to return the default value. If
     * the default value is inappropriate for the class, the method will
     * throw a {@link ClassCastException}. This exception is also throwing if
     * the type used for the members contained within the collection are
     * incorrect
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param <K> The type of the keys for this map
     * @param <V> The type of the values for this map
     * @param <M> The type of the map
     * @param map The class object of the map type to use
     * @param key The class object of the key types
     * @param value The class object of the value types
     * @return A casted value, or {@code null} if unable to cast. If the passed
     *         class parameter is of a primitive type or autoboxed primitive,
     *         then a casted value of -1 is returned, or {@code false} for
     *         booleans. If the passed class parameter is for {@link String},
     *         then {@link Object#toString()} is called on the value instead
     */
    @SuppressWarnings("rawtypes")
    default public <K, V, M extends Map<K, V>> M as(Class<M> map, Class<K> key, Class<V> value) {
        Map<?, ?> m = this.as(map);
        if (FileSerializable.class.isAssignableFrom(value)) {
            //precheck
            Object nonKey = m.keySet().stream().filter(k -> k != null && !key.isAssignableFrom(k.getClass())).findAny().orElse(null);
            if (nonKey != null) {
                throw new IllegalArgumentException("Mismatched key type. Expected: " + key.getName() + ", found: " + nonKey.getClass().getName());
            }
            Object nonMatch = m.entrySet().stream()
                    .filter(o -> o.getValue() != null && !(o.getValue() instanceof Map) && !value.isAssignableFrom(o.getValue().getClass()))
                    .findAny().map(Entry::getValue).orElse(null);
            if (nonMatch != null) {
                throw new IllegalArgumentException("Cannot deserialize non-map object: " + nonMatch);
            }
            //do a conversion
            try {
                //TODO: no raw reflection here
                if (Map.class.equals(map)) {
                    map = (Class<M>) LinkedHashMap.class;
                }
                M back = map.newInstance();
                Constructor<V> obj = value.getConstructor(Map.class);
                obj.setAccessible(true);
                AtomicBoolean broken = new AtomicBoolean();
                m.entrySet().stream()
                        .filter(ent -> broken.compareAndSet(false, ent.getValue() != null && !(ent.getValue() instanceof Map) && !value.isAssignableFrom(ent.getValue().getClass())))
                        .map(ent -> {
                            if (broken.get()) {
                                return null;
                            }
                            if (ent.getValue() == null || value.isAssignableFrom(ent.getValue().getClass())) {
                                back.put((K) ent.getKey(), (V) ent.getValue());
                                return ent.getValue();
                            }
                            Map<?, ?> o = (Map<?, ?>) ent.getValue();
                            try {
                                V vo = obj.newInstance(o);
                                back.put((K) ent.getKey(), vo);
                                return vo;
                            } catch (InstantiationException
                                    | IllegalAccessException
                                    | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .filter(o -> broken.compareAndSet(false, o == null)).count();
                if (back.size() != m.size()) { //TODO: Maybe not calculate to end? soft-fuse atm on that boolean
                    throw new IllegalStateException("Mismatched serialization of collection");
                }
                return back;
            } catch (NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry<?, ?> ent : m.entrySet()) {
            if (!key.isInstance(ent.getKey()) || !value.isInstance(ent.getValue())) {
                throw new ClassCastException("Inappropriate generic types for map");
            }
        }
        return (M) m;
    }

    /**
     * Sets a value in the {@link FileDataType}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param val The value to set
     * @return The previous {@link ConfigFile} value
     */
    default public ConfigFile set(Object val) {
        this.getConfig().set(this.getPath(), val);
        return this;
    }

    /**
     * Retrieves an anonymous value which can utilize a
     * {@link ConfigFile} parameter to retrieve data from any source
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param <T> Represents a {@link FileDataType} passed to the method
     * @param file The {@link FileDataType} to use
     * @param config The {@link ConfigFile} value to search with
     * @return An anonymous class wrapping of the configuration and keys
     */
    public static <T extends FileDataType> ConfigFile retrieve(T file, ConfigFile config) {
        Validate.notNull(file, "File cannot be null");
        Validate.notNull(config, "Config cannot be null");
        return ConfigFile.anonMutator(config.getPath(), config.getDefault(), file);
    }

    /**
     * Facade method for {@link ConfigFile#retrieve(FileDataType, ConfigFile)}
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param t A {@link FileDataType} to retrieve this config value from
     * @see ConfigFile#retrieve(FileDataType, ConfigFile)
     * @return A config value that can be used to retrieve values from
     */
    default public ConfigFile retrieve(FileDataType t) {
        return ConfigFile.retrieve(t, this);
    }

    public static <T extends FileDataType> ConfigFile anonMutator(String path, Object def, T file) {
        return new ConfigFile() {

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public Object getDefault() {
                return def;
            }

            @Override
            public T getConfig() {
                return file;
            }

            @Override
            public DataHolder<FileDataType> getData() {
                throw new UnsupportedOperationException("Anonymous ConfigFile classes do not have DataHolders");
            }

        };
    }
}