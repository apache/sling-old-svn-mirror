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

import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Task creator for configurations.
 */
public class ConfigTaskCreator {

    /** Interface of the config admin */
    private static String CONFIG_ADMIN_SERVICE_NAME = ConfigurationAdmin.class.getName();

    /** Service tracker for the configuration admin. */
    private final ServiceTracker configAdminServiceTracker;

    /**
     * Constructor
     */
    public ConfigTaskCreator(final BundleContext bc) {
        this.configAdminServiceTracker = new ServiceTracker(bc, CONFIG_ADMIN_SERVICE_NAME, null);
        this.configAdminServiceTracker.open();
    }

    /**
     * Deactivate this creator.
     */
    public void deactivate() {
        this.configAdminServiceTracker.close();
    }

	/**
     * Create a task to install or uninstall a configuration.
	 */
	public OsgiInstallerTask createTask(final RegisteredResource toActivate) {
	    final OsgiInstallerTask result;
		if (toActivate.getState() == RegisteredResource.State.UNINSTALL) {
		    // None of our resources are installable, remove corresponding config
		    // (task simply does nothing if config does not exist)
		    result = new ConfigRemoveTask(toActivate, this.configAdminServiceTracker);
		} else {
	        result = new ConfigInstallTask(toActivate, this.configAdminServiceTracker);
		}
		return result;
	}
}
