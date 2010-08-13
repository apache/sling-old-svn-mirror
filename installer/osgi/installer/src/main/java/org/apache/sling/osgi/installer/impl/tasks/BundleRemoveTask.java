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

    private final RegisteredResource resource;

    private final BundleTaskCreator creator;

    public BundleRemoveTask(final RegisteredResource r,
            final BundleTaskCreator creator) {
        this.creator = creator;
        this.resource = r;
    }

    @Override
    public String toString() {
    	return getClass().getSimpleName() + ": " + resource;
    }

    @Override
    public Result execute(OsgiInstallerContext ctx) {
        logExecution();
        final String symbolicName = (String)resource.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        final Bundle b = this.creator.getMatchingBundle(symbolicName);
        if(b == null) {
            throw new IllegalStateException("Bundle to remove (" + symbolicName + ") not found");
        }
        final int state = b.getState();
        try {
            if (state == Bundle.ACTIVE || state == Bundle.STARTING) {
            	b.stop();
            }
            b.uninstall();
        } catch (BundleException be) {
            ctx.addTaskToNextCycle(this);
            return Result.NOTHING;
        }
        ctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask(this.creator));
        return Result.SUCCESS;
    }

    @Override
    public String getSortKey() {
        return BUNDLE_REMOVE_ORDER + resource.getURL();
    }

}
