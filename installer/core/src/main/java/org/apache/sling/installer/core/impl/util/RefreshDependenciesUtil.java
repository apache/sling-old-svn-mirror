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

    private final Logger log = LoggerFactory.getLogger(getClass());
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
        final Set<Long> checkedId = new HashSet<Long>();
        final boolean result = isBundleAffected(checkedId, targetBundle, bundles);
        log.debug("isBundleAffected({}, {}) -> {}", new Object[] { targetBundle, bundles, result});
        return result;
    }
    
    private boolean isBundleAffected(Set<Long> checkedId, Bundle targetBundle, final List<Bundle> bundles) {
        
        // Avoid cycles
        if(checkedId.contains(targetBundle.getBundleId())) {
            return false;
        }
        checkedId.add(targetBundle.getBundleId());
        log.debug("Checking if {} is affected by {}", targetBundle, bundles);
        
        // ID of bundles that we check against
        final Set<Long> ids = new HashSet<Long>();
        for(final Bundle b : bundles) {
            ids.add(b.getBundleId());
        }
        
        // Find all bundles from which we import packages, return true if one of them is in our bundles list,
        // and call this method recursively on them as well.
        // We don't care about possible duplicates in there, will be removed by the above cycle avoidance code.
        for(final String name : getImportPackages(targetBundle)) {
            final ExportedPackage[] pcks = this.pckAdmin.getExportedPackages(name);
            if ( pcks == null ) {
                log.debug("No bundles present that export {}", name);
            } else {
                log.debug("{} imports {}", targetBundle, name);
                for(final ExportedPackage pck : pcks) {
                    final Bundle exportingBundle = pck.getExportingBundle();
                    log.debug("Checking {} which exports {}", exportingBundle, pck.getName());
                    if ( exportingBundle.getBundleId() == 0 || exportingBundle.getBundleId() == targetBundle.getBundleId() ) {
                        log.debug("Exporting bundle {} is framework bundle or self, ignored", exportingBundle);
                        continue;
                    } else if ( ids.contains(exportingBundle.getBundleId()) ) {
                        log.debug("Bundle {} affects {} due to package {}, returning true", 
                                new Object[] { exportingBundle, targetBundle, pck.getName() });
                        return true;
                    } else if(isBundleAffected(checkedId, exportingBundle, bundles)) {
                        log.debug("{} recursively affects {}, returning true", exportingBundle, targetBundle); 
                        return true;
                    }
                    log.debug("{} does not affect {}", exportingBundle, targetBundle); 
                }
            }
        }

        return false;
    }
}