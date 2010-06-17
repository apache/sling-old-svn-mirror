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

import java.io.File;
import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;

public class MockOsgiInstallerContext implements OsgiInstallerContext {

    private final PersistentBundleInfo persistentBundleInfo;

    public MockOsgiInstallerContext() throws IOException {
        final File f = File.createTempFile(MockOsgiInstallerContext.class.getSimpleName(), ".data");
        f.deleteOnExit();
        persistentBundleInfo = new PersistentBundleInfo(this, f);
    }

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

    public String getInstalledBundleDigest(Bundle b) throws IOException {
        return persistentBundleInfo.getDigest(b.getSymbolicName());
    }

    public String getInstalledBundleVersion(String symbolicName) throws IOException {
        return persistentBundleInfo.getInstalledVersion(symbolicName);
    }

    public void saveInstalledBundleInfo(Bundle b, String digest, String version) throws IOException {
        saveInstalledBundleInfo(b.getSymbolicName(), digest, version);
    }

    public void saveInstalledBundleInfo(String symbolicName, String digest, String version) throws IOException {
        persistentBundleInfo.putInfo(symbolicName, digest, version);
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logDebug(java.lang.String, java.lang.Throwable)
     */
    public void logDebug(String message, Throwable t) {
        // nothing to do - no logging
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logDebug(java.lang.String)
     */
    public void logDebug(String message) {
        // nothing to do - no logging
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logInfo(java.lang.String, java.lang.Throwable)
     */
    public void logInfo(String message, Throwable t) {
        // nothing to do - no logging
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logInfo(java.lang.String)
     */
    public void logInfo(String message) {
        // nothing to do - no logging
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logWarn(java.lang.String, java.lang.Throwable)
     */
    public void logWarn(String message, Throwable t) {
        // nothing to do - no logging
    }

    /**
     * @see org.apache.sling.osgi.installer.impl.OsgiInstallerContext#logWarn(java.lang.String)
     */
    public void logWarn(String message) {
        // nothing to do - no logging
    }
}
