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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.apache.sling.osgi.installer.impl.tasks.BundleInstallTask;
import org.apache.sling.osgi.installer.impl.tasks.BundleRemoveTask;
import org.apache.sling.osgi.installer.impl.tasks.BundleUpdateTask;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

/** TaskCreator that processes a list of bundle RegisteredResources */
class BundleTaskCreator {

    /** Store the digests of the bundles for which we create update tasks,
     *  keyed by symbolic name, to avoid generating repated updates
     *  for snapshot bundles
     */
    private final Map<String, String> digests = new HashMap<String, String>();
    
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
	public void createTasks(OsgiInstallerContext ctx, SortedSet<RegisteredResource> resources, SortedSet<OsgiInstallerTask> tasks) throws IOException {
		
		// Find the bundle that must be active: the resources collection is ordered according
		// to priorities, so we just need to find the first one that is installable
		RegisteredResource toActivate = null;
		for(RegisteredResource r : resources) {
			if(toActivate == null && r.isInstallable()) {
				toActivate = r;
				break;
			}
		}

		RegisteredResource toUpdate = null;
		String digestToSave = null;
		final RegisteredResource firstResource = resources.first(); 
		final String symbolicName = firstResource == null ? null : 
		    (String)firstResource.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
		
		if(toActivate == null) {
		    // None of our resources are installable, remove corresponding bundle if present
		    // and if we installed it
		    if(getBundleInfo(ctx, resources.first()) != null) {
		        if(ctx.getInstalledBundleVersion(symbolicName) == null) {
                    if(ctx.getLogService() != null) {
                        ctx.getLogService().log(LogService.LOG_INFO, 
                                "Bundle " + symbolicName 
                                + " was not installed by this module, not removed");
                    }
		        } else {
		            tasks.add(new BundleRemoveTask(resources.first()));
		        }
	        }
			
		} else {
			final BundleInfo info = getBundleInfo(ctx, toActivate);
			final Version newVersion = new Version((String)toActivate.getAttributes().get(Constants.BUNDLE_VERSION));
			if(info == null) {
			    // bundle is not installed yet: install and save digest to avoid
			    // unnecessary updates
				tasks.add(new BundleInstallTask(toActivate));
				digestToSave = toActivate.getDigest();
			} else {
			    final int compare = info.version.compareTo(newVersion); 
                if(compare < 0) {
                    // installed version is lower -> update
                    toUpdate = toActivate;
                } else if(compare > 0) {
	                // installed version is higher -> downgrade only if
                    // we installed that version
                    final String installedVersion = ctx.getInstalledBundleVersion(info.symbolicName);
                    if(info.version.toString().equals(installedVersion)) {
                        toUpdate = toActivate;
                        if(ctx.getLogService() != null) {
                            ctx.getLogService().log(LogService.LOG_INFO, 
                                    "Bundle " + info.symbolicName + " " + installedVersion 
                                    + " was installed by this module, downgrading to " + newVersion);
                        }
                    } else {
                        if(ctx.getLogService() != null) {
                            ctx.getLogService().log(LogService.LOG_INFO, 
                                    "Bundle " + info.symbolicName + " " + installedVersion 
                                    + " was not installed by this module, not downgraded");
                        }
                    }
			    } else if(compare == 0 && ctx.isSnapshot(newVersion)){
			        // installed, same version but SNAPSHOT
                    toUpdate = toActivate;
			    }
			}

			// Save the digest of installed and updated resources, keyed by
			// bundle symbolic name, to avoid unnecessary updates
			if(toUpdate != null) {
			    final String previousDigest = digests.get(symbolicName);
			    if(toUpdate.getDigest().equals(previousDigest)) {
			        if(ctx.getLogService() != null) {
			            ctx.getLogService().log(LogService.LOG_DEBUG, 
			                    "Ignoring update of " + toUpdate + ", digest didn't change");
			        }
                    digestToSave = previousDigest;
			    } else {
                    if(ctx.getLogService() != null) {
                        ctx.getLogService().log(LogService.LOG_DEBUG, 
                                "Scheduling update of " + toUpdate + ", digest has changed");
                    }
			        tasks.add(new BundleUpdateTask(toUpdate));
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

	protected BundleInfo getBundleInfo(OsgiInstallerContext ctx, RegisteredResource bundle) {
		final String symbolicName = (String)bundle.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
		final Bundle b = ctx.getMatchingBundle(symbolicName);
		if(b == null) {
		    return null;
        }
		return new BundleInfo(b);
	}
}
