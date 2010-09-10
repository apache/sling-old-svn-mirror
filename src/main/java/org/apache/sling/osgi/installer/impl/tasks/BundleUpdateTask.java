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
package org.apache.sling.osgi.installer.impl.tasks;

import java.io.InputStream;

import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** Update a bundle from a RegisteredResource. Creates
 *  a bundleStartTask to restart the bundle if it was
 *   active before the update.
 */
public class BundleUpdateTask extends OsgiInstallerTask {

    private static final String BUNDLE_UPDATE_ORDER = "40-";

    private boolean canRetry = true;

    private final BundleTaskCreator creator;

    public BundleUpdateTask(final RegisteredResource r,
                            final BundleTaskCreator creator) {
        super(r);
        this.creator = creator;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerTask#execute(org.apache.sling.osgi.installer.impl.OsgiInstallerContext)
     */
    public void execute(OsgiInstallerContext ctx) {
        final String symbolicName = (String)getResource().getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        final Bundle b = this.creator.getMatchingBundle(symbolicName);
        if (b == null) {
            this.getLogger().debug("Bundle to update ({}) not found", symbolicName);
            this.getResource().setState(RegisteredResource.State.IGNORED);
            return;
        }

        this.getResource().setState(RegisteredResource.State.INSTALLED);
        final Version newVersion = new Version((String)getResource().getAttributes().get(Constants.BUNDLE_VERSION));

        // check for system bundle update
        if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName) ) {
            this.creator.getBundleDigestStorage().putInfo(b.getSymbolicName(), getResource().getDigest(), newVersion.toString());
        }
        // Do not update if same version, unless snapshot
        boolean snapshot = false;
    	final Version currentVersion = new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
    	snapshot = this.creator.isSnapshot(newVersion);
    	if (currentVersion.equals(newVersion) && !snapshot) {
    	    // TODO : Isn't this already checked in the task creator?
    	    this.getLogger().debug("Same version is already installed, and not a snapshot, ignoring update: {}", getResource());
    		return;
    	}

    	try {
            // If snapshot and ready to update, cancel if digest didn't change - as the list
            // of RegisteredResources is not saved, this might not have been detected earlier,
            // if the snapshot was installed and the installer was later restarted
            if (snapshot) {
                final String oldDigest = this.creator.getBundleDigestStorage().getDigest(symbolicName);
                if (getResource().getDigest().equals(oldDigest)) {
                    this.getLogger().debug("Snapshot digest did not change, ignoring update: {}", getResource());
                    return;
                }
            }

            if (b.getState() == Bundle.ACTIVE) {
                // bundle was active before the update - restart it once updated, but
                // in sequence, not right now
                ctx.addTaskToCurrentCycle(new BundleStartTask(getResource(), b.getBundleId(), this.creator));
            }
            b.stop();
            final InputStream is = getResource().getInputStream();
            if(is == null) {
            	canRetry = false;
                throw new IllegalStateException(
                        "RegisteredResource provides null InputStream, cannot update bundle: "
                        + getResource());
            }
            b.update(is);
            this.creator.getBundleDigestStorage().putInfo(b.getSymbolicName(), getResource().getDigest(), newVersion.toString());
    	} catch (Exception e) {
            if ( canRetry ) {
                ctx.addTaskToNextCycle(this);
                return;
            }
            this.getLogger().warn("Removing failing tasks - unable to retry: " + this, e);
            return;
    	}
        ctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask(this.creator));
        this.getLogger().debug("Bundle updated: {}/{}", b.getBundleId(), b.getSymbolicName());
        return;
    }

    @Override
    public String getSortKey() {
        return BUNDLE_UPDATE_ORDER + getResource().getEntityId();
    }

}