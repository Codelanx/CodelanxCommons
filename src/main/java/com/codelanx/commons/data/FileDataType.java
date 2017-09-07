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
package com.codelanx.commons.data;

import com.codelanx.commons.config.ConfigFile;
import com.codelanx.commons.data.types.Json;
import com.codelanx.commons.data.types.Yaml;
import com.codelanx.commons.data.types.XML;
import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.logging.Logging;
import com.codelanx.commons.util.Reflections;
import com.codelanx.commons.util.exception.Exceptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Opens and loads a file into memory using the appropriate data type. This
 * data type should have a single-argument constructor which takes a
 * {@link File} argument.
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
//TODO: Thread safety for non-initial files, thread safety on indexes, incremental saving
public abstract class FileDataType implements DataType {

    protected static final boolean DEBUG_SERIALIZATION = true; //if true, will serialize on all #set calls
    protected static final String NEWLINE = System.getProperty("line.separator");
    private static final ExecutorService SAVER = Executors.newSingleThreadScheduledExecutor();

    protected final ReadWriteLock fileLock = new ReentrantReadWriteLock();
    //protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    //protected final IndexLock indexes = new IndexLock();
    /** The {@link File} location of this {@link FileDataType} */
    protected final File location;
    private final Object root; //can be a list or map, but what about null for empty data?

    protected FileDataType(File location) {
        this.location = location;
        Object root = null; //left as null for integrity safety (fails and won't overwrite a file to empty)
        try {
            if (this.location == null) {
                root = this.newMapping();
            } else {
                root = this.readRaw();
            }
        } catch (IOException ex) {
            Debugger.error(ex, "Error loading %s file '%s'", this.getClass().getSimpleName(), location.getPath());
        }
        if (root instanceof Map) {
            this.root = this.deserializeMap((Map<String, Object>) root);
        } else if (root instanceof Collection) {
            this.root = this.deserializeArray(root);
        } else {
            throw new IllegalArgumentException("Cannot have a literal as the root value");
        }
    }

    protected FileDataType(String data) {
        this.location = null;
        this.root = this.parse(data);
    }

    /**
     * Sets the value at the location specified by the passed path
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param path The path to set, delimited by '{@code .}'
     * @param value The value to set
     */
    public void set(String path, Object value) {
        if (this.isSeries()) {
            throw new UnsupportedOperationException("Cannot set mapping values on a series");
        }
        String[] ladder = FileDataType.getLadder(path);
        //Reflections.operateLock(this.lock.readLock(), () -> {
            Map<String, Object> data = this.traverse(true, ladder);
            //Reflections.operateLock(this.indexes.write(path), () -> {
                if (value != null) {
                    data.put(ladder[ladder.length - 1], DEBUG_SERIALIZATION ? this.parseSerializable(value) : value);
                } else {
                    data.remove(ladder[ladder.length - 1]);
                    //this.indexes.remove(path);
                }
            //});
        //});
    }

    public void setSeries(Collection<Object> value) {
        if (!this.isSeries()) {
            throw new UnsupportedOperationException("Cannot set a series value on a mapping");
        }
        Collection<Object> all = (Collection<Object>) this.root;
        all.clear();
        all.addAll(value);
    }

    /**
     * Returns whether or not there is an object located at the specified path
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param path The path to set, delimited by '{@code .}'
     * @return {@code true} if a value is found, {@code false} otherwise
     */
    public boolean isSet(String path) {
        if (this.isSeries()) {
            throw new UnsupportedOperationException("Cannot set mapping values on a series");
        }
        String[] ladder = FileDataType.getLadder(path);
        //return Reflections.operateLock(this.lock.readLock(), () -> {
            Map<String, Object> data = this.getContainer(ladder);
            return data.containsKey(ladder[ladder.length - 1]);//Reflections.operateLock(this.indexes.read(path), () -> data.containsKey(ladder[ladder.length - 1]));
        //});
    }

    /**
     * Gets the object at the specified path
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param path The path to set, delimited by '{@code .}'
     * @return The object found in memory at this location, or {@code null} if
     *         nothing is found
     */
    public Object get(String path) {
        return this.get(path, null);
    }

    public ConfigFile getMutable(String path) {
        return this.getMutable(path, null);
    }

    public ConfigFile getMutable(String path, Object def) {
        return ConfigFile.anonMutator(path, def, this);
    }

    public ConfigFile mutable() {
        return this.getMutable("", null);
    }

