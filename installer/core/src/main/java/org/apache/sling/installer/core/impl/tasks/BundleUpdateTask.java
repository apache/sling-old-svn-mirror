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

    private static final String BUNDLE_UPDATE_ORDER = "40-";

    private boolean canRetry = true;

    public BundleUpdateTask(final TaskResourceGroup r,
                            final BundleTaskCreator creator) {
        super(r, creator);
    }

    /**
     * Check if the bundle is active.
     * This is true if the bundle has the active state or of the bundle
     * is in the starting state and has the lazy activation policy.
     * Or if the bundle is a fragment, it's considered active as well
     */
    protected boolean isBundleActive(final Bundle b) {
        if ( super.isBundleActive(b) ) {
            return true;
        }
        final StartLevel startLevelService = this.getStartLevel();
        return startLevelService.isBundlePersistentlyStarted(b);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(InstallationContext ctx) {
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

            b.update(getResource().getInputStream());
            ctx.log("Updated bundle {} from resource {}", b, getResource());

            if (reactivate) {
                if ( isSystemBundleFragment(b) ) {
                    this.setFinishedState(ResourceState.INSTALLED);
                    ctx.addTaskToCurrentCycle(new SystemBundleUpdateTask(null, this.getCreator()));
                } else if ( this.getFragmentHostHeader(b) != null ) {
                    // if this is a fragment, we're done after a refresh
                    this.getPackageAdmin().refreshPackages(new Bundle[] {b});
                    this.setFinishedState(ResourceState.INSTALLED);
                } else {
                    this.getResource().setAttribute(BundleTaskCreator.ATTR_START, "true");
                    ctx.addTaskToCurrentCycle(new BundleStartTask(this.getResourceGroup(), b.getBundleId(), this.getCreator()));
                }
            } else {
                this.setFinishedState(ResourceState.INSTALLED);
            }
            this.getLogger().debug("Bundle updated: {}/{}", b.getBundleId(), b.getSymbolicName());
    	} catch (Exception e) {
            if ( !canRetry ) {
                this.getLogger().warn("Removing failing tasks - unable to retry: " + this, e);
                this.setFinishedState(ResourceState.IGNORED);
            }
    	}
    }

    @Override
    public String getSortKey() {
        return BUNDLE_UPDATE_ORDER + getResource().getEntityId();
    }

    private boolean isSystemBundleFragment(final Bundle installedBundle) {
        final String fragmentHeader = this.getFragmentHostHeader(installedBundle);
        return fragmentHeader != null
            && fragmentHeader.indexOf(Constants.EXTENSION_DIRECTIVE) > 0;
    }
}