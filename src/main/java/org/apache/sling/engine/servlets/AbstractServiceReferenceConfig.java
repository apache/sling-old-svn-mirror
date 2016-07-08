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
package org.apache.sling.engine.servlets;

import static org.apache.sling.engine.EngineConstants.SLING_SERLVET_NAME;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.component.ComponentConstants.COMPONENT_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext;

import org.osgi.framework.ServiceReference;

import aQute.bnd.annotation.ConsumerType;

/**
 * The <code>AbstractServiceReferenceConfig</code> may be used as a base class
 * to define <code>ServletConfig</code> and <code>FilterConfig</code>
 * instances from OSGi <code>ServiceReference</code> objects. The properties
 * of this service reference are used as the initialization parameters of the
 * configuration object.
 * <p>
 * The name of the servlet or filter may be retrieved from the service reference
 * by calling the static {@link #getName(ServiceReference)} method. This method
 * looks for the following service reference properties, assuming the first non-<code>null</code>
 * value found:
 * <ol>
 * <li><em>sling.core.servletName</em> - A service registration property
 * which may be set to define an independent servlet name.
 * <li><em>component.name</em> - The name of the Declarative Services
 * component if the OSGi service is registered using Declarative Services.
 * <li><em>service.pid</em> - The Service PID of the service.
 * <li><em>service.id</em> - The ID of the service as set by the OSGi
 * framework service registry. This property is guaranteed to always be set by
 * the OSGi specification.
 * </ol>
 *
 * @deprecated
 */
@Deprecated
@ConsumerType
public abstract class AbstractServiceReferenceConfig {

    /** The list of property names checked by {@link #getName(ServiceReference)} */
    private static final String[] NAME_PROPERTIES = { SLING_SERLVET_NAME,
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
     * @param name The name of this configuration.
     * @see #getName()
     */
    public AbstractServiceReferenceConfig(ServletContext servletContext,
            ServiceReference reference, String name) {
        this.servletContext = servletContext;
        this.reference = reference;
        this.name = name;
    }

    public String getInitParameter(String name) {
        Object prop = reference.getProperty(name);
        return (prop == null) ? null : String.valueOf(prop);
    }

    public Enumeration<?> getInitParameterNames() {
        List<?> keys = Arrays.asList(reference.getPropertyKeys());
        return Collections.enumeration(keys);
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Returns the name of this configuration object. Implementations may use
     * this value to implement the <code>ServletConfig.getServletName()</code>
     * or <code>FilterConfig.getFilterName()</code> methods.
     * @return The name
     */
    protected String getName() {
        return name;
    }

    /**
     * Looks for a name value in the service reference properties. See the
     * class comment at the top for the list of properties checked by this
     * method.
     * @param reference The service reference
     * @return The name
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

}
