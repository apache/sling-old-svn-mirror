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
package org.apache.sling.osgi.installer.impl.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.osgi.installer.impl.Logger;
import org.apache.sling.osgi.installer.impl.OsgiInstallerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/** TaskCreator that processes a list of bundle RegisteredResources */
public class BundleTaskCreator {

    /** Interface of the package admin */
    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();

    /** Interface of the start level */
    private static String START_LEVEL_NAME = StartLevel.class.getName();

    /** The filename for the digest storage. */
    private static final String FILE_DIGEST_STORAGE = "bundle-digests.properties";

    private static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

    /** Tracker for the package admin. */
    private final ServiceTracker packageAdminTracker;

    /** Tracker for the start level service. */
    private final ServiceTracker startLevelTracker;

    /** The storage for the bundle info. */
    private final PersistentBundleInfo bundleDigestsStorage;

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** Store the digests of the bundles for which we create update tasks,
     *  keyed by symbolic name, to avoid generating repated updates
     *  for snapshot bundles
     */
    private final Map<String, String> digests = new HashMap<String, String>();

    public BundleTaskCreator(final BundleContext bc) {
        this.bundleContext = bc;
        // create and start tracker
        this.packageAdminTracker = new ServiceTracker(bc, PACKAGE_ADMIN_NAME, null);
        this.packageAdminTracker.open();
        this.startLevelTracker = new ServiceTracker(bc, START_LEVEL_NAME, null);
        this.startLevelTracker.open();
        this.bundleDigestsStorage = new PersistentBundleInfo(bc.getDataFile(FILE_DIGEST_STORAGE));
    }

