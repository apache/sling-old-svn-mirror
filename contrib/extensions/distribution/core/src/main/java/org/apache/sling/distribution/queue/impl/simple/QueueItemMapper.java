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
package org.apache.sling.distribution.queue.impl.simple;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.apache.sling.distribution.queue.DistributionQueueItem;

/**
 * Serialize/Unserialize {@link DistributionQueueItem} items.
 */
public class QueueItemMapper {

    DistributionQueueItem readQueueItem(String line) {
        String[] split = line.split(" ", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid item found " + line);
        }
        String packageId = split[0];
        String infoString = split[1];

        Map<String, Object> info = new HashMap<String, Object>();

        JsonReader reader = Json.createReader(new StringReader(infoString));
        JsonObject jsonObject = reader.readObject();
        NumberFormat numberFormat = NumberFormat.getInstance();

        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            if (entry.getValue().getValueType().equals(JsonValue.ValueType.ARRAY)) {
                JsonArray value = jsonObject.getJsonArray(entry.getKey());
                String[] a = new String[value.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = value.getString(i);
                }
                info.put(entry.getKey(), a);
            } else if (JsonValue.NULL.equals(entry.getValue())) {
                info.put(entry.getKey(), null);
            } else if (entry.getValue().getValueType().equals(JsonValue.ValueType.NUMBER)) {
                try {
                    Number n = numberFormat.parse(entry.getValue().toString());
                    info.put(entry.getKey(), n);
                } catch (ParseException e) {
                    throw new IllegalStateException("Failed to read queue item", e);
                }
            } else {
                info.put(entry.getKey(), ((JsonString) entry.getValue()).getString());
            }
        }

        return new DistributionQueueItem(packageId, info);
    }

    String writeQueueItem(DistributionQueueItem item) {
        String packageId = item.getPackageId();
        StringWriter w = new StringWriter();
        JsonGenerator jsonWriter = Json.createGenerator(w);
        jsonWriter.writeStartObject();
        for (Map.Entry<String, Object> entry : item.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String[]) {
                jsonWriter.writeStartArray(entry.getKey());
                for (String s : ((String[]) value)) {
                    jsonWriter.write(s);
                }
                jsonWriter.writeEnd();
            } else if (value == null) {
                jsonWriter.write(key, JsonValue.NULL);
            } else if (value instanceof String) {
                jsonWriter.write(key, (String) value);
            } else if (value instanceof Boolean) {
                jsonWriter.write(key, (Boolean) value);
            } else if (value instanceof Integer) {
                jsonWriter.write(key, (Integer) value);
            } else if (value instanceof Float) {
                jsonWriter.write(key, (Float) value);
            }else if (value instanceof Double) {
                jsonWriter.write(key, (Double) value);
            } else if (value instanceof Long) {
                jsonWriter.write(key, (Long) value);
            } else {
                jsonWriter.write(key, String.valueOf(value));
            }
        }
        jsonWriter.writeEnd();
        jsonWriter.close();

        return packageId + " " + w.toString();
    }
}
