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
package org.apache.sling.launcher.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.felix.framework.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * This is the deployment package installer.
 * It is a delayed service that runs as soon as the framework is started
 * and the deployment admin is available.
 * It looks then in {@link BootstrapInstaller#PATH_BUNDLES} for
 * deloyment packages and tries to install/update them.
 *
 */
public class DeploymentPackageInstaller implements ServiceListener, FrameworkListener {

    public static final String DEPLOYMENT_ADMIN = DeploymentAdmin.class.getName();

    public static final String EXTENSION = ".dp";

    public static final String DATA_FILE = "dpi.data";

    private boolean frameworkStarted = false;
    private boolean serviceAvailable = false;
    private boolean deployed = false;

    private final BundleContext bundleContext;
    private final Logger logger;
    private final ResourceProvider resourceProvider;

    private DeploymentAdmin deploymentAdmin;
    private ServiceReference deploymentAdminReference;

    public DeploymentPackageInstaller(final BundleContext bundleContext,
            Logger logger, ResourceProvider resourceProvider) {
        this.logger = logger;
        this.resourceProvider = resourceProvider;
        this.bundleContext = bundleContext;
    }

    /**
     * Wait for the deployment admin service.
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public synchronized void serviceChanged(ServiceEvent event) {
        if ( event.getType() == ServiceEvent.REGISTERED && !this.deployed ) {
            this.deploymentAdminReference = this.bundleContext.getServiceReference(DEPLOYMENT_ADMIN);
            this.deploymentAdmin = (DeploymentAdmin) this.bundleContext.getService(this.deploymentAdminReference);
            this.serviceAvailable = true;
            if ( this.frameworkStarted ) {
                this.deploy();
            }
        }

    }

    /**
     * Wait for the framework start.
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public synchronized void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED && !this.deployed) {
            this.frameworkStarted = true;
            if ( this.serviceAvailable ) {
                this.deploy();
            }
        }

    }

    /**
     * Deploy the deployment packages.
     */
    private void deploy() {
        ArrayList<String> installedPcks = null;
        final File dataFile = this.bundleContext.getDataFile(DATA_FILE);
        if ( dataFile != null && dataFile.exists() ) {
            try {
                final FileInputStream fis = new FileInputStream(dataFile);
                try {
                    final ObjectInputStream ois = new ObjectInputStream(fis);
                    try {
                        installedPcks = (ArrayList<String>) ois.readObject();
                    } catch (ClassNotFoundException e) {
                        // this can never happen so we just log
                        logger.log(Logger.LOG_ERROR, "Class not found!", e);
                    } finally {
                        try {
                            ois.close();
                        } catch (IOException ignore) {}
                    }
                } finally {
                    try {
                        fis.close();
                    } catch (IOException ignore) {}
                }
            } catch (IOException ioe) {
                logger.log(Logger.LOG_ERROR, "IOException during reading of deployed packages.", ioe);
            }
        }
        try {
            Iterator<String> res = resourceProvider.getChildren(BootstrapInstaller.PATH_BUNDLES);
            while (res.hasNext()) {

                String path = res.next();

                if ( path.endsWith(EXTENSION) ) {
                    // check if we already installed this
                    final int pos = path.lastIndexOf('/');
                    final String name = path.substring(pos + 1);

                    if ( installedPcks != null && installedPcks.contains(name) ) {
                        continue;
                    }

                    // install this as a deployment package
                    final InputStream ins = resourceProvider.getResourceAsStream(path);
                    if (ins == null) {
                        continue;
                    }
                    try {
                        this.deploymentAdmin.installDeploymentPackage(ins);
                        logger.log(Logger.LOG_INFO, "Deployment Package "
                                + " installed from " + path);
                    } catch (DeploymentException e) {
                        logger.log(Logger.LOG_ERROR, "Deployment Package installation from "
                                + path + " failed", e);
                    }
                    if ( installedPcks == null ) {
                        installedPcks = new ArrayList<String>();
                    }
                    installedPcks.add(name);
                }
            }

        } catch (Throwable t) {
            logger.log(Logger.LOG_ERROR, "Unexpected error during package deployment.", t);
        }
        // update status
        if ( installedPcks != null ) {
            try {
                final FileOutputStream fos = new FileOutputStream(dataFile);
                try {
                    final ObjectOutputStream oos = new ObjectOutputStream(fos);
                    try {
                        oos.writeObject(installedPcks);
                    } finally {
                        try {
                            oos.close();
                        } catch (IOException ignore) {}
                    }
                } finally {
                    try {
                        fos.close();
                    } catch (IOException ignore) {}
                }
            } catch (IOException ioe) {
                logger.log(Logger.LOG_ERROR, "IOException during writing deployed packages.", ioe);
            }
        }
        // now clean up
        this.deployed = true;
        this.bundleContext.ungetService(this.deploymentAdminReference);
        this.bundleContext.removeFrameworkListener(this);
        this.bundleContext.removeServiceListener(this);
        this.deploymentAdmin = null;
        this.deploymentAdminReference = null;
    }
}
