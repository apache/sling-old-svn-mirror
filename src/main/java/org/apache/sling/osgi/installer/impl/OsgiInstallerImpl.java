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
import java.util.Collection;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.apache.sling.osgi.installer.OsgiInstallerStatistics;
import org.osgi.framework.BundleContext;

/** OsgiInstaller service implementation */
public class OsgiInstallerImpl
    implements OsgiInstaller, OsgiInstallerStatistics {

    /** The bundle context. */
	private final BundleContext bundleContext;

	/** The actual worker thread. */
    private final OsgiInstallerThread installerThread;

    /**
     * Construct a new service
     */
    public OsgiInstallerImpl(final BundleContext bc)
    throws IOException {
        this.bundleContext = bc;

        installerThread = new OsgiInstallerThread(bc);
        installerThread.setDaemon(true);
        installerThread.start();
    }

    /**
     * Deactivate this service.
     */
    public void deactivate() {
        installerThread.deactivate();

        Logger.logInfo("Waiting for installer thread to stop");
        try {
            installerThread.join();
        } catch (InterruptedException e) {
            // we simply ignore this
        }

        Logger.logWarn(OsgiInstaller.class.getName()
                + " service deactivated - this warning can be ignored if system is shutting down");
    }

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#addResource(java.lang.String, org.apache.sling.osgi.installer.InstallableResource)
	 */
	public void addResource(final String scheme, final InstallableResource r) {
        installerThread.addNewResource(r, scheme);
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#registerResources(java.lang.String, java.util.Collection)
	 */
	public void registerResources(final String urlScheme, final Collection<InstallableResource> data) {
        installerThread.addNewResources(data, urlScheme, bundleContext);
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstaller#removeResource(java.lang.String, String)
	 */
	public void removeResource(final String scheme, final String url) {
        installerThread.removeResource(url, scheme);
	}

	/**
	 * @see org.apache.sling.osgi.installer.OsgiInstallerStatistics#getCounters()
	 */
	public long[] getCounters() {
        return this.installerThread.getCounters();
    }
}