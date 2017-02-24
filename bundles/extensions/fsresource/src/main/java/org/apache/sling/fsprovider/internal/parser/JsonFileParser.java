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
package org.apache.sling.fsprovider.internal.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses JSON files that contains content fragments.
 */
class JsonFileParser {
    
    private static final Logger log = LoggerFactory.getLogger(JsonFileParser.class);
    
    private static final JsonReaderFactory JSON_READER_FACTORY;
    static {
        // allow comments in JSON files
        Map<String,Object> jsonReaderFactoryConfig = new HashMap<>();
        jsonReaderFactoryConfig.put("org.apache.johnzon.supports-comments", true);
        // workaround for JsonProvider classloader issue until https://issues.apache.org/jira/browse/GERONIMO-6560 is fixed
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JsonFileParser.class.getClassLoader());
            JSON_READER_FACTORY = Json.createReaderFactory(jsonReaderFactoryConfig);
        }
        finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
    
    private JsonFileParser() {
        // static methods only
    }
    
    /**
     * Parse JSON file.
     * @param file File
     * @return Content
     */
    public static Map<String,Object> parse(File file) {
        log.debug("Parse JSON content from {}", file.getPath());
        try (FileInputStream fis = new FileInputStream(file);
                JsonReader reader = JSON_READER_FACTORY.createReader(fis)) {
            return toMap(reader.readObject());
        }
        catch (IOException | JsonParsingException ex) {
            log.warn("Error parsing JSON content from " + file.getPath(), ex);
            return null;
        }
    }
    
    private static Map<String,Object> toMap(JsonObject object) {
        Map<String,Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            map.put(entry.getKey(), convertValue(entry.getValue()));
        }
        return map;
    }
    
    private static Object convertValue(JsonValue value) {
        switch (value.getValueType()) {
            case STRING:
                return ((JsonString)value).getString();
            case NUMBER:
                JsonNumber numberValue = (JsonNumber)value;
                if (numberValue.isIntegral()) {
                    return numberValue.longValue();
                }
                else {
                    return numberValue.doubleValue();
                }
            case TRUE:
                return true;
            case FALSE:
                return false;
            case NULL:
                return null;
            case ARRAY:
                JsonArray arrayValue = (JsonArray)value;
                Object[] values = new Object[arrayValue.size()];
                for (int i=0; i<values.length; i++) {
                    values[i] = convertValue(arrayValue.get(i));
                }
                return values;
            case OBJECT:
                return toMap((JsonObject)value);
            default:
                throw new IllegalArgumentException("Unexpected JSON value type: " + value.getValueType());
        }
    }
    
}
