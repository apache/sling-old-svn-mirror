/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.osgi.installer.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.logProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** Base class for OsgiInstaller testing */
class OsgiInstallerTestBase implements FrameworkListener {
	public final static String POM_VERSION = System.getProperty("osgi.installer.pom.version");
	public final static String JAR_EXT = ".jar";
	private int packageRefreshEventsCount;
	private ServiceTracker configAdminTracker;
	protected OsgiInstaller installer;
	private long [] counters;
	public static final long WAIT_FOR_ACTION_TIMEOUT_MSEC = 5000;
    public static final String BUNDLE_BASE_NAME = "org.apache.sling.osgi.installer.it-" + POM_VERSION;
    
    @Inject
    protected BundleContext bundleContext;
    
    public static final String URL_SCHEME = "OsgiInstallerTest";
    
    @SuppressWarnings("unchecked")
	protected <T> T getService(Class<T> clazz) {
    	final ServiceReference ref = bundleContext.getServiceReference(clazz.getName());
    	assertNotNull("getService(" + clazz.getName() + ") must find ServiceReference", ref);
    	final T result = (T)(bundleContext.getService(ref));
    	assertNotNull("getService(" + clazz.getName() + ") must find service", result);
    	return result;
    }
    
    public void setupInstaller() {
        installer = getService(OsgiInstaller.class);
        resetCounters();
    }
    
    public void tearDown() {
        if(configAdminTracker != null) {
            configAdminTracker.close();
            configAdminTracker = null;
        }
    }
    
