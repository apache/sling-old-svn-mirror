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
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.startlevel.StartLevel;

/** Update a bundle from a RegisteredResource. Creates
 *  a bundleStartTask to restart the bundle if it was
 *   active before the update.
 */
public class BundleUpdateTask extends AbstractBundleTask {

    private static final String BUNDLE_UPDATE_ORDER = "50-";

    public BundleUpdateTask(final TaskResourceGroup r,
                            final TaskSupport creator) {
        super(r, creator);
    }

    /**
     * Check if the bundle is active.
     * This is true if the bundle has the active state or of the bundle
     * is in the starting state and has the lazy activation policy.
     * Or if the bundle is a fragment, it's considered active as well
     */
    private boolean isBundleActive(final Bundle b) {
        if ( BundleUtil.isBundleActive(b) ) {
            return true;
        }
        final StartLevel startLevelService = this.getStartLevel();
        return startLevelService.isBundlePersistentlyStarted(b);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(final InstallationContext ctx) {
        final String symbolicName = (String)getResource().getAttribute(Constants.BUNDLE_SYMBOLICNAME);
        final Bundle b = BundleInfo.getMatchingBundle(this.getBundleContext(), symbolicName, null);
        if (b == null) {
            this.getLogger().debug("Bundle to update ({}) not found", symbolicName);
            this.setFinishedState(ResourceState.IGNORED);
            return;
        }

        final Version newVersion = new Version((String)getResource().getAttribute(Constants.BUNDLE_VERSION));

        // Do not update if same version, unless snapshot
        boolean snapshot = false;
    	final Version currentVersion = new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
    	snapshot = BundleInfo.isSnapshot(newVersion);
    	if (currentVersion.equals(newVersion) && !snapshot) {
    	    // TODO : Isn't this already checked in the task creator?
    	    this.getLogger().debug("Same version is already installed, and not a snapshot, ignoring update: {}", getResource());
    	    this.setFinishedState(ResourceState.INSTALLED);
    		return;
    	}

    	try {
            // If the bundle is active before the update - restart it once updated, but
            // in sequence, not right now
            final boolean reactivate = this.isBundleActive(b);
            // if this is not a fragment, stop the bundle
            final int state = b.getState();
            if (state == Bundle.ACTIVE || state == Bundle.STARTING) {
                b.stop();
            }

            // update bundle
            b.update(getResource().getInputStream());
            ctx.log("Updated bundle {} from resource {}", b, getResource());

            // start level handling - after update to avoid starting the bundle
            // just before the update
            final StartLevel startLevelService = this.getStartLevel();
            if ( startLevelService != null ) {
                final int newStartLevel = this.getBundleStartLevel();
                final int oldStartLevel = startLevelService.getBundleStartLevel(b);
                if ( newStartLevel != oldStartLevel && newStartLevel != 0 ) {
                    startLevelService.setBundleStartLevel(b, newStartLevel);
                    ctx.log("Set start level for bundle {} to {}", b, newStartLevel);
                }
            }

            if (reactivate) {
                if ( BundleUtil.isSystemBundleFragment(b) ) {
                    this.setFinishedState(ResourceState.INSTALLED);
                    ctx.addTaskToCurrentCycle(new SystemBundleUpdateTask(null, this.getTaskSupport()));
                } else if ( BundleUtil.getFragmentHostHeader(b) != null ) {
                    // if this is a fragment, we're done after a refresh of the host
                    final String fragmentHostHeader = BundleUtil.getFragmentHostHeader(b);
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
                    BundleUtil.markBundleStart(this.getResource());
                    RefreshBundlesTask.markBundleForRefresh(ctx, this.getTaskSupport(), b);
                    ctx.addTaskToCurrentCycle(new BundleStartTask(this.getResourceGroup(), b.getBundleId(), this.getTaskSupport()));
                }
            } else {
                this.setFinishedState(ResourceState.INSTALLED);
            }
    	} catch (final Exception e) {
            this.getLogger().info("Removing failing update task - unable to retry: " + this, e);
            this.setFinishedState(ResourceState.IGNORED);
    	}
    }

    @Override
    public String getSortKey() {
        return BUNDLE_UPDATE_ORDER + getSortableStartLevel() + "-" + getResource().getEntityId();
    }

}
