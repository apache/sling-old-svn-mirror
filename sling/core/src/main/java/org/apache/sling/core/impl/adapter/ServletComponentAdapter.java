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
package org.apache.sling.core.impl.adapter;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.components.BaseComponent;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentConstants;

/**
 * The <code>ServletComponentAdapter</code> class is a component which adapts
 * the <code>javax.servlet.Servlet</code> API to the component API. Instances
 * of this class are managed by the {@link ComponentAdapterManager} to adapt
 * services registered as <code>javax.servlet.Service</code> to components and
 * register them as components for use by the system.
 * <p>
 * The component ID of instances of this component is deduced from the service
 * registration properties of the <code>Servlet</code> service which is
 * adapted. If the service has a <code>component.name</code> property, its
 * value is used as the component ID. Otherwise, if the service has a
 * <code>service.pid</code> property, that value is used as the component ID.
 * If this property is not defined either, the fully qualified name of the
 * service class is used as the component ID.
 * <p>
 * This class only supports the rendering part of a component has no knowledge
 * of the type of <code>Content</code> referring to it. Therefore the
 * <code>getContentClassName()</code> always returns <code>null</code> and
 * instances of the content class can obviously not be created.
 * <p>
 * <b>Servlet Init Parameters</b>
 * <p>
 * The service properties of the originally registered <code>Servlet</code>
 * service are reused as service properties for the registered component with
 * some notable exceptions:
 * <dl>
 * <dt>objectClass, service.id, service.pid, service.ranking,
 * service.bundleLocation, service.factoryPid, component.id, component.name,
 * component.factory</dt>
 * <dd>These properties are copied and their name is prefixed with the
 * {@link ComponentAdapterManager#ADAPTED_PROPERTY_PREFIX}</dd>
 * <dt>service.description</dt>
 * <dd>This property is copied but the value is prefixed with the
 * {@link ComponentAdapterManager#ADAPTED_DESCRIPTION_PREFIX}</dd>
 * </dl>
 */
class ServletComponentAdapter extends BaseComponent {

    private static final String ADAPTED_COMPONENT_NAME = ComponentAdapterManager.ADAPTED_PROPERTY_PREFIX
        + ComponentConstants.COMPONENT_NAME;

    private static final String ADAPTED_SERVICE_PID = ComponentAdapterManager.ADAPTED_PROPERTY_PREFIX
        + Constants.SERVICE_PID;

    /**
     * The servlet service to which this instance adapts.
     */
    private final Servlet delegatee;

    /**
     * The service properties used to register this Component as an OSGi service
     * and as init-parameters to the adapted servlet.
     */
    private final Dictionary<String, ?> properties;

    /**
     * Creates an instance of this adapter.
     * 
     * @param delegatee The adapted Servlet
     * @param properties The service properties of this Component service also
     *            used as init-parameters for the servlet and to extract the
     *            component ID.
     */
    ServletComponentAdapter(Servlet delegatee, Dictionary<String, ?> properties) {
        this.delegatee = delegatee;
        this.properties = properties;

        // get the component id
        String id = (String) properties.get(ADAPTED_COMPONENT_NAME);
        if (id == null) {
            id = (String) properties.get(ADAPTED_SERVICE_PID);
            if (id == null) {
                id = delegatee.getClass().getName();
            }
        }
        setComponentId(id);
    }

    /**
     * Calls the <code>Servlet.init(ServletConfig)</code> method with a
     * servlet configuration as follows:
     * <ul>
     * <li>The service parameter of this Component service are used init-params
     * <li>The ID of this component is used as the servlet name
     * <li>The Component context of this component is used as the servlet
     * context. </li>
     * 
     * @throws ComponentException Wrapping the <code>ServletException</code>
     *             thrown by the <code>Servlet.init</code> method.
     */
    @Override
    protected void doInit() throws ComponentException {
        try {
            delegatee.init(new ServletConfig() {

                // ensure service properties are converted to string
                public String getInitParameter(String name) {
                    Object prop = properties.get(name);
                    if (prop == null) {
                        return null;
                    }

                    return prop.toString();
                }

                // inherit init-params from service properties
                public Enumeration<String> getInitParameterNames() {
                    return properties.keys();
                }

                // the component's context is used as the ServletContext
                public ServletContext getServletContext() {
                    return getComponentContext();
                }

                // the servlet name is equivalent to the component ID
                public String getServletName() {
                    return getId();
                }
            });
        } catch (ServletException se) {
            throw toComponentException(se);
        }
    }

    /**
     * Calls the <code>Servlet.service(ServletRequest, ServletResponse)</code>
     * method using the <code>request</code> and <code>response</code>
     * objects unmodified.
     * 
     * @param request The <code>ComponentRequest</code> used as the request
     *            parameter to the <code>Servlet.service</code> method.
     * @param response The <code>ComponentResponse</code> used as the response
     *            parameter to the <code>Servlet.service</code> method.
     * @throws ComponentException Wrapping the <code>ServletException</code>
     *             thrown by the <code>Servlet.service</code> method.
     * @throws IOException If thrown by the <code>Servlet.service</code>
     *             method.
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws ComponentException, IOException {
        try {
            delegatee.service(request, response);
        } catch (ServletException se) {
            throw toComponentException(se);
        }
    }

    /**
     * Calls the <code>Servlet.destroy()</code> method to take the servlet out
     * of service.
     */
    @Override
    public void destroy() {
        delegatee.destroy();
        super.destroy();
    }

    /**
     * Converts the given <code>ServletException</code> into a
     * <code>ComponentException</code> as follows:
     * <ul>
     * <li>If the servlet exception is a <code>ComponentException</code> the
     * exception is just returned.
     * <li>Else, if the root cause of the servlet exception is a
     * <code>ComponentException</code> the root cause is returned.
     * <li>Else, if the root cause is not <code>null</code>, the root cause
     * is wrapped in a <code>ComponentException</code> using the message of
     * the root cause as the new exception's message.
     * <li>Else, the servlet exception is wrapped in a
     * <code>ComponentException</code> using the message of the servlet
     * exception as the new exception's message.
     * </ul>
     * 
     * @param se The <code>ServletException</code> to turn into a
     *            <code>ComponentException</code>
     * @return The <code>ComponentException</code> created from the servlet
     *         exception.
     */
    private ComponentException toComponentException(ServletException se) {
        if (se instanceof ComponentException) {
            return (ComponentException) se;
        }

        Throwable cause = se.getRootCause();
        if (cause instanceof ComponentException) {
            return (ComponentException) cause;
        }

        if (cause == null) {
            cause = se;
        }

        return new ComponentException(cause.getMessage(), cause);
    }
}
