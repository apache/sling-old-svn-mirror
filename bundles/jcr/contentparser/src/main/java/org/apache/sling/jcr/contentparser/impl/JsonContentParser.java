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
package org.apache.sling.jcr.contentparser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
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

import org.apache.sling.jcr.contentparser.Content;
import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;

/**
 * Parses JSON files that contains content fragments.
 * Instance of this class is thread-safe.
 */
public final class JsonContentParser implements ContentParser {
    
    private final ParserHelper helper;    
    private final JsonReaderFactory jsonReaderFactory;
    
    public JsonContentParser(ParserOptions options) {
        this.helper = new ParserHelper(options);
        // allow comments in JSON files
        Map<String,Object> jsonReaderFactoryConfig = new HashMap<>();
        jsonReaderFactoryConfig.put("org.apache.johnzon.supports-comments", true);
        jsonReaderFactory = Json.createReaderFactory(jsonReaderFactoryConfig);
    }
    
    @Override
    public Content parse(InputStream is) throws IOException, ParseException {
        try (JsonReader reader = jsonReaderFactory.createReader(is)) {
            return toContent(null, reader.readObject());
        }
        catch (JsonParsingException ex) {
            throw new ParseException("Error parsing JSON content.", ex);
        }
    }
    
    private Content toContent(String name, JsonObject object) {
        Content content = new ContentImpl(name);
        Map<String,Object> properties = content.getProperties();
        Map<String,Content> children = content.getChildren();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            String childName = entry.getKey();
            Object value = convertValue(childName, entry.getValue());
            boolean isResource = (value instanceof Content);
            boolean ignore = false;
            if (isResource) {
                ignore = helper.ignoreResource(childName);
            }
            else {
                childName = helper.cleanupPropertyName(childName);
                ignore = helper.ignoreProperty(childName);
            }
            if (!ignore) {
                if (isResource) {
                    children.put(childName, (Content)value);
                }
                else {
                    properties.put(childName, value);
                }
            }
        }
        helper.ensureDefaultPrimaryType(content);
        return content;
    }
    
    private Object convertValue(String name, JsonValue value) {
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
                    return numberValue.bigDecimalValue();
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
                    values[i] = convertValue(name, arrayValue.get(i));
                }
                return helper.convertSingleTypeArray(values);
            case OBJECT:
                return toContent(name, (JsonObject)value);
            default:
                throw new ParseException("Unexpected JSON value type: " + value.getValueType());
        }
    }
    
}
