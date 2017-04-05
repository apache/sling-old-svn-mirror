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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.osgi.framework.Bundle;

/**
 * Refresh a set of bundles.
 */
public class RefreshBundlesTask
    extends AbstractBundleTask {

    private static final String REFRESH_PACKAGES_ORDER = "60-";

    /** Global set of bundles to refresh. */
    private static final Set<Long> BUNDLE_IDS = new HashSet<Long>();

    public static void markBundleForRefresh(final InstallationContext ctx,
                    final TaskSupport btc,
                    final Bundle bundle) {
        synchronized ( BUNDLE_IDS ) {
            BUNDLE_IDS.add(bundle.getBundleId());
            ctx.addTaskToCurrentCycle(new RefreshBundlesTask(btc));
        }
    }

    public RefreshBundlesTask(final TaskSupport btc) {
	    super(null, btc);
	}

	@Override
	public String getSortKey() {
		return REFRESH_PACKAGES_ORDER;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTask#execute(org.apache.sling.installer.api.tasks.InstallationContext)
     */
    @Override
    public void execute(final InstallationContext ctx) {
        boolean doFullRefresh = false;
        final List<Bundle> bundles = new ArrayList<Bundle>();
        synchronized ( BUNDLE_IDS ) {
            for(final Long id : BUNDLE_IDS) {
                final Bundle b = this.getBundleContext().getBundle(id);
                if ( b != null ) {
                    getLogger().debug("Will refresh bundle {}", b);
                    bundles.add(b);
                } else {
                    getLogger().debug("Bundle {} is already gone. Full refresh", id);
                    doFullRefresh = true;
                }
            }
            BUNDLE_IDS.clear();
        }
        if ( doFullRefresh || bundles.size() > 0 ) {
            // check if the installer bundle is affected
            if ( !this.getBundleRefresher().isInstallerBundleAffected(bundles) ) {
                this.getBundleRefresher().refreshBundles(ctx, (doFullRefresh ? null : bundles), true);
            } else {
                ctx.log("Installer bundle is affected by bundle refresh, initiating asynchronous refresh");
                ctx.addTaskToCurrentCycle(new AsyncRefreshBundlesTask(this.getTaskSupport(), (doFullRefresh ? null : bundles)));
            }
        }
	}

    private final class AsyncRefreshBundlesTask extends AbstractBundleTask {

        private final List<Bundle> bundles;

        public AsyncRefreshBundlesTask(final TaskSupport btc, final List<Bundle> bundles) {
            super(null, btc);
            this.bundles = bundles;
        }

        @Override
        public void execute(final InstallationContext ctx) {
            this.getBundleRefresher().refreshBundles(ctx, bundles, false);
        }

        @Override
        public String getSortKey() {
            return "07-";
        }

        @Override
        public boolean isAsynchronousTask() {
            return true;
        }
    }
}
