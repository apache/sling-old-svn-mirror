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
package org.apache.sling.caconfig.impl.override;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.caconfig.spi.metadata.PropertyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses override configuration strings like these:
 * <ul>
 * <li><code>{configName}/{propertyName}={propertyJsonValue}</code></li>
 * <li><code>{configName}={propertyJsonObject}</code></li>
 * <li><code>[{contextPath}]{configName}/{propertyName}={propertyJsonValue}</code></li>
 * <li><code>[{contextPath}]{configName}={propertyJsonObject}</code></li>
 * </ul>
 */
class OverrideStringParser {
    
    private static final Logger log = LoggerFactory.getLogger(OverrideStringParser.class);
    
    private static final Pattern OVERRIDE_PATTERN = Pattern.compile("^(\\[([^\\[\\]=]+)\\])?([^\\[\\]=]+)=(.*)$");
    
    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Collections.<String,Object>emptyMap());
    
    private OverrideStringParser() {
        // static method sonly
    }
    
    /**
     * Parses a list of override strings from a override provider.
     * @param overrideStrings Override strings
     * @return Override objects
     */
    public static Collection<OverrideItem> parse(Collection<String> overrideStrings) {
        List<OverrideItem> result = new ArrayList<>();
        
        for (String overrideString : overrideStrings) {
            
            // check if override generic pattern is matched
            Matcher matcher = OVERRIDE_PATTERN.matcher(StringUtils.defaultString(overrideString));
            if (!matcher.matches()) {
                log.warn("Ignore config override string - invalid syntax: {}", overrideString);
                continue;
            }
            
            // get single parts
            String path = StringUtils.trim(matcher.group(2));
            String configName = StringUtils.trim(matcher.group(3));
            String value = StringUtils.trim(StringUtils.defaultString(matcher.group(4)));
            
            OverrideItem item;
            try {
                // check if value is JSON = defines whole parameter map for a config name
                JsonObject json = toJson(value);
                if (json != null) {
                    item = new OverrideItem(path, configName, toMap(json), true);
                }
                else {
                    // otherwise it defines a key/value pair in a single line
                    String propertyName = StringUtils.substringAfterLast(configName, "/");
                    if (StringUtils.isEmpty(propertyName)) {
                        log.warn("Ignore config override string - missing property name: {}", overrideString);
                        continue;
                    }
                    configName = StringUtils.substringBeforeLast(configName, "/");
                    Map<String,Object> props = new HashMap<>();
                    props.put(propertyName, convertJsonValue(value));
                    item = new OverrideItem(path, configName, props, false);
                }
            }
            catch (JsonException ex) {
                log.warn("Ignore config override string - invalid JSON syntax ({}): {}", ex.getMessage(), overrideString);
                continue;
            }
            
            // validate item
            if (!isValid(item, overrideString)) {
                continue;
            }
            
            // if item does not contain a full property set try to merge with existing one
            if (!item.isAllProperties()) {
                boolean foundMatchingItem = false;
                for (OverrideItem existingItem : result) {
                    if (!existingItem.isAllProperties()
                            && StringUtils.equals(item.getPath(), existingItem.getPath())
                            && StringUtils.equals(item.getConfigName(), existingItem.getConfigName())) {
                        existingItem.getProperties().putAll(item.getProperties());
                        foundMatchingItem = true;
                        break;
                    }
                }
                if (foundMatchingItem) {
                    continue;
                }
            }
            
            // add item to result
            result.add(item);
        }
        
        return result;
    }
    
    /**
     * Try to convert value to JSON object
     * @param value Value string
     * @return JSON object or null if the string does not start with "{"
     * @throws JSONException when JSON parsing failed
     */
    private static JsonObject toJson(String value) {
        if (!StringUtils.startsWith(value, "{")) {
            return null;
        }
        try (Reader reader = new StringReader(value);
                JsonReader jsonReader = JSON_READER_FACTORY.createReader(reader)) {
            return jsonReader.readObject();
        }
        catch (IOException ex) {
            return null;
        }
    }
    
    /**
     * Convert JSON object to map.
     * @param json JSON object
     * @return Map (keys/values are not validated)
     */
    private static Map<String,Object> toMap(JsonObject json) {
        Map<String,Object> props = new HashMap<>();
        Iterator<String> keys = json.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            props.put(key, convertJsonValue(json.get(key)));
        }
        return props;
    }
    
    /**
     * Convert single JSON-conformant value object
     * @param jsonValue JSON value
     * @return Object
     * @throws JSONException If JSON-parsing of value failed
     */
    private static Object convertJsonValue(String jsonValue) {
        String jsonString = "{\"value\":" + jsonValue + "}";
        JsonObject json = toJson(jsonString);
        return convertJsonValue(json.get("value"));
    }
    
    private static Object convertJsonValue(JsonValue jsonValue) {
        switch (jsonValue.getValueType()) {
        case STRING:
            return ((JsonString)jsonValue).getString();
        case NUMBER:
            JsonNumber number = (JsonNumber)jsonValue;
            if (number.isIntegral()) {
                return number.longValue();
            }
            else {
                return number.doubleValue();
            }
        case TRUE:
            return true;
        case FALSE:
            return false;
        case NULL:
            return null;
        case ARRAY:
            return convertJsonArray((JsonArray)jsonValue);
        default:
            throw new RuntimeException("Unexpected JSON value type: " + jsonValue.getValueType() + ": " + jsonValue);
        }
    }
    
    private static Object convertJsonArray(JsonArray jsonArray) {
        if (jsonArray.size() > 0) {             
            Object firstValue = convertJsonValue(jsonArray.get(0));
            if (firstValue != null) {
                Class firstType = firstValue.getClass();
                Object convertedArray = Array.newInstance(firstType, jsonArray.size());
                for (int i=0; i<jsonArray.size(); i++) {
                    Array.set(convertedArray, i, convertJsonValue(jsonArray.get(i)));
                }
                return convertedArray;
            }
        }
        return new String[0];
    }
    
    /**
     * Validate override item and it's properties map.
     * @param item Override item
     * @param overrideString Override string
     * @return true if item is valid
     */
    private static boolean isValid(OverrideItem item, String overrideString) {
        if (item.getPath() != null && (!StringUtils.startsWith(item.getPath(), "/") || StringUtils.contains(item.getPath(), ".."))) {
            log.warn("Ignore config override string - invalid path: {}", overrideString);
            return false;
        }
        if (StringUtils.startsWith(item.getConfigName(), "/") || StringUtils.contains(item.getConfigName(), "..")) {
            log.warn("Ignore config override string - invalid configName: {}", overrideString);
            return false;
        }
        for (Map.Entry<String, Object> entry : item.getProperties().entrySet()) {
            String propertyName = entry.getKey();
            if (StringUtils.isEmpty(propertyName) || StringUtils.contains(propertyName, "/")) {
                log.warn("Ignore config override string - invalid property name ({}): {}", propertyName, overrideString);
                return false;
            }
            Object value = entry.getValue();
            if (value == null || !isSupportedType(value)) {
                log.warn("Ignore config override string - invalid property value ({} - {}): {}", value, value != null ? value.getClass().getName() : "", overrideString);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Validate if the given object is not null, and the type is supported for configuration values.
     * @param value Value
     * @return true if valid
     */
    private static boolean isSupportedType(Object value) {
        if (value == null) {
            return false;
        }
        Class clazz = value.getClass();
        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        for (Class type : PropertyMetadata.SUPPORTED_TYPES) {
            if (type.equals(clazz )) {
                return true;
            }
            if (type.isPrimitive() && ClassUtils.primitiveToWrapper(type).equals(clazz)) {
                return true;
            }
        }
        return false;
    }

}
