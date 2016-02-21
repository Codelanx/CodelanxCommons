package com.codelanx.commons.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Rogue on 1/16/16.
 */
public interface FileSerializable {

    public static final String IDENTIFIER_KEY = "==";

    public Map<String, Object> serialize();

    default public Map<String, Object> getData() {
        Map<String, Object> ser = this.serialize();
        Map<String, Object> back = new LinkedHashMap<>();
        back.put(IDENTIFIER_KEY, this.getKey());
        back.putAll(ser);
        return back;
    }

    default public String getKey() {
        return this.getClass().getName();
    }

}
