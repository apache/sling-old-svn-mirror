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
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** Update a bundle from a RegisteredResource. Creates
 *  a bundleStartTask to restart the bundle if it was
 *   active before the update.
 */
public class BundleUpdateTask extends InstallTask {

    private static final String BUNDLE_UPDATE_ORDER = "40-";

    private boolean canRetry = true;

    private final BundleTaskCreator creator;

    public BundleUpdateTask(final TaskResourceGroup r,
                            final BundleTaskCreator creator) {
        super(r);
        this.creator = creator;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(InstallationContext ctx) {
        final String symbolicName = (String)getResource().getAttribute(Constants.BUNDLE_SYMBOLICNAME);
        final Bundle b = this.creator.getMatchingBundle(symbolicName);
        if (b == null) {
            this.getLogger().debug("Bundle to update ({}) not found", symbolicName);
            this.setFinishedState(ResourceState.IGNORED);
            return;
        }

        final Version newVersion = new Version((String)getResource().getAttribute(Constants.BUNDLE_VERSION));

        // Do not update if same version, unless snapshot
        boolean snapshot = false;
    	final Version currentVersion = new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
    	snapshot = this.creator.isSnapshot(newVersion);
    	if (currentVersion.equals(newVersion) && !snapshot) {
    	    // TODO : Isn't this already checked in the task creator?
    	    this.getLogger().debug("Same version is already installed, and not a snapshot, ignoring update: {}", getResource());
    	    this.setFinishedState(ResourceState.INSTALLED);
    		return;
    	}

    	try {
            // If the bundle is active before the update - restart it once updated, but
            // in sequence, not right now
            final boolean reactivate = BundleStartTask.isBundleActive(b);
            b.stop();

            b.update(getResource().getInputStream());
            ctx.log("Updated bundle {} from resource {}", b, getResource());

            if (reactivate) {
                this.getResource().setAttribute(BundleTaskCreator.ATTR_START, "true");
                ctx.addTaskToCurrentCycle(new BundleStartTask(this.getResourceGroup(), b.getBundleId(), this.creator));
            } else {
                this.setFinishedState(ResourceState.INSTALLED);
            }
            ctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask(this.creator));
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

}