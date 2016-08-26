package com.codelanx.commons.config;

import com.codelanx.commons.data.FileDataType;

public interface MemoryConfig extends ConfigFile {

    @Override
    Object get();

    @Override
    default public String getPath() {
        return null;
    }

    //this can be overriden if desired
    @Override
    default public Object getDefault() {
        return null;
    }

    @Override
    default public ConfigFile set(Object val) {
        this.setValue(val);
        return null;
    }

    void setValue(Object val);

    @Override
    default public DataHolder<? extends FileDataType> getData() {
        return null;
    }

    @Override
    default public <T extends FileDataType> T init(Class<T> clazz) {
        return null;
    }
}
