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
import java.util.List;

import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.core.impl.AbstractInstallTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Checks for bundles with optional imports that are not wired.
 */
public class RefreshOptionalPackagesTask extends AbstractInstallTask {

    private static final String REFRESH_PACKAGES_ORDER = "80-";

    private static final String DIRECTIVE = "resolution";
    private static final String DIRECTIVE_OPTIONAL = "optional";
    private static final String MARKER = DIRECTIVE + ":=" + DIRECTIVE_OPTIONAL;

    /** Tracker for the package admin. */
    private final BundleTaskCreator bundleTaskCreator;

	public RefreshOptionalPackagesTask(final BundleTaskCreator bundleTaskCreator) {
	    super(null);
	    this.bundleTaskCreator = bundleTaskCreator;
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
    public void execute(final InstallationContext ctx) {
        getLogger().info("** Invoking refresh optional packages!");
        final PackageAdmin packageAdmin = this.getPackageAdmin();

        ExportedPackage[] exports = null;
        final List<Bundle> refreshBundles = new ArrayList<Bundle>();
        final Bundle[] bundles = this.bundleTaskCreator.getBundleContext().getBundles();
        for(final Bundle bundle : bundles) {
            if ( bundle.getState() == Bundle.RESOLVED || bundle.getState() == Bundle.ACTIVE ) {
                final String importHeader = (String)bundle.getHeaders().get(Constants.IMPORT_PACKAGE);
                if ( importHeader != null && importHeader.contains(MARKER) ) {
                    boolean needsRefresh = false;
                    final ManifestHeader mf = ManifestHeader.parse(importHeader);
                    for(final ManifestHeader.Entry entry : mf.getEntries()) {
                        if ( DIRECTIVE_OPTIONAL.equals(entry.getDirectiveValue(DIRECTIVE)) ) {
                            final String pkgName = entry.getValue();
                            getLogger().info("Found optional dependency {} in bundle {}", pkgName, bundle.getSymbolicName());
                            if ( exports == null ) {
                                exports = packageAdmin.getExportedPackages( ( Bundle ) null );
                                // just a sanity check
                                if ( exports == null ) {
                                    exports = new ExportedPackage[0];
                                }
                            }
                            boolean wiringFound = false;
                            boolean packageFound = false;
                            for(final ExportedPackage ep : exports) {
                                if ( ep.getName().equals(pkgName) ) {
                                    packageFound = true;
                                    final Bundle[] importingBundles = ep.getImportingBundles();
                                    for(final Bundle ib : importingBundles) {
                                        if ( ib.getBundleId() == bundle.getBundleId() ) {
                                            wiringFound = true;
                                            break;
                                        }
                                    }
                                }
                                if ( wiringFound ) {
                                    break;
                                }
                            }
                            if ( !wiringFound && packageFound ) {
                                getLogger().info("Found unresolved optional dependency {} in bundle {} which might now be resolvable", pkgName, bundle.getSymbolicName());
                                needsRefresh = true;
                                break;
                            }
                        }
                    }

                    if ( needsRefresh ) {
                        refreshBundles.add(bundle);
                    }
                }
            }
        }
        if ( refreshBundles.size() > 0 ) {
            getLogger().info("** Refreshing bundles {}", refreshBundles);
            packageAdmin.refreshPackages(refreshBundles.toArray(new Bundle[refreshBundles.size()]));
        }
        getLogger().info("** Finished refresh optional packages!");
	}
}
