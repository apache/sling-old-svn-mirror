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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * The <code>JavaServletConfig</code>
 * is passed to the compiled servlets.
 */
public class JavaServletConfig implements ServletConfig {

    /** The servlet context. */
    private final ServletContext servletContext;

    private final Map<String, String> initParams;

    public JavaServletConfig(ServletContext servletContext, final Map<String, Object> config) {
        this.servletContext = servletContext;

        // copy the "java." properties
        initParams = new HashMap<String, String>();
        for (final Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getKey().startsWith("java.")) {
                if ( entry.getValue() != null ) {
                    final String strValue = String.valueOf(entry.getValue()).trim();
                    if ( strValue.length() > 0 ) {
                        initParams.put(entry.getKey().substring("java.".length()), strValue);
                    }
                }
            }
        }
    }

    /**
     * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
     */
    @Override
    public String getInitParameter(String name) {
        return initParams.get(name);
    }

    /**
     * @see javax.servlet.ServletConfig#getInitParameterNames()
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParams.keySet());
    }

    /**
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * @see javax.servlet.ServletConfig#getServletName()
     */
    @Override
    public String getServletName() {
        return JavaScriptEngineFactory.DESCRIPTION;
    }
}