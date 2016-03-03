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
package com.codelanx.commons.data;

import java.util.LinkedHashMap;
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
