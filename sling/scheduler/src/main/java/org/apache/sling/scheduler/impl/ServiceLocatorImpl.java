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
package org.apache.sling.scheduler.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.services.InvalidServiceFilterSyntaxException;
import org.apache.sling.api.services.ServiceLocator;
import org.apache.sling.api.services.ServiceNotAvailableException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This is a default implementation of a {@link ServiceLocator}. We start with
 * a simple implementation just adding all references into a list.
 */
public class ServiceLocatorImpl implements ServiceLocator {

    protected final BundleContext bundleContext;

    /**
     * The list of references - we don't need to synchronize this as we are
     * running in one single request.
     */
    protected final List<ServiceReference> references = new ArrayList<ServiceReference>();

    /** A map of found services. */
    protected final Map<String, Object> services = new HashMap<String, Object>();

    public ServiceLocatorImpl(BundleContext ctx) {
        this.bundleContext = ctx;
    }

    /**
     * @see org.apache.sling.api.services.ServiceLocator#getRequiredService(java.lang.Class)
     */
    public <ServiceType> ServiceType getRequiredService(Class<ServiceType> type)
            throws ServiceNotAvailableException {
        final ServiceType service = this.getService(type);
        if (service == null) {
            throw new ServiceNotAvailableException("Service " + type.getName()
                + " is not available.");
        }
        return service;
    }

    /**
     * @see org.apache.sling.api.services.ServiceLocator#getService(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(Class<ServiceType> type) {
        ServiceType service = (ServiceType) this.services.get(type.getName());
        if (service == null) {
            final ServiceReference ref = this.bundleContext.getServiceReference(type.getName());
            if (ref != null) {
                this.references.add(ref);
                service = (ServiceType) this.bundleContext.getService(ref);
                this.services.put(type.getName(), service);
            }
        }
        return service;
    }

    /**
     * @see org.apache.sling.api.services.ServiceLocator#getServices(java.lang.Class,
     *      java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType[] getServices(
            Class<ServiceType> serviceType, String filter)
            throws InvalidServiceFilterSyntaxException {
        try {
            final ServiceReference[] refs = this.bundleContext.getServiceReferences(
                serviceType.getName(), filter);
            ServiceType[] result = null;
            if (refs != null) {
                final List<ServiceType> objects = new ArrayList<ServiceType>();
                for (int i = 0; i < refs.length; i++) {
                    this.references.add(refs[i]);
                    final ServiceType service = (ServiceType) this.bundleContext.getService(refs[i]);
                    if (service != null) {
                        objects.add(service);
                    }
                }
                if (objects.size() > 0) {
                    result = (ServiceType[]) objects.toArray();
                }
            }
            return result;
        } catch (InvalidSyntaxException ise) {
            throw new InvalidServiceFilterSyntaxException(
                "Invalid filter syntax: " + filter, ise);
        }
    }

    public void dispose() {
        final Iterator<ServiceReference> i = this.references.iterator();
        while (i.hasNext()) {
            final ServiceReference ref = i.next();
            this.bundleContext.ungetService(ref);
        }
        this.references.clear();
        this.services.clear();
    }
}
