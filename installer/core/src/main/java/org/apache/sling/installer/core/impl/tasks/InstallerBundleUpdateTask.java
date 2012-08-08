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

import java.util.Collections;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.osgi.framework.Bundle;

/**
 * Update the installer itself
 */
public class InstallerBundleUpdateTask extends AbstractInstallTask {

    private static final String BUNDLE_UPDATE_ORDER = "02-";

    private final Integer count;

    public InstallerBundleUpdateTask(final TaskResourceGroup r,
                                     final TaskSupport taskSupport) {
        super(r, taskSupport);
        this.count = (Integer)this.getResource().getAttribute(InstallTask.ASYNC_ATTR_NAME);
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(final InstallationContext ctx) {
        final Bundle b = this.getBundleContext().getBundle();
        if ( this.count == null ) {
            // first step: update bundle

            try {
                b.update(getResource().getInputStream());
                ctx.log("Updated bundle {} from resource {}", b, getResource());
            } catch (final Exception e) {
                getLogger().info("Removing failing tasks - unable to retry: " + this, e);
                this.setFinishedState(ResourceState.IGNORED);
                ctx.asyncTaskFailed(this);
            }
        } else if ( this.count == 1 ) {
            // second step: refresh
            this.getBundleRefresher().refreshBundles(ctx, Collections.singletonList(b), false);
        } else {
            // finished
            this.getResource().setAttribute(ASYNC_ATTR_NAME, null);
            this.setFinishedState(ResourceState.INSTALLED);
        }
    }

    @Override
    public boolean isAsynchronousTask() {
        return this.count == null || this.count == 1;
    }

    @Override
    public String getSortKey() {
        return BUNDLE_UPDATE_ORDER + getResource().getEntityId();
    }
}