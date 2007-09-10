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
        return (ComponentRequest) getRequest();
    }

    public Enumeration getChildren(Content content) throws ComponentException {
        return getComponentRequest().getChildren(content);
    }

    public Content getContent() {
        return getComponentRequest().getContent();
    }

    public Content getContent(String path) throws ComponentException {
        return getComponentRequest().getContent(path);
    }

    public Cookie getCookie(String name) {
        return getComponentRequest().getCookie(name);
    }

    public String getExtension() {
        return getComponentRequest().getExtension();
    }

    public String getLocalAddr() {
        return getComponentRequest().getLocalAddr();
    }
    
    public String getLocalName() {
        return getComponentRequest().getLocalName();
    }
    
    public int getLocalPort() {
        return getComponentRequest().getLocalPort();
    }
    
    public ComponentRequestDispatcher getRequestDispatcher(Content content) {
        return getComponentRequest().getRequestDispatcher(content);
    }

    public RequestParameter getRequestParameter(String name) {
        return getComponentRequest().getRequestParameter(name);
    }

    public Map getRequestParameterMap() {
        return getComponentRequest().getRequestParameterMap();
    }

    public int getRemotePort() {
        return getComponentRequest().getRemotePort();
    }
    
    public RequestParameter[] getRequestParameters(String name) {
        return getComponentRequest().getRequestParameters(name);
    }

    public ResourceBundle getResourceBundle(Locale locale) {
        return getComponentRequest().getResourceBundle(locale);
    }

    public String getResponseContentType() {
        return getComponentRequest().getResponseContentType();
    }

    public Enumeration getResponseContentTypes() {
        return getComponentRequest().getResponseContentTypes();
    }

    public String getSelector(int i) {
        return getComponentRequest().getSelector(i);
    }

    public String[] getSelectors() {
        return getComponentRequest().getSelectors();
    }

    public String getSelectorString() {
        return getComponentRequest().getSelectorString();
    }

    public String getSuffix() {
        return getComponentRequest().getSuffix();
    }
}
