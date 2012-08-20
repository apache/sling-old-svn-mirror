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
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Find out if a packages refresh will affect a given bundle */
class RefreshDependenciesUtil {

    private final PackageAdmin pckAdmin;

    public RefreshDependenciesUtil(final PackageAdmin pa) {
        this.pckAdmin = pa;
    }

    private Set<String> getImportPackages(final Bundle bundle) {
        final Set<String> packages = new HashSet<String>();
        final String imports = (String)bundle.getHeaders().get(Constants.IMPORT_PACKAGE);
        if(imports != null) {
            final ManifestHeader header = ManifestHeader.parse(imports);
            for(final ManifestHeader.Entry entry : header.getEntries()) {
                packages.add(entry.getValue());
            }
        }
        return packages;
    }

    /**
     * Check whether a packages refresh of the supplied bundles would affect targetBundle
     */
    boolean isBundleAffected(Bundle targetBundle, final List<Bundle> bundles) {
        // we put all bundle ids into a set
        final Set<Long> ids = new HashSet<Long>();
        for(final Bundle b : bundles) {
            ids.add(b.getBundleId());
        }

        final Set<Long> processed = new HashSet<Long>();
        final List<Bundle> toProcess = new ArrayList<Bundle>();
        toProcess.add(targetBundle);
        processed.add(targetBundle.getBundleId());

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
                        if ( exportingBundle.getBundleId() == 0 || exportingBundle.getBundleId() == targetBundle.getBundleId() ) {
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
}