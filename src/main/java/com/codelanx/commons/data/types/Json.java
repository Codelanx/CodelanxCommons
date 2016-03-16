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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
        super(null);
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.1.0
     * @version 0.1.0
     *
     * @throws IOException {@inheritDoc}
     */
    @Override
    public void save() throws IOException {
        this.save(this.location);
    }

    @Override
    protected JSONObject readRaw(File target) throws IOException {
        if (FileUtils.readFileToString(target).trim().isEmpty()) {
            return this.newSection();
        }
        try {
            return (JSONObject) JSON_PARSER.get().parse(new FileReader(target));
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public JSONObject serializeMap(Map<String, Object> toFileFormat) {
        JSONObject obj = this.newSection();
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
        if (array instanceof JSONArray) {
            JSONArray arr = (JSONArray) array;
            arr.replaceAll(this::parseDeserializable);
        }
        return array;
    }

    @Override
    protected JSONObject newSection() {
        return new JSONObject();
    }

    @Override
    public String toString() {
        return Json.format(this.getRoot().toJSONString());
    }

    @Override
    protected JSONObject getRoot() {
        return (JSONObject) super.getRoot();
    }

    public static String format(String json) {
        int level = 0;
        StringBuilder sb = new StringBuilder();
        char[] itr = json.toCharArray();
        for (int i = 0; i < itr.length; i++) {
            switch(itr[i]) {
                case '}':
                case ']':
                    level--;
                    sb.append(FileDataType.NEWLINE);
                    for (int w = 0; w < level; w++) {
                        sb.append("    "); //4 spaces
                    }
                    break;
            }
            sb.append(itr[i]);
            switch(itr[i]) {
                case ':':
                    sb.append(' ');
                    break;
                case ',':
                    sb.append(FileDataType.NEWLINE);
                    for (int w = 0; w < level; w++) {
                        sb.append("    "); //4 spaces
                    }
                    break;
                case '{':
                case '[':
                    level++;
                    sb.append(FileDataType.NEWLINE);
                    for (int w = 0; w < level; w++) {
                        sb.append("    "); //4 spaces
                    }
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    protected String toString(Map<String, Object> section) {
        if (section instanceof JSONObject) {
            return Json.format(((JSONObject) section).toJSONString());
        } else {
            return Json.format(section.toString()); //hope it doesn't break shit
        }
    }
}
