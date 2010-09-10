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

import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/** Remove a bundle from a RegisteredResource.
 *  Creates a SynchronousRefreshPackagesTask when
 *  executed.
 */
public class BundleRemoveTask extends OsgiInstallerTask {

    private static final String BUNDLE_REMOVE_ORDER = "30-";

    private final BundleTaskCreator creator;

    public BundleRemoveTask(final RegisteredResource r,
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
            // nothing to do, so just stop
            this.getResource().setState(RegisteredResource.State.UNINSTALLED);
            return;
        }
        final int state = b.getState();
        try {
            if (state == Bundle.ACTIVE || state == Bundle.STARTING) {
            	b.stop();
            }
            b.uninstall();
            ctx.log("Uninstalled bundle {} from resource {}", b, getResource());
            this.getResource().setState(RegisteredResource.State.UNINSTALLED);
            ctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask(this.creator));
        } catch (final BundleException be) {
            this.getLogger().debug("Exception during removal of bundle " + this.getResource() + " : " + be.getMessage() + ". Retrying later.", be);
            ctx.addTaskToNextCycle(this);
        }
    }

    @Override
    public String getSortKey() {
        return BUNDLE_REMOVE_ORDER + getResource().getURL();
    }

}
