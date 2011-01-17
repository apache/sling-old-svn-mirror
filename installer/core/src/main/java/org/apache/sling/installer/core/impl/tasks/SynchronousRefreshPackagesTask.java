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
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.PackageAdmin;

/** Execute an OSGi "refresh packages" operation, synchronously */
public class SynchronousRefreshPackagesTask extends AbstractInstallTask implements FrameworkListener {

    /** Tracker for the package admin. */
    private final BundleTaskCreator bundleTaskCreator;

    private static final String REFRESH_PACKAGES_ORDER = "60-";

    /** Max time allowed to refresh packages (TODO configurable??) */
    public static final int MAX_REFRESH_PACKAGES_WAIT_SECONDS = 30;

	private volatile int packageRefreshEventsCount;

	public SynchronousRefreshPackagesTask(final BundleTaskCreator btc) {
	    super(null);
	    this.bundleTaskCreator = btc;
	}

    /**
     * Handles the PACKAGES_REFRESHED framework event which is sent after
     * the PackageAdmin.refreshPackages has finished its work of refreshing
     * the packages. When packages have been refreshed all bundles which are
     * expected to be active (those active before refreshing the packages and
     * newly installed or updated bundles) are started.
     */
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
    	    this.getLogger().debug("FrameworkEvent.PACKAGES_REFRESHED");
        	packageRefreshEventsCount++;
        }
    }

	@Override
	public String getSortKey() {
		return REFRESH_PACKAGES_ORDER;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

    private PackageAdmin getPackageAdmin() {
        return this.bundleTaskCreator.getPackageAdmin();
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    public void execute(InstallationContext ctx) {
        final int targetEventCount = packageRefreshEventsCount + 1;
        final long start = System.currentTimeMillis();
        final long timeout = System.currentTimeMillis() + MAX_REFRESH_PACKAGES_WAIT_SECONDS * 1000L;

        // Refreshing packages might cause some bundles to be stopped,
        // make sure all currently active ones are restarted after
        // this task executes
    	for(final Bundle b : this.bundleTaskCreator.getBundleContext().getBundles()) {
    	    if ( BundleStartTask.isBundleActive(b) ) {
    	        final InstallTask t = new BundleStartTask(null, b.getBundleId(), this.bundleTaskCreator);
    			ctx.addTaskToCurrentCycle(t);
    			this.getLogger().debug("Added {} to restart bundle if needed after refreshing packages", t);
    		}
    	}

        // It seems like (at least with Felix 1.0.4) we won't get a FrameworkEvent.PACKAGES_REFRESHED
        // if one happened very recently and there's nothing to refresh
        this.bundleTaskCreator.getBundleContext().addFrameworkListener(this);
        try {
            this.getPackageAdmin().refreshPackages(null);
            while(true) {
                if(System.currentTimeMillis() > timeout) {
                    this.getLogger().warn("No FrameworkEvent.PACKAGES_REFRESHED event received within {}"
        	    				+ " seconds after refresh", MAX_REFRESH_PACKAGES_WAIT_SECONDS);
                    break;
                }
                if(packageRefreshEventsCount >= targetEventCount) {
                    final long delta = System.currentTimeMillis() - start;
                    this.getLogger().debug("FrameworkEvent.PACKAGES_REFRESHED received {}"
        	    				+ " msec after refreshPackages call", delta);
                    break;
                }
                try {
                    Thread.sleep(250L);
                } catch(InterruptedException ignore) {
                }
            }
        } finally {
        	this.bundleTaskCreator.getBundleContext().removeFrameworkListener(this);
        }
        return;
	}
}
