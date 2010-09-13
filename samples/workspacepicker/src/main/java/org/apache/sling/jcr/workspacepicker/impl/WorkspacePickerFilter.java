/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.jcr.workspacepicker.impl;

import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;

/**
 * Servlet Filter which looks for a request parameter or cookie.
 * 
 */
@Component(label="%workspacepicker.name", description="%workspacepicker.description", metatype=true)
@Service
@Properties({
    @Property(name="service.description", value="Apache Sling Workspace Picker"),
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="pattern", value="/.*", propertyPrivate=true)
})
public class WorkspacePickerFilter implements Filter {
    
    private static final String DEFAULT_PARAM_NAME = "sling.workspace";

    @Property(label="%workspacepicker.param.name.name", description="%workspacepicker.param.name.description", value=DEFAULT_PARAM_NAME)
    private static final String PROP_PARAM_NAME = "param.name";
    
    private static final String DEFAULT_COOKIE_NAME = "sling.workspace";
    
    @Property(label="%workspacepicker.cookie.name.name", description="%workspacepicker.cookie.name.description", value=DEFAULT_COOKIE_NAME)
    private static final String PROP_COOKIE_NAME = "cookie.name";
    
    private String parameterName;
    
    private String cookieName;

    @SuppressWarnings("rawtypes")
    protected void activate(ComponentContext ctx) {
        Dictionary props = ctx.getProperties();
        this.parameterName = OsgiUtil.toString(props.get(PROP_PARAM_NAME), DEFAULT_PARAM_NAME);
        this.cookieName = OsgiUtil.toString(props.get(PROP_COOKIE_NAME), DEFAULT_COOKIE_NAME);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
        }
    }

    private void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String workspace = getWorkspace(request, response);
        if (workspace != null) {
            request.setAttribute("org.apache.sling.api.resource.ResourceResolver/use.workspace", workspace);
        }
        chain.doFilter(request, response);
    }

    private String getWorkspace(HttpServletRequest request, HttpServletResponse response) {
        String fromReq = request.getParameter(parameterName);
        if (fromReq != null) {
            setCookie(response, fromReq);
            return fromReq;
        } else {
            return getCookie(request);
        }
    }

    private void setCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(cookieName, value);
        response.addCookie(cookie);
    }

    private String getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void destroy() {
    }

}
