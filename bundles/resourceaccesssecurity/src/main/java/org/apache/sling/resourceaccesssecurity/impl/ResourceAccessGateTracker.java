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

    private List<ResourceAccessGateHandler> resourceAccessGateHandlers;

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
    public void removedService(ServiceReference reference, Object service) {
        super.removedService(reference, service);
        resourceAccessGateHandlers = null;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        super.modifiedService(reference, service);
        resourceAccessGateHandlers = null;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(ServiceReference reference) {
        Object returnValue = super.addingService(reference);
        resourceAccessGateHandlers = null;
        return returnValue;
    }

    public List<ResourceAccessGateHandler> getResourceAccessGateHandlers() {
        List<ResourceAccessGateHandler> returnValue = resourceAccessGateHandlers;

        if (returnValue == null) {
            resourceAccessGateHandlers = new ArrayList<ResourceAccessGateHandler>();
            for (ServiceReference serviceReference : getSortedServiceReferences()) {
                resourceAccessGateHandlers.add(new ResourceAccessGateHandler(
                        serviceReference));
            }
            resourceAccessGateHandlers = Collections
                    .unmodifiableList(resourceAccessGateHandlers);
            returnValue = resourceAccessGateHandlers;
        }

        return returnValue;
    }

}
