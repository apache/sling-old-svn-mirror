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

import java.util.SortedSet;

import org.apache.sling.osgi.installer.impl.tasks.BundleInstallTask;
import org.apache.sling.osgi.installer.impl.tasks.BundleRemoveTask;
import org.apache.sling.osgi.installer.impl.tasks.BundleUpdateTask;
import org.apache.sling.osgi.installer.impl.tasks.TaskUtilities;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/** TaskCreator that processes a list of bundle RegisteredResources */
class BundleTaskCreator implements OsgiInstallerThread.TaskCreator {

    public static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";
    
    /** Holds the bundle info that we need, makes it easier to test
     *  without an OSGi framework */ 
    static class BundleInfo {
        final Version version;
        final int state;
        
        BundleInfo(Version version, int state) {
            this.version = version;
            this.state = state;
        }
        
        BundleInfo(Bundle b) {
            this.version = new Version((String)b.getHeaders().get(Constants.BUNDLE_VERSION));
            this.state = b.getState();
        }
    }
    
	/** Create tasks for a set of RegisteredResource that all represent the same bundle.
	 * 	Selects the bundle with the highest priority (i.e. the first one in the group that
	 *  has desired state == active, and generates the appropriate OSGi tasks to
	 *  reach this state. 
	 */
	public void createTasks(BundleContext ctx, SortedSet<RegisteredResource> resources, SortedSet<OsgiInstallerTask> tasks) {
		
		// Find the bundle that must be active: the resources collection is ordered according
		// to priorities, so we just need to find the first one that is installable
		RegisteredResource toActivate = null;
		for(RegisteredResource r : resources) {
			if(toActivate == null && r.isInstallable()) {
				toActivate = r;
				break;
			}
		}

		if(toActivate == null) {
		    // None of our resources are installable, remove corresponding bundle
		    final BundleInfo info = getBundleInfo(ctx, resources.first());
		    if(info != null) {
	            tasks.add(new BundleRemoveTask(resources.first()));
	        }
			
		} else {
			final BundleInfo info = getBundleInfo(ctx, toActivate);
			final Version newVersion = (Version)toActivate.getAttributes().get(Constants.BUNDLE_VERSION);
			if(info == null) {
			    // bundle is not installed yet
				tasks.add(new BundleInstallTask(toActivate));
			} else {
			    final int compare = info.version.compareTo(newVersion); 
			    if(compare != 0) {
	                // installed but different version. Can be a later version if 
			        // the newer version resource was removed, in case we downgrade
	                tasks.add(new BundleUpdateTask(toActivate));
			    } else if(compare == 0 && newVersion.toString().indexOf(MAVEN_SNAPSHOT_MARKER) >= 0){
			        // installed, same version but SNAPSHOT
			        // TODO: update only if not updated with same digest yet
			        tasks.add(new BundleUpdateTask(toActivate));
			    }
			}
		}
	}

	protected BundleInfo getBundleInfo(BundleContext ctx, RegisteredResource bundle) {
		final String symbolicName = (String)bundle.getAttributes().get(Constants.BUNDLE_SYMBOLICNAME);
		final Bundle b = TaskUtilities.getMatchingBundle(ctx, symbolicName);
		if(b == null) {
		    return null;
        }
		return new BundleInfo(b);
	}
}
