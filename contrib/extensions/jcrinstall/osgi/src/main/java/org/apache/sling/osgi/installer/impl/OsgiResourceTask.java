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

import static org.apache.sling.osgi.installer.InstallResultCode.IGNORED;

import java.io.IOException;
import java.util.Map;

import org.apache.sling.osgi.installer.InstallableData;
import org.apache.sling.osgi.installer.OsgiResourceProcessor;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An install/upgrade/uninistall task, meant to be executed
 * 	by the OsgiController worker thread.
 */
class OsgiResourceTask implements OsgiControllerTask {
	
	private final String uri;
	private final InstallableData data;
	
    private static final Logger log = LoggerFactory.getLogger(OsgiResourceTask.class);

    /** Create a task that will install, update or uninstall a resource.
     * @param data if not null, operation is install or update, else uninstall
     */
	OsgiResourceTask(
			String uri, 
			InstallableData data,
			BundleContext bc) throws IOException
	{
		this.uri = uri;
		this.data = (data == null ? null : new InstallableDataWrapper(data, bc));
	}
	
	@Override
	public String toString() {
		return 
			getClass().getSimpleName()
			+ ", "
			+ (isInstallOrUpdate() ? "install/update" : "uninstall")
			+ ", "
			+ uri
		;
	}

	
	public void execute(OsgiControllerTask.Context context) throws Exception {
		// TODO Auto-generated method stub
		if(isInstallOrUpdate()) {
			executeInstallOrUpdate(context);
		} else {
			executeUninstall(context);
		}
		
		// Cleanup InstallableDataWrapper
		if(data instanceof InstallableDataWrapper) {
			((InstallableDataWrapper)data).cleanup();
		}
	}

	boolean isInstallOrUpdate() {
		return data != null;
	}

	private void executeUninstall(OsgiControllerTask.Context context) throws Exception {
        // If a corresponding higher priority resource is installed, ignore this request
        if(context.getResourceOverrideRules() != null) {
            for(String r : context.getResourceOverrideRules().getHigherPriorityResources(uri)) {
                if(context.getStorage().contains(r)) {
                    log.info("Resource {} won't be uninstalled, overridden by {} which has higher priority",
                            uri, r);
                    return;
                }
            }
        }
        
        // let each processor try to uninstall, one of them
    	// should know how that handle uri
    	for(OsgiResourceProcessor p : context.getProcessors()) {
                p.uninstall(uri, context.getStorage().getMap(uri));
    	}
    	
    	context.getStorage().remove(uri);
    	context.getStorage().saveToFile();
	}

	private void executeInstallOrUpdate(OsgiControllerTask.Context context) throws Exception {
        // If a corresponding higher priority resource is already installed, ignore this one
        if(context.getResourceOverrideRules() != null) {
            for(String r : context.getResourceOverrideRules().getHigherPriorityResources(uri)) {
                if(context.getStorage().contains(r)) {
                    log.info("Resource {} ignored, overridden by {} which has higher priority",
                            uri, r);
                    return;
                }
            }
        }
        
        // If a corresponding lower priority resource is installed, uninstall it first
        if(context.getResourceOverrideRules() != null) {
            for(String r : context.getResourceOverrideRules().getLowerPriorityResources(uri)) {
                if(context.getStorage().contains(r)) {
                    log.info("Resource {} overrides {}, uninstalling the latter",
                            uri, r);
                    executeUninstall(context);
                }
            }
        }
        
        // let suitable OsgiResourceProcessor process install
        final OsgiResourceProcessor p = context.getProcessors().getProcessor(uri, data);
        if (p != null) {
            final Map<String, Object> map = context.getStorage().getMap(uri);
            if(p.installOrUpdate(uri, map, data) != IGNORED) {
                map.put(OsgiControllerImpl.KEY_DIGEST, data.getDigest());
            }
            context.getStorage().saveToFile();
        }
        return;
		
	}
}
