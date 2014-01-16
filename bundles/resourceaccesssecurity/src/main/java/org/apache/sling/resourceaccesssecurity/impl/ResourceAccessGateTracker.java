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
package org.apache.sling.resourceaccesssecurity.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.commons.osgi.SortingServiceTracker;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ResourceAccessGateTracker extends
        SortingServiceTracker<ResourceAccessGate> {

    private List<ResourceAccessGateHandler> providerResourceAccessGateHandlers;
    private List<ResourceAccessGateHandler> applicationResourceAccessGateHandlers;

    /**
     * Constructor
     */
    public ResourceAccessGateTracker(final BundleContext context) {
        super(context, ResourceAccessGate.class.getName());
    }

    /**
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    @Override
    public void removedService(final ServiceReference reference, final Object service) {
        super.removedService(reference, service);
        this.clearCache();
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    @Override
    public void modifiedService(final ServiceReference reference, final Object service) {
        super.modifiedService(reference, service);
        this.clearCache();
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(final ServiceReference reference) {
        final Object returnValue = super.addingService(reference);
        this.clearCache();
        return returnValue;
    }

    private void clearCache() {
        this.providerResourceAccessGateHandlers = null;
        this.applicationResourceAccessGateHandlers = null;
    }

    public List<ResourceAccessGateHandler> getApplicationResourceAccessGateHandlers() {
        List<ResourceAccessGateHandler> returnValue = this.applicationResourceAccessGateHandlers;

        if (returnValue == null) {
            returnValue = new ArrayList<ResourceAccessGateHandler>();
            for (ServiceReference serviceReference : getSortedServiceReferences()) {
                final String context = (String) serviceReference.getProperty(ResourceAccessGate.CONTEXT);
                if ( ResourceAccessGate.APPLICATION_CONTEXT.equals(context) ) {
                    returnValue.add(new ResourceAccessGateHandler(serviceReference));
                }
            }
            returnValue = Collections.unmodifiableList(returnValue);
            this.applicationResourceAccessGateHandlers = returnValue;
        }

        return returnValue;
    }

    public List<ResourceAccessGateHandler> getProviderResourceAccessGateHandlers() {
        List<ResourceAccessGateHandler> returnValue = this.providerResourceAccessGateHandlers;

        if (returnValue == null) {
            returnValue = new ArrayList<ResourceAccessGateHandler>();
            for (ServiceReference serviceReference : getSortedServiceReferences()) {
                final String context = (String) serviceReference.getProperty(ResourceAccessGate.CONTEXT);
                if ( ResourceAccessGate.PROVIDER_CONTEXT.equals(context) || context == null || context.trim().length() == 0 ) {
                    returnValue.add(new ResourceAccessGateHandler(serviceReference));
                }
            }
            returnValue = Collections.unmodifiableList(returnValue);
            this.providerResourceAccessGateHandlers = returnValue;
        }

        return returnValue;
    }
}
