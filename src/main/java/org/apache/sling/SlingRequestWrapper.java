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
package org.apache.sling;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * The <code>SlingRequestWrapper</code> class is a default wrapper class
 * around a {@link SlingRequest} which may be extended to amend the
 * functionality of the original request object.
 */
public class SlingRequestWrapper extends HttpServletRequestWrapper implements
        SlingRequest {

    public SlingRequestWrapper(SlingRequest delegatee) {
        super(delegatee);
    }

    /**
     * Return the original {@link SlingRequest} object wrapped by this.
     */
    public SlingRequest getSlingRequest() {
        return (SlingRequest) this.getRequest();
    }

    public Enumeration<Content> getChildren(Content content)
            throws SlingException {
        return this.getSlingRequest().getChildren(content);
    }

    public Content getContent() {
        return this.getSlingRequest().getContent();
    }

    public Content getContent(String path) throws SlingException {
        return this.getSlingRequest().getContent(path);
    }

    public Cookie getCookie(String name) {
        return this.getSlingRequest().getCookie(name);
    }

    public String getExtension() {
        return this.getSlingRequest().getExtension();
    }

    public String getLocalAddr() {
        return this.getSlingRequest().getLocalAddr();
    }

    public String getLocalName() {
        return this.getSlingRequest().getLocalName();
    }

    public int getLocalPort() {
        return this.getSlingRequest().getLocalPort();
    }

    public RequestDispatcher getRequestDispatcher(Content content) {
        return this.getSlingRequest().getRequestDispatcher(content);
    }

    public RequestParameter getRequestParameter(String name) {
        return this.getSlingRequest().getRequestParameter(name);
    }

    public Map<String, RequestParameter> getRequestParameterMap() {
        return this.getSlingRequest().getRequestParameterMap();
    }

    public int getRemotePort() {
        return this.getSlingRequest().getRemotePort();
    }

    public RequestParameter[] getRequestParameters(String name) {
        return this.getSlingRequest().getRequestParameters(name);
    }

    public ResourceBundle getResourceBundle(Locale locale) {
        return this.getSlingRequest().getResourceBundle(locale);
    }

    public String getResponseContentType() {
        return this.getSlingRequest().getResponseContentType();
    }

    public Enumeration<String> getResponseContentTypes() {
        return this.getSlingRequest().getResponseContentTypes();
    }

    public String getSelector(int i) {
        return this.getSlingRequest().getSelector(i);
    }

    public String[] getSelectors() {
        return this.getSlingRequest().getSelectors();
    }

    public String getSelectorString() {
        return this.getSlingRequest().getSelectorString();
    }

    public String getSuffix() {
        return this.getSlingRequest().getSuffix();
    }
}
