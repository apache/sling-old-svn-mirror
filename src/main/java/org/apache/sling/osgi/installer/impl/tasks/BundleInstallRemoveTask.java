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

import org.apache.sling.osgi.installer.InstallResultCode;
import org.apache.sling.osgi.installer.InstallableData;
import org.apache.sling.osgi.installer.OsgiControllerServices;
import org.apache.sling.osgi.installer.impl.OsgiControllerImpl;
import org.apache.sling.osgi.installer.impl.OsgiControllerTaskContext;
import org.apache.sling.osgi.installer.impl.Storage;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

/** Install/remove task for bundles */
public class BundleInstallRemoveTask extends InstallRemoveTask {

	private final BundleContext bundleContext;
	private final OsgiControllerServices ocs;
	
    public static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

    public BundleInstallRemoveTask(String uri, InstallableData data, BundleContext ctx, OsgiControllerServices ocs) {
    	super(uri, data);
    	this.bundleContext = ctx;
    	this.ocs = ocs;
    }
    
	@Override
	public String getSortKey() {
		if(isInstallOrUpdate()) {
			return TaskOrder.BUNDLE_INSTALL_ORDER + uri;
		} else {
			return TaskOrder.BUNDLE_UNINSTALL_ORDER + uri;
		}
	}

	@Override
	protected void doUninstall(OsgiControllerTaskContext tctx, Map<String, Object> attributes) throws Exception {
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
	protected InstallResultCode doInstallOrUpdate(OsgiControllerTaskContext tctx, Map<String, Object> attributes) throws Exception {
        int retryCount = 0;
        if ( attributes.get("RETRY_COUNT") != null ) {
            retryCount = (Integer)attributes.get("RETRY_COUNT");
        }
        retryCount++;
        attributes.put("RETRY_COUNT", retryCount);

    	// Check that we have bundle data and manifest
    	InputStream is = data.adaptTo(InputStream.class);
    	if(is == null) {
    		throw new IOException("InstallableData does not adapt to an InputStream: " + uri);
    	}

		final Manifest m = TaskUtilities.getManifest(data);
		if(m == null) {
			throw new IOException("Manifest not found for InstallableData " + uri);
		}

        // Update if we already have a bundle id, else install
		Bundle b;
		boolean updated;
		try {
			b = null;
			updated = false;

			// check whether we know the bundle and it exists
			final Long longId = (Long) attributes.get(Storage.KEY_BUNDLE_ID);
			if (longId != null) {
			    b = bundleContext.getBundle(longId);
			}

			// either we don't know the bundle yet or it does not exist,
			// so check whether the bundle can be found by its symbolic name
			if (b == null) {
			    b = TaskUtilities.getMatchingBundle(bundleContext, m);
			}

			// If the bundle (or one with the same symbolic name) is
			// already installed, ignore the new one if it's a lower
			// version
			if (b != null && retryCount == 1) {
				final Version installedVersion = new Version((String)(b.getHeaders().get(Constants.BUNDLE_VERSION)));
				final Version newBundleVersion = new Version(m.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
				if(ignoreNewBundle(b.getSymbolicName(), uri, installedVersion, newBundleVersion)) {
		            return InstallResultCode.IGNORED;
				}
			}

			if (b != null) {
				// Existing bundle -> stop, update, restart
			    if ( retryCount == 1 ) {
			    	if(ocs.getLogService() != null) {
			    		ocs.getLogService().log(LogService.LOG_DEBUG, "Calling Bundle.stop() and updating " + uri);
			    	}
			        b.stop();
			        b.update(is);
	                b.start();
			    } else {
			    	if(ocs.getLogService() != null) {
			    		ocs.getLogService().log(LogService.LOG_DEBUG, "Calling Bundle.start " + uri);
			    	}
	                b.start();
			    }
			    updated = true;
			    tctx.addTaskToCurrentCycle(new SynchronousRefreshPackagesTask());
			} else {
				// New bundle -> install
			    final String fullUri = OsgiControllerImpl.getResourceLocation(uri);
			    int level = data.getBundleStartLevel();
			    if(level > 0) {
			    	throw new BundleException("Non-zero start level is not supported anymore (" + level + ")");
			    }
			    b = bundleContext.installBundle(fullUri, is);
		    	if(ocs.getLogService() != null) {
		    		ocs.getLogService().log(LogService.LOG_DEBUG, "No matching Bundle, installed " + fullUri);
		    	}
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

        // start bundle
        tctx.addTaskToCurrentCycle(new BundleStartTask(b.getBundleId()));
        
        // TODO - remove retry
        // Successful - reset retry count
        retryCount = 0;
        attributes.put("RETRY_COUNT", retryCount);

        return updated ? InstallResultCode.UPDATED : InstallResultCode.INSTALLED;
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