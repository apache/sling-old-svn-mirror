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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Update the system bundle from a RegisteredResource.
 */
public class SystemBundleUpdateTask extends AbstractInstallTask {

    private static final String BUNDLE_UPDATE_ORDER = "01-";

    private static final String SYSTEM_BUNDLE_UPDATE_ORDER = BUNDLE_UPDATE_ORDER + "systembundle(0)";

    public SystemBundleUpdateTask(final TaskResourceGroup r,
            final TaskSupport taskSupport) {
        super(r, taskSupport);
    }

    @Override
    public void execute(final InstallationContext ctx) {
        final Bundle systemBundle = this.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        // sanity check
        if ( systemBundle == null ) {
            this.setFinishedState(ResourceState.IGNORED, null, "Cannot update system bundle!");
            ctx.asyncTaskFailed(this);
            return;
        }

        // restart system bundle
        if ( this.getResource() == null ) {
            ctx.log("Refreshing system bundle.");
            this.getBundleRefresher().refreshBundles(ctx, Collections.singletonList(systemBundle), false);
        } else {
            InputStream is = null;
            try {
                is = getResource().getInputStream();
                if (is == null) {
                    String message = MessageFormat.format("RegisteredResource provides null InputStream, cannot update system bundle: {0}", getResource());
                    getLogger().warn(message);
                    this.setFinishedState(ResourceState.IGNORED, null, message);
                    ctx.asyncTaskFailed(this);
                } else {
                    try {
                        systemBundle.update(is);
                    } catch (final BundleException e) {
                        String message = MessageFormat.format("Updating system bundle failed due to {0} - unable to retry: {1}", e.getLocalizedMessage(), this);
                        getLogger().warn(message, e);
                        this.setFinishedState(ResourceState.IGNORED, null, message);
                        ctx.asyncTaskFailed(this);
                    }
                }
            } catch (final IOException e) {
                String message = MessageFormat.format("Removing failing task due to  due to {0} - unable to retry: {1}", e.getLocalizedMessage(), this);
                this.getLogger().warn(message, e);
                this.setFinishedState(ResourceState.IGNORED, null, message);
                ctx.asyncTaskFailed(this);
            } finally {
                if ( is != null ) {
                    try {
                        is.close();
                    } catch (final IOException ignore) {}
                }
            }
        }
    }

    @Override
    public boolean isAsynchronousTask() {
        return true;
    }

    @Override
    public String getSortKey() {
        if ( getResource() == null ) {
            return SYSTEM_BUNDLE_UPDATE_ORDER;
        }
        return BUNDLE_UPDATE_ORDER + getResource().getURL();
    }
}