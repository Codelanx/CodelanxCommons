package com.codelanx.commons.data.types;

import com.codelanx.commons.data.FileDataType;
import com.codelanx.commons.util.Reflections;
import org.json.simple.JSONObject;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
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
        super(null);
    }

    @Override
    protected Map<String, Object> readRaw(File target) throws IOException {
        return (Map<String, Object>) CRYPTEX.get().load(Files.newBufferedReader(target.toPath()));
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
    protected Map<String, Object> newSection() {
        return new HashMap<>();
    }

    @Override
    protected String toString(Map<String, Object> section) {
        return CRYPTEX.get().dump(this.replaceUnserializables(section));
    }

    @Override
    public String toString() {
        return this.toString(this.getRoot());
    }

    private Map<String, Object> replaceUnserializables(Map<String, Object> in) {
        Map<String, Object> back = new HashMap<>();
        back.putAll(in);
        back.replaceAll((k, v) -> {
            if (v instanceof Map
                    || v.getClass().isArray()
                    || v instanceof List
                    || v == null) {
                return v;
            } else {
                return Reflections.objectString(v);
            }
        });
        return back;
    }
}
