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

package org.apache.sling.commons.metrics;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/** Utility that provides a MetricsService to any class that
 *  has been loaded from an OSGi bundle.
 *  This is meant to make it as easy to access the MetricsService
 *  as it is to get a Logger, for example.
 */
public class MetricsServiceFactory {
    
    /** Provide a MetricsService mapped to the Bundle that loaded class c 
     *  @param c a Class loaded by an OSGi bundle
     *  @return a MetricsService
     */
    public static MetricsService getMetricsService(Class<?> c) {
        if(c == null) {
            throw new IllegalArgumentException("Class parameter is required");
        }
        
        final Bundle b = FrameworkUtil.getBundle(c);
        if(b == null) {
            throw new IllegalArgumentException("No BundleContext, Class was not loaded from a Bundle?: " 
                    + c.getClass().getName());
        }
        
        final BundleContext ctx = b.getBundleContext();

        // In theory we should unget this reference, but the OSGi framework
        // ungets all references held by a bundle when it stops and we cannot
        // do much better than that anyway.
        final ServiceReference ref = ctx.getServiceReference(MetricsService.class.getName());
        if(ref == null) {
            throw new IllegalStateException("MetricsService not found for Bundle "
                    + b.getSymbolicName());
        }
        
        return (MetricsService)ctx.getService(ref);
    }
}