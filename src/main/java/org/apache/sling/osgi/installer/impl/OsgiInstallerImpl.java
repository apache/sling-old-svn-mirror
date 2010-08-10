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
import org.apache.sling.osgi.installer.OsgiInstallerStatistics;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** OsgiInstaller service implementation */
public class OsgiInstallerImpl implements OsgiInstaller, OsgiInstallerStatistics, OsgiInstallerContext {

    /** Interface of the package admin */
    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();

    /** Interface of the config admin */
    private static String CONFIG_ADMIN_SERVICE_NAME = ConfigurationAdmin.class.getName();

    public static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

    /** The bundle context. */
	private final BundleContext bundleContext;

	/** The actual worker thread. */
    private final OsgiInstallerThread installerThread;

    private long [] counters = new long[COUNTERS_SIZE];
    private PersistentBundleInfo bundleDigestsStorage;

    /** Tracker for the package admin. */
    private final ServiceTracker packageAdminTracker;

    /** Tracker for the configuration admin. */
    private final ServiceTracker configAdminServiceTracker;

    /**
     * Construct a new service
     */
    public OsgiInstallerImpl(final BundleContext bc)
    throws IOException {
        this.bundleContext = bc;
        // create and start tracker
        this.packageAdminTracker = new ServiceTracker(bc, PACKAGE_ADMIN_NAME, null);
        this.configAdminServiceTracker = new ServiceTracker(bc, CONFIG_ADMIN_SERVICE_NAME, null);
        this.packageAdminTracker.open();
        this.configAdminServiceTracker.open();

        bundleDigestsStorage = new PersistentBundleInfo(bc.getDataFile("bundle-digests.properties"));

        installerThread = new OsgiInstallerThread(this);
        installerThread.setDaemon(true);
        installerThread.start();
    }

    /**
     * Deactivate this service.
     */
    public void deactivate() {
        installerThread.deactivate();

        final TreeSet<String> installedBundlesSymbolicNames = new TreeSet<String>();
        // do we really want to iterate here? Over all bundles? TODO
        for(Bundle b : bundleContext.getBundles()) {
            final String name = b.getSymbolicName();
            if ( name != null ) {
                installedBundlesSymbolicNames.add(b.getSymbolicName());
            }
        }
        try {
            bundleDigestsStorage.purgeAndSave(installedBundlesSymbolicNames);
        } catch (IOException e) {
            Logger.logWarn(OsgiInstaller.class.getName() + " service failed to save state.", e);
        }

        Logger.logInfo("Waiting for installer thread to stop");
        try {
            installerThread.join();
        } catch (InterruptedException e) {
            // we simply ignore this
        }

        this.packageAdminTracker.close();
        this.configAdminServiceTracker.close();

        Logger.logWarn(OsgiInstaller.class.getName()
                + " service deactivated - this warning can be ignored if system is shutting down");
    }

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#getConfigurationAdmin()
	 */
	public ConfigurationAdmin getConfigurationAdmin() {
	    return (ConfigurationAdmin)this.configAdminServiceTracker.getService();
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#addTaskToCurrentCycle(org.apache.sling.osgi.installer.impl.OsgiInstallerTask)
	 */
	public void addTaskToCurrentCycle(OsgiInstallerTask t) {
		installerThread.addTaskToCurrentCycle(t);
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#addTaskToNextCycle(org.apache.sling.osgi.installer.impl.OsgiInstallerTask)
	 */
	public void addTaskToNextCycle(OsgiInstallerTask t) {
	    Logger.logDebug("adding task to next cycle:" + t);
		installerThread.addTaskToNextCycle(t);
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#getBundleContext()
	 */
	public BundleContext getBundleContext() {
		return bundleContext;
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#getPackageAdmin()
	 */
	public PackageAdmin getPackageAdmin() {
		return (PackageAdmin)this.packageAdminTracker.getService();
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstallerStatistics#getCounters()
	 */
	public long [] getCounters() {
		return counters;
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#addResource(org.apache.sling.osgi.installer.InstallableResource)
	 */
	public void addResource(final InstallableResource r) {
        installerThread.addNewResource(r);
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#registerResources(java.util.Collection, java.lang.String)
	 */
	public void registerResources(final Collection<InstallableResource> data, String urlScheme) {
        installerThread.addNewResources(data, urlScheme, bundleContext);
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#removeResource(java.lang.String)
	 */
	public void removeResource(final String url) {
        installerThread.removeResource(url);
	}

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#incrementCounter(int)
	 */
	public void incrementCounter(int index) {
	    counters[index]++;
	}

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#setCounter(int, long)
     */
    public void setCounter(int index, long value) {
        counters[index] = value;
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#getMatchingBundle(java.lang.String)
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

	/**
	 * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#isSnapshot(org.osgi.framework.Version)
	 */
	public boolean isSnapshot(Version v) {
		return v.toString().indexOf(MAVEN_SNAPSHOT_MARKER) >= 0;
	}

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#getInstalledBundleDigest(org.osgi.framework.Bundle)
     */
    public String getInstalledBundleDigest(Bundle b) throws IOException {
        if(bundleDigestsStorage == null) {
            return null;
        }
        return bundleDigestsStorage.getDigest(b.getSymbolicName());
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#getInstalledBundleVersion(java.lang.String)
     */
    public String getInstalledBundleVersion(String symbolicName) throws IOException {
        if(bundleDigestsStorage == null) {
            return null;
        }
        return bundleDigestsStorage.getInstalledVersion(symbolicName);
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#saveInstalledBundleInfo(org.osgi.framework.Bundle, java.lang.String, java.lang.String)
     */
    public void saveInstalledBundleInfo(Bundle b, String digest, String version) throws IOException {
        bundleDigestsStorage.putInfo(b.getSymbolicName(), digest, version);
    }
}