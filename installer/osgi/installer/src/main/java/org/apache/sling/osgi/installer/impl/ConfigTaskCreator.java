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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.apache.sling.osgi.installer.impl.tasks.ConfigInstallTask;
import org.apache.sling.osgi.installer.impl.tasks.ConfigRemoveTask;
import org.osgi.service.log.LogService;

/** TaskCreator that processes a list of config RegisteredResources */
class ConfigTaskCreator {

    /** Store digests of the installed configs, keyed by config pid */
    private final Map<String, String> digests = new HashMap<String, String>();

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
		    tasks.add(new ConfigRemoveTask(first));
		    digests.remove(getDigestKey(first));
		} else {
		    final String key = getDigestKey(toActivate);
		    final String previousDigest = digests.get(key);
		    if(toActivate.getDigest().equals(previousDigest)) {
		        if(ctx.getLogService() != null) {
		            ctx.getLogService().log(LogService.LOG_DEBUG, "Configuration (" + key+ ") already installed, ignored: " + toActivate);
		        }
		    } else {
		        tasks.add(new ConfigInstallTask(toActivate));
		        digests.put(key, toActivate.getDigest());
                if(ctx.getLogService() != null) {
                    ctx.getLogService().log(LogService.LOG_DEBUG,
                            "Scheduling update/install of config " + toActivate + ", digest has changed or was absent");
                }
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
