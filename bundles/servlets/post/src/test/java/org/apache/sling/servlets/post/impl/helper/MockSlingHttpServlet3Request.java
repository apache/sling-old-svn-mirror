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

import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * When MockSlingHttpServletRequest supports Servlet 3 correctly, delete this class.
 */
public class MockSlingHttpServlet3Request extends MockSlingHttpServletRequest {
    public MockSlingHttpServlet3Request(String o, String o1, String o2, String o3, String o4) {
        super(o,o1,o2,o3,o4);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return  (Enumeration<Locale>) super.getLocales();
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return (Enumeration<String>) super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return (Enumeration<String>) super.getHeaderNames();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return (Enumeration<String>)  super.getAttributeNames();
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return ( Map<String, String[]> ) super.getParameterMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return (Enumeration<String>) super.getParameterNames();
    }

    @Override
    public Enumeration<String> getResponseContentTypes() {
        return (Enumeration<String>) super.getResponseContentTypes();
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}
