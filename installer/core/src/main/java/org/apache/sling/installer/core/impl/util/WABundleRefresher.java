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
package org.apache.sling.installer.core.impl.util;

import java.util.Collection;
import java.util.List;

import org.apache.sling.installer.api.tasks.InstallationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the bundle refresher based on framework wiring.
 */
public class WABundleRefresher implements BundleRefresher, FrameworkListener {

    private final FrameworkWiring frameworkWiring;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Max time allowed to refresh packages */
    private static final int MAX_REFRESH_PACKAGES_WAIT_SECONDS = 90;

    /** Counter for package refresh events. */
    private volatile long refreshEventCount;

    /** Lock object for syncing. */
    private  final Object lock = new Object();

    private final BundleContext bundleContext;

    public WABundleRefresher(final FrameworkWiring wiring, final BundleContext bundleContext) {
        this.frameworkWiring = wiring;
        this.bundleContext = bundleContext;
    }

    /**
     * @see org.apache.sling.installer.core.impl.util.BundleRefresher#refreshBundles(org.apache.sling.installer.api.tasks.InstallationContext, java.util.List, boolean)
     */
    public void refreshBundles(final InstallationContext ctx,
            final List<Bundle> bundles,
            final boolean wait) {
        if ( bundles.size() > 0 ) {
            ctx.log("Refreshing {} bundles: {}", bundles.size(), bundles);
            if ( !wait ) {
                this.frameworkWiring.refreshBundles(bundles);
            } else {
                this.refreshEventCount = 0;
                this.frameworkWiring.refreshBundles(bundles, this);
                final long end = System.currentTimeMillis() + (MAX_REFRESH_PACKAGES_WAIT_SECONDS * 1000);
                do {
                    synchronized ( this.lock ) {
                        final long waitTime = end - System.currentTimeMillis();
                        if ( this.refreshEventCount < 1 && waitTime > 0 ) {
                            try {
                                ctx.log("Waiting up to {} seconds for bundles refresh", MAX_REFRESH_PACKAGES_WAIT_SECONDS);
                                this.lock.wait(waitTime);
                            } catch (final InterruptedException ignore) {
                                // ignore
                            }
                            if ( end <= System.currentTimeMillis() && this.refreshEventCount < 1 ) {
                                logger.warn("No FrameworkEvent.PACKAGES_REFRESHED event received within {}"
                                        + " seconds after refresh, aborting wait.",
                                        MAX_REFRESH_PACKAGES_WAIT_SECONDS);
                                this.refreshEventCount++;
                            }
                        }
                    }
                } while ( this.refreshEventCount < 1);
            }
            ctx.log("Done refreshing {} bundles", bundles.size());
        }
    }

    /**
     * @see org.apache.sling.installer.core.impl.util.BundleRefresher#isInstallerBundleAffected(java.util.List)
     */
    public boolean isInstallerBundleAffected(final List<Bundle> bundles) {
        final long installerId = this.bundleContext.getBundle().getBundleId();
        final Collection<Bundle> dependencyClosure = this.frameworkWiring.getDependencyClosure(bundles);
        for(final Bundle b : dependencyClosure) {
            if ( b.getBundleId() == installerId ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public void frameworkEvent(final FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
            logger.debug("FrameworkEvent.PACKAGES_REFRESHED");
            synchronized (this.lock) {
                this.refreshEventCount++;
                this.lock.notify();
            }
        }
    }
}
