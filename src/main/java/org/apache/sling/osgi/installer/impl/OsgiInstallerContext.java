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
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Installer context, gives access to selected methods of the {@link OsgiInstallerImpl}
 */
public interface OsgiInstallerContext {

    /**
     * Return the bundle context.
     */
    BundleContext getBundleContext();

    /**
     * Return the package admin.
     */
    PackageAdmin getPackageAdmin();

    /**
     * Return the config admin.
     */
    ConfigurationAdmin getConfigurationAdmin();

    void incrementCounter(int index);
    void setCounter(int index, long value);
    /**
     * Finds the bundle with given symbolic name in our BundleContext.
     */
    Bundle getMatchingBundle(String bundleSymbolicName);
    boolean isSnapshot(Version v);

	/** Schedule a task for execution in the current OsgiController cycle */
	void addTaskToCurrentCycle(OsgiInstallerTask t);

	/** Schedule a task for execution in the next OsgiController cycle,
	 * 	usually to indicate that a task must be retried
	 */
	void addTaskToNextCycle(OsgiInstallerTask t);

	/** Store a bundle's digest and installed version, keyed by symbolic ID */
	void saveInstalledBundleInfo(Bundle b, String digest, String version) throws IOException;

	/** Retrieve a bundle's digest that was stored by saveInstalledBundleInfo
	 *  @return null if no digest was stored
	 * */
	String getInstalledBundleDigest(Bundle b) throws IOException;

    /** Retrieve a bundle's version that was stored by saveInstalledBundleInfo
     *  @return null if no version was stored
     * */
    String getInstalledBundleVersion(String symbolicName) throws IOException;
}