    protected void generateBundleEvent() throws Exception {
        // install a bundle manually to generate a bundle event
        final File f = getTestBundle("org.apache.sling.osgi.installer.it-" + POM_VERSION + "-testbundle-1.0.jar");
        final InputStream is = new FileInputStream(f);
        Bundle b = null;
        try {
            b = bundleContext.installBundle(getClass().getName(), is);
            b.start();
            final long timeout = System.currentTimeMillis() + 2000L;
            while(b.getState() != Bundle.ACTIVE && System.currentTimeMillis() < timeout) {
                sleep(10L);
            }
        } finally {
            if(is != null) {
                is.close();
            }
            if(b != null) {
                b.uninstall();
            }
        }
    }
    
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
            packageRefreshEventsCount++;
        }
    }
    
    protected void refreshPackages() {
        bundleContext.addFrameworkListener(this);
        final int MAX_REFRESH_PACKAGES_WAIT_SECONDS = 5;
        final int targetEventCount = packageRefreshEventsCount + 1;
        final long timeout = System.currentTimeMillis() + MAX_REFRESH_PACKAGES_WAIT_SECONDS * 1000L;
        
        final PackageAdmin pa = getService(PackageAdmin.class);
        pa.refreshPackages(null);
        
        try {
            while(true) {
                if(System.currentTimeMillis() > timeout) {
                    break;
                }
                if(packageRefreshEventsCount >= targetEventCount) {
                    break;
                }
                sleep(250L);
            }
        } finally {
            bundleContext.removeFrameworkListener(this);
        }
    }
    
    protected Configuration findConfiguration(String pid) throws Exception {
    	final ConfigurationAdmin ca = getService(ConfigurationAdmin.class);
    	if(ca != null) {
	    	final Configuration[] cfgs = ca.listConfigurations(null);
	    	if(cfgs != null) {
		    	for(Configuration cfg : cfgs) {
		    		if(cfg.getPid().equals(pid)) {
		    			return cfg;
		    		}
		    	}
	    	}
    	}
    	return null;
    }
    
    protected Configuration waitForConfiguration(String info, String pid, long timeoutMsec, boolean shouldBePresent) throws Exception {
        if(info == null) {
            info = "";
        } else {
            info += ": ";
        }
        
        Configuration result = null;
        final long end = System.currentTimeMillis() + timeoutMsec;
        do {
            result = findConfiguration(pid);
            if(result != null) {
                break;
            }
            if(!shouldBePresent && result == null) {
                break;
            }
            sleep(25);
        } while(System.currentTimeMillis() < end);
        
        if(shouldBePresent && result == null) {
            fail(info + "Configuration not found (" + pid + ")");
        } else if(!shouldBePresent && result != null) {
            fail(info + "Configuration is still present (" + pid + ")");
        }
        return result;
    }
    
    protected Bundle findBundle(String symbolicName) {
    	for(Bundle b : bundleContext.getBundles()) {
    		if(symbolicName.equals(b.getSymbolicName())) {
    			return b;
    		}
    	}
    	return null;
    }
    
    protected Bundle assertBundle(String info, String symbolicName, String version, int state) {
        final Bundle b = findBundle(symbolicName);
        if(info == null) {
            info = "";
        } else {
            info += ": ";
        }
        assertNotNull(info + "Expected bundle " + symbolicName + " to be installed", b);
        if(version != null) {
            assertEquals(info + "Expected bundle " + symbolicName + " to be version " + version,
                    version, b.getHeaders().get(Constants.BUNDLE_VERSION));
        }
        if(state >= 0) {
            assertEquals(info + "Expected bundle " + symbolicName + " to be in state " + state,
                    state, b.getState());
        }
        return b;
    }
    
    protected File getTestBundle(String bundleName) {
    	return new File(System.getProperty("osgi.installer.base.dir"), bundleName);
    }
    
    protected InstallableResource getInstallableResource(File testBundle) throws IOException {
        return getInstallableResource(testBundle, null);
    }
    
    protected InstallableResource getInstallableResource(File testBundle, String digest) throws IOException {
        return getInstallableResource(testBundle, digest, InstallableResource.DEFAULT_PRIORITY);
    }
    
    protected InstallableResource getInstallableResource(File testBundle, String digest, int priority) throws IOException {
        final String url = URL_SCHEME + ":" + testBundle.getAbsolutePath();
        if(digest == null) {
            digest = testBundle.getAbsolutePath() + testBundle.lastModified();
        }
        final InstallableResource result = new InstallableResource(url, new FileInputStream(testBundle), digest);
        result.setPriority(priority);
        return result;
    }
    
    protected InstallableResource getInstallableResource(String configPid, Dictionary<String, Object> data) {
        return getInstallableResource(configPid, data, InstallableResource.DEFAULT_PRIORITY);
    }
    
    protected InstallableResource getInstallableResource(String configPid, Dictionary<String, Object> data, int priority) {
        final InstallableResource result = new InstallableResource(URL_SCHEME + ":/" + configPid, data);
        result.setPriority(priority);
        return result;
    }
    
    protected void waitForConfigAdmin(boolean shouldBePresent) {
        if(configAdminTracker == null) {
            synchronized (this) {
                configAdminTracker = new ServiceTracker(bundleContext, ConfigurationAdmin.class.getName(), null);
                configAdminTracker.open();
            }
        }
        
    	final int timeout = 5;
    	final long waitUntil = System.currentTimeMillis() + (timeout * 1000L);
    	do {
    		boolean isPresent = configAdminTracker.getService() != null;
    		if(isPresent == shouldBePresent) {
    			return;
    		}
    		sleep(100L);
    	} while(System.currentTimeMillis() < waitUntil);
    	fail("ConfigurationAdmin service not available after waiting " + timeout + " seconds");
    }
    
    protected void resetCounters() {
        final long [] src = installer.getCounters();
        counters = new long[src.length];
        System.arraycopy(installer.getCounters(), 0, counters, 0, src.length);
    }
    
    protected void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch(InterruptedException ignored) {
        }
    }
    
    protected void waitForInstallerAction(int counterType, long howMany) {
        waitForInstallerAction(null, counterType, howMany);
    }
    
    /** @param howMany negative values means absolute, instead of relative to current value */
    protected void waitForInstallerAction(String info, int counterType, long howMany) {
        if(info == null) {
            info = "";
        } else {
            info += ": ";
        }
        
        final boolean waitForCycles = counterType == OsgiInstaller.INSTALLER_CYCLES_COUNTER;
        
        long targetValue = howMany <  0 ? -howMany : counters[counterType] + howMany;
        if(waitForCycles) {
            // if waiting for installer cycles, get initial value from
            // that counter - we know we want to wait from now on, not from an 
            // earlier resetCounters() call
            targetValue = installer.getCounters()[counterType] + howMany;
        }
        
        final long endTime = System.currentTimeMillis() + WAIT_FOR_ACTION_TIMEOUT_MSEC;
        long lastValue = 0;
        while(System.currentTimeMillis() < endTime) {
            lastValue = installer.getCounters()[counterType];
            if(lastValue >= targetValue) {
                return;
            } else if(waitForCycles && installer.getCounters()[OsgiInstaller.WORKER_THREAD_IS_IDLE_COUNTER] == 1) {
                // Waiting for controller cycles, but worker thread became idle -> ok
                return;
            }
            sleep(10);
        }
        fail(info + "waitForInstallerAction(" + counterType + "," + howMany 
                + ") fails after " + WAIT_FOR_ACTION_TIMEOUT_MSEC + " msec"
                + ", expected value " + targetValue + ", actual " + lastValue);
    }
    
    /** Verify that no OSGi actions are executed in next two installer cycles */
    protected void assertNoOsgiTasks(String info) {
    	final long actionsCounter = installer.getCounters()[OsgiInstaller.OSGI_TASKS_COUNTER];
    	waitForInstallerAction(OsgiInstaller.INSTALLER_CYCLES_COUNTER, 2);
    	assertEquals(info + ": OSGi tasks counter should not have changed", 
    			actionsCounter, installer.getCounters()[OsgiInstaller.OSGI_TASKS_COUNTER]);
    }
    
    public void assertCounter(int index, long value) {
        assertEquals("Expected value matches for counter " + index, value, installer.getCounters()[index]);
    }
    
    public static Option[] defaultConfiguration() {
    	String vmOpt = "-Dosgi.installer.testing";
    	
    	// This runs in the VM that runs the build, but the tests run in another one.
    	// Make all osgi.installer.* system properties available to OSGi framework VM
    	for(Object o : System.getProperties().keySet()) {
    		final String key = (String)o;
    		if(key.startsWith("osgi.installer.")) {
    			vmOpt += " -D" + key + "=" + System.getProperty(key);
    		}
    	}

    	// optional debugging
    	final String paxDebugLevel = System.getProperty("pax.exam.log.level");
    	final String paxDebugPort = System.getProperty("pax.exam.debug.port");
    	if(paxDebugPort != null && paxDebugPort.length() > 0) {
        	vmOpt += " -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + paxDebugPort; 
    	}
    	
        return options(
                felix(),
                vmOption(vmOpt),
                waitForFrameworkStartup(),
                
                logProfile(),
                systemProperty( "org.ops4j.pax.logging.DefaultServiceLog.level" ).value(paxDebugLevel),
                
                provision(
        	            mavenBundle("org.apache.felix", "org.apache.felix.scr"),
        	            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
        	            mavenBundle("org.apache.sling", "org.apache.sling.commons.log"),
        	        	mavenBundle("org.apache.sling", "org.apache.sling.osgi.installer", POM_VERSION)
        		)
        );
    }
}