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

import org.apache.sling.osgi.installer.OsgiControllerServices;
import org.apache.sling.osgi.installer.impl.OsgiControllerTask;
import org.apache.sling.osgi.installer.impl.OsgiControllerTaskContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.log.LogService;

/** Task that executes an OSGi "refresh packages" operation, synchronously */
public class SynchronousRefreshPackagesTask extends OsgiControllerTask implements FrameworkListener {

    /** Max time allowed to refresh packages (TODO configurable??) */
    public static final int MAX_REFRESH_PACKAGES_WAIT_SECONDS = 30;

	private int packageRefreshEventsCount;
	private OsgiControllerServices ocs;

    /**
     * Handles the PACKAGES_REFRESHED framework event which is sent after
     * the PackageAdmin.refreshPackages has finished its work of refreshing
     * the packages. When packages have been refreshed all bundles which are
     * expected to be active (those active before refreshing the packages and
     * newly installed or updated bundles) are started.
     */
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
        	if(ocs.getLogService() != null) {
	    		ocs.getLogService().log(LogService.LOG_DEBUG,
	    				"FrameworkEvent.PACKAGES_REFRESHED");
        	}
        	packageRefreshEventsCount++;
        }
    }
    
	@Override
	public String getSortKey() {
		return TaskOrder.REFRESH_PACKAGES_ORDER;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public void execute(OsgiControllerTaskContext ctx) throws Exception {
        final int targetEventCount = packageRefreshEventsCount + 1;
        final long start = System.currentTimeMillis();
        final long timeout = System.currentTimeMillis() + MAX_REFRESH_PACKAGES_WAIT_SECONDS * 1000L;
        ocs = ctx.getOsgiControllerServices();

        // Refreshing packages might cause some bundles to be stopped,
        // make sure all currently active ones are restarted after
        // this task executes
    	for(Bundle b : ctx.getBundleContext().getBundles()) {
    		if(b.getState() == Bundle.ACTIVE) {
    			final OsgiControllerTask t = new BundleStartTask(b.getBundleId());
    			ctx.addTaskToCurrentCycle(t);
            	if(ctx.getOsgiControllerServices().getLogService() != null) {
            		ctx.getOsgiControllerServices().getLogService().log(LogService.LOG_DEBUG, 
            				"Added " + t + " to restart bundle if needed after refreshing packages"); 
            	}
    		}
    	}

        // It seems like (at least with Felix 1.0.4) we won't get a FrameworkEvent.PACKAGES_REFRESHED
        // if one happened very recently and there's nothing to refresh
        ctx.getPackageAdmin().refreshPackages(null);
        ctx.getBundleContext().addFrameworkListener(this);
        try {
            while(true) {
                if(System.currentTimeMillis() > timeout) {
                	if(ocs.getLogService() != null) {
        	    		ocs.getLogService().log(LogService.LOG_WARNING,
        	    				"No FrameworkEvent.PACKAGES_REFRESHED event received within "
        	    				+ MAX_REFRESH_PACKAGES_WAIT_SECONDS
        	    				+ " seconds after refresh");
                	}
                    break;
                }
                if(packageRefreshEventsCount >= targetEventCount) {
                    final long delta = System.currentTimeMillis() - start;
                    if(ocs.getLogService() != null) {
        	    		ocs.getLogService().log(LogService.LOG_DEBUG,
        	    				"FrameworkEvent.PACKAGES_REFRESHED received "
        	    				+ delta
        	    				+ " msec after refreshPackages call");
                    }
                    break;
                }
                try {
                    Thread.sleep(250L);
                } catch(InterruptedException ignore) {
                }
            }
        } finally {
        	ctx.getBundleContext().removeFrameworkListener(this);
        }
	}
}
