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
package org.apache.sling.osgi.installer.impl;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.impl.config.ConfigurationPid;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** Comparator that defines priorities between RegisteredResources.
 * 	The RegisteredResources are grouped by OSGi "entity" (bundle symbolic
 * 	name, config PID, etc.) in sorted sets, and this comparator is used
 *  to sort the resources in the sets.
 */
class RegisteredResourceComparator implements Comparator<RegisteredResource>, Serializable {

    private static final long serialVersionUID = 1L;

    public int compare(RegisteredResource a, RegisteredResource b) {
    	final boolean aBundle = a.getType().equals(InstallableResource.TYPE_BUNDLE);
    	final boolean bBundle = b.getType().equals(InstallableResource.TYPE_BUNDLE);

        if(aBundle && bBundle) {
            return compareBundles(a, b);
        } else if(!aBundle && !bBundle){
            return compareConfig(a, b);
        } else if(aBundle) {
        	return 1;
        } else {
        	return -1;
        }
    }

    int compareBundles(RegisteredResource a, RegisteredResource b) {

        boolean isSnapshot = false;
        int result = 0;

        // Order first by symbolic name
        final String nameA = (String)a.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        final String nameB = (String)b.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
        if(nameA != null && nameB != null) {
            result = nameA.compareTo(nameB);
        }

        // Then by version
        if(result == 0) {
            final Version va = new Version((String)a.getAttributes().get(Constants.BUNDLE_VERSION));
            final Version vb = new Version((String)b.getAttributes().get(Constants.BUNDLE_VERSION));
            isSnapshot = va.toString().contains(OsgiInstallerImpl.MAVEN_SNAPSHOT_MARKER);
            // higher version has more priority, must come first so invert comparison
            result = vb.compareTo(va);
        }

        // Then by priority, higher values first
        if(result == 0) {
            if(a.getPriority() < b.getPriority()) {
                result = 1;
            } else if(a.getPriority() > b.getPriority()) {
                result = -1;
            }
        }

        if(result == 0 && isSnapshot) {
            // For snapshots, compare serial numbers so that snapshots registered
            // later get priority
            if(a.getSerialNumber() < b.getSerialNumber()) {
                result = 1;
            } else if(a.getSerialNumber() > b.getSerialNumber()) {
                result = -1;
            }
        }

        return result;
    }

    int compareConfig(RegisteredResource a, RegisteredResource b) {
        int result = 0;

        // First compare by pid
        final ConfigurationPid pA = (ConfigurationPid)a.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE);
        final ConfigurationPid pB = (ConfigurationPid)b.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE);
        if(pA != null && pA.getCompositePid() != null && pB != null && pB.getCompositePid() != null) {
            result = pA.getCompositePid().compareTo(pB.getCompositePid());
        }

        // Then by priority, higher values first
        if(result == 0) {
            if(a.getPriority() < b.getPriority()) {
                result = 1;
            } else if(a.getPriority() > b.getPriority()) {
                result = -1;
            }
        }

        return result;
    }
}
