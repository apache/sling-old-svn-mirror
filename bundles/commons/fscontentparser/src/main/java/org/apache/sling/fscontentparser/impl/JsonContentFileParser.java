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
package org.apache.sling.fscontentparser.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
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

import org.apache.sling.fscontentparser.ContentFileParser;
import org.apache.sling.fscontentparser.ParseException;
import org.apache.sling.fscontentparser.ParserOptions;

/**
 * Parses JSON files that contains content fragments.
 * Instance of this class is thread-safe.
 */
public final class JsonContentFileParser implements ContentFileParser {
    
    private final ParserHelper helper;    
    private final JsonReaderFactory jsonReaderFactory;
    
    public JsonContentFileParser(ParserOptions options) {
        this.helper = new ParserHelper(options);
        // allow comments in JSON files
        Map<String,Object> jsonReaderFactoryConfig = new HashMap<>();
        jsonReaderFactoryConfig.put("org.apache.johnzon.supports-comments", true);
        jsonReaderFactory = Json.createReaderFactory(jsonReaderFactoryConfig);
    }
    
    @Override
    public Map<String,Object> parse(File file) throws IOException, ParseException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return parse(fis);
        }
    }
    
    @Override
    public Map<String,Object> parse(InputStream is) throws IOException, ParseException {
        try (JsonReader reader = jsonReaderFactory.createReader(is)) {
            return toMap(reader.readObject());
        }
        catch (JsonParsingException ex) {
            throw new ParseException("Error parsing JSON content.", ex);
        }
    }
    
    private Map<String,Object> toMap(JsonObject object) {
        Map<String,Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            String childName = entry.getKey();
            Object value = convertValue(entry.getValue());
            boolean ignore = false;
            if (value instanceof Map) {
                ignore = helper.ignoreResource(childName);
            }
            else {
                childName = helper.cleanupPropertyName(childName);
                ignore = helper.ignoreProperty(childName);
            }
            if (!ignore) {
                map.put(childName, value);
            }
        }
        helper.ensureDefaultPrimaryType(map);
        return map;
    }
    
    private Object convertValue(JsonValue value) {
        switch (value.getValueType()) {
            case STRING:
                String stringValue = ((JsonString)value).getString();
                Calendar calendarValue = helper.tryParseCalendar(stringValue);
                if (calendarValue != null) {
                    return calendarValue;
                }
                else {
                    return stringValue;
                }
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
                return helper.convertSingleTypeArray(values);
            case OBJECT:
                return toMap((JsonObject)value);
            default:
                throw new ParseException("Unexpected JSON value type: " + value.getValueType());
        }
    }
    
}
