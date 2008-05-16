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
package org.apache.sling.engine.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.engine.SlingSettingsService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * This is the bundle activator for the Sling engine.
 * It registers the SlingSettingsService.
 *
 */
public class EngineBundleActivator implements BundleActivator {

    /** The service registration */
    private ServiceRegistration serviceRegistration;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        final Object service = new SlingSettingsServiceImpl(context);
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, service.getClass().getName());
        props.put(Constants.SERVICE_DESCRIPTION,
            "Apache Sling Settings Service");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        serviceRegistration = context.registerService(SlingSettingsService.class.getName(),
                                                      service,
                                                      props);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        if ( serviceRegistration != null ) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
