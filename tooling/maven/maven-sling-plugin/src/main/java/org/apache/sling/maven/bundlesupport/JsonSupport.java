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
package org.apache.sling.maven.bundlesupport;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

public final class JsonSupport {

    /** Mime type for json response. */
    public static final String JSON_MIME_TYPE = "application/json";
    
    private static final JsonReaderFactory JSON_READER_FACTORY;
    static {
        // allow comments in JSON files
        Map<String,Object> jsonFactoryConfig = new HashMap<>();
        jsonFactoryConfig.put("org.apache.johnzon.supports-comments", true);
        JSON_READER_FACTORY = Json.createReaderFactory(jsonFactoryConfig);
    }
    
    private JsonSupport() {
        // static methods only
    }
    
    /**
     * Parse String to JSON object.
     * @param jsonString JSON string
     * @return JSON object
     */
    public static JsonObject parseObject(String jsonString) {
        try (StringReader reader = new StringReader(jsonString);
                JsonReader jsonReader = JSON_READER_FACTORY.createReader(reader)) {
            return jsonReader.readObject();
        }
    }
    
    /**
     * Parse String to JSON array.
     * @param jsonString JSON string
     * @return JSON array
     */
    public static JsonArray parseArray(String jsonString) {
        try (StringReader reader = new StringReader(jsonString);
                JsonReader jsonReader = JSON_READER_FACTORY.createReader(reader)) {
            return jsonReader.readArray();
        }
    }
    
    /**
     * Validate JSON structure
     * @param jsonString JSON string
     * @throws javax.json.JsonException when JSON structure is invalid
     */
    public static void validateJsonStructure(String jsonString) {
        try (StringReader reader = new StringReader(jsonString);
                JsonReader jsonReader = JSON_READER_FACTORY.createReader(reader)) {
            jsonReader.read();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void accumulate(Map<String,Object> obj, String name, String value) {
        Object current = obj.get(name);
        if (current == null) {
            obj.put(name, value);
        }
        else if (current instanceof List) {
            List<String> array = new ArrayList<>((List)current);
            array.add(value);
            obj.put(name, array);
        }
        else {
            List<String> array = new ArrayList<>();
            array.add((String)current);
            array.add(value);
            obj.put(name, array);
        }
    }

    @SuppressWarnings("unchecked")
    public static JsonObject toJson(Map<String,Object> map) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String,Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                builder.add(entry.getKey(), toJson((Map<String,Object>)entry.getValue()));
            }
            else if (entry.getValue() instanceof List) {
                JsonArrayBuilder array = Json.createArrayBuilder();
                for (String value : (List<String>)entry.getValue()) {
                    array.add(value);
                }
                builder.add(entry.getKey(), array.build());
            }
            else {
                builder.add(entry.getKey(), (String)entry.getValue());
            }
        }
        return builder.build();
    }

}
