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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

import org.apache.sling.component.Component;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>ComponentAdapterManager</code> is a component watching for
 * <code>javax.servlet.Servlet</code> services being registered. Each registered
 * servlet is then wrapped in a Component adapter and registered (again) as a
 * <code>Component</code> which is then recognized by Sling for rendering.
 * 
 * @scr.component metatype="false"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Component Adapter Manager"
 * @scr.reference name="Servlet" interface="javax.servlet.Servlet"
 *                cardinality="0..n" policy="dynamic"
 */
public class ComponentAdapterManager {

    /**
     * The prefix prependend to private property names added to the adapted
     * Component (value is "sling.component.adapter.")
     */
    public static final String ADAPTED_PROPERTY_PREFIX = "sling.component.adapter.";

    /**
     * The prefix prependend to service description to be used for the adapter
     * (value is "Component Adapter to ")
     */
    public static final String ADAPTED_DESCRIPTION_PREFIX = "Component Adapter to ";

    /**
     * The set of service properties considered private. These properties are
     * still copied from the adapted service to the adapter service but their
     * keys are prefixed with the {@link #ADAPTED_PROPERTY_PREFIX}.
     */
    private static final Set<String> privateProperties;

    /*
     * Static initializer to set up the set of private property names
     */
    static {
        privateProperties = new HashSet<String>();

        // well-known service properties
        privateProperties.add(Constants.OBJECTCLASS);
        privateProperties.add(Constants.SERVICE_ID);
        privateProperties.add(Constants.SERVICE_PID);
        privateProperties.add(Constants.SERVICE_RANKING);

        // configuration admin standard properties
        privateProperties.add(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
        privateProperties.add(ConfigurationAdmin.SERVICE_FACTORYPID);

        // declarative services standard properties
        privateProperties.add(ComponentConstants.COMPONENT_ID);
        privateProperties.add(ComponentConstants.COMPONENT_NAME);
        privateProperties.add(ComponentConstants.COMPONENT_FACTORY);
    }

    private ComponentContext componentContext;

    /**
     * List of service references bound to this instance while the instance has
     * not yet been activated. As soon as this instance is activated all
     * references are resolved into the respective service and adapters are
     * created and registered.
     */
    private final List<ServiceReference> references = new ArrayList<ServiceReference>();

    /**
     * The map of adapters registered for the adapted adapters.
     */
    private final Map<Long, ServiceRegistration> adapters = new HashMap<Long, ServiceRegistration>();

    protected void activate(ComponentContext context) {

        ServiceReference[] refs;
        synchronized (this) {
            componentContext = context;
            refs = references.toArray(new ServiceReference[references.size()]);
            references.clear();
        }

        // register the services outside the synchronization to prevent deadlock
        for (int i = 0; i < refs.length; i++) {
            doBindServlet(componentContext, refs[i]);
        }
    }

    protected void deactivate(ComponentContext context) {
        ServiceRegistration[] adapterRegs;
        synchronized (this) {
            componentContext = null;

            // just to be sure we clear our references
            references.clear();

            // unregister all adapters now
            adapterRegs = adapters.values().toArray(
                new ServiceRegistration[adapters.size()]);
            adapters.clear();
        }

        for (int i = 0; i < adapterRegs.length; i++) {
            try {
                adapterRegs[i].unregister();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    protected void bindServlet(ServiceReference servletReference) {

        ComponentContext context;
        synchronized (this) {
            context = componentContext;

            // if there is no bundle context (yet), store the servlet
            // reference in the references list
            if (context == null) {
                references.add(servletReference);
                return;
            }
        }

        // register the service outside the synchronization to prevent deadlock
        doBindServlet(context, servletReference);
    }

    protected synchronized void unbindServlet(ServiceReference servletReference) {
        Long id = (Long) servletReference.getProperty(Constants.SERVICE_ID);

        ServiceRegistration sr;
        synchronized (this) {
            sr = adapters.remove(id);
        }

        // unregister the service outside the synchronization to prevent
        // deadlock
        if (sr != null) {
            sr.unregister();
        }

        // just in case ...
        references.remove(servletReference);
    }

    private void doBindServlet(ComponentContext context,
            ServiceReference servletReference) {
        Servlet servlet = (Servlet) context.locateService("Servlet",
            servletReference);
        if (servlet != null) {
            Dictionary<String, Object> properties = getProperties(servletReference);
            ServletComponentAdapter adapter = new ServletComponentAdapter(
                servlet, properties);
            ServiceRegistration sr = context.getBundleContext().registerService(
                Component.class.getName(), adapter, properties);

            Long id = (Long) servletReference.getProperty(Constants.SERVICE_ID);
            synchronized (this) {
                adapters.put(id, sr);
            }
        }
    }

    private Dictionary<String, Object> getProperties(
            ServiceReference servletReference) {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        String[] keys = servletReference.getPropertyKeys();
        for (String key : keys) {
            Object value = servletReference.getProperty(key);

            if (privateProperties.contains(key)) {
                key = ADAPTED_PROPERTY_PREFIX + key;
            } else if (Constants.SERVICE_DESCRIPTION.equals(key)) {
                value = ADAPTED_DESCRIPTION_PREFIX + value;
            }

            properties.put(key, value);
        }
        return properties;
    }
}
