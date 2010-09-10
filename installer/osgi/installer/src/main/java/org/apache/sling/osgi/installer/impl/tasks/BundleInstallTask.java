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

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.startlevel.StartLevel;

/** Install a bundle supplied as a RegisteredResource.
 *  Creates a BundleStartTask to start the bundle */
public class BundleInstallTask extends OsgiInstallerTask {

    private static final String BUNDLE_INSTALL_ORDER = "50-";

    private final BundleTaskCreator creator;

    public BundleInstallTask(final RegisteredResource r,
            final BundleTaskCreator creator) {
        super(r);
        this.creator = creator;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerTask#execute(org.apache.sling.osgi.installer.impl.OsgiInstallerContext)
     */
    public void execute(final OsgiInstallerContext ctx) {
        int startLevel = 0;
        final Object providedLevel = (this.getResource().getDictionary() != null
            ? this.getResource().getDictionary().get(InstallableResource.BUNDLE_START_LEVEL) : null);
        if ( providedLevel != null ) {
            if ( providedLevel instanceof Number ) {
                startLevel = ((Number)providedLevel).intValue();
            } else {
                startLevel = Integer.valueOf(providedLevel.toString());
            }
        }
        // get the start level service (if possible) so we can set the initial start level
        final StartLevel startLevelService = this.creator.getStartLevel();
        try {
            final Bundle b = this.creator.getBundleContext().installBundle(getResource().getURL(), getResource().getInputStream());
            // optionally set the start level
            if ( startLevel > 0 ) {
                if (startLevelService != null) {
                    startLevelService.setBundleStartLevel(b, startLevel);
                } else {
                    this.getLogger().warn("Ignoring start level {} for bundle {} - start level service not available.",
                            startLevel, b);
                }
            }
            final Version newVersion = new Version((String)getResource().getAttributes().get(Constants.BUNDLE_VERSION));
            this.creator.getBundleDigestStorage().putInfo(b.getSymbolicName(), getResource().getDigest(), newVersion.toString());

            // mark this resource as installed and to be started
            this.getResource().getAttributes().put(BundleTaskCreator.ATTR_START, "true");
            ctx.addTaskToCurrentCycle(new BundleStartTask(getResource(), b.getBundleId(), this.creator));
        } catch (Exception ex) {
            // if something goes wrong we simply try it again
            this.getLogger().debug("Exception during install of bundle " + this.getResource() + " : " + ex.getMessage() + ". Retrying later.", ex);
            ctx.addTaskToNextCycle(this);
        }
    }

    @Override
    public String getSortKey() {
        return BUNDLE_INSTALL_ORDER + getResource().getURL();
    }
}
