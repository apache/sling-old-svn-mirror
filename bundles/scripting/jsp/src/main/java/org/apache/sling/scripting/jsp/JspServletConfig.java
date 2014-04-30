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
package org.apache.sling.scripting.jsp;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.osgi.framework.Constants;

class JspServletConfig implements ServletConfig {

    private final ServletContext servletContext;

    private String servletName;

    private Map<String, String> properties;

    JspServletConfig(ServletContext servletContext, Dictionary<?, ?> config) {
        this.servletContext = servletContext;

        // set the servlet name
        servletName = (String) config.get(Constants.SERVICE_DESCRIPTION);
        if (servletName == null) {
            servletName = "JSP Script Handler";
        }

        // copy the "jasper." properties
        properties = new TreeMap<String, String>();
        for (Enumeration<?> ke = config.keys(); ke.hasMoreElements();) {
            String key = (String) ke.nextElement();
            if (key.startsWith("jasper.")) {
                properties.put(key.substring("jasper.".length()),
                    String.valueOf(config.get(key)));
            }
        }
    }

    public String getInitParameter(String name) {
        return properties.get(name);
    }

    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(properties.keySet());
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String getServletName() {
        return servletName;
    }

    /**
     * Return a unique key for the jasper configuration
     */
    public String getConfigKey() {
        final StringBuilder sb = new StringBuilder();
        for(final Map.Entry<String, String> entry : this.properties.entrySet() ) {
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(entry.getValue());
            sb.append(';');
        }
        return sb.toString();
    }
}