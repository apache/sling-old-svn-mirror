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

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.osgi.installer.impl.Activator;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/** Start a bundle given its bundle ID
 *  Restarts if the bundle does not start on the first try,
 *  but only after receiving a bundle or framework event,
 *  indicating that it's worth retrying
 */
public class BundleStartTask extends OsgiInstallerTask {

	private final long bundleId;
	private final String sortKey;
	private long eventsCountForRetrying;
	private int retryCount = 0;

	public BundleStartTask(long bundleId) {
		this.bundleId = bundleId;
		sortKey = TaskOrder.BUNDLE_START_ORDER + new DecimalFormat("00000").format(bundleId);
	}

	@Override
	public String getSortKey() {
		return sortKey;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": bundle " + bundleId;
	}

	public void execute(OsgiInstallerContext ctx) throws Exception {
		final Bundle b = ctx.getBundleContext().getBundle(bundleId);
		boolean needToRetry = false;

        if(bundleId == 0) {
            ctx.logDebug("Bundle 0 is the framework bundle, ignoring request to start it");
            return;
        }

		if(b == null) {
		    ctx.logInfo("Cannot start bundle, id not found:" + bundleId);
			return;
		}

		try {
	        if(b.getState() == Bundle.ACTIVE) {
	            ctx.logDebug("Bundle already started, no action taken:" + bundleId + "/" + b.getSymbolicName());
	        } else {
	            // Try to start bundle, and if that doesn't work we'll need to retry
	            logExecution(ctx);
	            try {
	                b.start();
	                ctx.logInfo("Bundle started (retry count=" + retryCount + ", bundle ID=" + bundleId + ") " + b.getSymbolicName());
	            } catch(BundleException e) {
	                ctx.logInfo("Could not start bundle (retry count=" + retryCount + ", " + e
	                            + "), will retry: " + bundleId + "/" + b.getSymbolicName());
	                needToRetry = true;
	            }

	        }
		} finally {
	        if(needToRetry) {

	            // Do the first retry immediately (in case "something" happenened right now
	            // that warrants a retry), but for the next ones wait for at least one bundle
	            // event or framework event
	            if(retryCount == 0) {
	                eventsCountForRetrying = Activator.getTotalEventsCount();
	            } else {
                    eventsCountForRetrying = Activator.getTotalEventsCount() + 1;
	            }

	            ctx.addTaskToNextCycle(this);
	        }
		}
		retryCount++;
		ctx.incrementCounter(OsgiInstaller.OSGI_TASKS_COUNTER);
	}

	/** Do not execute this task if waiting for events */
    public boolean isExecutable(OsgiInstallerContext tctx) {
        final long eventsCount = Activator.getTotalEventsCount();
        final boolean result = eventsCount >= eventsCountForRetrying;
        if(!result) {
            tctx.logDebug(this + " is not executable at this time, counters=" + eventsCountForRetrying + "/" + eventsCount);
        }
        return result;
    }
}
