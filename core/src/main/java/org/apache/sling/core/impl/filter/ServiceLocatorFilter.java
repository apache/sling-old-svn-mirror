/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.impl.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.ServiceLocator;

/**
 * The <code>ServiceLocatorFilter</code> adds the service locator to the request
 * attributes. It should run as the first filter.
 *
 * @scr.component immediate="true"
 * @scr.property name="service.description" value="Service Locator Filter"
 * @scr.property name="filter.scope" value="request" private="true"
 * @scr.property name="filter.order" value="-3000" type="Integer" private="true"
 * @scr.service interface="org.apache.sling.component.ComponentFilter"
 */
public class ServiceLocatorFilter implements ComponentFilter {

    /** The bundle context. */
    protected BundleContext bundleContext;

    protected void activate(org.osgi.service.component.ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    protected void deactivate(org.osgi.service.component.ComponentContext context) {
        this.bundleContext = null;
    }

    /**
     * @see org.apache.sling.component.ComponentFilter#doFilter(org.apache.sling.component.ComponentRequest, org.apache.sling.component.ComponentResponse, org.apache.sling.component.ComponentFilterChain)
     */
    public void doFilter(ComponentRequest request,
                         ComponentResponse response,
                         ComponentFilterChain filterChain)
    throws IOException, ComponentException {
        // we create one locator instance per request
        final ServiceLocatorImpl locator = new ServiceLocatorImpl(this.bundleContext);
        request.setAttribute(ServiceLocator.REQUEST_ATTRIBUTE_NAME, locator);
        try {
            filterChain.doFilter(request, response);
        } finally {
            request.removeAttribute(ServiceLocator.REQUEST_ATTRIBUTE_NAME);
            locator.clear();
        }
    }

    /**
     * @see org.apache.sling.component.ComponentFilter#init(org.apache.sling.component.ComponentContext)
     */
    public void init(ComponentContext context) {
        // nothing to do
    }

    /**
     * @see org.apache.sling.component.ComponentFilter#destroy()
     */
    public void destroy() {
        // nothing to do
    }

    /**
     * We start with a simple implementation just adding all references into a list.
     */
    protected static final class ServiceLocatorImpl implements ServiceLocator {

        protected final BundleContext bundleContext;

        /** The list of references - we don't need to synchronize this as we are running in one single request. */
        protected final List references = new ArrayList();

        /** A map of found services. */
        protected final Map services = new HashMap();

        public ServiceLocatorImpl(BundleContext ctx) {
            this.bundleContext = ctx;
        }

        /**
         * @see org.apache.sling.ServiceLocator#getService(java.lang.String, java.lang.String)
         */
        public Object[] getService(String serviceName, String filter) throws InvalidSyntaxException {
            final ServiceReference[] refs = this.bundleContext.getServiceReferences(serviceName, filter);
            Object[] result = null;
            if ( refs != null ) {
                final List objects = new ArrayList();
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
         * @see org.apache.sling.ServiceLocator#getService(java.lang.String)
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
}
