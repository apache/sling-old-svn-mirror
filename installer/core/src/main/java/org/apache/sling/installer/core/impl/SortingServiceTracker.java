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

import org.apache.sling.installer.api.tasks.RetryHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class SortingServiceTracker<T> extends
        org.apache.sling.commons.osgi.SortingServiceTracker<T> {

    private final RetryHandler listener;

    /**
     * Constructor
     */
    public SortingServiceTracker(final BundleContext context,
            final String clazz,
            final RetryHandler listener) {
        super(context, clazz);
        this.listener = listener;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(ServiceReference reference) {
        Object returnValue = super.addingService(reference);
        if ( listener != null ) {
            // new factory or resource transformer has been added, wake up main thread
            this.listener.scheduleRetry();
        }
        return returnValue;
    }

    /**
     * Check if a service with the given name is registered.
     * @param name Name
     * @return {@code true} if it exists, {@code false} otherwise.
     */
    public boolean check(final String key, final String name) {
        final ServiceReference[] refs = this.getServiceReferences();
        if ( refs != null ) {
            for(final ServiceReference ref : refs) {
                final Object val = ref.getProperty(key);
                if ( name.equals(val) ) {
                    return true;
                }
            }
        }
        return false;
    }


}
