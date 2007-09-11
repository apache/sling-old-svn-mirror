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
package org.apache.sling.component;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * The <code>ComponentRequestWrapper</code> class is a default wrapper class
 * around a {@link ComponentRequest} which may be extended to amend the
 * functionality of the original request object.
 */
public class ComponentRequestWrapper extends HttpServletRequestWrapper implements ComponentRequest {

    public ComponentRequestWrapper(ComponentRequest delegatee) {
        super(delegatee);
    }

    /**
     * Return the original {@link ComponentRequest} object wrapped by this.
     */
    public ComponentRequest getComponentRequest() {
        return (ComponentRequest) this.getRequest();
    }

    public Enumeration<Content> getChildren(Content content) throws ComponentException {
        return this.getComponentRequest().getChildren(content);
    }

    public Content getContent() {
        return this.getComponentRequest().getContent();
    }

    public Content getContent(String path) throws ComponentException {
        return this.getComponentRequest().getContent(path);
    }

    public Cookie getCookie(String name) {
        return this.getComponentRequest().getCookie(name);
    }

    public String getExtension() {
        return this.getComponentRequest().getExtension();
    }

    public String getLocalAddr() {
        return this.getComponentRequest().getLocalAddr();
    }

    public String getLocalName() {
        return this.getComponentRequest().getLocalName();
    }

    public int getLocalPort() {
        return this.getComponentRequest().getLocalPort();
    }

    public ComponentRequestDispatcher getRequestDispatcher(Content content) {
        return this.getComponentRequest().getRequestDispatcher(content);
    }

    public RequestParameter getRequestParameter(String name) {
        return this.getComponentRequest().getRequestParameter(name);
    }

    public Map<String, RequestParameter> getRequestParameterMap() {
        return this.getComponentRequest().getRequestParameterMap();
    }

    public int getRemotePort() {
        return this.getComponentRequest().getRemotePort();
    }

    public RequestParameter[] getRequestParameters(String name) {
        return this.getComponentRequest().getRequestParameters(name);
    }

    public ResourceBundle getResourceBundle(Locale locale) {
        return this.getComponentRequest().getResourceBundle(locale);
    }

    public String getResponseContentType() {
        return this.getComponentRequest().getResponseContentType();
    }

    public Enumeration<String> getResponseContentTypes() {
        return this.getComponentRequest().getResponseContentTypes();
    }

    public String getSelector(int i) {
        return this.getComponentRequest().getSelector(i);
    }

    public String[] getSelectors() {
        return this.getComponentRequest().getSelectors();
    }

    public String getSelectorString() {
        return this.getComponentRequest().getSelectorString();
    }

    public String getSuffix() {
        return this.getComponentRequest().getSuffix();
    }
}
