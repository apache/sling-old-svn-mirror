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

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.Bundle;
import org.osgi.service.startlevel.StartLevel;

/**
 * Install a bundle supplied as a RegisteredResource.
 * Creates a BundleStartTask to start the bundle.
 */
public class BundleInstallTask extends AbstractBundleTask {

    private static final String BUNDLE_INSTALL_ORDER = "40-";

    public BundleInstallTask(final TaskResourceGroup r,
            final TaskSupport creator) {
        super(r, creator);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    @Override
    public void execute(final InstallationContext ctx) {
        final int startLevel = this.getBundleStartLevel();
        try {
            final Bundle b = this.getBundleContext().installBundle(getResource().getURL(), getResource().getInputStream());
            ctx.log("Installed bundle {} from resource {}", b, getResource());
            // optionally set the start level
            if ( startLevel > 0 ) {
                // get the start level service (if possible) so we can set the initial start level
                final StartLevel startLevelService = this.getStartLevel();
                if (startLevelService != null) {
                    startLevelService.setBundleStartLevel(b, startLevel);
                    ctx.log("Set start level for bundle {} to {}", b, startLevel);
                } else {
                    this.getLogger().info("Ignoring start level {} for bundle {} - start level service not available.",
                            startLevel, b);
                }
            }

            // fragment?
            if ( BundleUtil.isSystemBundleFragment(b) ) {
                // first install of a system fragment does not need a refresh of the host
                // so we can just set the state and are done.
                this.setFinishedState(ResourceState.INSTALLED);
            } else {
                final String fragmentHostHeader = BundleUtil.getFragmentHostHeader(b);
                if (fragmentHostHeader != null) {
                    this.getLogger().debug("Need to do a refresh of the bundle's {} host", b);
                    for (final Bundle bundle : this.getBundleContext().getBundles()) {
                        if (fragmentHostHeader.equals(bundle.getSymbolicName())) {
                            this.getLogger().debug("Found host bundle for {} to refresh: {}", b, bundle);
                            RefreshBundlesTask.markBundleForRefresh(ctx, this.getTaskSupport(), bundle);
                            break;
                        }
                    }

                    this.setFinishedState(ResourceState.INSTALLED);
                } else {

                    // mark this resource as to be started
                    BundleUtil.markBundleStart(getResource());
                    ctx.addTaskToCurrentCycle(new BundleStartTask(getResourceGroup(), b.getBundleId(), this.getTaskSupport()));
                }
            }
        } catch (final Exception ex) {
            // if something goes wrong we simply try it again
            this.getLogger().info("Exception during install of bundle " + this.getResource() + " : " + ex.getMessage() + ". Retrying later.", ex);
        }
    }

    @Override
    public String getSortKey() {
        return BUNDLE_INSTALL_ORDER + getSortableStartLevel() + "-" + getResource().getURL();
    }
}
