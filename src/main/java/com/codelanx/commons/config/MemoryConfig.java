package com.codelanx.commons.config;

import com.codelanx.commons.data.FileDataType;

public interface MemoryConfig<T> extends ConfigFile {

    @Override
    default public Object get() {
        return this.getValue();
    }

    T getValue();

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
        this.setValue((T) val);
        return null;
    }

    void setValue(T val);

    @Override
    default public DataHolder<? extends FileDataType> getData() {
        return null;
    }

    @Override
    default public <T extends FileDataType> T init(Class<T> clazz) {
        return null;
    }
}