    /**
     * Gets the object at the specified path, or returns the passed "default"
     * value if nothing is found
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param path The path to set, delimited by '{@code .}'
     * @param def The default value to return upon not finding a value
     * @return The relevant object, or the default if no value is found
     */
    public Object get(String path, Object def) {
        if (this.isSeries()) {
            throw new UnsupportedOperationException("Cannot retrieve key values from a series");
        }
        if (!this.isSet(path)) {
            return def;
        }
        String[] ladder = FileDataType.getLadder(path);
        //Object back = Reflections.operateLock(this.lock.readLock(), () -> {
            Map<String, Object> data = this.getContainer(ladder);
            //return Reflections.operateLock(this.indexes.read(path), () -> data.get(ladder[ladder.length - 1]));
            Object back = data.get(ladder[ladder.length - 1]);
        //});
        return this.parseDeserializable(back);
    }

    public boolean isSeries() {
        return this.root instanceof Collection;
    }

    /**
     * Saves any information in memory to the file it was loaded from.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @throws IOException Any read/write locks or permission errors on the file
     */
    public void save() throws IOException {
        this.save(this.location);
    }

    /**
     * Saves any information in memory to the file specified.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param target The file to save to
     * @throws IOException Any read/write locks or permission errors on the file
     */
    public void save(File target) throws IOException {
        Validate.notNull(target, "Cannot save to a null file");
        Runnable r = () -> {
            boolean lock = false;//target.equals(this.location);
            if (lock) {
                this.fileLock.writeLock().lock();
            }
            try {
                System.out.println("Writing to file...");
                this.writeRaw(target); //TODO: Use #write
            } finally {
                if (lock) {
                    this.fileLock.writeLock().unlock();
                }
            }
        };
        r.run();
        if (true) {
            return;
        }
        if (SAVER.isShutdown() || SAVER.isTerminated()) {
            r.run();
        } else {
            SAVER.submit(r);
        }
    }

    @Override
    public String toString() {
        return this.toString(this.getRoot());
    }

    private void writeRaw(File target) {
        System.out.println("getting copy");
        Object out = this.serializationCopy(); //for now this is outside so a deadlock won't wipe a file
        System.out.println("writing to file");
        try (FileWriter fw = new FileWriter(target)) {
            System.out.println("new: " + out);
            fw.write(this.toString(out));
            System.out.println("written and flushing now...");
            fw.flush();
        } catch (IOException e) {
            System.err.println("Error saving file to target");
            e.printStackTrace();
        } catch (Exception ex) {
            System.out.println("asdkjagsdh");
            ex.printStackTrace(System.err);
        }
    }

    //TODO: Automatically determine spacing, how to read a key in a section (to determine position)
    private void write(File target, Map<String, Object> out) {
        //TODO: Make this work
        if (true) {
            this.writeRaw(target);
            throw new UnsupportedOperationException("Incremental File saving not complete yet");
        }
        try {
            if (!target.exists()) {
                try (BufferedWriter wr = Files.newBufferedWriter(target.toPath(), StandardOpenOption.CREATE_NEW)) {
                    wr.write(this.toString(out));
                }
                return;
            }
            //this will probably throw a CCE with the wrong file, but this method shouldn't be invoked anyhow
            Map<String, Object> read = (Map<String, Object>) this.readRaw(target);
            Map<String, Object> toSet = Reflections.difference(read, out);
            StringBuilder output = new StringBuilder();
            int level = 0;
            int spaces = -1;
            try (BufferedReader br = Files.newBufferedReader(target.toPath())) {
                String line = br.readLine();
                int counted = line.indexOf(" ");
                if (counted > 0) {
                    if (spaces < 0) {
                        spaces = counted;
                    } else if (counted % spaces != 0) {
                        System.out.println("Mismatched spacing provided!");
                    }
                }
            }
        } catch(IOException ex) {
            Debugger.error(ex, "Error saving %s file '%s'", this.getClass().getSimpleName(), target.getPath());
        }
    }

    /**
     * Returns a new instance of a {@link FileDataType} based on the passed
     * class instance.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param <T> Represents the type that implements {@link FileDataType}
     * @param clazz The class object to be used for a new instance
     * @param location The location of the file to parse and use
     * @return The new instance of the requested {@link FileDataType}
     */
    public static <T extends FileDataType> T newInstance(Class<T> clazz, File location) {
        try {
            Constructor<T> r = clazz.getDeclaredConstructor(File.class);
            r.setAccessible(true);
            return r.newInstance(location);
        } catch (NoSuchMethodException ex) {
            Logging.simple().error(ex, "No File constructor found in FileDataType '%s'", clazz.getName());
        } catch (SecurityException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException ex) {
            Debugger.error(ex, "Error parsing data file");
        }
        return null;
    }

