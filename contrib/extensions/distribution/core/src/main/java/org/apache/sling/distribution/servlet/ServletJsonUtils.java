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

package org.apache.sling.distribution.servlet;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.distribution.DistributionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for writing json data to http responses.
 */
class ServletJsonUtils {

    private final static Logger log = LoggerFactory.getLogger(ServletJsonUtils.class);

    public static void writeJson(SlingHttpServletResponse response, DistributionResponse distributionResponse) throws IOException {
        JsonObjectBuilder json = Json.createObjectBuilder();
        try {
            json.add("success", distributionResponse.isSuccessful());
            json.add("state", distributionResponse.getState().name());
            json.add("message", distributionResponse.getMessage());

        } catch (JsonException e) {
            log.error("Cannot write json", e);
        }

        switch (distributionResponse.getState()) {
            case DISTRIBUTED:
                response.setStatus(200);
                break;
            case DROPPED:
                response.setStatus(400);
                break;
            case ACCEPTED:
                response.setStatus(202);
                break;
            default:
                // TODO
                break;
        }
        append(json.build(), response.getWriter());
    }

    public static void writeJson(SlingHttpServletResponse response, int status, String message,
                                 @Nullable Map<String, String> kv) throws IOException {
        JsonObjectBuilder json = Json.createObjectBuilder();
        try {
            json.add("message", message);
            if (kv != null && kv.size() > 0) {
                for (Map.Entry<String, String> entry : kv.entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }
            }
        } catch (JsonException e) {
            log.error("Cannot write json", e);
        }
        response.setStatus(status);

        append(json.build(), response.getWriter());
    }
    
    private static void append(JsonObject json, Writer writer) throws IOException {
        StringWriter buffer = new StringWriter();
        Json.createWriter(buffer).writeObject(json);
        writer.append(buffer.toString());
    }
}
