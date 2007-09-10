/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.sling.RequestUtil;
import org.apache.sling.component.ComponentResponse;

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
        return requestData;
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#flushBuffer()
     */
    public void flushBuffer() throws IOException {
        getRequestData().getContentData().flushBuffer();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#getBufferSize()
     */
    public int getBufferSize() {
        return getRequestData().getContentData().getBufferSize();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#getCharacterEncoding()
     */
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#getContentType()
     */
    public String getContentType() {
        return contentType + ";charset=" + characterEncoding;
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#getLocale()
     */
    public Locale getLocale() {
        // TODO Should use our Locale Resolver and not let the component set the locale, right ??
        return getResponse().getLocale();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#getNamespace()
     */
    public String getNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return getRequestData().getBufferProvider().getOutputStream();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        return getRequestData().getBufferProvider().getWriter();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#isCommitted()
     */
    public boolean isCommitted() {
        // TODO: integrate with our output catcher
        return getResponse().isCommitted();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#reset()
     */
    public void reset() {
        // TODO: integrate with our output catcher
        getResponse().reset();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#resetBuffer()
     */
    public void resetBuffer() {
        getRequestData().getContentData().resetBuffer();
    }

    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#setBufferSize(int)
     */
    public void setBufferSize(int size) {
        getRequestData().getContentData().setBufferSize(size);
    }

    public void setCharacterEncoding(String charset) {
        if (charset != null && charset.length() > 0) {
            // should actually check for charset validity ??
            this.characterEncoding = charset;
        }
    }
    
    /* (non-Javadoc)
     * @see com.day.components.ComponentResponse#setContentType(java.lang.String)
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
                setCharacterEncoding(charset);
            }
        }
        
        this.contentType = type;
        
        // set the content type with charset on the underlying response
        super.setContentType(getContentType());
    }
}
