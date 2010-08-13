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
package org.apache.sling.osgi.installer.impl.config;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.apache.sling.osgi.installer.impl.Logger;
import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** TaskCreator that processes a list of config RegisteredResources */
public class ConfigTaskCreator {

    /** Interface of the config admin */
    private static String CONFIG_ADMIN_SERVICE_NAME = ConfigurationAdmin.class.getName();

    /** Store digests of the installed configs, keyed by config pid */
    private final Map<String, String> digests = new HashMap<String, String>();

    private final ServiceTracker configAdminServiceTracker;

    public ConfigTaskCreator(final BundleContext bc) {
        this.configAdminServiceTracker = new ServiceTracker(bc, CONFIG_ADMIN_SERVICE_NAME, null);
        this.configAdminServiceTracker.open();
    }

    public void deactivate() {
        this.configAdminServiceTracker.close();
    }

	/** Create tasks for a set of RegisteredResource that all represent the same config PID.
	 */
	public void createTasks(OsgiInstallerContext ctx, SortedSet<RegisteredResource> resources, SortedSet<OsgiInstallerTask> tasks) {

		// Find the config that must be active: the resources collection is ordered according
		// to priorities, so we just need to find the first one that is installable
		RegisteredResource toActivate = null;
		for(RegisteredResource r : resources) {
			if (r.isInstallable()) {
				toActivate = r;
				break;
			}
		}

		if(toActivate == null) {
		    // None of our resources are installable, remove corresponding config
		    // (task simply does nothing if config does not exist)
		    final RegisteredResource first = resources.first();
		    tasks.add(new ConfigRemoveTask(first, this.configAdminServiceTracker));
		    digests.remove(getDigestKey(first));
		} else {
		    final String key = getDigestKey(toActivate);
		    final String previousDigest = digests.get(key);
		    if(toActivate.getDigest().equals(previousDigest)) {
		        Logger.logDebug("Configuration (" + key+ ") already installed, ignored: " + toActivate);
		    } else {
		        tasks.add(new ConfigInstallTask(toActivate, this.configAdminServiceTracker));
		        digests.put(key, toActivate.getDigest());
		        Logger.logDebug("Scheduling update/install of config " + toActivate + ", digest has changed or was absent");
		    }
		}
	}

	private String getDigestKey(RegisteredResource r) {
        final ConfigurationPid cp = (ConfigurationPid)r.getAttributes().get(RegisteredResource.CONFIG_PID_ATTRIBUTE);
        if(cp == null) {
            throw new IllegalArgumentException("Resource does not provide a CONFIG_PID_ATTRIBUTE: " + r);
        }
        return cp.getCompositePid();
	}
}
