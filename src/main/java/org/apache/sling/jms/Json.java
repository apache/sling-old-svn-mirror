/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jms;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ieb on 31/03/2016.
 */
public class Json {

    public static Map<String, Object> toMap(String text) {
        JsonElement root = new JsonParser().parse(text);
        return toMapValue(root);
    }

    private static <T> T toMapValue(JsonElement element) {
        if (element.isJsonObject()) {
            return (T) toMapValue(element.getAsJsonObject());
        } else if (element.isJsonArray()) {
            return (T) toMapValue(element.getAsJsonArray());
        } else if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            return (T) toMapValue(element.getAsJsonPrimitive());
        }
        throw new IllegalArgumentException("Encountered JsonElement that is not an object, array, primitive or null: "+element);
    }
    private static <T> T toMapValue(JsonArray array) {
        List<Object> list = new ArrayList<Object>();
        for( JsonElement e : array) {
            list.add(toMapValue(e));
        }
        return (T) list;
    }

    private static <T> T toMapValue(JsonObject obj) {
        Map<String, Object> m = new HashMap<String, Object>();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            m.put(e.getKey(), toMapValue(e.getValue()));
        }
        return (T) m;
    }

    private static <T> T toMapValue(JsonPrimitive p) {
        if (p.isString()) {
            return (T) p.getAsString();
        } else if (p.isBoolean()) {
            return (T) ((Boolean)p.getAsBoolean());
        } else if (p.isNumber()) {
            double d = p.getAsDouble();
            if (Math.floor(d) == d) {
                return (T)((Long)p.getAsLong());
            }
            return (T)((Double)d);
        } else {
            return null;
        }
    }

    public static String toJson(Map<String, Object> message) {
        return new Gson().toJson(message);
    }


}
