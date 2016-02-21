package com.codelanx.commons.data.types;

import com.codelanx.commons.data.FileDataType;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Rogue on 1/18/16.
 */
public class XML extends FileDataType {

    public XML(File location) {
        super(location);
        throw new UnsupportedOperationException("XML Not complete yet"); //TODO
    }

    public XML() {
        this(null);
    }

    @Override
    public void save() throws IOException {

    }

    @Override
    public void save(File target) throws IOException {

    }

    @Override
    protected Map<String, Object> readRaw(File target) throws IOException {
        return null;
    }

    @Override
    public Object serializeMap(Map<String, Object> toFileFormat) {
        return null;
    }

    @Override
    public Object serializeArray(Object array) {
        return null;
    }

    @Override
    public Object deserializeArray(Object array) {
        return null;
    }

    @Override
    protected Map<String, Object> newSection() {
        return null;
    }

    @Override
    public Map<String, Object> getRoot() {
        return null;
    }

    @Override
    protected String toString(Map<String, Object> section) {
        return null;
    }
}
