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
package org.apache.sling.testing.mock.osgi;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.framework.FilterImpl;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.OsgiMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.ImmutableList;

/**
 * Mock {@link ServiceRegistration} implementation.
 */
class MockServiceRegistration implements ServiceRegistration, Comparable<MockServiceRegistration> {

    private static volatile long serviceCounter;

    private final Long serviceId;
    private final Set<String> clazzes;
    private final Object service;
    private Dictionary<String, Object> properties;
    private final ServiceReference serviceReference;
    private final MockBundleContext bundleContext;

    public MockServiceRegistration(final Bundle bundle, final String[] clazzes, final Object service,
            final Dictionary<String, Object> properties, MockBundleContext bundleContext) {
        this.serviceId = ++serviceCounter;
        this.clazzes = new HashSet<String>(ImmutableList.copyOf(clazzes));
        
        if (service instanceof ServiceFactory) {
            this.service = ((ServiceFactory)service).getService(bundleContext.getBundle(), this);
        }
        else {
            this.service = service;
        }
        
        this.properties = properties != null ? properties : new Hashtable<String,Object>();
        this.properties.put(Constants.SERVICE_ID, this.serviceId);
        this.properties.put(Constants.OBJECTCLASS, clazzes);
        this.serviceReference = new MockServiceReference(bundle, this);
        this.bundleContext = bundleContext;
        
        readOsgiMetadata();
    }

    @Override
    public ServiceReference getReference() {
        return this.serviceReference;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setProperties(final Dictionary properties) {
        this.properties = properties;
    }

    @Override
    public void unregister() {
        bundleContext.unregisterService(this);
    }

    Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    boolean matches(final String clazz, final String filter) throws InvalidSyntaxException {
        // ignore filter for now
        return this.clazzes.contains(clazz)
                && (filter == null || new FilterImpl(filter).match(properties));
    }
    
    Set<String> getClasses() {
        return clazzes;
    }

    Object getService() {
        return this.service;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MockServiceRegistration)) {
            return false;
        }
        return serviceId.equals(((MockServiceRegistration)obj).serviceId);
    }

    @Override
    public int hashCode() {
        return serviceId.hashCode();
    }

    @Override
    public int compareTo(MockServiceRegistration obj) {
        return serviceId.compareTo(obj.serviceId);
    }

    /**
     * Try to read OSGI-metadata from /OSGI-INF and read all implemented
     * interfaces and service properties
     */
    private void readOsgiMetadata() {
        Class<?> serviceClass = service.getClass();
        OsgiMetadata metadata = OsgiMetadataUtil.getMetadata(serviceClass);
        if (metadata == null) {
            return;
        }

        // add service interfaces from OSGi metadata
        clazzes.addAll(metadata.getServiceInterfaces());

        // add properties from OSGi metadata
        Map<String, Object> props = metadata.getProperties();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String toString() {
        return "#" + serviceId + " [" + StringUtils.join(clazzes, ",") + "]: " + service.toString();
    }

}
