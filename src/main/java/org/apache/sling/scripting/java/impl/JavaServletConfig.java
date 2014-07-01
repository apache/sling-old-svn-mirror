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
package org.apache.sling.scripting.java.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.osgi.framework.Constants;

/**
 * The <code>JavaServletConfig</code>
 * is passed to the compiled servlets.
 */
public class JavaServletConfig implements ServletConfig {

    /** The servlet context. */
    private final ServletContext servletContext;

    /** The name of the servlet. */
    private final String servletName;

    private final Map<String, String> initParams;

    public JavaServletConfig(ServletContext servletContext, Dictionary<?, ?> config) {
        this.servletContext = servletContext;

        // set the servlet name
        if (config.get(Constants.SERVICE_DESCRIPTION) == null) {
            servletName = "Java Script Handler";
        } else{
            servletName = config.get(Constants.SERVICE_DESCRIPTION).toString();
        }

        // copy the "java." properties
        initParams = new HashMap<String, String>();
        for (Enumeration<?> ke = config.keys(); ke.hasMoreElements();) {
            final String key = (String) ke.nextElement();
            if (key.startsWith("java.")) {
                final Object value = config.get(key);
                if ( value != null ) {
                    final String strValue = String.valueOf(value).trim();
                    if ( strValue.length() > 0 ) {
                        initParams.put(key.substring("java.".length()), strValue);
                    }
                }
            }
        }
    }

    /**
     * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        return initParams.get(name);
    }

    /**
     * @see javax.servlet.ServletConfig#getInitParameterNames()
     */
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }

    /**
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * @see javax.servlet.ServletConfig#getServletName()
     */
    public String getServletName() {
        return servletName;
    }
}