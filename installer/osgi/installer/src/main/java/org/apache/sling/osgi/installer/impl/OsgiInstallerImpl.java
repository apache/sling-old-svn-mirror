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

import java.io.IOException;
import java.util.Collection;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** OsgiInstaller service implementation */
public class OsgiInstallerImpl implements OsgiInstaller, OsgiControllerContext {

	private final BundleContext bundleContext;
    private final PackageAdmin packageAdmin;
    private final ServiceTracker logServiceTracker;
    private long [] counters = new long[COUNTERS_SIZE];

    public OsgiInstallerImpl(final BundleContext bc,
                              final PackageAdmin pa,
                              final ServiceTracker logServiceTracker)
    throws IOException {
        this.bundleContext = bc;
        this.packageAdmin = pa;
        this.logServiceTracker = logServiceTracker;
    }

    public void deactivate() {
        if(getLogService() != null) {
            getLogService().log(LogService.LOG_WARNING,
                    OsgiInstaller.class.getName()
                    + " service deactivated - this warning can be ignored if system is shutting down");
        }
    }

	public ConfigurationAdmin getConfigurationAdmin() {
		// TODO ConfigurationAdmin should be bound/unbound rather than
		// looking it up every time, but that caused problems in the it/OsgiControllerTest
		if(bundleContext != null) {
		   	final ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
		    if(ref != null) {
		    	return (ConfigurationAdmin)bundleContext.getService(ref);
		    }
		}
		return null;
	}

	public LogService getLogService() {
		return (LogService)logServiceTracker.getService();
	}

	public void addTaskToCurrentCycle(OsgiControllerTask t) {
		if(getLogService() != null) {
			getLogService().log(LogService.LOG_DEBUG, "adding task to current cycle:" + t);
		}
		// TODO
		//tasksForThisCycle.add(t);
	}

	public void addTaskToNextCycle(OsgiControllerTask t) {
		if(getLogService() != null) {
			getLogService().log(LogService.LOG_DEBUG, "adding task to next cycle:" + t);
		}
		// TODO
		//tasksForNextCycle.add(t);
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}
	
	public PackageAdmin getPackageAdmin() {
		return packageAdmin;
	}
	
	public long [] getCounters() {
		return counters;
	}

	public void addResource(InstallableResource d) throws IOException {
		// TODO
	}

	public void registerResources(Collection<InstallableResource> data,
			String urlScheme) throws IOException {
		// TODO
	}

	public void removeResource(InstallableResource d) throws IOException {
		// TODO
	}

	public Storage getStorage() {
		// TODO
		return null;
	}
}