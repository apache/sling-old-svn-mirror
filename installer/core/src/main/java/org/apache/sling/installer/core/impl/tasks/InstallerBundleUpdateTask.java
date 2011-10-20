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
package org.apache.sling.installer.core.impl.tasks;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.apache.sling.installer.core.impl.OsgiInstallerImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update the installer itself
 */
public class InstallerBundleUpdateTask extends AbstractInstallTask {

    private static final String BUNDLE_UPDATE_ORDER = "98-";

    private final BundleTaskCreator creator;

    public InstallerBundleUpdateTask(final TaskResourceGroup r,
                            final BundleTaskCreator creator) {
        super(r);
        this.creator = creator;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(final InstallationContext ctx) {
        final Bundle b = this.creator.getBundleContext().getBundle();
        final PackageAdmin pa = this.creator.getPackageAdmin();

        ctx.addTaskToNextCycle(new InstallTask(this.getResourceGroup()) {

            private final Logger logger = LoggerFactory.getLogger(this.getClass());

            @Override
            public String getSortKey() {
                return OsgiInstallerImpl.ASYNC_TASK_KEY;
            }

            @Override
            public void execute(final InstallationContext ctx) {
                try {
                    b.update(getResource().getInputStream());
                    ctx.log("Updated bundle {} from resource {}", b, getResource());

                    if ( pa != null ) {
                        // wait for asynchronous bundle start tasks to finish
                        try {
                            Thread.sleep(2000L);
                        } catch (final InterruptedException ignore) {
                            // just ignore
                        }

                        pa.refreshPackages( new Bundle[] { b } );
                    }
                    logger.debug("Bundle updated: {}/{}", b.getBundleId(), b.getSymbolicName());
                } catch (final Exception e) {
                    logger.warn("Removing failing tasks - unable to retry: " + this, e);
                }
            }
        });
    }

    @Override
    public String getSortKey() {
        return BUNDLE_UPDATE_ORDER + getResource().getEntityId();
    }
}