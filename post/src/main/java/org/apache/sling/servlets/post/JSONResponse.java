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

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

    private JSONObject json = new JSONObject();

    private JSONArray changes = new JSONArray();

    private Throwable error;

    public JSONResponse() throws JSONResponseException {
        try {
            json = new JSONObject();
            changes = new JSONArray();
            json.put(PROP_CHANGES, changes);
        } catch (Throwable e) {
            throw new JSONResponseException(e);
        }
    }

    public void onChange(String type, String... arguments)
            throws JSONResponseException {
        try {
            JSONObject change = new JSONObject();
            change.put(PROP_TYPE, type);
            for (String argument : arguments) {
                change.accumulate(PROP_ARGUMENT, argument);
            }
            changes.put(change);
        } catch (JSONException e) {
            throw new JSONResponseException(e);
        }
    }

    @Override
    public void setError(Throwable error) {
        try {
            this.error = error;
            JSONObject jsonError = new JSONObject();
            jsonError.put("class", error.getClass().getName());
            jsonError.put("message", error.getMessage());
            json.put("error", jsonError);
        } catch (JSONException e) {
            throw new JSONResponseException(e);
        }
    }

    @Override
    public Throwable getError() {
        return this.error;
    }

    @Override
    public void setProperty(String name, Object value) {
        try {
            this.json.put(name, value);
        } catch (Throwable e) {
            throw new JSONResponseException("Error setting JSON property '"
                + name + "' to '" + value + "'", e);
        }
    }

    @Override
    public Object getProperty(String name) throws JSONResponseException {
        try {
            if (json.has(name)) {
                return json.get(name);
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new JSONResponseException("Error getting JSON property '"
                + name + "'", e);
        }
    }

    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
    @Override
    protected void doSend(HttpServletResponse response) throws IOException {

        response.setContentType(RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(RESPONSE_CHARSET);

        try {
            json.write(response.getWriter());
        } catch (JSONException e) {
            IOException ioe = new IOException("Error creating JSON response");
            ioe.initCause(e);
            throw ioe;
        }
    }

    JSONObject getJson() {
        return json;
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
