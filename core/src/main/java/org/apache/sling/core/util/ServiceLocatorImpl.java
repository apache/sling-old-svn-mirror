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
package org.apache.sling.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.core.ServiceLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This is a default implementation of a {@link ServiceLocator].
 *
 * We start with a simple implementation just adding all references into a list.
 */
public class ServiceLocatorImpl implements ServiceLocator {

    protected final BundleContext bundleContext;

    /** The list of references - we don't need to synchronize this as we are running in one single request. */
    protected final List<ServiceReference> references = new ArrayList<ServiceReference>();

    /** A map of found services. */
    protected final Map<String, Object> services = new HashMap<String, Object>();

    public ServiceLocatorImpl(BundleContext ctx) {
        this.bundleContext = ctx;
    }

    /**
     * @see org.apache.sling.core.ServiceLocator#getService(java.lang.String, java.lang.String)
     */
    public Object[] getService(String serviceName, String filter) throws InvalidSyntaxException {
        final ServiceReference[] refs = this.bundleContext.getServiceReferences(serviceName, filter);
        Object[] result = null;
        if ( refs != null ) {
            final List<Object> objects = new ArrayList<Object>();
            for(int i=0; i<refs.length; i++ ) {
                this.references.add(refs[i]);
                final Object service = this.bundleContext.getService(refs[i]);
                if ( service != null) {
                    objects.add(service);
                }
            }
            if ( objects.size() > 0 ) {
                result = objects.toArray();
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.core.ServiceLocator#getService(java.lang.String)
     */
    public Object getService(String serviceName) {
        Object service = this.services.get(serviceName);
        if ( service == null ) {
            final ServiceReference ref = this.bundleContext.getServiceReference(serviceName);
            if ( ref != null ) {
                this.references.add(ref);
                service = this.bundleContext.getService(ref);
                this.services.put(serviceName, service);
            }
        }
        return service;
    }

    public void clear() {
        final Iterator i = this.references.iterator();
        while ( i.hasNext() ) {
            final ServiceReference ref = (ServiceReference)i.next();
            this.bundleContext.ungetService(ref);
        }
        this.references.clear();
        this.services.clear();
    }
}
