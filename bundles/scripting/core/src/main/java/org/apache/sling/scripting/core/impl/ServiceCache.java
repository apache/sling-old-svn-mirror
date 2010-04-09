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
package org.apache.sling.scripting.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class ServiceCache implements ServiceListener {

    private final BundleContext bundleContext;

    private static final Reference NULL_REFERENCE = new Reference();

    private final ConcurrentHashMap<String, Reference> cache = new ConcurrentHashMap<String, Reference>();

    /**
     * The list of references - we don't need to synchronize this as we are
     * running in one single request.
     */
    protected final List<ServiceReference> references = new ArrayList<ServiceReference>();

    public ServiceCache(final BundleContext ctx) {
        this.bundleContext = ctx;
        this.bundleContext.addServiceListener(this);
    }

    public void dispose() {
        this.bundleContext.removeServiceListener(this);
        for(final Reference ref : cache.values()) {
            if ( ref != NULL_REFERENCE ) {
                this.bundleContext.ungetService(ref.reference);
            }
        }
    }

    /**
     * Return a service for the given service class.
     * @param <ServiceType> The service class / interface
     * @param type The requested service
     * @return The service or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(Class<ServiceType> type) {
        final String key = type.getName();
        Reference reference = this.cache.get(key);
        if (reference == null ) {
            // if the service is not in the cache, we have to go into a synchronized
            // block to avoid concurrent gets of the same service
            synchronized (this) {
                reference = this.cache.get(key);
                if ( reference == null ) {
                    // try to get service from bundle context
                    final ServiceReference ref = this.bundleContext.getServiceReference(key);
                    if (ref != null) {
                        final Object service = this.bundleContext.getService(ref);
                        if ( service != null ) {
                            reference = new Reference();
                            reference.service = service;
                            reference.reference = ref;
                        }
                    }
                    if ( reference == null ) {
                        reference = NULL_REFERENCE;
                    }
                    this.cache.put(key, reference);
                }
            }

        }
        if ( reference == NULL_REFERENCE ) {
            return null;
        }
        return (ServiceType) reference.service;
    }

    /**
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent event) {
        final String[] objectClasses = (String[])event.getServiceReference().getProperty(Constants.OBJECTCLASS);
        if ( objectClasses != null) {
            for(final String key : objectClasses) {
                Reference ref = null;
                synchronized ( this ) {
                    ref = this.cache.remove(key);
                }
                if ( ref != null && ref != NULL_REFERENCE ) {
                    this.bundleContext.ungetService(ref.reference);
                }
            }
        }
    }

    protected static final class Reference {
        public ServiceReference reference;
        public Object           service;
    }
}
