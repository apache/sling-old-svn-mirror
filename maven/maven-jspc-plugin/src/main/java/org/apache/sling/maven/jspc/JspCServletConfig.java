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
package org.apache.sling.maven.jspc;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class JspCServletConfig implements ServletConfig {

    private final ServletContext servletContext;

    private String servletName;

    private final Map<String, String> properties;

    public JspCServletConfig(final ServletContext servletContext) {
        this.servletContext = servletContext;

        this.servletName = "JSPC Script Handler";
        this.properties = new HashMap<String, String>();
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
}