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
import com.codelanx.commons.util.Reflections;
import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.util.exception.Exceptions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Represents a file containing mappings that is owned by a plugin, and can
 * be automatically initialized
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public interface InfoFile {

    /**
     * The {@link FileDataType} path to store this value in
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @return The path to the file value
     */
    public String getPath();

    /**
     * Returns the default value of the key
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @return The key's default value
     */
    public Object getDefault();

    /**
     * Returns the relevant {@link FileDataType} for this file.
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The internal {@link FileDataType} of this {@link ConfigFile}
     */
    default public FileDataType getConfig() {
        //3 from PluginConfig#fileLoc
        return this.getData().get(this);
    }

    /**
     * Gets the current object in memory
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The Object found at the relevant location
     */
    default public Object get() {
        return this.getConfig().get(this.getPath(), this.getDefault());
    }

    /**
     * Returns the relevant {@link DataHolder} for this file, which provides
     * thread-safety for the {@link FileDataType} object initialization
     * 
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @return The {@link DataHolder} for this {@link InfoFile}
     */
    public DataHolder<? extends FileDataType> getData();

    default public File getFileLocation() {
        Class<? extends InfoFile> clazz = this.getClass();
        Exceptions.illegalState(Reflections.hasAnnotation(clazz, RelativePath.class),
                "'" + clazz.getName() + "' is missing the RelativePath annotation");
        return new File(clazz.getAnnotation(RelativePath.class).value());
    }

    /**
     * Loads the {@link InfoFile} values from the configuration file.
     * Safe to use for reloading
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param <T> The type of {@link FileDataType} to return
     * @param clazz The {@link Class} of the returned {@link FileDataType}
     * @return The relevant {@link FileDataType} for all the file info
     */
    default public <T extends FileDataType> T init(Class<T> clazz) {
        Class<? extends InfoFile> me = this.getClass();
        //Get fields
        Iterable<? extends InfoFile> itr;
        if (me.isEnum()) {
            itr = Arrays.asList(me.getEnumConstants());
        } else if (Iterable.class.isAssignableFrom(me)) {
            itr = ((Iterable<? extends InfoFile>) this);
        } else {
            throw new IllegalStateException("'" + me.getName() + "' is neither an enum nor an Iterable");
        }
        //Initialize file
        String path = null;
        try {
            File ref = this.getFileLocation();
            path = ref.getPath();
            if (!ref.exists()) {
                ref.createNewFile();
            }
            FileDataType use = FileDataType.newInstance(clazz, ref);
            for (InfoFile l : itr) {
                if (!use.isSet(l.getPath())) {
                    use.set(l.getPath(), l.getDefault());
                }
            }
            use.save();
            return (T) use;
        } catch (IOException ex) {
            Debugger.error(ex, "Error creating plugin file '%s'", path);
            return null;
        }
    }

    /**
     * Saves the current file data from memory
     *
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @throws IOException Failed to save to the file
     */
    default public void save() throws IOException {
        this.save(this.getFileLocation());
    }

    /**
     * Saves the current file data from memory to a specific {@link File}
     *
     * @since 0.1.0
     * @version 0.1.0
     * 
     * @param file The file to save to
     * @throws IOException Failed to save to the file
     */
    default public void save(File file) throws IOException {
        this.getConfig().save();
    }

}
