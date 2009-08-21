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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** OsgiInstaller service implementation */
public class OsgiInstallerImpl implements OsgiInstaller, OsgiInstallerContext {

	private final BundleContext bundleContext;
    private final PackageAdmin packageAdmin;
    private final ServiceTracker logServiceTracker;
    private final OsgiInstallerThread installerThread;
    private long [] counters = new long[COUNTERS_SIZE];

    public OsgiInstallerImpl(final BundleContext bc,
                              final PackageAdmin pa,
                              final ServiceTracker logServiceTracker)
    throws IOException {
        this.bundleContext = bc;
        this.packageAdmin = pa;
        this.logServiceTracker = logServiceTracker;
        
        installerThread = new OsgiInstallerThread(this);
        installerThread.setDaemon(true);
        installerThread.start();
    }

    public void deactivate() {
        installerThread.deactivate();
        if(getLogService() != null) {
            getLogService().log(LogService.LOG_WARNING,
                    OsgiInstaller.class.getName()
                    + " service deactivated - this warning can be ignored if system is shutting down");
        }
    }

	public ConfigurationAdmin getConfigurationAdmin() {
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

	public void addTaskToCurrentCycle(OsgiInstallerTask t) {
		installerThread.addTaskToCurrentCycle(t);
	}

	public void addTaskToNextCycle(OsgiInstallerTask t) {
		if(getLogService() != null) {
			getLogService().log(LogService.LOG_DEBUG, "adding task to next cycle:" + t);
		}
		installerThread.addTaskToNextCycle(t);
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

	public void addResource(InstallableResource r) throws IOException {
	    synchronized (installerThread) {
	        installerThread.addNewResource(new RegisteredResourceImpl(bundleContext, r));
        }
	}

	public void registerResources(Collection<InstallableResource> data, String urlScheme) throws IOException {
        installerThread.addNewResources(data, urlScheme, bundleContext);
	}

	public void removeResource(InstallableResource r) throws IOException {
		final RegisteredResource rr = new RegisteredResourceImpl(bundleContext, r);
		rr.setInstallable(false);
        synchronized (installerThread) {
            installerThread.addNewResource(rr);
        }
	}

	public void incrementCounter(int index) {
	    counters[index]++;
	}

    public void setCounter(int index, long value) {
        counters[index] = value;
    }
    
    /**
     * Finds the bundle with given symbolic name in our BundleContext.
     */
    public Bundle getMatchingBundle(String bundleSymbolicName) {
        if (bundleSymbolicName != null) {
            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
                    return bundle;
                }
            }
        }
        return null;
    }
}