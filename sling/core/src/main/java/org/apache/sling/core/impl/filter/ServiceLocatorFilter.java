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
package org.apache.sling.core.impl.filter;

import java.io.IOException;

import org.apache.sling.component.ComponentContext;
import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.core.ServiceLocator;
import org.apache.sling.core.util.ServiceLocatorImpl;
import org.osgi.framework.BundleContext;

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
     * @see org.apache.sling.core.component.ComponentFilter#doFilter(org.apache.sling.core.component.ComponentRequest, org.apache.sling.core.component.ComponentResponse, org.apache.sling.core.component.ComponentFilterChain)
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
     * @see org.apache.sling.core.component.ComponentFilter#init(org.apache.sling.core.component.ComponentContext)
     */
    public void init(ComponentContext context) {
        // nothing to do
    }

    /**
     * @see org.apache.sling.core.component.ComponentFilter#destroy()
     */
    public void destroy() {
        // nothing to do
    }
}
