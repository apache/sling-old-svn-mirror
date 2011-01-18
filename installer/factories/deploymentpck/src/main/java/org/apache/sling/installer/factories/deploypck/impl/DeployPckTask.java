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

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployPckTask extends InstallTask {

    private static final String INSTALL_ORDER = "55-";

    private final DeploymentAdmin deploymentAdmin;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DeployPckTask(final TaskResourceGroup grp, final DeploymentAdmin dp) {
        super(grp);
        this.deploymentAdmin = dp;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        final TaskResource tr = this.getResource();

        // get and check symbolic name
        final String symbolicName = (String)tr.getAttribute(DeploymentPackageInstaller.DEPLOYMENTPACKAGE_SYMBOLICMAME);
        if ( symbolicName == null ) {
            logger.error("Resource {} has no symbolic name - ignoring.", tr);
            this.getResourceGroup().setFinishState(ResourceState.IGNORED);
            return;
        }

        // get package if available
        final DeploymentPackage dp = this.deploymentAdmin.getDeploymentPackage(symbolicName);

        if ( tr.getState() == ResourceState.INSTALL) {
            InputStream is = null;
            try {
                is = tr.getInputStream();
                if ( is == null ) {
                    // something went wrong
                    logger.error("Resource {} does not provide an input stream!", tr);
                    this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                } else {
                    final Version newVersion = new Version((String)tr.getAttribute(DeploymentPackageInstaller.DEPLOYMENTPACKAGE_VERSION));
                    // check version
                    if ( dp != null ) {
                        final int compare = dp.getVersion().compareTo(newVersion);
                        if (compare < 0) {
                            // installed version is lower -> update
                            this.deploymentAdmin.installDeploymentPackage(is);
                            ctx.log("Installed deployment package {} : {}", symbolicName, newVersion);
                            this.getResourceGroup().setFinishState(ResourceState.INSTALLED);
                        } else if (compare >= 0) {
                            logger.debug("Deployment package " + symbolicName + " " + newVersion
                                        + " is not installed, package with higher or same version is already installed.");
                        }
                    } else {
                        this.deploymentAdmin.installDeploymentPackage(is);
                        ctx.log("Installed deployment package {} : {}", symbolicName, newVersion);
                        this.getResourceGroup().setFinishState(ResourceState.INSTALLED);
                    }
                }
            } catch (final DeploymentException e) {
                logger.error("Unable to install deployment package {} from resource {}",
                        symbolicName,
                        tr);
                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
            } catch (final IOException ioe) {
                logger.error("Unable to install deployment package {} from resource {}",
                        symbolicName,
                        tr);
                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
            } finally {
                if ( is != null ) {
                    try {
                        is.close();
                    } catch (IOException ignore) {}
                }
            }
        } else { // uninstall
            if ( dp != null ) {
                try {
                    dp.uninstall();
                } catch (final DeploymentException e) {
                    logger.error("Unable to uninstall deployment package {} from resource {}", symbolicName, tr);
                }
            } else {
                logger.info("Unable to find deployment package with symbolic name {} for uninstalling.",
                        symbolicName);
            }
            this.getResourceGroup().setFinishState(ResourceState.UNINSTALLED);
        }
    }

    @Override
    public String getSortKey() {
        return INSTALL_ORDER + getResource().getURL();
    }
}
