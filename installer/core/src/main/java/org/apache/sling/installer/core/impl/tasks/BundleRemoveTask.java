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
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/** Remove a bundle from a RegisteredResource.
 *  Creates a SynchronousRefreshPackagesTask when
 *  executed.
 */
public class BundleRemoveTask extends InstallTask {

    private static final String BUNDLE_REMOVE_ORDER = "30-";

    private final BundleTaskCreator creator;

    public BundleRemoveTask(final TaskResourceGroup r,
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
            // nothing to do, so just stop
            this.setFinishedState(ResourceState.IGNORED);
            return;
        }
        final int state = b.getState();
        try {
            if (state == Bundle.ACTIVE || state == Bundle.STARTING) {
            	b.stop();
            }
            b.uninstall();
            ctx.log("Uninstalled bundle {} from resource {}", b, getResource());
            this.setFinishedState(ResourceState.UNINSTALLED);
            ctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask(this.creator));
        } catch (final BundleException be) {
            this.getLogger().debug("Exception during removal of bundle " + this.getResource() + " : " + be.getMessage() + ". Retrying later.", be);
        }
    }

    @Override
    public String getSortKey() {
        return BUNDLE_REMOVE_ORDER + getResource().getURL();
    }

}
