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

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/** Update a bundle from a RegisteredResource. Creates
 *  a bundleStartTask to restart the bundle if it was
 *   active before the update.
 */
public class BundleUpdateTask extends OsgiInstallerTask {

    private final RegisteredResource resource;
    
    public BundleUpdateTask(RegisteredResource r) {
        this.resource = r;
    }
    
    public RegisteredResource getResource() {
        return resource;
    }
    
    @Override 
    public String toString() {
        return getClass().getSimpleName() + ": " + resource;
    }
    
    @Override
    public void execute(OsgiInstallerContext ctx) throws Exception {
        super.execute(ctx);
        final String symbolicName = (String)resource.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        final Bundle b = TaskUtilities.getMatchingBundle(ctx.getBundleContext(), symbolicName);
        if(b == null) {
            throw new IllegalStateException("Bundle to update (" + symbolicName + ") not found");
        }
        if(b.getState() == Bundle.ACTIVE) {
            // bundle was active before the update - restart it once updated, but
            // in sequence, not right now
            ctx.addTaskToCurrentCycle(new BundleStartTask(b.getBundleId()));
        }
        b.stop();
        b.update(resource.getInputStream());
        if(ctx.getLogService() != null) {
            ctx.getLogService().log(LogService.LOG_DEBUG, "Bundle updated: " + b.getBundleId() + "/" + b.getSymbolicName());
        }
        ctx.incrementCounter(OsgiInstaller.OSGI_TASKS_COUNTER);
    }

    @Override
    public String getSortKey() {
        return TaskOrder.BUNDLE_UPDATE_ORDER + resource.getUrl();
    }

}
