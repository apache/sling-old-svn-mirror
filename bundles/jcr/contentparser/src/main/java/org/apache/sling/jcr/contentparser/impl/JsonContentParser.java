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
import java.io.StringReader;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.sling.jcr.contentparser.ContentHandler;
import org.apache.sling.jcr.contentparser.ContentParser;
import org.apache.sling.jcr.contentparser.JsonParserFeature;
import org.apache.sling.jcr.contentparser.ParseException;
import org.apache.sling.jcr.contentparser.ParserOptions;

/**
 * Parses JSON files that contains content fragments.
 * Instance of this class is thread-safe.
 */
public final class JsonContentParser implements ContentParser {
    
    private final ParserHelper helper;
    /*
     * Implementation note: This parser uses JsonReader instead of the (more memory-efficient) 
     * JsonParser Stream API because otherwise it would not be possible to report parent resources
     * including all properties properly before their children.
     */
    private final JsonReaderFactory jsonReaderFactory;
    
    private final boolean jsonQuoteTickets;
    
    public JsonContentParser(ParserOptions options) {
        this.helper = new ParserHelper(options);
        
        Map<String,Object> jsonReaderFactoryConfig = new HashMap<>();
        
        // allow comments in JSON files?
        if (options.getJsonParserFeatures().contains(JsonParserFeature.COMMENTS)) {
            jsonReaderFactoryConfig.put("org.apache.johnzon.supports-comments", true);
        }
        jsonQuoteTickets = options.getJsonParserFeatures().contains(JsonParserFeature.QUOTE_TICK);
        
        jsonReaderFactory = Json.createReaderFactory(jsonReaderFactoryConfig);
    }
    
    @Override
    public void parse(ContentHandler handler, InputStream is) throws IOException, ParseException {
        parse(handler, toJsonObject(is), "/");
    }
    
    private JsonObject toJsonObject(InputStream is) {
        if (jsonQuoteTickets) {
            return toJsonObjectWithJsonTicks(is);
        }
        try (JsonReader reader = jsonReaderFactory.createReader(is)) {
            return reader.readObject();
        }
        catch (JsonParsingException ex) {
            throw new ParseException("Error parsing JSON content: " + ex.getMessage(), ex);
        }
    }

    private JsonObject toJsonObjectWithJsonTicks(InputStream is) {
        String jsonString;
        try {
            jsonString = IOUtils.toString(is, CharEncoding.UTF_8);
        }
        catch (IOException ex) {
            throw new ParseException("Error getting JSON string.", ex);
        }
        
        // convert ticks to double quotes
        jsonString = JsonTicksConverter.tickToDoubleQuote(jsonString);
        
        try (JsonReader reader = jsonReaderFactory.createReader(new StringReader(jsonString))) {
            return reader.readObject();
        }
        catch (JsonParsingException ex) {
            throw new ParseException("Error parsing JSON content: " + ex.getMessage(), ex);
        }
    }

    private void parse(ContentHandler handler, JsonObject object, String path) {
        // parse JSON object
        Map<String,Object> properties = new HashMap<>();
        Map<String,JsonObject> children = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            String childName = entry.getKey();
            Object value = null;
            boolean ignore = false;
            try {
                value = convertValue(entry.getValue());
            }
            catch (ParseException ex) {
                if (helper.ignoreResource(childName) || helper.ignoreProperty(helper.cleanupPropertyName(childName))) {
                    ignore = true;
                }
                else {
                    throw ex;
                }
            }
            boolean isResource = (value instanceof JsonObject);
            if (!ignore) {
                if (isResource) {
                    ignore = helper.ignoreResource(childName);
                }
                else {
                    childName = helper.cleanupPropertyName(childName);
                    ignore = helper.ignoreProperty(childName);
                }
            }
            if (!ignore) {
                if (isResource) {
                    children.put(childName, (JsonObject)value);
                }
                else {
                    properties.put(childName, value);
                }
            }
        }
        helper.ensureDefaultPrimaryType(properties);
        
        // report current JSON object
        handler.resource(path, properties);
        
        // parse and report children
        for (Map.Entry<String,JsonObject> entry : children.entrySet()) {
            String childPath = helper.concatenatePath(path, entry.getKey());;
            parse(handler, entry.getValue(), childPath);
        }
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
                    values[i] = convertValue(arrayValue.get(i));
                }
                return helper.convertSingleTypeArray(values);
            case OBJECT:
                return (JsonObject)value;
            default:
                throw new ParseException("Unexpected JSON value type: " + value.getValueType());
        }
    }
    
}
