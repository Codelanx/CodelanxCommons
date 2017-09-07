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
package com.codelanx.commons.data.types;

import com.codelanx.commons.data.FileDataType;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Represents a JSON file that has been parsed and loaded into memory.
 *
 * @since 0.1.0
 * @author 1Rogue
 * @version 0.1.0
 */
public class Json extends FileDataType {

    private static final ThreadLocal<JSONParser> JSON_PARSER = ThreadLocal.withInitial(JSONParser::new);
    private static final ContainerFactory ORDERED = new ContainerFactory() {
        @Override
        public Map createObjectContainer() {
            return new LinkedHashMap<>();
        }

        @Override
        public List creatArrayContainer() {
            return new LinkedList<>();
        }
    };

    /**
     * Reads and loads a JSON file into memory
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @param location The location of the file to parse
     * @throws ParseException If the file is not in standard JSON format
     */
    public Json(File location) throws ParseException {
        super(location);
    }

    public Json() {
        super((File) null);
    }

    public Json(String data) {
        super(data);
    }

    @Override
    public Object parse(Reader reader) throws IOException {
        try {
            return JSON_PARSER.get().parse(reader, ORDERED);
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Object parse(String in) {
        try {
            return JSON_PARSER.get().parse(in);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse input: '" + (in.length() > 32 ? in.substring(0, 32) + "..." : in), e);
        }
    }

    @Override
    public Map<String, Object> serializeMap(Map<String, Object> toFileFormat) {
        Map<String, Object> obj = this.newMapping();
        toFileFormat.forEach((k, v) -> {
            obj.put(k, this.parseSerializable(v));
        });
        return obj;
    }

    @Override
    public Object serializeArray(Object array) {
        Class<?> type = array.getClass().getComponentType();
        JSONArray back = new JSONArray();
        if (type.isPrimitive()) {
            //we have to do some fuckeronies
            int length = Array.getLength(array);
            for (int i = 0; i < length; i++) {
                back.add(Array.get(array, i));
            }
        } else {
            Object[] objs = (Object[]) array;
            Arrays.stream(objs).map(this::parseSerializable).forEach(back::add);
        }
        return back;
    }

    @Override
    public Object deserializeArray(Object array) {
        if (array instanceof List) {
            ((List<Object>) array).replaceAll(this::parseDeserializable);
        }
        return array;
    }

    @Override
    protected Map<String, Object> newMapping() {
        return ORDERED.createObjectContainer();
    }

    @Override
    protected Collection<Object> newSeries() {
        return ORDERED.creatArrayContainer();
    }

    public static String format(String json) {
        int level = 0;
        StringBuilder sb = new StringBuilder();
        char[] itr = json.toCharArray();
        boolean text = false;
        boolean newlined = false;
        for (int i = 0; i < itr.length; i++) {
            switch(itr[i]) { //precheck
                case '}':
                case ']':
                    if (!text) {
                        level--;
                        sb.append(FileDataType.NEWLINE);
                        for (int w = 0; w < level; w++) {
                            sb.append("    "); //4 spaces
                        }
                        newlined = true;
                    }
                    break;
                case '"':
                    text = !text;
                    break;
            }
            switch(itr[i]) { //newline sanity
                case '\n':
                case ' ':
                    if (newlined) {
                        continue;
                    }
                    break;
                default:
                    if (newlined) {
                        newlined = false;
                    }
            }
            sb.append(itr[i]);
            if (!text) { //postcheck
                switch(itr[i]) {
                    case ':':
                        sb.append(' ');
                        break;
                    case ',':
                        sb.append(FileDataType.NEWLINE);
                        for (int w = 0; w < level; w++) {
                            sb.append("    "); //4 spaces
                        }
                        newlined = true;
                        break;
                    case '{':
                    case '[':
                        level++;
                        sb.append(FileDataType.NEWLINE);
                        for (int w = 0; w < level; w++) {
                            sb.append("    "); //4 spaces
                        }
                        newlined = true;
                        break;
                }
            }
        }
        return sb.toString();
    }

    @Override
    protected String toString(Object section) {
        if (section instanceof JSONObject) {
            return Json.format(((JSONObject) section).toJSONString());
        } else {
            return Json.format(JSONValue.toJSONString(section)); //hope it doesn't break shit
        }
    }
}
