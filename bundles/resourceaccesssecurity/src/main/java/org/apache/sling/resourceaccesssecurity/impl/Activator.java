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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.security.ResourceAccessSecurity;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    /** Tracker for all resource access gate services. */
    private ResourceAccessGateTracker resourceAccessGateTracker;

    private ServiceRegistration appReg;

    private ServiceRegistration provReg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        this.resourceAccessGateTracker = new ResourceAccessGateTracker(context);
        this.resourceAccessGateTracker.open();

        final Dictionary<String, Object> appProps = new Hashtable<String, Object>();
        appProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Application Resource Access Security");

        this.appReg = context.registerService(ResourceAccessSecurity.class.getName(),
                new ResourceAccessSecurityImpl(this.resourceAccessGateTracker, true), appProps);

        final Dictionary<String, Object> provProps = new Hashtable<String, Object>();
        provProps.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Provider Resource Access Security");

        this.provReg = context.registerService(ResourceAccessSecurity.class.getName(),
                new ResourceAccessSecurityImpl(this.resourceAccessGateTracker, false), provProps);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        if ( this.appReg != null ) {
            this.appReg.unregister();
            this.appReg = null;
        }
        if ( this.provReg != null ) {
            this.provReg.unregister();
            this.provReg = null;
        }
        if ( this.resourceAccessGateTracker != null ) {
            this.resourceAccessGateTracker.close();
            this.resourceAccessGateTracker = null;
        }
    }


}
