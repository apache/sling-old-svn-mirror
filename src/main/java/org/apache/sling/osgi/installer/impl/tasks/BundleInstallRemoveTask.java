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
import java.io.InputStream;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.sling.osgi.installer.impl.OsgiInstallerContext;
import org.apache.sling.osgi.installer.impl.RegisteredResource;
import org.apache.sling.osgi.installer.impl.Storage;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

/** Install/remove task for bundles 
 *  TODO unusued? remove? */
public class BundleInstallRemoveTask extends InstallRemoveTask {

	private final BundleContext bundleContext;
	
    public static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

    public BundleInstallRemoveTask(String uri, RegisteredResource data, OsgiInstallerContext ocs) {
    	super(uri, data, ocs);
    	this.bundleContext = ocs.getBundleContext();
    }
    
	@Override
	public String getSortKey() {
		if(isInstallOrUpdate()) {
			return TaskOrder.BUNDLE_INSTALL_ORDER + uri;
		} else {
			return TaskOrder.BUNDLE_REMOVE_ORDER + uri;
		}
	}

	@Override
	protected void doUninstall(OsgiInstallerContext tctx, Map<String, Object> attributes) throws Exception {
        final Long longId = (Long) attributes.get(Storage.KEY_BUNDLE_ID);
        if (longId == null) {
        	if(ocs.getLogService() != null) {
	    		ocs.getLogService().log(LogService.LOG_DEBUG, 
                "Bundle cannot be uninstalled, bundle id not found, ignored: " + uri);
        	}
        } else {
            final Bundle b = bundleContext.getBundle(longId);
            if (b == null) {
            	if(ocs.getLogService() != null) {
		    		ocs.getLogService().log(LogService.LOG_DEBUG, "Bundle not found, cannot uninstall (" + longId + ")");
            	}
            } else {
            	if(ocs.getLogService() != null) {
		    		ocs.getLogService().log(LogService.LOG_DEBUG, "Uninstalling Bundle " + b.getLocation());
            	}
                b.uninstall();
			    tctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask());
            }
        }
	}

	@Override
	protected boolean doInstallOrUpdate(OsgiInstallerContext tctx, Map<String, Object> attributes) throws Exception {

    	// Check that we have bundle data and manifest
    	InputStream is = data.getInputStream();
    	if(is == null) {
    		throw new IOException("RegisteredResource does not adapt to an InputStream: " + uri);
    	}

        // Update if we already have a bundle id, else install
		Bundle b = null;
		try {
			b = null;

			// check whether we know the bundle and it exists
			final Long longId = (Long) attributes.get(Storage.KEY_BUNDLE_ID);
			if (longId != null) {
			    b = bundleContext.getBundle(longId);
			}

			// either we don't know the bundle yet or it does not exist,
			// so check whether the bundle can be found by its symbolic name
			if (b == null) {
			    b = TaskUtilities.getMatchingBundle(bundleContext, (String)data.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME));
			}

			// If the bundle (or one with the same symbolic name) is
			// already installed, ignore the new one if it's a lower
			// version
			if (b != null) {
				final Version installedVersion = new Version((String)(b.getHeaders().get(Constants.BUNDLE_VERSION)));
				final Version newBundleVersion = (Version)(data.getAttributes().get(Constants.BUNDLE_VERSION));
				if(ignoreNewBundle(b.getSymbolicName(), uri, installedVersion, newBundleVersion)) {
		            return false;
				}
			}

			if (b != null) {
				// Existing bundle -> stop, update, restart
		    	if(ocs.getLogService() != null) {
		    		ocs.getLogService().log(LogService.LOG_INFO, 
		    				"Bundle " + b.getBundleId() + " already present, calling Bundle.stop() and updating " + uri);
		    	}
		        b.stop();
		        b.update(is);
			    tctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask());
			    tctx.addTaskToCurrentCycle(new BundleStartTask(b.getBundleId()));
			} else {
				// New bundle -> install
			    final String fullUri = data.getURL();
			    b = bundleContext.installBundle(fullUri, is);
		    	if(ocs.getLogService() != null) {
		    		ocs.getLogService().log(LogService.LOG_INFO, 
		    				"Bundle was not present, installed " + b.getBundleId() + "/" + fullUri);
		    	}
			    tctx.addTaskToCurrentCycle(new BundleStartTask(b.getBundleId()));
			}
		} finally {
		    // data is never null here
		    try {
				is.close();
			} catch (IOException ioe) {
			}
		}

        // ensure the bundle id in the attributes, this may be overkill
        // in simple update situations, but is required for installations
        // and updates where there are no attributes yet
        attributes.put(Storage.KEY_BUNDLE_ID, b.getBundleId());

        return true;
	}

    /** Decide if new bundle musg be ignored, based on the supplied Versions */
    boolean ignoreNewBundle(String symbolicName, String uri, Version installedVersion, Version newBundleVersion) {
    	
    	boolean ignore = false;
    	final int comparison = newBundleVersion.compareTo(installedVersion);
    	
    	// Same version but snapshots - do not ignore
    	if(comparison == 0  && installedVersion.toString().contains(MAVEN_SNAPSHOT_MARKER)) {
	    	if(ocs.getLogService() != null) {
	    		ocs.getLogService().log(LogService.LOG_DEBUG, 
	    				"Bundle " + symbolicName + " (" + uri + ") "
        				+ " has same version (" + installedVersion + ") than installed bundle, but version contains "
        				+ MAVEN_SNAPSHOT_MARKER + ", will be updated");
    		}
    		
    	} else if (comparison <= 0) {
			ignore = true;
	    	if(ocs.getLogService() != null) {
	    		ocs.getLogService().log(LogService.LOG_DEBUG, 
	    				"Ignoring update of bundle " + symbolicName + " (" + uri
						+ ") as installed version (" + installedVersion + ") is equal or higher than " + newBundleVersion
						);
			}
	    }
		
		return ignore;
    }
}