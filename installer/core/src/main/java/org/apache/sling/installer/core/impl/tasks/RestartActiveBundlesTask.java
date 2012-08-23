/*
k * Licensed to the Apache Software Foundation (ASF) under one
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

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.apache.sling.installer.core.impl.PersistentResourceList;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Restart all active bundles.
 * This task is added to each installer cycle.
 */
public class RestartActiveBundlesTask extends AbstractInstallTask {

    private static final String SORT_KEY = "99-" + PersistentResourceList.RESTART_ACTIVE_BUNDLES_ID;

    private static final String ATTR = "bundles";

    /**
     * Constructor
     */
    public RestartActiveBundlesTask(final TaskResourceGroup erl, final TaskSupport support) {
        super(erl, support);
        // calculate bundle set
        @SuppressWarnings("unchecked")
        Set<Long> ids = (Set<Long>) erl.getActiveResource().getAttribute(ATTR);
        if ( ids == null ) {
            ids = new HashSet<Long>();
        }
        for(final Bundle bundle : support.getBundleContext().getBundles()) {
            if ( bundle.getBundleId() > 0 && BundleUtil.getFragmentHostHeader(bundle) == null && bundle.getState() == Bundle.ACTIVE ) {
                ids.add(bundle.getBundleId());
            }
        }
        erl.getActiveResource().setAttribute(ATTR, ids);
    }

    @Override
    public void execute(final InstallationContext ctx) {
        @SuppressWarnings("unchecked")
        final Set<Long> ids = (Set<Long>) this.getResource().getAttribute(ATTR);
        int started = 0;
        if ( ids != null ) {
            final Set<Long> remove = new HashSet<Long>();
            for(final Long id : ids) {
                final Bundle bundle = this.getBundleContext().getBundle(id);
                if ( bundle != null
                     && bundle.getState() != Bundle.ACTIVE
                     && bundle.getState() != Bundle.STARTING
                     && bundle.getState() != Bundle.STOPPING
                     && bundle.getState() != Bundle.UNINSTALLED) {
                    try {
                        bundle.start();
                        started++;
                        ctx.log("Started bundle {}", bundle);
                        remove.add(id);
                    } catch (final BundleException e) {
                        getLogger().info("Unable to start bundle {} : {}", bundle, e.getMessage());
                    }
                } else {
                    // bundle might be null(!)
                    getLogger().debug("Bundle does not need restart: {} (state {})", bundle, (bundle == null ? "uninstalled" : bundle.getState()));
                    remove.add(id);
                }
            }
            ids.removeAll(remove);
        }
        getLogger().debug("{} bundles were started", started);
    }

    @Override
    public String getSortKey() {
        return SORT_KEY;
    }
}
