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

import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.RequestUtil;

/**
 * The <code>ComponentResponseImpl</code> TODO
 */
class ComponentResponseImpl extends HttpServletResponseWrapper implements ComponentResponse {

    private final RequestData requestData;
    private String contentType = "text/html";
    private String characterEncoding = "ISO-8859-1";

    protected ComponentResponseImpl(RequestData requestData) {
        super(requestData.getServletResponse());
        this.requestData = requestData;
    }

    protected ComponentResponseImpl(ComponentResponseImpl response) {
        super(response);
        this.requestData = response.getRequestData();
    }

    protected final RequestData getRequestData() {
        return this.requestData;
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    public void flushBuffer() throws IOException {
        this.getRequestData().getContentData().flushBuffer();
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#getBufferSize()
     */
    public int getBufferSize() {
        return this.getRequestData().getContentData().getBufferSize();
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#getCharacterEncoding()
     */
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    /**
     * @see org.apache.sling.core.component.ComponentResponse#getContentType()
     */
    public String getContentType() {
        return this.contentType + ";charset=" + this.characterEncoding;
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#getLocale()
     */
    public Locale getLocale() {
        // TODO Should use our Locale Resolver and not let the component set the locale, right ??
        return this.getResponse().getLocale();
    }

    /**
     * @see org.apache.sling.core.component.ComponentResponse#getNamespace()
     */
    public String getNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return this.getRequestData().getBufferProvider().getOutputStream();
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        return this.getRequestData().getBufferProvider().getWriter();
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#isCommitted()
     */
    public boolean isCommitted() {
        // TODO: integrate with our output catcher
        return this.getResponse().isCommitted();
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#reset()
     */
    public void reset() {
        // TODO: integrate with our output catcher
        this.getResponse().reset();
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#resetBuffer()
     */
    public void resetBuffer() {
        this.getRequestData().getContentData().resetBuffer();
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#setBufferSize(int)
     */
    public void setBufferSize(int size) {
        this.getRequestData().getContentData().setBufferSize(size);
    }

    public void setCharacterEncoding(String charset) {
        if (charset != null && charset.length() > 0) {
            // should actually check for charset validity ??
            this.characterEncoding = charset;
        }
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#setContentType(java.lang.String)
     */
    public void setContentType(String type) {
        // ignore empty values
        if (type == null || type.length() == 0) {
            return;
        }

        if (type.indexOf(';') > 0) {
            Map parsedType = RequestUtil.parserHeader(type);
            if (parsedType.size() == 1) {
                // expected single entry of token being content type and
                // a single parameter charset, otherwise just ignore and
                // use type as is...
                Map.Entry entry = (Map.Entry) parsedType.entrySet().iterator().next();
                type = (String) entry.getKey();
                String charset = (String) ((Map) entry.getValue()).get("charset");
                this.setCharacterEncoding(charset);
            }
        }

        this.contentType = type;

        // set the content type with charset on the underlying response
        super.setContentType(this.getContentType());
    }
}
