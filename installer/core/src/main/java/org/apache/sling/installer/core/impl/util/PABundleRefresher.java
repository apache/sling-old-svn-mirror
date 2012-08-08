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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation using the package admin
 */
public class PABundleRefresher implements BundleRefresher, FrameworkListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** Max time allowed to refresh packages */
    private static final int MAX_REFRESH_PACKAGES_WAIT_SECONDS = 90;

    /** Counter for package refresh events. */
    private volatile long refreshEventCount;

    private final PackageAdmin pckAdmin;

    private final BundleContext bundleContext;

    /** Lock object for syncing. */
    private  final Object lock = new Object();

    public PABundleRefresher(final PackageAdmin pa, final BundleContext bundleContext) {
        this.pckAdmin = pa;
        this.bundleContext = bundleContext;
    }

    private Set<String> getImportPackages(final Bundle bundle) {
        final ManifestHeader header = ManifestHeader.parse(bundle.getHeaders().get(Constants.IMPORT_PACKAGE).toString());
        final Set<String> packages = new HashSet<String>();
        for(final ManifestHeader.Entry entry : header.getEntries()) {
            packages.add(entry.getValue());
        }
        return packages;
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
                this.pckAdmin.refreshPackages(bundles.toArray(new Bundle[bundles.size()]));
            } else {
                this.refreshEventCount = -1;
                this.bundleContext.addFrameworkListener(this);
                try {
                    this.refreshEventCount = 0;
                    this.pckAdmin.refreshPackages(bundles.toArray(new Bundle[bundles.size()]));
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
                } finally {
                    this.bundleContext.removeFrameworkListener(this);
                }
            }
            ctx.log("Done refreshing {} bundles", bundles.size());
        }
    }

    /**
     * Check whether a bundle refresh would affect the installer bundle
     * @see org.apache.sling.installer.core.impl.util.BundleRefresher#isInstallerBundleAffected(java.util.List)
     */
    public boolean isInstallerBundleAffected(final List<Bundle> bundles) {
        // we put all bundle ids into a set
        final Set<Long> ids = new HashSet<Long>();
        for(final Bundle b : bundles) {
            ids.add(b.getBundleId());
        }

        final Set<Long> processed = new HashSet<Long>();
        final List<Bundle> toProcess = new ArrayList<Bundle>();
        toProcess.add(this.bundleContext.getBundle());
        processed.add(this.bundleContext.getBundle().getBundleId());

        while ( !toProcess.isEmpty() ) {
            final Bundle bundle = toProcess.remove(0);

            if ( ids.contains(bundle.getBundleId()) ) {
                return true;
            }

            for(final String name : this.getImportPackages(bundle) ) {

                final ExportedPackage[] pcks = this.pckAdmin.getExportedPackages(name);
                if ( pcks != null ) {
                    for(final ExportedPackage pck : pcks) {
                        final Bundle exportingBundle = pck.getExportingBundle();
                        if ( exportingBundle.getBundleId() == 0 || exportingBundle.getBundleId() == this.bundleContext.getBundle().getBundleId() ) {
                            continue;
                        }
                        if ( ids.contains(exportingBundle.getBundleId()) ) {
                            return true;
                        }
                        if ( !processed.contains(exportingBundle.getBundleId())) {
                            processed.add(exportingBundle.getBundleId());
                            toProcess.add(exportingBundle);
                        }
                    }
                }
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
