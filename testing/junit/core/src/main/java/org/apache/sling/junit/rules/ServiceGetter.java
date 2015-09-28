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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/** Implements the logic used to get a service */
class ServiceGetter {
    final ServiceReference serviceReference;
    final Object service;

    @SuppressWarnings("unchecked")
    ServiceGetter(BundleContext bundleContext, Class<?> serviceClass, String ldapFilter) {
        Object s;
        
        if (serviceClass.equals(BundleContext.class)) {
            // Special case to provide the BundleContext to tests
            s = serviceClass.cast(bundleContext);
            serviceReference = null;
        } else {
            if(ldapFilter != null && !ldapFilter.isEmpty()) {
                try {
                    final ServiceReference [] services = bundleContext.getServiceReferences(serviceClass.getName(), ldapFilter);
                    if(services == null) {
                        serviceReference = null;
                    } else {
                        // Prefer services which have a higher ranking
                        final List<ServiceReference> sorted = Arrays.asList(services);
                        Collections.sort(sorted);
                        serviceReference = sorted.get(sorted.size() - 1);
                    }
                } catch (InvalidSyntaxException e) {
                    throw new IllegalStateException("Invalid filter syntax:" + ldapFilter, e);
                }
            } else {
                serviceReference = bundleContext.getServiceReference(serviceClass.getName());
            }
            
            if (serviceReference == null) {
                throw new IllegalStateException(
                        "unable to get a service reference, class=" + serviceClass.getName() + ", filter='" + ldapFilter + "'");
            }

            final Object service = bundleContext.getService(serviceReference);

            if (service == null) {
                throw new IllegalStateException(
                        "unable to get an instance of the service");
            }

            s = serviceClass.cast(service);
        }
        
        service = s;
    }
}