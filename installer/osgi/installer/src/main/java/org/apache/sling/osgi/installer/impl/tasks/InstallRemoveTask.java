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

import java.util.Map;

import org.apache.sling.osgi.installer.impl.OsgiControllerContext;
import org.apache.sling.osgi.installer.impl.OsgiControllerTask;
import org.apache.sling.osgi.installer.impl.RegisteredResource;

/** Base class for OsgiControllerTasks that install or
 * 	remove OSGi bundles or configs (or deployment packages, etc.)
 */
abstract class InstallRemoveTask extends OsgiControllerTask {

	protected final String uri;
	protected final RegisteredResource data;
	protected final OsgiControllerContext ocs;

    protected InstallRemoveTask(String uri, RegisteredResource data, OsgiControllerContext ocs) {
    	this.uri = uri;
    	this.data = data;
    	this.ocs = ocs;
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

	/** {@inheritDoc} */
	public void execute(OsgiControllerContext context) throws Exception {
		// TODO cleanup stored data? (previously RegisteredResourceWrapper)
	}
	
	/** @return true if this is an install/update task, false if it is an uninstall */
	protected boolean isInstallOrUpdate() {
		return data != null;
	}
	
	/** Do the actual uninstall */
	protected abstract void doUninstall(OsgiControllerContext ctx, Map<String, Object> attributes) throws Exception;
	
	/** Do the actual install or update */
	protected abstract boolean doInstallOrUpdate(OsgiControllerContext ctx, Map<String, Object> attributes) throws Exception;
}
