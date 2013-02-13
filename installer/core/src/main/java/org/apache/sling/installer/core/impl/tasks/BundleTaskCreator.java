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
import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.RetryHandler;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.core.impl.InternalService;
import org.apache.sling.installer.core.impl.PersistentResourceList;
import org.apache.sling.installer.core.impl.Util;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task creator for bundles
 */
public class BundleTaskCreator
    implements InternalService, InstallTaskFactory, FrameworkListener, BundleListener {

    /** If this property is set, the bundle is installed if the currently installed version
     * is the version specified by the property.
     */
    private final static String FORCE_INSTALL_VERSION = "force.install.version";

    /** The logger */
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());

    /** Support service for the tasks */
    private TaskSupport taskSupport;

    /** The bundle context. */
    private BundleContext bundleContext;

    /** The retry handler. */
    private RetryHandler retryHandler;

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#init(org.osgi.framework.BundleContext, org.apache.sling.installer.api.ResourceChangeListener, RetryHandler)
     */
    public void init(final BundleContext bc, final ResourceChangeListener listener, final RetryHandler retryHandler) {
        this.bundleContext = bc;
        this.retryHandler = retryHandler;

        this.bundleContext.addBundleListener(this);
        this.bundleContext.addFrameworkListener(this);

        this.taskSupport = new TaskSupport(bc);
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#deactivate()
     */
    public void deactivate() {
        if ( this.bundleContext != null ) {
            this.bundleContext.removeBundleListener(this);
            this.bundleContext.removeFrameworkListener(this);
        }
        if ( this.taskSupport != null ) {
            this.taskSupport.deactivate();
            this.taskSupport = null;
        }
    }

    /**
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public void frameworkEvent(final FrameworkEvent event) {
        if ( event.getType() == FrameworkEvent.PACKAGES_REFRESHED ) {
            logger.debug("Received FrameworkEvent triggering a retry of the installer: {}", event);
            this.retryHandler.scheduleRetry();
        }
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(final BundleEvent event) {
        final int t = event.getType();
        if (t == BundleEvent.INSTALLED || t == BundleEvent.RESOLVED || t == BundleEvent.STARTED || t == BundleEvent.UPDATED) {
            logger.debug("Received BundleEvent triggering a retry of the installer: {}", event);
            this.retryHandler.scheduleRetry();
        }
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#getDescription()
     */
    public String getDescription() {
        return "Apache Sling Bundle Install Task Factory";
    }

	/**
     * Create a bundle task - install, update or remove
     *
	 * @see org.apache.sling.installer.api.tasks.InstallTaskFactory#createTask(org.apache.sling.installer.api.tasks.TaskResourceGroup)
	 */
	public InstallTask createTask(final TaskResourceGroup resourceList) {
	    // quick check of the resource type.
	    final TaskResource toActivate = resourceList.getActiveResource();
	    if ( toActivate.getType().equals(PersistentResourceList.RESTART_ACTIVE_BUNDLES_TYPE) ) {
	        return new RestartActiveBundlesTask(resourceList, this.taskSupport);
	    }
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
                    BundleUtil.clearBundleStart(second);
                    logger.debug("Detected downgrade of bundle {}", symbolicName);
                    result = new ChangeStateTask(resourceList, ResourceState.UNINSTALLED);
	            } else {
	                // prevent uninstalling the installer itself!
	                if ( isInstallerCoreBundle ) {
	                    logger.debug("Prevent completely uninstalling installer bundle {}", symbolicName);
	                    result = new ChangeStateTask(resourceList, ResourceState.UNINSTALLED);
	                } else {
	                    result = new BundleRemoveTask(resourceList, this.taskSupport);
	                }
	            }
		    } else {
	            logger.debug("Bundle {}:{} is not installed anymore - nothing to remove.", symbolicName,
	                    toActivate.getAttribute(Constants.BUNDLE_VERSION));
	            result = new ChangeStateTask(resourceList, ResourceState.UNINSTALLED);
	        }

		// Install
		} else {
		    // check for installer and system update
		    final Integer asyncTaskCounter = (Integer)toActivate.getAttribute(InstallTask.ASYNC_ATTR_NAME);
		    if ( asyncTaskCounter != null ) {
                if ( isInstallerCoreBundle ) {
                    result = new InstallerBundleUpdateTask(resourceList, this.taskSupport);
                } else {
                    // system bundle
                    result = new ChangeStateTask(resourceList, ResourceState.INSTALLED, null,
                                    new String[] {InstallTask.ASYNC_ATTR_NAME});
                }
		    } else {
    		    // for install and update, we want the bundle with the highest version
    	        final BundleInfo info = this.getBundleInfo(symbolicName, null);

    		    // check if we should start the bundle as we installed it in the previous run
    		    if (info == null) {
    			    // bundle is not installed yet: install
    			    result = new BundleInstallTask(resourceList, this.taskSupport);
    		    } else if ( BundleUtil.isBundleStart(toActivate) ) {
    	            result = new BundleStartTask(resourceList, info.id, this.taskSupport);
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

                        // installed, same version but SNAPSHOT
    			        doUpdate = true;
    			    }
                    if (doUpdate) {

                        logger.debug("Scheduling update of {}", toActivate);
                        // check if this is the system bundle
                        if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName) ) {
                            result = new SystemBundleUpdateTask(resourceList, this.taskSupport);
                            // check if this is a installer update
                        } else if ( isInstallerCoreBundle ) {
                            result = new InstallerBundleUpdateTask(resourceList, this.taskSupport);
                        } else {
                            result = new BundleUpdateTask(resourceList, this.taskSupport);
                        }
                    } else if ( compare == 0 && (isInstallerCoreBundle || Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName)) ) {
                        // the installer core bundle / system bundle has been updated, just set state
                        result = new ChangeStateTask(resourceList, ResourceState.INSTALLED);
                    } else {
                        logger.debug("Nothing to install for {}, same version {} already installed.", toActivate, newVersion);
                        result = new ChangeStateTask(resourceList, ResourceState.IGNORED);
                    }
    			}
		    }
            toActivate.setAttribute(FORCE_INSTALL_VERSION, null);
		}
		return result;
	}

    protected BundleInfo getBundleInfo(final String symbolicName, final String version) {
        return BundleInfo.getBundleInfo(this.bundleContext, symbolicName, version);
    }
}
