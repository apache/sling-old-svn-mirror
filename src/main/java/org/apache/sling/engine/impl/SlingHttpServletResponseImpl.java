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
package org.apache.sling.engine.impl;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.engine.impl.request.RequestData;

public class SlingHttpServletResponseImpl extends HttpServletResponseWrapper implements SlingHttpServletResponse {

    private final RequestData requestData;

    public SlingHttpServletResponseImpl(RequestData requestData,
            HttpServletResponse response) {
        super(response);
        this.requestData = requestData;
    }

    protected final RequestData getRequestData() {
        return requestData;
    }

    //---------- Adaptable interface

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return getRequestData().adaptTo(this, type);
    }

    // ---------- Redirection support through PathResolver --------------------

    @Override
    public String encodeURL(String url) {
        // make the path absolute
        url = makeAbsolutePath(url);

        // resolve the url to as if it would be a resource path
        url = map(url);

        // have the servlet container to further encodings
        return super.encodeURL(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        // make the path absolute
        url = makeAbsolutePath(url);

        // resolve the url to as if it would be a resource path
        url = map(url);

        // have the servlet container to further encodings
        return super.encodeRedirectURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    // ---------- Error handling through Sling Error Resolver -----------------

    @Override
    public void sendError(int status) throws IOException {
        sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        checkCommitted();

        SlingRequestProcessorImpl eh = getRequestData().getSlingRequestProcessor();
        eh.handleError(status, message, requestData.getSlingRequest(), this);
    }

    // ---------- Internal helper ---------------------------------------------

    private void checkCommitted() {
        if (isCommitted()) {
            throw new IllegalStateException(
                "Response has already been committed");
        }
    }

    private String makeAbsolutePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }

        String base = getRequestData().getContentData().getResource().getPath();
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash >= 0) {
            path = base.substring(0, lastSlash+1) + path;
        } else {
            path = "/" + path;
        }

        return path;
    }

    private String map(String url) {
        return getRequestData().getResourceResolver().map(url);
    }
}
