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
package org.apache.sling.commons.threads.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * This activator registers the thread pool manager.
 */
public class Activator implements BundleActivator {

    /** The service registration for the thread pool manager. */
    private ServiceRegistration serviceReg;

    /** The thread pool manager. */
    private DefaultThreadPoolManager service;

    /** The bundle context. */
    private BundleContext bundleContext;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) {
        this.bundleContext = context;
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Thread Pool Manager");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_PID, DefaultThreadPool.class.getName() + ".factory");
        this.service = new DefaultThreadPoolManager(this.bundleContext, props);
        this.serviceReg = this.bundleContext.registerService(new String[] {ThreadPoolManager.class.getName(),
                ManagedServiceFactory.class.getName()}, service, props);

        WebConsolePrinter.initPlugin(this.bundleContext, this.service);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) {
        WebConsolePrinter.destroyPlugin();
        if ( this.serviceReg != null ) {
            this.serviceReg.unregister();
            this.serviceReg = null;
        }

        if ( this.service != null ) {
            this.service.destroy();
            this.service = null;
        }
        this.bundleContext = null;
    }
}