    public void deactivate() {
        this.packageAdminTracker.close();
        this.startLevelTracker.close();
        final TreeSet<String> installedBundlesSymbolicNames = new TreeSet<String>();
        // do we really want to iterate here? Over all bundles? TODO
        for(Bundle b : bundleContext.getBundles()) {
            final String name = b.getSymbolicName();
            if ( name != null ) {
                installedBundlesSymbolicNames.add(b.getSymbolicName());
            }
        }
        try {
            this.bundleDigestsStorage.purgeAndSave(installedBundlesSymbolicNames);
        } catch (IOException e) {
            Logger.logWarn(OsgiInstaller.class.getName() + " service failed to save state.", e);
        }
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public StartLevel getStartLevel() {
        return (StartLevel) this.startLevelTracker.getService();
    }

    public PackageAdmin getPackageAdmin() {
        return (PackageAdmin)this.packageAdminTracker.getService();
    }

    /** Holds the bundle info that we need, makes it easier to test
     *  without an OSGi framework */
    static class BundleInfo {
        final String symbolicName;
        final Version version;
        final int state;

        BundleInfo(String symbolicName, Version version, int state) {
            this.symbolicName = symbolicName;
            this.version = version;
            this.state = state;
        }

        BundleInfo(Bundle b) {
            this.symbolicName = b.getSymbolicName();
            this.version = new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
            this.state = b.getState();
        }
    }

	/** Create tasks for a set of RegisteredResource that all represent the same bundle.
	 * 	Selects the bundle with the highest priority (i.e. the first one in the group that
	 *  has desired state == active, and generates the appropriate OSGi tasks to
	 *  reach this state.
	 */
	public void createTasks(SortedSet<RegisteredResource> resources, SortedSet<OsgiInstallerTask> tasks) throws IOException {

		// Find the bundle that must be active: the resources collection is ordered according
		// to priorities, so we just need to find the first one that is installable
		RegisteredResource toActivate = null;
		for(RegisteredResource r : resources) {
			if (r.isInstallable()) {
				toActivate = r;
				break;
			}
		}

		String digestToSave = null;
		final RegisteredResource firstResource = resources.first();
		final String symbolicName = firstResource == null ? null :
		    (String)firstResource.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);

		if (toActivate == null) {
		    // None of our resources are installable, remove corresponding bundle if present
		    // and if we installed it
		    if (getBundleInfo(resources.first()) != null) {
		        if (this.getInstalledBundleVersion(symbolicName) == null) {
		            Logger.logInfo("Bundle " + symbolicName
                                + " was not installed by this module, not removed");
		        } else {
		            tasks.add(new BundleRemoveTask(resources.first(),
		                    this));
		        }
	        }

		} else {
			final BundleInfo info = getBundleInfo(toActivate);
			final Version newVersion = new Version((String)toActivate.getAttributes().get(Constants.BUNDLE_VERSION));
	        RegisteredResource toUpdate = null;
			if (info == null) {
			    // bundle is not installed yet: install and save digest to avoid
			    // unnecessary updates
				tasks.add(new BundleInstallTask(toActivate,
				        this));
				digestToSave = toActivate.getDigest();
			} else {
			    final int compare = info.version.compareTo(newVersion);
                if(compare < 0) {
                    // installed version is lower -> update
                    toUpdate = toActivate;
                } else if(compare > 0) {
	                // installed version is higher -> downgrade only if
                    // we installed that version
                    final String installedVersion = this.getInstalledBundleVersion(info.symbolicName);
                    if(info.version.toString().equals(installedVersion)) {
                        toUpdate = toActivate;
                        Logger.logInfo("Bundle " + info.symbolicName + " " + installedVersion
                                    + " was installed by this module, downgrading to " + newVersion);
                    } else {
                        Logger.logInfo("Bundle " + info.symbolicName + " " + installedVersion
                                    + " was not installed by this module, not downgraded");
                    }
			    } else if(compare == 0 && this.isSnapshot(newVersion)){
			        // installed, same version but SNAPSHOT
                    toUpdate = toActivate;
			    }
			}

			// Save the digest of installed and updated resources, keyed by
			// bundle symbolic name, to avoid unnecessary updates
			if (toUpdate != null) {
			    final String previousDigest = digests.get(symbolicName);
			    if(toUpdate.getDigest().equals(previousDigest)) {
			        Logger.logDebug("Ignoring update of " + toUpdate + ", digest didn't change");
                    digestToSave = previousDigest;
			    } else {
			        Logger.logDebug("Scheduling update of " + toUpdate + ", digest has changed");
			        tasks.add(new BundleUpdateTask(toUpdate,
			                this));
			        digestToSave = toUpdate.getDigest();
			    }
			}

			if(digestToSave == null) {
			    if(symbolicName != null) {
			        digests.remove(symbolicName);
			    }
			} else {
			    digests.put(symbolicName, digestToSave);
			}
		}
	}

	protected BundleInfo getBundleInfo(final RegisteredResource bundle) {
		final String symbolicName = (String)bundle.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
		final Bundle b = this.getMatchingBundle(symbolicName);
		if (b == null) {
		    return null;
        }
		return new BundleInfo(b);
	}

    /**
     * Retrieve a bundle's digest that was stored by saveInstalledBundleInfo
     * @return null if no digest was stored
     */
    public String getInstalledBundleDigest(Bundle b) throws IOException {
        return bundleDigestsStorage.getDigest(b.getSymbolicName());
    }

    /**
     * Retrieve a bundle's version that was stored by saveInstalledBundleInfo
     * @return null if no version was stored
     */
    public String getInstalledBundleVersion(String symbolicName) throws IOException {
        return bundleDigestsStorage.getInstalledVersion(symbolicName);
    }

    /**
     * Store a bundle's digest and installed version, keyed by symbolic ID
     */
    public void saveInstalledBundleInfo(String symbolicName, String digest, String version) throws IOException {
        bundleDigestsStorage.putInfo(symbolicName, digest, version);
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

    /**
     * Check if the version is a snapshot version
     */
    public boolean isSnapshot(Version v) {
        return v.toString().indexOf(MAVEN_SNAPSHOT_MARKER) >= 0;
    }
}
