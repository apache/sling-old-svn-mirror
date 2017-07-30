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
package org.apache.sling.servlets.post;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>JSONResponse</code> is an {@link AbstractPostResponse} preparing
 * the response in JSON.
 */
public class JSONResponse extends AbstractPostResponse {

    public static final String RESPONSE_CONTENT_TYPE = "application/json";

    // package private because it is used by the unit test
    static final String PROP_TYPE = "type";

    // package private because it is used by the unit test
    static final String PROP_ARGUMENT = "argument";

    // package private because it is used by the unit test
    static final String RESPONSE_CHARSET = "UTF-8";

    private static final String PROP_CHANGES = "changes";

    private Map<String, Object> json = new HashMap<>();

    private List<Map<String, Object>> changes = new ArrayList<>();

    private Throwable error;

    public void onChange(String type, String... arguments) {
        Map<String,Object> change = new HashMap<>();
        change.put(PROP_TYPE, type);
        
        if (arguments.length > 1) {
            change.put(PROP_ARGUMENT, Arrays.asList(arguments));
        }
        else if (arguments.length == 1) {
            change.put(PROP_ARGUMENT, arguments[0]);
        }
        changes.add(change);
    }

    @Override
    public void setError(Throwable error) {
        this.error = error;
    }

    @Override
    public Throwable getError() {
        return this.error;
    }

    @Override
    public void setProperty(String name, Object value) {
        json.put(name, value);
    }

    @Override
    public Object getProperty(String name) {
        return PROP_CHANGES.equals(name) ? getJson().getJsonArray(PROP_CHANGES) : 
            "error".equals(name) && this.error != null ? getJson().get("error") : json.get(name);
    }

    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
    @Override
    protected void doSend(HttpServletResponse response) throws IOException {

        response.setContentType(RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(RESPONSE_CHARSET);

        Json.createGenerator(response.getWriter()).write(getJson()).close();
    }

    JsonObject getJson() {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            if (entry.getValue() != null) {
                jsonBuilder.add(entry.getKey(), entry.getValue().toString());
            }
            else {
                jsonBuilder.addNull(entry.getKey());
            }
        }
        if (this.error != null) {
            jsonBuilder
                .add("error", Json.createObjectBuilder()
                    .add("class", error.getClass().getName())
                    .add("message", error.getMessage()));
        }
        JsonArrayBuilder changesBuilder = Json.createArrayBuilder();
        if (this.error == null) {
            for (Map<String, Object> entry : changes) {
                JsonObjectBuilder entryBuilder = Json.createObjectBuilder();
                entryBuilder.add(PROP_TYPE, (String) entry.get(PROP_TYPE));

                Object arguments = entry.get(PROP_ARGUMENT);

                if (arguments != null) {
                    if (arguments instanceof List) {
                        JsonArrayBuilder argumentsBuilder = Json.createArrayBuilder();

                        for (String argument : ((List<String>) arguments)) {
                            argumentsBuilder.add(argument);
                        }

                        entryBuilder.add(PROP_ARGUMENT, argumentsBuilder);
                    } else {
                        entryBuilder.add(PROP_ARGUMENT, (String) arguments);
                    }
                }
                changesBuilder.add(entryBuilder);
            }
        }
        jsonBuilder.add(PROP_CHANGES, changesBuilder);
        return jsonBuilder.build();
    }
    
    public class JSONResponseException extends RuntimeException {
        public JSONResponseException(String message, Throwable exception) {
           super(message, exception);
        }
        public JSONResponseException(Throwable e) {
           super("Error building JSON response", e);
        }
    }
}
