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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

public class MockOsgiInstallerContext implements OsgiInstallerContext {

	public void addTaskToCurrentCycle(OsgiInstallerTask t) {
	}

	public void addTaskToNextCycle(OsgiInstallerTask t) {
	}

	public BundleContext getBundleContext() {
		return null;
	}

	public ConfigurationAdmin getConfigurationAdmin() {
		return null;
	}

	public LogService getLogService() {
		return null;
	}

	public PackageAdmin getPackageAdmin() {
		return null;
	}

    public void incrementCounter(int index) {
    }
    
    public void setCounter(int index, long value) {
    }

    public Bundle getMatchingBundle(String bundleSymbolicName) {
        return null;
    }
    
	public boolean isSnapshot(Version v) {
		return v.toString().indexOf(OsgiInstallerImpl.MAVEN_SNAPSHOT_MARKER) >= 0;
	}

    public String getBundleDigest(Bundle b) throws IOException {
        return null;
    }

    public void saveBundleDigest(Bundle b, String digest) throws IOException {
    }
}
