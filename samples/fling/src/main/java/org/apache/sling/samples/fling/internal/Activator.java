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
package org.apache.sling.samples.fling.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration serviceRegistration;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        final Object service = new Object();
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Fling Sample “Sling Authentication Requirements”");
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        properties.put("sling.auth.requirements", "/fling/auth");
        serviceRegistration = bundleContext.registerService(Object.class, service, properties);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

}
