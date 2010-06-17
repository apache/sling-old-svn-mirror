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
import java.util.TreeSet;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** OsgiInstaller service implementation */
public class OsgiInstallerImpl implements OsgiInstaller, OsgiInstallerContext {

    public static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

	private final BundleContext bundleContext;
    private final PackageAdmin packageAdmin;
    private final ServiceTracker logServiceTracker;
    private final OsgiInstallerThread installerThread;
    private long [] counters = new long[COUNTERS_SIZE];
    private PersistentBundleInfo bundleDigestsStorage;

    public OsgiInstallerImpl(final BundleContext bc,
                              final PackageAdmin pa,
                              final ServiceTracker logServiceTracker)
    throws IOException {
        this.bundleContext = bc;
        this.packageAdmin = pa;
        this.logServiceTracker = logServiceTracker;
        bundleDigestsStorage = new PersistentBundleInfo(this, bc.getDataFile("bundle-digests.properties"));

        installerThread = new OsgiInstallerThread(this);
        installerThread.setDaemon(true);
        installerThread.start();
    }

    public void deactivate() throws InterruptedException, IOException {
        installerThread.deactivate();

        final TreeSet<String> installedBundlesSymbolicNames = new TreeSet<String>();
        for(Bundle b : bundleContext.getBundles()) {
            installedBundlesSymbolicNames.add(b.getSymbolicName());
        }
        bundleDigestsStorage.purgeAndSave(installedBundlesSymbolicNames);

        this.logInfo("Waiting for installer thread to stop");
        installerThread.join();

        this.logWarn(OsgiInstaller.class.getName()
                    + " service deactivated - this warning can be ignored if system is shutting down");
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

	public void addTaskToCurrentCycle(OsgiInstallerTask t) {
		installerThread.addTaskToCurrentCycle(t);
	}

	public void addTaskToNextCycle(OsgiInstallerTask t) {
		this.logDebug("adding task to next cycle:" + t);
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

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#addResource(org.apache.sling.osgi.installer.InstallableResource)
	 */
	public void addResource(InstallableResource r) {
        installerThread.addNewResource(r);
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#registerResources(java.util.Collection, java.lang.String)
	 */
	public void registerResources(Collection<InstallableResource> data, String urlScheme) {
        installerThread.addNewResources(data, urlScheme, bundleContext);
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#removeResource(org.apache.sling.osgi.installer.InstallableResource)
	 */
	public void removeResource(InstallableResource r) {
        installerThread.removeResource(r);
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

	public boolean isSnapshot(Version v) {
		return v.toString().indexOf(MAVEN_SNAPSHOT_MARKER) >= 0;
	}

    public String getInstalledBundleDigest(Bundle b) throws IOException {
        if(bundleDigestsStorage == null) {
            return null;
        }
        return bundleDigestsStorage.getDigest(b.getSymbolicName());
    }

    public String getInstalledBundleVersion(String symbolicName) throws IOException {
        if(bundleDigestsStorage == null) {
            return null;
        }
        return bundleDigestsStorage.getInstalledVersion(symbolicName);
    }

    public void saveInstalledBundleInfo(Bundle b, String digest, String version) throws IOException {
        bundleDigestsStorage.putInfo(b.getSymbolicName(), digest, version);
    }

    /**
     * Internal method for logging.
     * This method checks if the LogService is available and only then logs
     */
    private void log(final int level, final String message, final Throwable t) {
        final LogService ls = (LogService) this.logServiceTracker.getService();
        if ( ls != null ) {
            if ( t != null ) {
                ls.log(level, message, t);
            } else {
                ls.log(level, message);
            }
        }
    }
    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logDebug(java.lang.String, java.lang.Throwable)
     */
    public void logDebug(String message, Throwable t) {
        log(LogService.LOG_DEBUG, message, t);
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logDebug(java.lang.String)
     */
    public void logDebug(String message) {
        log(LogService.LOG_DEBUG, message, null);
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logInfo(java.lang.String, java.lang.Throwable)
     */
    public void logInfo(String message, Throwable t) {
        log(LogService.LOG_INFO, message, t);
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logInfo(java.lang.String)
     */
    public void logInfo(String message) {
        log(LogService.LOG_INFO, message, null);
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logWarn(java.lang.String, java.lang.Throwable)
     */
    public void logWarn(String message, Throwable t) {
        log(LogService.LOG_WARNING, message, t);
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logWarn(java.lang.String)
     */
    public void logWarn(String message) {
        log(LogService.LOG_WARNING, message, null);
    }

}