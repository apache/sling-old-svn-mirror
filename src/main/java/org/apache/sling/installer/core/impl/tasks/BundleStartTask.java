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

import java.text.DecimalFormat;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Start a bundle given its bundle ID
 * Restarts if the bundle does not start on the first try,
 * but only after receiving a bundle or framework event,
 * indicating that it's worth retrying
 */
public class BundleStartTask extends AbstractBundleTask {

    private static final String BUNDLE_START_ORDER = "70-";

    private final long bundleId;
    private final String sortKey;

    public BundleStartTask(final TaskResourceGroup r, final long bundleId, final TaskSupport btc) {
        super(r, btc);
        this.bundleId = bundleId;
        this.sortKey = BUNDLE_START_ORDER + new DecimalFormat("00000").format(bundleId);
    }

    @Override
    public String getSortKey() {
        return sortKey;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": bundle " + bundleId;
    }

    @Override
    public void setFinishedState(final ResourceState state) {
        if ( this.getResource() != null ) {
            BundleUtil.clearBundleStart(this.getResource());
        }
        super.setFinishedState(state);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(final InstallationContext ctx) {
        // this is just a sanity check which should never be reached
        if (bundleId == 0) {
            this.getLogger().debug("Bundle 0 is the framework bundle, ignoring request to start it");
            if ( this.getResource() != null ) {
                this.setFinishedState(ResourceState.INSTALLED);
            }
            return;
        }

        // and another sanity check
        final Bundle b = this.getBundleContext().getBundle(bundleId);
        if (b == null) {
            this.getLogger().debug("Cannot start bundle, id not found: {}", bundleId);
            this.setFinishedState(ResourceState.IGNORED);
            return;
        }

        if (BundleUtil.isBundleActive(b) ) {
            this.getLogger().debug("Bundle already started, no action taken: {}", b);
            this.setFinishedState(ResourceState.INSTALLED);
        } else {
            // Try to start bundle, and if that doesn't work we'll need to retry
            try {
                b.start();
                this.setFinishedState(ResourceState.INSTALLED);
                ctx.log("Started bundle {}", b);
            } catch (final BundleException e) {
                this.getLogger().info("Could not start bundle {}. Reason: {}. Will retry.",
                        new Object[] {b, e});
            }
        }
    }
}
