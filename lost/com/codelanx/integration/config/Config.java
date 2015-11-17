package com.codelanx.integration.config;

import com.codelanx.commons.config.ConfigFile;
import com.codelanx.commons.config.InfoFile;
import com.codelanx.commons.config.RelativePath;
import com.codelanx.commons.util.Reflections;

import java.io.File;

/**
 * Created by Rogue on 11/17/2015.
 */
public interface Config extends ConfigFile {

    @Override
    default public File getFileLocation() {
        Class<? extends InfoFile> clazz = this.getClass();
        if (!(Reflections.hasAnnotation(clazz, PluginClass.class)
                && Reflections.hasAnnotation(clazz, RelativePath.class))) {
            throw new IllegalStateException("'" + clazz.getName() + "' is missing either PluginClass or RelativePath annotations");
        }
        File folder = Configs.getPlugin(clazz).getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, clazz.getAnnotation(RelativePath.class).value());
    }

}
