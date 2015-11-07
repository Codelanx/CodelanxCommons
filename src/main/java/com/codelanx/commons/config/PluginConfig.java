package com.codelanx.commons.config;

import com.codelanx.commons.data.FileDataType;
import com.codelanx.commons.data.types.Yaml;

import java.io.File;
import java.util.ArrayList;

@RelativePath("clconfig.yml")
public enum PluginConfig implements Config {

    DISABLED_COMMANDS("disabled-commands", new ArrayList<>()),
    ;

    private static final DataHolder<Yaml> DATA = new DataHolder<>(Yaml.class);
    private final String key;
    private final Object def;

    private PluginConfig(String key, Object def) {
        this.key = key;
        this.def = def;
    }

    @Override
    public String getPath() {
        return this.key;
    }

    @Override
    public Object getDefault() {
        return this.def;
    }

    @Override
    public DataHolder<? extends FileDataType> getData() {
        return null;
    }

    @Override
    public File getFileLocation() {
        return null;
    }
}
