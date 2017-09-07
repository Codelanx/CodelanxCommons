package com.codelanx.commons.data.types;

import com.codelanx.commons.data.FileDataType;
import com.codelanx.commons.util.Reflections;
import org.json.simple.JSONObject;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Rogue on 1/16/16.
 */
public class Yaml extends FileDataType {

    private static final DumperOptions OUTPUT = new DumperOptions() {{
        this.setDefaultFlowStyle(FlowStyle.BLOCK);
    }};
    private static final ThreadLocal<org.yaml.snakeyaml.Yaml> CRYPTEX = new ThreadLocal<org.yaml.snakeyaml.Yaml>() {
        @Override
        protected org.yaml.snakeyaml.Yaml initialValue() {
            return new org.yaml.snakeyaml.Yaml(OUTPUT);
        }
    };

    public Yaml(File source) {
        super(source);
    }

    public Yaml() {
        super((File) null);
    }

    public Yaml(String data) {
        super(data);
    }

    @Override
    public Object parse(String in) {
        return CRYPTEX.get().load(in);
    }

    @Override
    public Object parse(Reader reader) throws IOException {
        return CRYPTEX.get().load(reader);
    }

    @Override
    public Object serializeMap(Map<String, Object> toFileFormat) {
        JSONObject obj = new JSONObject();
        toFileFormat.forEach((k, v) -> {
            obj.put(k, this.parseSerializable(v));
        });
        return obj;
    }

    @Override
    public Object serializeArray(Object array) {
        if (array.getClass().getComponentType().isPrimitive()) {
            return array;
        } else {
            Object[] objs = (Object[]) array;
            return Arrays.stream(objs).map(this::parseSerializable).toArray();
        }
    }

    @Override
    public Object deserializeArray(Object array) {
        if (array.getClass().getComponentType().isPrimitive()) {
            return array;
        } else {
            Object[] back = (Object[]) array;
            return Arrays.stream(back).map(this::parseDeserializable).toArray();
        }
    }

    @Override
    protected Map<String, Object> newMapping() {
        return new LinkedHashMap<>();
    }

    @Override
    protected Collection<Object> newSeries() {
        return new ArrayList<>();
    }

    @Override
    protected String toString(Object section) {
        return CRYPTEX.get().dump(this.replaceUnserializables(section));
    }

    private Object replaceUnserializables(Object in) {
        if (in instanceof Map) {
            Map<String, Object> base = (Map<String, Object>) in;
            Map<String, Object> back = new HashMap<>();
            back.putAll(base);
            back.replaceAll((k, v) -> {
                return this.unserializeMapping(v);
            });
            return back;
        } else if (in instanceof Collection) {
            Collection<Object> base = (Collection<Object>) in;
            List<Object> back = new ArrayList<>();
            back.addAll(base);
            back.replaceAll(this::unserializeMapping);
            return back;
        } else {
            throw new IllegalArgumentException("Cannot have a literal as the root value");
        }
    }

    private Object unserializeMapping(Object v) {
        if (v instanceof Map
                || v.getClass().isArray()
                || v instanceof List
                || v == null) {
            return v;
        } else {
            return Reflections.objectString(v);
        }
    }
}
