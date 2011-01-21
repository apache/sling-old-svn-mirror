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
package org.apache.sling.installer.core.impl.tasks;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.InternalService;
import org.apache.sling.installer.core.impl.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task creator for bundles
 */
public class BundleTaskCreator implements InternalService, InstallTaskFactory {

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    public static final String ATTR_START = "sling.osgi.installer.start.bundle";

    /** Interface of the package admin */
    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();

    /** Interface of the start level */
    private static String START_LEVEL_NAME = StartLevel.class.getName();

    private static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

    /** Tracker for the package admin. */
    private ServiceTracker packageAdminTracker;

    /** Tracker for the start level service. */
    private ServiceTracker startLevelTracker;

    /** The bundle context. */
    private BundleContext bundleContext;

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#init(org.osgi.framework.BundleContext)
     */
    public void init(final BundleContext bc) {
        this.bundleContext = bc;
        // create and start tracker
        this.packageAdminTracker = new ServiceTracker(bc, PACKAGE_ADMIN_NAME, null);
        this.packageAdminTracker.open();
        this.startLevelTracker = new ServiceTracker(bc, START_LEVEL_NAME, null);
        this.startLevelTracker.open();
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#deactivate()
     */
    public void deactivate() {
        if ( this.packageAdminTracker != null ) {
            this.packageAdminTracker.close();
            this.packageAdminTracker = null;
        }
        if ( this.startLevelTracker != null ) {
            this.startLevelTracker.close();
            this.startLevelTracker = null;
        }
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#getDescription()
     */
    public String getDescription() {
        return "Apache Sling Bundle Install Task Factory";
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
        final long id;

        BundleInfo(String symbolicName, Version version, int state, long id) {
            this.symbolicName = symbolicName;
            this.version = version;
            this.state = state;
            this.id = id;
        }

        BundleInfo(Bundle b) {
            this.symbolicName = b.getSymbolicName();
            this.version = new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
            this.state = b.getState();
            this.id = b.getBundleId();
        }
    }

	/**
     * Create a bundle task - install, update or remove
     *
	 * @see org.apache.sling.installer.api.tasks.InstallTaskFactory#createTask(org.apache.sling.installer.api.tasks.TaskResourceGroup)
	 */
	public InstallTask createTask(final TaskResourceGroup resourceList) {
	    final TaskResource toActivate = resourceList.getActiveResource();
	    if ( !toActivate.getType().equals(InstallableResource.TYPE_BUNDLE) ) {
	        return null;
	    }

	    // check if symbolic name and version is provided in the attributes
        if ( toActivate.getAttribute(Constants.BUNDLE_SYMBOLICNAME) == null ) {
            final Util.BundleHeaders headers = Util.readBundleHeaders(toActivate);
            if ( headers == null ) {
                logger.info("Resource of type bundle {} is not really a bundle - manifest entries are missing.", toActivate);
                return new ChangeStateTask(resourceList, ResourceState.IGNORED);
            }
            toActivate.setAttribute(Constants.BUNDLE_SYMBOLICNAME, headers.symbolicName);
            toActivate.setAttribute(Constants.BUNDLE_VERSION, headers.version);
            if ( headers.activationPolicy != null ) {
                toActivate.setAttribute(Constants.BUNDLE_ACTIVATIONPOLICY, headers.activationPolicy);
            }
        }
        final String symbolicName = (String)toActivate.getAttribute(Constants.BUNDLE_SYMBOLICNAME);
        final BundleInfo info = this.getBundleInfo(symbolicName);

		// Uninstall
        final InstallTask result;
		if (toActivate.getState() == ResourceState.UNINSTALL) {
		    // Remove corresponding bundle if present and if we installed it
		    if (info != null
		        && info.version.equals(new Version((String)toActivate.getAttribute(Constants.BUNDLE_VERSION))) ) {
		        result = new BundleRemoveTask(resourceList, this);
		    } else {
	            logger.info("Bundle {} was not installed by this module, not removed", symbolicName);
	            result = new ChangeStateTask(resourceList, ResourceState.IGNORED);
	        }

		// Install
		} else {
		    // check if we should start the bundle as we installed it in the previous run
		    if (info == null) {
			    // bundle is not installed yet: install
			    result = new BundleInstallTask(resourceList, this);
		    } else if ( toActivate.getAttribute(ATTR_START) != null ) {
	            result = new BundleStartTask(resourceList, info.id, this);
			} else {
	            boolean doUpdate = false;

	            final Version newVersion = new Version((String)toActivate.getAttribute(Constants.BUNDLE_VERSION));
			    final int compare = info.version.compareTo(newVersion);
                if (compare < 0) {
                    // installed version is lower -> update
                    doUpdate = true;
                } else if (compare > 0) {
                    logger.debug("Bundle " + info.symbolicName + " " + newVersion
                                + " is not installed, bundle with higher version is already installed.");
			    } else if (compare == 0 && this.isSnapshot(newVersion)) {
			        // installed, same version but SNAPSHOT
			        doUpdate = true;
			    }
                if (doUpdate) {

                    logger.debug("Scheduling update of {}", toActivate);
                    if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName) ) {
                        result = new SystemBundleUpdateTask(resourceList, this);
                    } else {
                        result = new BundleUpdateTask(resourceList, this);
                    }
                } else {
                    logger.debug("Nothing to install for {}, same version {} already installed.", toActivate, newVersion);
                    result = new ChangeStateTask(resourceList, ResourceState.IGNORED);
                }
			}
		}
		return result;
	}

	protected BundleInfo getBundleInfo(final String symbolicName) {
		final Bundle b = this.getMatchingBundle(symbolicName);
		if (b == null) {
		    return null;
        }
		return new BundleInfo(b);
	}

    /**
     * Finds the bundle with given symbolic name in our bundle context.
     */
    public Bundle getMatchingBundle(String bundleSymbolicName) {
        if (bundleSymbolicName != null) {
            // check if this is the system bundle
            if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(bundleSymbolicName) ) {
                return bundleContext.getBundle(0);
            }
            final Bundle[] bundles = bundleContext.getBundles();
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
