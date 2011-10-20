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

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.apache.sling.installer.core.impl.OsgiInstallerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update the system bundle from a RegisteredResource.
 */
public class SystemBundleUpdateTask extends AbstractInstallTask {

    private static final String BUNDLE_UPDATE_ORDER = "99-";

    private final BundleTaskCreator creator;

    public SystemBundleUpdateTask(final TaskResourceGroup r,
            final BundleTaskCreator creator) {
        super(r);
        this.creator = creator;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        final Bundle systemBundle = this.creator.getBundleContext().getBundle(0);
        // sanity check
        if ( systemBundle == null ) {
            return;
        }

        // restart system bundle
        if ( this.getResource() == null ) {
            // do an async update
            ctx.addTaskToNextCycle(new InstallTask(this.getResourceGroup()) {

                private final Logger logger = LoggerFactory.getLogger(this.getClass());

                @Override
                public String getSortKey() {
                    return OsgiInstallerImpl.ASYNC_TASK_KEY;
                }

                @Override
                public void execute(final InstallationContext ctx) {
                    try {
                        systemBundle.update();
                    } catch (final BundleException e) {
                        logger.warn("Updating system bundle failed - unable to retry: " + this, e);
                    }
                }
            });
        } else {
            InputStream is = null;
            try {
                is = getResource().getInputStream();
                if (is == null) {
                    throw new IllegalStateException(
                            "RegisteredResource provides null InputStream, cannot update bundle: "
                            + getResource());
                }
                // delayed system bundle update
                final InputStream backgroundIS = is;
                is = null;
                ctx.addTaskToNextCycle(new InstallTask(this.getResourceGroup()) {

                    private final Logger logger = LoggerFactory.getLogger(this.getClass());

                    @Override
                    public String getSortKey() {
                        return OsgiInstallerImpl.ASYNC_TASK_KEY;
                    }

                    @Override
                    public void execute(final InstallationContext ctx) {
                        try {
                            systemBundle.update(backgroundIS);
                        } catch (final BundleException e) {
                            logger.warn("Updating system bundle failed - unable to retry: " + this, e);
                        } finally {
                            try {
                                backgroundIS.close();
                            } catch (IOException ignore) {}
                        }
                    }
                });
            } catch (final IOException e) {
                this.getLogger().warn("Removing failing tasks - unable to retry: " + this, e);
                this.setFinishedState(ResourceState.IGNORED);
            } finally {
                if ( is != null ) {
                    try {
                        is.close();
                    } catch (IOException ignore) {}
                }
            }
        }
    }

    @Override
    public String getSortKey() {
        return BUNDLE_UPDATE_ORDER + getResource().getURL();
    }
}