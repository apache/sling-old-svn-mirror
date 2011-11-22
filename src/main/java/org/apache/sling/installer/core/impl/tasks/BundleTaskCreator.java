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
import org.apache.sling.installer.api.ResourceChangeListener;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.InternalService;
import org.apache.sling.installer.core.impl.Util;
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

    /** If this property is set, the bundle is installed if the currently installed version
     * is the version specified by the property.
     */
    private final static String FORCE_INSTALL_VERSION = "force.install.version";

    private final static String SPECIAL_ATTR = "sling.osgi.installer.special";

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    public static final String ATTR_START = "sling.osgi.installer.start.bundle";

    /** Interface of the package admin */
    private static String PACKAGE_ADMIN_NAME = PackageAdmin.class.getName();

    /** Interface of the start level */
    private static String START_LEVEL_NAME = StartLevel.class.getName();

    /** Tracker for the package admin. */
    private ServiceTracker packageAdminTracker;

    /** Tracker for the start level service. */
    private ServiceTracker startLevelTracker;

    /** The bundle context. */
    private BundleContext bundleContext;

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#init(org.osgi.framework.BundleContext, org.apache.sling.installer.api.ResourceChangeListener)
     */
    public void init(final BundleContext bc, final ResourceChangeListener listener) {
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

	/**
     * Create a bundle task - install, update or remove
     *
	 * @see org.apache.sling.installer.api.tasks.InstallTaskFactory#createTask(org.apache.sling.installer.api.tasks.TaskResourceGroup)
	 */
	public InstallTask createTask(final TaskResourceGroup resourceList) {
	    // quick check of the resource type.
	    final TaskResource toActivate = resourceList.getActiveResource();
	    if ( !toActivate.getType().equals(InstallableResource.TYPE_BUNDLE) ) {
	        return null;
	    }

	    // check if symbolic name and version is provided in the attributes
        if ( toActivate.getAttribute(Constants.BUNDLE_SYMBOLICNAME) == null ) {
            final Util.BundleHeaders headers = Util.readBundleHeaders(toActivate, logger);
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
        final boolean isInstallerCoreBundle = this.bundleContext.getBundle().getSymbolicName().equals(symbolicName);

        // Uninstall
        final InstallTask result;
		if (toActivate.getState() == ResourceState.UNINSTALL) {
            // find the info with the exact version
            final BundleInfo info = this.getBundleInfo(
                    symbolicName,
                    (String)toActivate.getAttribute(Constants.BUNDLE_VERSION));
		    // Remove corresponding bundle if present and if we installed it
		    if ( info != null ) {
	            // if this is an uninstall, check if we have to install an older version
	            // in this case we should do an update instead of uninstall/install (!)
	            final TaskResource second = resourceList.getNextActiveResource();
	            if ( second != null &&
	                ( second.getState() == ResourceState.IGNORED || second.getState() == ResourceState.INSTALLED || second.getState() == ResourceState.INSTALL ) ) {
                    second.setAttribute(FORCE_INSTALL_VERSION, info.version.toString());
                    logger.debug("Detected downgrad of bundle {}", symbolicName);
                    result = new ChangeStateTask(resourceList, ResourceState.UNINSTALLED);
	            } else {
	                // prevent uninstalling the installer itself!
	                if ( isInstallerCoreBundle ) {
	                    logger.debug("Prevent completely uninstalling installer bundle {}", symbolicName);
	                    result = new ChangeStateTask(resourceList, ResourceState.UNINSTALLED);
	                } else {
	                    result = new BundleRemoveTask(resourceList, this);
	                }
	            }
		    } else {
	            logger.debug("Bundle {}:{} is not installed anymore - nothing to remove.", symbolicName,
	                    toActivate.getAttribute(Constants.BUNDLE_VERSION));
	            result = new ChangeStateTask(resourceList, ResourceState.IGNORED);
	        }

		// Install
		} else {
		    // for install and update, we want the bundle with the highest version
	        final BundleInfo info = this.getBundleInfo(symbolicName, null);
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
                    final String forceVersion = (String) toActivate.getAttribute(FORCE_INSTALL_VERSION);
                    if ( forceVersion != null && info.version.compareTo(new Version(forceVersion)) == 0 ) {
                        doUpdate = true;
                    } else {
                        logger.debug("Bundle " + info.symbolicName + " " + newVersion
                                    + " is not installed, bundle with higher version is already installed.");
                    }
			    } else if (compare == 0 && BundleInfo.isSnapshot(newVersion)) {
			        // check if system bundle or installer bundle
			        if ( isInstallerCoreBundle || Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName) ) {
			            if ( toActivate.getAttribute(SPECIAL_ATTR) != null ) {
			                toActivate.setAttribute(SPECIAL_ATTR, null);
			                result = new ChangeStateTask(resourceList, ResourceState.INSTALLED);
			                return result;
                        }
		                toActivate.setAttribute(SPECIAL_ATTR, "installingsnapshot");
			        }
			        // installed, same version but SNAPSHOT
			        doUpdate = true;
			    }
                if (doUpdate) {

                    logger.debug("Scheduling update of {}", toActivate);
                    // check if this is the system bundle
                    if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName) ) {
                        result = new SystemBundleUpdateTask(resourceList, this);
                        // check if this is a installer update
                    } else if ( isInstallerCoreBundle ) {
                        result = new InstallerBundleUpdateTask(resourceList, this);
                    } else {
                        result = new BundleUpdateTask(resourceList, this);
                    }
                } else if ( compare == 0 && (isInstallerCoreBundle || Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName)) ) {
                    // the installer core bundle / system bundle has been updated, just set state
                    result = new ChangeStateTask(resourceList, ResourceState.INSTALLED);
                } else {
                    logger.debug("Nothing to install for {}, same version {} already installed.", toActivate, newVersion);
                    result = new ChangeStateTask(resourceList, ResourceState.IGNORED);
                }
			}
		    toActivate.setAttribute(FORCE_INSTALL_VERSION, null);
		}
		return result;
	}

    protected BundleInfo getBundleInfo(final String symbolicName, final String version) {
        return BundleInfo.getBundleInfo(this.getBundleContext(), symbolicName, version);
    }
}
