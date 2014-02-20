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
package org.apache.sling.engine.impl.helper;

import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.component.ComponentConstants.COMPONENT_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.osgi.framework.ServiceReference;

public class SlingFilterConfig implements FilterConfig {

    /** The list of property names checked by {@link #getName(ServiceReference)} */
    private static final String[] NAME_PROPERTIES = { "sling.core.servletName",
        COMPONENT_NAME, SERVICE_PID, SERVICE_ID };

    /** The <code>ServletContext</code> of this configuration object */
    private ServletContext servletContext;

    /** The <code>ServiceReference</code> providing the properties */
    private ServiceReference reference;

    /** The name of this configuration object */
    private String name;


    /**
     * Sets up this base configuration object.
     *
     * @param servletContext The <code>ServletContext</code> attached to this
     *            configuration.
     * @param reference The service reference providing the initialization
     *            parameter values.
     * @param filterName The name of this configuration.
     */
    public SlingFilterConfig(final ServletContext servletContext,
                             final ServiceReference reference,
                             final String filterName) {
        this.servletContext = servletContext;
        this.reference = reference;
        this.name = filterName;
    }

    /**
     * @see javax.servlet.FilterConfig#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name) {
        Object prop = reference.getProperty(name);
        return (prop == null) ? null : String.valueOf(prop);
    }

    /**
     * @see javax.servlet.FilterConfig#getInitParameterNames()
     */
    public Enumeration<String> getInitParameterNames() {
        List<String> keys = Arrays.asList(reference.getPropertyKeys());
        return Collections.enumeration(keys);
    }

    /**
     * @see javax.servlet.FilterConfig#getServletContext()
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Looks for a name value in the service reference properties. See the
     * class comment at the top for the list of properties checked by this
     * method.
     */
    public static String getName(ServiceReference reference) {
        String servletName = null;
        for (int i = 0; i < NAME_PROPERTIES.length
            && (servletName == null || servletName.length() == 0); i++) {
            Object prop = reference.getProperty(NAME_PROPERTIES[i]);
            if (prop != null) {
                servletName = String.valueOf(prop);
            }
        }
        return servletName;
    }

    /**
     * @see javax.servlet.FilterConfig#getFilterName()
     */
    public String getFilterName() {
        return name;
    }
}