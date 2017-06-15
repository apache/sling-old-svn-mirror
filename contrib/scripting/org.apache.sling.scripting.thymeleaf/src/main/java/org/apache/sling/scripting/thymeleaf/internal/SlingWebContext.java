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
package org.apache.sling.scripting.thymeleaf.internal;

import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.thymeleaf.DefaultSlingContext;
import org.thymeleaf.context.IWebContext;

public class SlingWebContext extends DefaultSlingContext implements IWebContext {

    private final SlingHttpServletRequest servletRequest;

    private final SlingHttpServletResponse servletResponse;

    private final ServletContext servletContext;

    public SlingWebContext(final SlingHttpServletRequest servletRequest, final SlingHttpServletResponse servletResponse, final ServletContext servletContext, final ResourceResolver resourceResolver, final Locale locale, final Map<String, Object> variables) {
        super(resourceResolver, locale, variables);
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.servletContext = servletContext;
    }

    @Override
    public HttpServletRequest getRequest() {
        return servletRequest;
    }

    @Override
    public HttpServletResponse getResponse() {
        return servletResponse;
    }

    @Override
    public HttpSession getSession() {
        return servletRequest.getSession(false);
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

}
