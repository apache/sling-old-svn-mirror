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
package org.apache.sling.installer.factories.deploypck.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.DeploymentAdmin;

public class Activator implements ServiceListener, BundleActivator {

    private static final String DEPLOYMENT_ADMIN = "org.osgi.service.deploymentadmin.DeploymentAdmin";

    /** The bundle context. */
    private BundleContext bundleContext;

    /** The service reference to the deployment admin. */
    private ServiceReference deploymentAdminReference;

    /** The service registration for the install service. */
    private ServiceRegistration serviceReg;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        this.bundleContext = context;
        this.getAdmin();
        this.bundleContext.addServiceListener(this, "(" + Constants.OBJECTCLASS
                + "=" + DEPLOYMENT_ADMIN + ")");

        }
        /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        this.bundleContext.removeServiceListener(this);
        this.unregister();
        this.bundleContext = null;
    }

    private void getAdmin() {
        this.deploymentAdminReference = this.bundleContext.getServiceReference(DEPLOYMENT_ADMIN);
        if ( this.deploymentAdminReference != null ) {
            final DeploymentAdmin deploymentAdmin = (DeploymentAdmin) this.bundleContext.getService(this.deploymentAdminReference);
            if ( deploymentAdmin == null ) {
                this.deploymentAdminReference = null;
            } else {
                final Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Installer Support for Deployment Packages");
                props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
                this.serviceReg = this.bundleContext.registerService(new String[] {ResourceTransformer.class.getName(),
                        InstallTaskFactory.class.getName()},
                    new DeploymentPackageInstaller(deploymentAdmin), props);
            }
        }
    }

    /**
     * Wait for the deployment admin service.
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public synchronized void serviceChanged(final ServiceEvent event) {
        if ( event.getType() == ServiceEvent.REGISTERED && this.deploymentAdminReference == null ) {
            this.getAdmin();
        } else if ( event.getType() == ServiceEvent.UNREGISTERING && this.deploymentAdminReference != null ) {
            this.unregister();
        }
    }

    private void unregister() {
        if ( this.deploymentAdminReference != null ) {
            this.bundleContext.ungetService(this.deploymentAdminReference);
            this.deploymentAdminReference = null;
        }
        if ( serviceReg != null ) {
            serviceReg.unregister();
            serviceReg = null;
        }
    }
}
