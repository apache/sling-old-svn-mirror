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

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.thymeleaf.SlingContext;
import org.thymeleaf.context.AbstractContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.context.VariablesMap;
import org.thymeleaf.context.WebContextExecutionInfo;
import org.thymeleaf.util.Validate;

public final class SlingWebContext implements SlingContext, IWebContext {

    private final SlingHttpServletRequest servletRequest;

    private final SlingHttpServletResponse servletResponse;

    private final ServletContext servletContext;

    private final ResourceResolver resourceResolver;

    private final Locale locale;

    private final VariablesMap<String, Object> variables = new VariablesMap<String, Object>();

    public SlingWebContext(final SlingHttpServletRequest servletRequest, final SlingHttpServletResponse servletResponse, final ServletContext servletContext, final ResourceResolver resourceResolver, final Locale locale, final Map<String, ?> variables) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.servletContext = servletContext;
        this.resourceResolver = resourceResolver;
        this.locale = locale;
        this.variables.putAll(variables);
    }

    @Override
    public SlingHttpServletRequest getHttpServletRequest() {
        return servletRequest;
    }

    @Override
    public SlingHttpServletResponse getHttpServletResponse() {
        return servletResponse;
    }

    @Override
    public HttpSession getHttpSession() {
        return servletRequest.getSession(false);
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public VariablesMap<String, Object> getVariables() {
        return variables;
    }

    @Override
    public VariablesMap<String, String[]> getRequestParameters() {
        throw new UnsupportedOperationException("Deprecated method is not supported.");
    }

    @Override
    public VariablesMap<String, Object> getRequestAttributes() {
        throw new UnsupportedOperationException("Deprecated method is not supported.");
    }

    @Override
    public VariablesMap<String, Object> getSessionAttributes() {
        throw new UnsupportedOperationException("Deprecated method is not supported.");
    }

    @Override
    public VariablesMap<String, Object> getApplicationAttributes() {
        throw new UnsupportedOperationException("Deprecated method is not supported.");
    }

    @Override
    public void addContextExecutionInfo(String templateName) {
        Validate.notEmpty(templateName, "Template name cannot be null or empty");
        final Calendar now = Calendar.getInstance();
        final WebContextExecutionInfo webContextExecutionInfo = new WebContextExecutionInfo(templateName, now);
        variables.put(AbstractContext.EXEC_INFO_VARIABLE_NAME, webContextExecutionInfo);
    }

}