    public static <T extends FileDataType> T newInstance(Class<T> clazz, String data) {
        try {
            Constructor<T> r = clazz.getDeclaredConstructor(String.class);
            r.setAccessible(true);
            return r.newInstance(data);
        } catch (NoSuchMethodException ex) {
            Logging.simple().error(ex, "No File constructor found in FileDataType '%s'", clazz.getName());
        } catch (SecurityException
                | InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException ex) {
            Debugger.error(ex, "Error parsing data file");
        }
        return null;
    }

    public final Object parseSerializable(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Map) {
            return this.serializeMap((Map<String, Object>) o);
        } else if (o instanceof FileSerializable) {
            return this.serializeMap(((FileSerializable) o).getData());
        } else if (o.getClass().isArray()) {
            return this.serializeArray(o);
        } else if (o instanceof Collection) {
            return this.serializeArray(((Collection) o).toArray());
        } else if (o instanceof Enum<?>) {
            return ((Enum<?>) o).name();
        } else {
            return o;
        }
    }

    protected final Object parseDeserializable(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Map) {
            return this.deserializeMap((Map<String, Object>) o);
        } else if (o.getClass().isArray() || o instanceof List) {
            return this.deserializeArray(o);
        }
        return o;
    }

    public abstract Object parse(Reader reader) throws IOException;

    public abstract Object parse(String in);

    final Object serializationCopy() {
        //System.out.println("lock#write: " + this.lock.writeLock().tryLock());
        return this.serializationCopy(this.getRoot());//Reflections.operateLock(this.lock.writeLock(), () -> this.serializationCopy(this.getRoot()));
    }

    protected final Object serializationCopy(Object original) {
        System.out.println("Starting serialization copy");
        if (original instanceof Map) {
            Map<String, Object> back = this.newMapping();
            back.putAll((Map<String, Object>) original);
            back.replaceAll((k, v) -> this.parseSerializable(v));
            System.out.println("returning: " + back);
            return back;
        } else if (original instanceof Collection) {
            Collection<Object> orig = (Collection<Object>) original;
            List<Object> back = new ArrayList<>(orig.size());
            back.addAll(orig);
            back.replaceAll(this::parseSerializable);
            orig = this.newSeries();
            orig.addAll(back);
            return orig;
        } else {
            throw new IllegalArgumentException("Cannot copy a literal value");
        }
    }

    protected final Object deserializeMap(Map<String, Object> data) {
        data.replaceAll((k, v) -> this.parseDeserializable(v));
        Object ident = data.get(FileSerializable.IDENTIFIER_KEY);
        if (ident != null && ident instanceof String) {
            try {
                Class<?> base = Class.forName((String) ident);
                if (!FileSerializable.class.isAssignableFrom(base)) {
                    Debugger.error(new IllegalArgumentException("Cannot deserialize a non FileSerializable"), "Listed class does not implement FileSerializable");
                    return data;
                }
                Constructor<?> baseCon = base.getDeclaredConstructor(Map.class);
                Type[] types = baseCon.getGenericParameterTypes();
                if (types.length == 1 && types[0].getTypeName().equals("java.util.Map<java.lang.String, java.lang.Object>")) {
                    baseCon.setAccessible(true);
                    data.remove(FileSerializable.IDENTIFIER_KEY);
                    return baseCon.newInstance(data);
                }
            } catch (ClassNotFoundException e) {
                Debugger.error(e, "Attempted to deserialize nonexistant class: '%s'", ident);
            } catch (NoSuchMethodException e) {
                Debugger.error(e, "%s is missing a Map<String, Object> constructor", ident);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                Debugger.error(e, "Error while instantiating '%s'", ident);
            }
        }
        return data;
    }

    /**
     * Reads all the information currently saved in the file, disregarding
     * any changes in memory
     *
     * @return The raw {@link Map Map&lt;String, Object&gt;} value of the file. This method
     *         must return an empty {@link Map Map&lt;String, Object&gt;} if the file is
     *         empty or a reader returns null.
     * @throws IOException If the file could not be read
     */
    protected Object readRaw() throws IOException {
        this.fileLock.readLock().lock();
        try {
            return this.readRaw(this.location);
        } finally {
            this.fileLock.readLock().unlock();
        }
    }

    /**
     * Reads all the information currently saved in the file, disregarding
     * any changes in memory
     *
     * @param target The {@link File} to be read
     * @return The raw {@link Map Map&lt;String, Object&gt;} value of the {@code target} {@link File}
     *         This method must return an empty {@link Map Map&lt;String, Object&gt;} if the
     *         file is empty or a reader returns null.
     * @throws IOException If the file could not be read
     */
    protected Object readRaw(File target) throws IOException {
        if (FileUtils.readFileToString(target).trim().isEmpty()) {
            return this.newMapping();
        }
        Object o = this.parse(Files.newBufferedReader(target.toPath()));
        return o == null ? this.newMapping() : o;
    }

    protected abstract Object serializeMap(Map<String, Object> toFileFormat);

    protected abstract Object serializeArray(Object array);

    protected abstract Object deserializeArray(Object array);

    protected abstract Map<String, Object> newMapping();

    protected abstract Collection<Object> newSeries();

    /**
     * Returns a {@link Class} value representative of a {@link FileDataType},
     * or {@code null} if none is matched
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param in The string to parse
     * @return A relevant {@link FileDataType} class, or {@code null}
     */
    public static Class<? extends FileDataType> fromString(String in) {
        switch (Reflections.nullSafeMutation(in, String::toLowerCase)) {
            case "json":
                return Json.class;
            case "yaml":
            case "yml":
                return Yaml.class;
            case "xml":
                return XML.class;
        }
        return null;
    }

    /**
     * Converts a period-delimited string into a String array
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param path The path to split
     * @return The split path
     */
    private static String[] getLadder(String path) {
        return path.split("\\.");
    }

    /**
     * Traverses a {@link Map} tree from the internal root node. Will
     * return a {@link Map} container of the relevant element at the end
     * of the search, or just an empty {@link Map} if nothing exists.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param makePath Whether to fill empty space with {@link JSONObject}s
     * @param ladder A String array depicting the location to search in
     * @return A {@link JSONObject} containing the last node in the ladder
     */
    //not thread safe, wrap calls with proper index locks
    private Map<String, Object> traverse(boolean makePath, String... ladder) {
        if (this.isSeries()) {
            throw new UnsupportedOperationException("Cannot traverse mapping values in a series");
        }
        Map<String, Object> container = (Map<String, Object>) this.getRoot();
        if (ladder.length == 1 && ladder[0].isEmpty()) {
            //return root
            return container;
        }
        Exceptions.illegalState(container != null, "File failed to load, aborting operation");
        for (int i = 0; i < ladder.length - 1; i++) {
            if (!container.containsKey(ladder[i]) && makePath) {
                container.put(ladder[i], this.newMapping());
            }
            Map<String, Object> temp = (Map<String, Object>) container.get(ladder[i]);
            if (temp == null) {
                //purposefully set as null
                break;
            } else {
                container = temp;
            }
        }
        return container;
    }

    /**
     * Gets the {@link JSONObject} above the requested object specified by the
     * supplied {@code ladder} parameter.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param ladder A string array, already split in order of levels to
     * traverse
     * @return The {@link JSONObject} above the requested object
     */
    private Map<String, Object> getContainer(String... ladder) {
        return this.traverse(false, ladder);
    }

    /**
     * The root node of this {@link FileDataType}
     *
     * @since 0.3.2
     * @version 0.3.2
     * @deprecated Public as convenience, may not be exposed when more proper api comes around (key iteration)
     *
     * @return The root {@link Map} that represents this {@link FileDataType}
     */
    public Object getRoot() {
        return this.root;
    }

    protected abstract String toString(Object section);

    //TODO: make this work
    private static class IndexLock {

        private final Map<String, ReadWriteLock> locks = new HashMap<>();
        private final Map<String, IndexLock> indexes = new HashMap<>();

        public Lock write(String path) {
            if (true) {
                return this.getWrite(path.toLowerCase());
            }
            String[] ladder = FileDataType.getLadder(path.toLowerCase());
            return this.locks.computeIfAbsent(path.toLowerCase(), k -> new ReentrantReadWriteLock()).writeLock();
        }

        public Lock read(String path) {
            if (true) {
                return this.getRead(path.toLowerCase());
            }
            int dot = path.indexOf(".");
            Lock curr = this.getRead(path.substring(0, dot));
            if (dot < 0) {
                //we're at the end
                return curr;
            } else if (curr.tryLock()) {
                try {
                    return this.read(path.substring(dot + 1));
                } finally {
                    curr.unlock();
                }
            } else {

            }
            return null;
        }

        private Lock getRead(String key) {
            return this.locks.computeIfAbsent(key.toLowerCase(), k -> new ReentrantReadWriteLock()).readLock();
        }

        private Lock getWrite(String key) {
            return this.locks.computeIfAbsent(key.toLowerCase(), k -> new ReentrantReadWriteLock()).writeLock();
        }

        public ReadWriteLock remove(String path) {
            return this.locks.remove(path);
        }

    }

}
