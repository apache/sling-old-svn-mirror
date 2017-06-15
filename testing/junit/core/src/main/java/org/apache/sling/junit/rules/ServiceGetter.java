/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.junit.rules;

import java.io.Closeable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/** Implements the logic used to get a service */
class ServiceGetter<T> implements Closeable {

    private final ServiceTracker tracker;
    private final BundleContext bundleContext;

    public static <T> ServiceGetter<T> create(BundleContext bundleContext, Class<T> serviceClass, String ldapFilter) {
        return new ServiceGetter<T>(bundleContext, serviceClass, ldapFilter);
    }

    @SuppressWarnings("unchecked")
    private ServiceGetter(BundleContext bundleContext, Class<T> serviceClass, String ldapFilter) {
        if (serviceClass.equals(BundleContext.class)) {
            // Special case to provide the BundleContext to tests
            this.bundleContext = bundleContext;
            this.tracker = null;
        } else {
            this.bundleContext = null;
            final String classFilter = String.format("(%s=%s)", Constants.OBJECTCLASS, serviceClass.getName());
            final String combinedFilter;
            if (ldapFilter == null || ldapFilter.trim().length() == 0) {
                combinedFilter = classFilter;
            } else {
                combinedFilter = String.format("(&%s%s)", classFilter, ldapFilter);
            }
            final Filter filter;
            try {
                filter = FrameworkUtil.createFilter(combinedFilter);
                tracker = new ServiceTracker(bundleContext, filter, null);
                tracker.open();
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Syntax of argument ldapFilter is invalid", e);
            }
        }
    }

    public T getService() {
        if (tracker == null) {
            return (T)bundleContext;
        } else {
            return (T)tracker.getService();
        }
    }

    public T getService(long timeout) throws InterruptedException {
        if (tracker == null) {
            return (T)bundleContext;
        } else {
            return (T)tracker.waitForService(timeout);
        }
    }

    @Override
    public void close() {
        if (tracker != null) {
            tracker.close();
        }
    }
}