package com.codelanx.commons.data.types;

import com.codelanx.commons.data.FileDataType;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
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
        super((File) null);
    }

    public XML(String data) {
        super(data);
    }

    @Override
    public Object parse(Reader reader) throws IOException {
        return null;
    }

    @Override
    public Object parse(String in) {
        return null;
    }

    @Override
    protected Object serializeMap(Map<String, Object> toFileFormat) {
        return null;
    }

    @Override
    protected Object serializeArray(Object array) {
        return null;
    }

    @Override
    protected Object deserializeArray(Object array) {
        return null;
    }

    @Override
    protected Map<String, Object> newMapping() {
        return null;
    }

    @Override
    protected Collection<Object> newSeries() {
        return null;
    }

    @Override
    protected String toString(Object section) {
        return null;
    }
}