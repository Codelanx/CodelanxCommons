package com.codelanx.commons.lost.config;

import com.codelanx.commons.config.RelativePath;
import com.codelanx.commons.util.Reflections;

import java.io.File;
import java.util.Map;

/**
 * Created by Rogue on 11/6/2015.
 */
public class ConfigUtil {

    /**
     * Returns a {@link Map} representative of the passed Object that represents
     * a section of a YAML file. This method neglects the implementation of the
     * section (whether it be {@link ConfigurationSection} or just a
     * {@link Map}), and returns the appropriate value.
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param o The object to interpret
     * @return A {@link Map} representing the section
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getConfigSectionValue(Object o) {
        if (o == null) {
            return null;
        }
        Map<String, Object> map;
        if (o instanceof ConfigurationSection) {
            map = ((ConfigurationSection) o).getValues(false);
        } else if (o instanceof Map) {
            map = (Map<String, Object>) o;
        } else {
            return null;
        }
        return map;
    }


    /**
     * Returns the save location for passed {@link ConfigFile} argument
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param clazz An implementing class with the {@link PluginClass} and
     *              {@link RelativePath} annotations
     * @return A {@link File} pointing to the location containing saved values
     *           for this configuration type
     */
    public static File getFileLocation(Class<? extends ConfigFile> clazz) {
        if (!(Reflections.hasAnnotation(clazz, PluginClass.class)
                && Reflections.hasAnnotation(clazz, RelativePath.class))) {
            throw new IllegalStateException("'" + clazz.getName() + "' is missing either PluginClass or RelativePath annotations");
        }
        File folder = Reflections.getPlugin(clazz).getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, clazz.getAnnotation(RelativePath.class).value());
    }
}