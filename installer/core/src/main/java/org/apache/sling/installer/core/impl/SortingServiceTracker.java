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
package org.apache.sling.installer.core.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implementation providing a sorted list of services
 * by service ranking.
 */
public class SortingServiceTracker<T>
    extends ServiceTracker  {

    private final OsgiInstallerImpl listener;

    private int lastCount = -1;

    private int lastRefCount = -1;

    private List<T> sortedServiceCache;

    private List<ServiceReference> sortedReferences;

    /**
     * Constructor
     */
    public SortingServiceTracker(final BundleContext context,
            final String clazz,
            final OsgiInstallerImpl listener) {
        super(context, clazz, null);
        this.listener = listener;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    @Override
    public void removedService(ServiceReference reference, Object service) {
        this.sortedServiceCache = null;
        this.sortedReferences = null;
        this.context.ungetService(reference);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        this.sortedServiceCache = null;
        this.sortedReferences = null;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(ServiceReference reference) {
        this.sortedServiceCache = null;
        this.sortedReferences = null;
        // new factory or resource transformer has been added, wake up main thread
        this.listener.wakeUp();
        return context.getService(reference);
    }

    /**
     * Return a sorted list of the services.
     */
    public List<T> getSortedServices() {
        if ( this.sortedServiceCache == null || this.lastCount < this.getTrackingCount() ) {
            this.lastCount = this.getTrackingCount();
            final ServiceReference[] references = this.getServiceReferences();
            if ( references == null || references.length == 0 ) {
                this.sortedServiceCache = Collections.emptyList();
            } else {
                Arrays.sort(references);
                this.sortedServiceCache = new ArrayList<T>();
                for(int i=0;i<references.length;i++) {
                    @SuppressWarnings("unchecked")
                    final T service = (T)this.getService(references[references.length - 1 - i]);
                    if ( service != null ) {
                        this.sortedServiceCache.add(service);
                    }
                }
            }
        }
        return this.sortedServiceCache;
    }

    /**
     * Return a sorted list of the services references.
     */
    public List<ServiceReference> getSortedServiceReferences() {
        if ( this.sortedReferences == null || this.lastRefCount < this.getTrackingCount() ) {
            this.lastRefCount = this.getTrackingCount();
            final ServiceReference[] references = this.getServiceReferences();
            if ( references == null || references.length == 0 ) {
                this.sortedReferences = Collections.emptyList();
            } else {
                Arrays.sort(references);
                this.sortedReferences = new ArrayList<ServiceReference>();
                for(int i=0;i<references.length;i++) {
                    this.sortedReferences.add(references[references.length - 1 - i]);
                }
            }
        }
        return this.sortedReferences;
    }
}
