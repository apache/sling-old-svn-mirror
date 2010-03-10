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

package org.apache.sling.servlets.post.impl.helper;

import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Represents a JSON response to be sent to the client. For backward compatibility,
 * this extends {@link org.apache.sling.api.servlets.HtmlResponse}.
 */
public class JSONResponse extends HtmlResponse {
    private JSONObject json = new JSONObject();
    private JSONArray changes = new JSONArray();
    private Boolean delayedIsCreateRequest;
    static final String PROP_CHANGES = "changes";
    static final String PROP_TYPE = "type";
    static final String PROP_ARGUMENT = "argument";
    public static final String RESPONSE_CONTENT_TYPE = "application/json";
    static final String RESPONSE_CHARSET = "UTF-8";
    private Throwable error;

    public JSONResponse() throws JSONResponseException {
        try {
            json = new JSONObject();
            changes = new JSONArray();
            json.put(PROP_CHANGES, changes);
            if (delayedIsCreateRequest != null) {
                this.setCreateRequest(this.delayedIsCreateRequest);
            }
        } catch (Throwable e) {
            throw new JSONResponseException(e);
        }
    }

    @Override
    public void onChange(String type, String... arguments)  throws JSONResponseException {
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
    public void setCreateRequest(boolean isCreateRequest) {
        if (json != null) {
            super.setCreateRequest(isCreateRequest);
        } else {
            // This is called by HtmlResponse constructor, before our json object is initiated.
            // Store this in a member variable, so we can set it from our own constructor.
            this.delayedIsCreateRequest = isCreateRequest;
        }
    }

    @Override
    public void setProperty(String name, Object value) {
        try {
            this.json.put(name, value);
        } catch (Throwable e) {
            throw new JSONResponseException("Error setting JSON property '" + name + "' to '" + value + "'", e);
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
            throw new JSONResponseException("Error getting JSON property '" + name + "'", e);
        }
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public void send(HttpServletResponse response, boolean setStatus) throws IOException {
        String path = getPath();
        if (getProperty(PN_STATUS_CODE) == null) {
            if (getError() != null) {
                setStatus(500, getError().toString());
                setTitle("Error while processing " + path);
            } else {
                if (isCreateRequest()) {
                    setStatus(201, "Created");
                    setTitle("Content created " + path);
                } else {
                    setStatus(200, "OK");
                    setTitle("Content modified " + path);
                }
            }
        }

        String referer = getReferer();
        if (referer == null) {
            referer = "";
        }
        setReferer(referer);
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
