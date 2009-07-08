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

import java.text.DecimalFormat;

import org.apache.sling.osgi.installer.impl.OsgiControllerTask;
import org.apache.sling.osgi.installer.impl.OsgiControllerTaskContext;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/** Task that starts a bundle */
public class BundleStartTask extends OsgiControllerTask {

	private final long bundleId;
	private final String sortKey;
	
	public BundleStartTask(long bundleId) {
		this.bundleId = bundleId;
		sortKey = TaskOrder.BUNDLE_START_ORDER + new DecimalFormat("00000").format(bundleId); 
	}
	
	@Override
	public String getSortKey() {
		return sortKey; 
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (bundle " + bundleId + ")";
	}

	public void execute(OsgiControllerTaskContext ctx) throws Exception {
		final Bundle b = ctx.getBundleContext().getBundle(bundleId);
		final LogService log = ctx.getOsgiControllerServices().getLogService();
		
		if(b == null) {
			if(log != null) {
				log.log(LogService.LOG_INFO, "Cannot start bundle, id not found:" + bundleId);
			}
			return;
		}
		
		if(b.getState() == Bundle.ACTIVE) {
			if(log != null) {
				log.log(LogService.LOG_DEBUG, "Bundle already started, no action taken:" + bundleId + "/" + b.getSymbolicName());
			}
		} else {
			if(log != null) {
				log.log(LogService.LOG_INFO, "Starting bundle:" + bundleId + "/" + b.getSymbolicName());
			}
			b.start();
		}
	}
}
