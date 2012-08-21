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
import java.util.List;

import org.osgi.framework.Bundle;
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

    /**
     * Check whether a packages refresh of the supplied bundles would affect targetBundle
     */
    boolean isBundleAffected(Bundle target, final List<Bundle> bundles) {
        log.debug("isBundleAffected({}, {})", target, bundles);
     
        final List<Long> idChecked = new ArrayList<Long>();
        for(Bundle b : bundles) {
            if(dependsOn(idChecked, target, b)) {
                log.debug("isBundleAffected({}) is true, dependency on bundle {}", target, b);
                return true;
            }
        }
        
        log.debug("isBundleAffected({}) is false, no dependencies on {}", target, bundles);
        return false;
    }
    
    /** True if target depends on source via package imports */
    private boolean dependsOn(List<Long> idChecked, Bundle target, Bundle source) {
        
        if(idChecked.contains(source.getBundleId())) {
            return false;
        }
        idChecked.add(source.getBundleId());
        
        final ExportedPackage [] eps = pckAdmin.getExportedPackages(source);
        if(eps == null) {
            return false;
        }
        
        for(ExportedPackage ep : eps) {
            final Bundle [] importers = ep.getImportingBundles();
            if(importers == null) {
                continue;
            }
            for(Bundle b : importers) {
                if(b.getBundleId() == target.getBundleId()) {
                    log.debug("{} depends on {} via package {}", 
                            new Object[] { target, source, ep.getName() });
                    return true;
                }
                if(dependsOn(idChecked, target, b)) {
                    log.debug("{} depends on {} which depends on {}, returning true",
                            new Object[] { target, b, source });
                    return true;
                }
            }
        }
        
        return false;
    }
}