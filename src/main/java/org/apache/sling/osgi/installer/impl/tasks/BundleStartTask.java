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

import java.text.DecimalFormat;

import org.apache.sling.osgi.installer.impl.EntityResourceList;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerImpl;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/** Start a bundle given its bundle ID
 *  Restarts if the bundle does not start on the first try,
 *  but only after receiving a bundle or framework event,
 *  indicating that it's worth retrying
 */
public class BundleStartTask extends OsgiInstallerTask {

    private static final String BUNDLE_START_ORDER = "70-";

    private final long bundleId;
	private final String sortKey;
	private long eventsCountForRetrying;
	private int retryCount = 0;

	private final BundleTaskCreator creator;

	public BundleStartTask(final EntityResourceList r, final long bundleId, final BundleTaskCreator btc) {
	    super(r);
		this.bundleId = bundleId;
		this.creator = btc;
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

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerTask#execute(org.apache.sling.osgi.installer.impl.OsgiInstallerContext)
	 */
	public void execute(final OsgiInstallerContext ctx) {
	    // this is just a sanity check which should never be reached
        if (bundleId == 0) {
            this.getLogger().debug("Bundle 0 is the framework bundle, ignoring request to start it");
            if ( this.getResource() != null ) {
                this.setFinishedState(RegisteredResource.State.INSTALLED);
            }
            return;
        }

        // Do not execute this task if waiting for events
        final long eventsCount = OsgiInstallerImpl.getTotalEventsCount();
        if (eventsCount < eventsCountForRetrying) {
            this.getLogger().debug("Task is not executable at this time, counters={}/{}",
                    eventsCountForRetrying, eventsCount);
            if ( this.getResource() == null ) {
                ctx.addTaskToNextCycle(this);
            }
            return;
        }

        final Bundle b = this.creator.getBundleContext().getBundle(bundleId);
		if (b == null) {
		    this.getLogger().info("Cannot start bundle, id not found: {}", bundleId);
			return;
		}

        if (b.getState() == Bundle.ACTIVE) {
            this.getLogger().debug("Bundle already started, no action taken: {}/{}", bundleId, b.getSymbolicName());
            if ( this.getResource() != null ) {
                this.setFinishedState(RegisteredResource.State.INSTALLED);
            }
            return;
        }
        // Try to start bundle, and if that doesn't work we'll need to retry
        try {
            b.start();
            if ( this.getResource() != null ) {
                this.setFinishedState(RegisteredResource.State.INSTALLED);
            }
            this.getLogger().info("Bundle started (retry count={}, bundle ID={}) : {}",
                    new Object[] {retryCount, bundleId, b.getSymbolicName()});
        } catch(BundleException e) {
            this.getLogger().info("Could not start bundle (retry count={}, bundle ID={}) : {}. Reason: {}. Will retry.",
                    new Object[] {retryCount, bundleId, b.getSymbolicName(), e});

            // Do the first retry immediately (in case "something" happenened right now
            // that warrants a retry), but for the next ones wait for at least one bundle
            // event or framework event
            if (retryCount == 0) {
                eventsCountForRetrying = OsgiInstallerImpl.getTotalEventsCount();
            } else {
                eventsCountForRetrying = OsgiInstallerImpl.getTotalEventsCount() + 1;
            }
            retryCount++;
            if ( this.getResource() == null ) {
                ctx.addTaskToNextCycle(this);
            }
        }
	}
}
