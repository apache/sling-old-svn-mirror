/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.pipes.internal;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.sling.jcr.contentparser.impl.JsonTicksConverter;

public class JsonUtil {
    public static JsonStructure parse(String input) throws JsonException {
        return Json.createReader(new StringReader(JsonTicksConverter.tickToDoubleQuote(input))).read();
    }

    public static JsonObject parseObject(String input) throws JsonException{
        return (JsonObject) parse(input);
    }

    public static JsonArray parseArray(String input) throws JsonException {
        return (JsonArray) parse(input);
    }

    public static Object unbox(JsonValue value, Function<JsonStructure, Object> convert) throws JsonException {
        switch (value.getValueType()) {
            case ARRAY:
            case OBJECT:
                return convert.apply((JsonStructure) value);
            case FALSE:
                return Boolean.FALSE;
            case TRUE:
                return Boolean.TRUE;
            case NULL:
                return null;
            case NUMBER:
                JsonNumber number = (JsonNumber) value;
                return number.isIntegral() ? number.longValue() : number.doubleValue();
            case STRING:
                return ((JsonString) value).getString();
            default:
                throw new JsonException("Unknow value type");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unbox(JsonValue value) {
        return (T) unbox(value, json -> json.getValueType() == ValueType.ARRAY ? 
                ((JsonArray) json).stream()
                    .map(JsonUtil::unbox)
                    .collect(Collectors.toList())
                :
                ((JsonObject) json).entrySet().stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(),unbox(entry.getValue())))
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }

    public static String toString(JsonValue value) {
        StringWriter writer = new StringWriter();
        Json.createGenerator(writer).write(value).close();
        return writer.toString();
    }

    public static String toString(JsonArrayBuilder builder) {
        return toString(builder.build());
    }
    
    public static String toString(JsonObjectBuilder builder) {
        return toString(builder.build());
    }
}
