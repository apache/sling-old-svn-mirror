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
package org.apache.sling.core.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.core.RequestUtil;
import org.apache.sling.core.impl.helper.RequestData;

/**
 * The <code>SlingHttpServletResponseImpl</code> TODO
 */
class SlingHttpServletResponseImpl extends HttpServletResponseWrapper implements SlingHttpServletResponse {

    private final RequestData requestData;
    private String contentType = "text/html";
    private String characterEncoding = null;

    protected SlingHttpServletResponseImpl(RequestData requestData) {
        super(requestData.getServletResponse());
        this.requestData = requestData;
    }

    protected SlingHttpServletResponseImpl(SlingHttpServletResponseImpl response) {
        super(response);
        requestData = response.getRequestData();
    }

    protected final RequestData getRequestData() {
        return requestData;
    }

    @Override
    public void flushBuffer() throws IOException {
        getRequestData().getContentData().flushBuffer();
    }

    @Override
    public int getBufferSize() {
        return getRequestData().getContentData().getBufferSize();
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        // plaing content type if there is no character encoding
        if (characterEncoding == null) {
            return contentType;
        }

        // otherwise append the charset
        return contentType + ";charset=" + characterEncoding;
    }

    @Override
    public Locale getLocale() {
        // TODO Should use our Locale Resolver and not let the component set the locale, right ??
        return getResponse().getLocale();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return getRequestData().getBufferProvider().getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return getRequestData().getBufferProvider().getWriter();
    }

    @Override
    public boolean isCommitted() {
        // TODO: integrate with our output catcher
        return getResponse().isCommitted();
    }

    @Override
    public void reset() {
        // TODO: integrate with our output catcher
        getResponse().reset();
    }

    @Override
    public void resetBuffer() {
        getRequestData().getContentData().resetBuffer();
    }

    @Override
    public void setBufferSize(int size) {
        getRequestData().getContentData().setBufferSize(size);
    }

    @Override
    public void setCharacterEncoding(String charset) {
        // should actually check for charset validity ??
        characterEncoding = charset;
    }

    @Override
    public void setContentType(String type) {
        // ignore empty values
        if (type == null || type.length() == 0) {
            return;
        }

        if (type.indexOf(';') > 0) {
            Map<String, Map<String, String>> parsedType = RequestUtil.parserHeader(type);
            if (parsedType.size() == 1) {
                // expected single entry of token being content type and
                // a single parameter charset, otherwise just ignore and
                // use type as is...
                Map.Entry<String, Map<String, String>> entry = parsedType.entrySet().iterator().next();
                type = entry.getKey();
                String charset = entry.getValue().get("charset");
                if (charset != null && charset.length() > 0) {
                    setCharacterEncoding(charset);
                }
            }
        }

        contentType = type;

        // set the content type with charset on the underlying response
        super.setContentType(getContentType());
    }
}
