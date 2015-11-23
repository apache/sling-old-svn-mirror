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
package org.apache.sling.testing.mock.osgi.context;

import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.testing.mock.osgi.MockEventAdmin;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import aQute.bnd.annotation.ConsumerType;

/**
 * Defines OSGi context objects and helper methods. Should not be used directly
 * but via the {@link org.apache.sling.testing.mock.osgi.junit.OsgiContext} JUnit rule.
 */
@ConsumerType
public class OsgiContextImpl {

    protected ComponentContext componentContext;

    /**
     * Setup actions before test method execution
     */
    protected void setUp() {
        registerDefaultServices();
    }
    
    /**
     * Teardown actions after test method execution
     */
    protected void tearDown() {
        if (componentContext != null) {
            // deactivate all services
            MockOsgi.shutdown(componentContext.getBundleContext());
        }

        this.componentContext = null;
    }

    /**
     * Default services that should be available for every unit test
     */
    private void registerDefaultServices() {
        registerInjectActivateService(new MockEventAdmin());
    }
    
    /**
     * @return OSGi component context
     */
    public final ComponentContext componentContext() {
        if (this.componentContext == null) {
            this.componentContext = MockOsgi.newComponentContext();
        }
        return this.componentContext;
    }

    /**
     * @return OSGi Bundle context
     */
    public final BundleContext bundleContext() {
        return componentContext().getBundleContext();
    }

    /**
     * Registers a service in the mocked OSGi environment.
     * @param <T> Service type
     * @param service Service instance
     * @return Registered service instance
     */
    public final <T> T registerService(final T service) {
        return registerService(null, service, null);
    }

    /**
     * Registers a service in the mocked OSGi environment.
     * @param <T> Service type
     * @param serviceClass Service class
     * @param service Service instance
     * @return Registered service instance
     */
    public final <T> T registerService(final Class<T> serviceClass, final T service) {
        return registerService(serviceClass, service, null);
    }

    /**
     * Registers a service in the mocked OSGi environment.
     * @param <T> Service type
     * @param serviceClass Service class
     * @param service Service instance
     * @param properties Service properties (optional)
     * @return Registered service instance
     */
    public final <T> T registerService(final Class<T> serviceClass, final T service, final Map<String, Object> properties) {
        Dictionary<String, Object> serviceProperties = null;
        if (properties != null) {
            serviceProperties = new Hashtable<String, Object>(properties);
        }
        bundleContext().registerService(serviceClass != null ? serviceClass.getName() : null, service, serviceProperties);
        return service;
    }

    /**
     * Injects dependencies, activates and registers a service in the mocked
     * OSGi environment.
     * @param <T> Service type
     * @param service Service instance
     * @return Registered service instance
     */
    public final <T> T registerInjectActivateService(final T service) {
        return registerInjectActivateService(service, null);
    }

    /**
     * Injects dependencies, activates and registers a service in the mocked
     * OSGi environment.
     * @param <T> Service type
     * @param service Service instance
     * @param properties Service properties (optional)
     * @return Registered service instance
     */
    public final <T> T registerInjectActivateService(final T service, final Map<String, Object> properties) {
        MockOsgi.injectServices(service, bundleContext());
        MockOsgi.activate(service, bundleContext(), properties);
        registerService(null, service, properties);
        return service;
    }

    /**
     * Lookup a single service
     * @param <ServiceType> Service type
     * @param serviceType The type (interface) of the service.
     * @return The service instance, or null if the service is not available.
     */
    @SuppressWarnings("unchecked")
    public final <ServiceType> ServiceType getService(final Class<ServiceType> serviceType) {
        ServiceReference serviceReference = bundleContext().getServiceReference(serviceType.getName());
        if (serviceReference != null) {
            return (ServiceType)bundleContext().getService(serviceReference);
        } else {
            return null;
        }
    }

    /**
     * Lookup one or several services
     * @param <ServiceType> Service type
     * @param serviceType The type (interface) of the service.
     * @param filter An optional filter (LDAP-like, see OSGi spec)
     * @return The services instances or an empty array.
     * @throws RuntimeException If the <code>filter</code> string is not a valid OSGi service filter string.
     */
    @SuppressWarnings("unchecked")
    public final <ServiceType> ServiceType[] getServices(final Class<ServiceType> serviceType, final String filter) {
        try {
            ServiceReference[] serviceReferences = bundleContext().getServiceReferences(serviceType.getName(), filter);
            if (serviceReferences != null) {
                ServiceType[] services = (ServiceType[])Array.newInstance(serviceType, serviceReferences.length);
                for (int i = 0; i < serviceReferences.length; i++) {
                    services[i] = (ServiceType)bundleContext().getService(serviceReferences[i]);
                }
                return services;
            } else {
                return (ServiceType[])ArrayUtils.EMPTY_OBJECT_ARRAY;
            }
        } catch (InvalidSyntaxException ex) {
            throw new RuntimeException("Invalid filter syntax: " + filter, ex);
        }
    }

}
