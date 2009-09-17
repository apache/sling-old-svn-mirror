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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** OsgiInstaller service implementation */
public class OsgiInstallerImpl implements OsgiInstaller, OsgiInstallerContext {

    public static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";
    
	private final BundleContext bundleContext;
    private final PackageAdmin packageAdmin;
    private final ServiceTracker logServiceTracker;
    private final OsgiInstallerThread installerThread;
    private long [] counters = new long[COUNTERS_SIZE];
    
    public static String BUNDLE_DIGEST_PREFIX = "bundle-digest-";

    public OsgiInstallerImpl(final BundleContext bc,
                              final PackageAdmin pa,
                              final ServiceTracker logServiceTracker)
    throws IOException {
        this.bundleContext = bc;
        this.packageAdmin = pa;
        this.logServiceTracker = logServiceTracker;
        
        installerThread = new OsgiInstallerThread(this);
        installerThread.setDaemon(true);
        installerThread.start();
    }

    public void deactivate() throws InterruptedException {
        installerThread.deactivate();
        
        if(getLogService() != null) {
            getLogService().log(LogService.LOG_INFO, "Waiting for installer thread to stop");
        }
        installerThread.join();
        
        if(getLogService() != null) {
            getLogService().log(LogService.LOG_WARNING,
                    OsgiInstaller.class.getName()
                    + " service deactivated - this warning can be ignored if system is shutting down");
        }
    }

	public ConfigurationAdmin getConfigurationAdmin() {
		if(bundleContext != null) {
		   	final ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
		    if(ref != null) {
		    	return (ConfigurationAdmin)bundleContext.getService(ref);
		    }
		}
		return null;
	}

	public LogService getLogService() {
		return (LogService)logServiceTracker.getService();
	}

	public void addTaskToCurrentCycle(OsgiInstallerTask t) {
		installerThread.addTaskToCurrentCycle(t);
	}

	public void addTaskToNextCycle(OsgiInstallerTask t) {
		if(getLogService() != null) {
			getLogService().log(LogService.LOG_DEBUG, "adding task to next cycle:" + t);
		}
		installerThread.addTaskToNextCycle(t);
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}
	
	public PackageAdmin getPackageAdmin() {
		return packageAdmin;
	}
	
	public long [] getCounters() {
		return counters;
	}

	public void addResource(InstallableResource r) {
	    RegisteredResource rr = null; 
        try {
            rr = new RegisteredResourceImpl(bundleContext, r);
        } catch(IOException ioe) {
            if(getLogService() != null) {
                getLogService().log(
                        LogService.LOG_WARNING,
                        "Cannot create RegisteredResource (resource will be ignored):" + r, ioe);
            }
            return;
        }
        
        synchronized (installerThread) {
            installerThread.addNewResource(rr);
        }
	}

	public void registerResources(Collection<InstallableResource> data, String urlScheme) {
        installerThread.addNewResources(data, urlScheme, bundleContext);
	}

	public void removeResource(InstallableResource r) {
        synchronized (installerThread) {
            installerThread.removeResource(r);
        }
	}

	public void incrementCounter(int index) {
	    counters[index]++;
	}

    public void setCounter(int index, long value) {
        counters[index] = value;
    }
    
    /**
     * Finds the bundle with given symbolic name in our BundleContext.
     */
    public Bundle getMatchingBundle(String bundleSymbolicName) {
        if (bundleSymbolicName != null) {
            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
                    return bundle;
                }
            }
        }
        return null;
    }
    
	public boolean isSnapshot(Version v) {
		return v.toString().indexOf(MAVEN_SNAPSHOT_MARKER) >= 0;
	}

    public String getBundleDigest(Bundle b) throws IOException {
        // TODO it would be cleaner to use a single file to 
        // store those digests - and currently digests files
        // are not purged
        String result = null;
        final File f = getBundleDigestFile(b);
        if(f.exists()) {
            final FileReader fr = new FileReader(f);
            try {
                result = new BufferedReader(fr).readLine();
            } finally {
                fr.close();
            }
        }
        return result;
    }

    public void saveBundleDigest(Bundle b, String digest) throws IOException {
        final File f = getBundleDigestFile(b);
        final FileWriter fw = new FileWriter(f);
        try {
            new PrintWriter(fw).write(digest);
        } finally {
            fw.close();
        }
    }
    
    private File getBundleDigestFile(Bundle b) {
        final String version = (String)b.getHeaders().get(Constants.BUNDLE_VERSION);
        final String filename = BUNDLE_DIGEST_PREFIX + b.getSymbolicName() + version + ".txt";
        return bundleContext.getDataFile(filename);
    }
}