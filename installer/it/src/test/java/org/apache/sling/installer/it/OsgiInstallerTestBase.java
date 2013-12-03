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
package org.apache.sling.installer.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/** Base class for OsgiInstaller testing */
public class OsgiInstallerTestBase implements FrameworkListener {
	private final static String POM_VERSION = System.getProperty("osgi.installer.pom.version", "POM_VERSION_NOT_SET");
    private final static String CONFIG_VERSION = System.getProperty("installer.configuration.version", "INSTALLER_VERSION_NOT_SET");

	public final static String JAR_EXT = ".jar";
	private int packageRefreshEventsCount;
	private ServiceTracker configAdminTracker;

	protected OsgiInstaller installer;

    public static final long WAIT_FOR_ACTION_TIMEOUT_MSEC = 6000;
    public static final String BUNDLE_BASE_NAME = "org.apache.sling.installer.it-" + POM_VERSION;

    @Inject
    protected BundleContext bundleContext;

    public static final String URL_SCHEME = "OsgiInstallerTest";

    static abstract class Condition {
    	abstract boolean isTrue() throws Exception;
    	String additionalInfo() { return null; }
    	void onFailure() { }
    	long getMsecBetweenEvaluations() { return 100L; }
    }

    /**
     * Helper method to get a service of the given type
     */
    @SuppressWarnings("unchecked")
	protected <T> T getService(Class<T> clazz) {
    	final ServiceReference ref = bundleContext.getServiceReference(clazz.getName());
    	assertNotNull("getService(" + clazz.getName() + ") must find ServiceReference", ref);
    	final T result = (T)(bundleContext.getService(ref));
    	assertNotNull("getService(" + clazz.getName() + ") must find service", result);
    	return result;
    }

    /** Set up the installer service. */
    protected void setupInstaller() {
        installer = getService(OsgiInstaller.class);
    }

    @Before
    public void setup() {
        configAdminTracker = new ServiceTracker(bundleContext, ConfigurationAdmin.class.getName(), null);
        configAdminTracker.open();
    }

    /** Tear down everything. */
    public void tearDown() {
        synchronized (this) {
            if (configAdminTracker != null) {
                configAdminTracker.close();
                configAdminTracker = null;
            }
        }
    }

    /**
     * Restart the installer.
     */
    protected void restartInstaller() throws BundleException {
        final String symbolicName = "org.apache.sling.installer.core";
        final Bundle b = findBundle(symbolicName);
        if (b == null) {
            fail("Bundle " + symbolicName + " not found");
        }
        log(LogService.LOG_INFO, "Restarting " + symbolicName + " bundle");
        b.stop();
        b.start();
        setupInstaller();
    }

    protected void generateBundleEvent() throws Exception {
        // install a bundle manually to generate a bundle event
        final File f = getTestBundle("org.apache.sling.installer.it-" + POM_VERSION + "-testbundle-1.0.jar");
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
            is.close();
            if (b != null) {
                b.uninstall();
            }
        }
    }

    /**
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
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
    	final ConfigurationAdmin ca = this.waitForConfigAdmin(true);
    	if (ca != null) {
	    	final Configuration[] cfgs = ca.listConfigurations(null);
	    	if (cfgs != null) {
		    	for(Configuration cfg : cfgs) {
		    	    try {
    		    		if(cfg.getPid().equals(pid)) {
    		    			return cfg;
    		    		}
		    	    } catch (IllegalStateException e) {}
		    	}
	    	}
    	}
    	return null;
    }

    protected void waitForCondition(String info, long timeoutMsec, Condition c) throws Exception {
        final long end = System.currentTimeMillis() + timeoutMsec;
        do {
        	if(c.isTrue()) {
        		return;
        	}
        	sleep(c.getMsecBetweenEvaluations());
        } while(System.currentTimeMillis() < end);

        if(c.additionalInfo() != null) {
        	info += " " + c.additionalInfo();
        }

        c.onFailure();
        fail("WaitForCondition failed: " + info);
    }

    protected void waitForConfigValue(String info, String pid, long timeoutMsec, String key, String value) throws Exception {
        final long end = System.currentTimeMillis() + timeoutMsec;
        do {
        	final Configuration c = waitForConfiguration(info, pid, timeoutMsec, true);
        	if(value.equals(c.getProperties().get(key))) {
        		return;
        	}
        	sleep(100L);
        } while(System.currentTimeMillis() < end);
        fail("Did not get " + key + "=" + value + " for config " + pid);
    }

    protected Configuration waitForConfiguration(String info, String pid, long timeoutMsec, boolean shouldBePresent) throws Exception {
        if (info == null) {
            info = "";
        } else {
            info += ": ";
        }

        Configuration result = null;
        final long start = System.currentTimeMillis();
        final long end = start + timeoutMsec;
        log(LogService.LOG_DEBUG, "Starting config check at " + start + "; ending by " + end);
        do {
            result = findConfiguration(pid);
            if ((shouldBePresent && result != null) ||
                    (!shouldBePresent && result == null)) {
                break;
            }
            log(LogService.LOG_DEBUG, "Config check failed at " + System.currentTimeMillis() + "; sleeping");
            sleep(25);
        } while(System.currentTimeMillis() < end);

        if (shouldBePresent && result == null) {
            fail(info + "Configuration not found (" + pid + ")");
        } else if (!shouldBePresent && result != null) {
            fail(info + "Configuration is still present (" + pid + ")");
        }
        return result;
    }

    protected Bundle findBundle(String symbolicName) {
    	for(Bundle b : bundleContext.getBundles()) {
    		if (symbolicName.equals(b.getSymbolicName())) {
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

    protected InstallableResource[] getInstallableResource(File testBundle) throws IOException {
        return getInstallableResource(testBundle, null);
    }

    protected String[] getNonInstallableResourceUrl(File testBundle) throws IOException {
    	return new String[] {testBundle.getAbsolutePath()};
    }

    protected InstallableResource[] getInstallableResource(File testBundle, String digest) throws IOException {
        return getInstallableResource(testBundle, digest, InstallableResource.DEFAULT_PRIORITY);
    }

    protected InstallableResource[] getInstallableResource(File testBundle, String digest, int priority) throws IOException {
        final String url = testBundle.getAbsolutePath();
        if (digest == null) {
            digest = String.valueOf(testBundle.lastModified());
        }
        final InstallableResource result = new MockInstallableResource(url, new FileInputStream(testBundle), digest, null, priority);
        return new InstallableResource[] {result};
    }

    protected InstallableResource[] getInstallableResource(String configPid, Dictionary<String, Object> data) {
        return getInstallableResource(configPid, copy(data), InstallableResource.DEFAULT_PRIORITY);
    }

    protected InstallableResource[] getInstallableResource(String configPid, Dictionary<String, Object> data, int priority) {
        final InstallableResource result = new MockInstallableResource("/" + configPid, copy(data), null, null, priority);
        return new InstallableResource[] {result};
    }

    protected Dictionary<String, Object> copy(Dictionary<String, Object> data) {
        final Dictionary<String, Object> copy = new Hashtable<String, Object>();
        final Enumeration<String> keys = data.keys();
        while(keys.hasMoreElements()) {
            final String key = keys.nextElement();
            copy.put(key, data.get(key));
        }
        return copy;
    }

    protected ConfigurationAdmin waitForConfigAdmin(final boolean shouldBePresent) {
    	ConfigurationAdmin result = null;

        final int timeout = 5;
    	final long waitUntil = System.currentTimeMillis() + (timeout * 1000L);
    	boolean isPresent;
    	do {
    		result = (ConfigurationAdmin)configAdminTracker.getService();
    		isPresent = result != null;
    		if ( shouldBePresent == isPresent ) {
    		    return result;
    		}
    	} while(System.currentTimeMillis() < waitUntil);

        assertEquals("Expected ConfigurationAdmin to be " + (shouldBePresent ? "present" : "absent"),
                shouldBePresent, isPresent);
    	return result;
    }

    protected Bundle getConfigAdminBundle() {
        this.waitForConfigAdmin(true);
        return this.configAdminTracker.getServiceReference().getBundle();
    }

    /**
     * Helper method for sleeping.
     */
    protected void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch(InterruptedException ignored) {
        }
    }

    protected void log(int level, String msg) {
    	final LogService log = getService(LogService.class);
    	log.log(level, msg);
    }

    protected Option[] defaultConfiguration() {
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
    	final String paxDebugLevel = System.getProperty("pax.exam.log.level", "INFO");
    	final String paxDebugPort = System.getProperty("pax.exam.debug.port");
    	if(paxDebugPort != null && paxDebugPort.length() > 0) {
        	vmOpt += " -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + paxDebugPort;
    	}

    	String localRepo = System.getProperty("maven.repo.local", "");

    	return options(
                junitBundles(),
                when( localRepo.length() > 0 ).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
                ),
                systemProperty( "org.ops4j.pax.logging.DefaultServiceLog.level" ).value(paxDebugLevel),
                provision(
                        mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "3.0.0"),
                        mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.2"),

                        mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
                        mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.4"),
                        mavenBundle("org.slf4j", "log4j-over-slf4j", "1.6.4"),

        	            mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.8.0"),
        	            mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.8"),
                        mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.0.2"),
        	        	mavenBundle("org.apache.sling", "org.apache.sling.installer.core", POM_VERSION),
                        mavenBundle("org.apache.sling", "org.apache.sling.installer.factory.configuration", CONFIG_VERSION)
        		)
        );
    }

    protected Object startObservingBundleEvents() {
        final BundleEventListener listener = new BundleEventListener();
        this.bundleContext.addBundleListener(listener);
        return listener;
    }

    public static final class BundleEvent {
        public final String symbolicName;
        public final Version version;
        public final int    state;

        public BundleEvent(final String sn, final String v, final int s) {
            this.symbolicName = sn;
            this.version = (v == null ? null : Version.parseVersion(v));
            this.state = s;
        }

        public BundleEvent(final String sn, final int s) {
            this(sn, null, s);
        }

        @Override
        public String toString() {
            return "BundleEvent " + symbolicName + ", version=" + version + ", state="+state;
        }
    }

    protected void waitForBundleEvents(final String msg, final Object l, BundleEvent... events)
    throws Exception {
        final BundleEventListener listener = (BundleEventListener)l;
        try {
            listener.wait(msg, events, WAIT_FOR_ACTION_TIMEOUT_MSEC);
        } finally {
            this.bundleContext.removeBundleListener(listener);
        }
    }

    protected void waitForBundleEvents(final String msg, final Object l, long timeout, BundleEvent... events)
    throws Exception {
        final BundleEventListener listener = (BundleEventListener)l;
        try {
            listener.wait(msg, events, timeout);
        } finally {
            this.bundleContext.removeBundleListener(listener);
        }
    }

    protected void assertNoBundleEvents(final String msg, final Object l, final String symbolicName) {
        final BundleEventListener listener = (BundleEventListener)l;
        try {
            listener.assertNoBundleEvents(msg, symbolicName);
        } finally {
            this.bundleContext.removeBundleListener(listener);
        }
    }

    protected boolean isPackageExported(Bundle b, String packageName) {
        final BundleWiring wiring = b.adapt(BundleWiring.class);
        assertNotNull("Expecting non-null BundleWiring for bundle " + b, wiring);
        for(BundleCapability c : wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
            if(packageName.equals(c.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
                return true;
            }
        }
        return false;
    }

    public void logInstalledBundles() {
        for(Bundle b : bundleContext.getBundles()) {
            log(LogService.LOG_DEBUG, "Installed bundle: " + b.getSymbolicName());
        }
    }

    private final class BundleEventListener implements SynchronousBundleListener {

        private final List<BundleEvent> events = new ArrayList<BundleEvent>();

        public void bundleChanged(org.osgi.framework.BundleEvent event) {
            synchronized ( this ) {
                events.add(new BundleEvent(event.getBundle().getSymbolicName(), event.getBundle().getVersion().toString(), event.getType()));
            }
        }

        public void wait(final String msg, final BundleEvent[] checkEvents, final long timeoutMsec)
        throws Exception {
            if ( checkEvents == null || checkEvents.length == 0 ) {
                return;
            }
            final long start = System.currentTimeMillis();
            final long end = start + timeoutMsec;
            log(LogService.LOG_DEBUG, "Starting event check at " + start + "; ending by " + end);
            while ( System.currentTimeMillis() < end ) {
                synchronized ( this) {
                    if ( this.events.size() >= checkEvents.length ) {
                        int found = 0;
                        for(final BundleEvent e : checkEvents ) {
                            int startIndex = 0;
                            final int oldFound = found;
                            while ( oldFound == found && startIndex < this.events.size() ) {
                                final BundleEvent bundleEvent = this.events.get(startIndex);
                                // first check symbolic name
                                if ( e.symbolicName == null || e.symbolicName.equals(bundleEvent.symbolicName) ) {
                                    if ( e.version == null || e.version.equals(bundleEvent.version) ) {
                                        if ( e.state == bundleEvent.state ) {
                                            found++;
                                        }
                                    }
                                }
                                if ( oldFound == found ) {
                                    startIndex++;
                                }
                            }
                        }
                        if ( found == checkEvents.length ) {
                            return;
                        }
                    }
                }
                sleep(100);
            }
            logInstalledBundles();
            final StringBuilder sb = new StringBuilder();
            sb.append(msg);
            sb.append(" : Expected events=[\n");
            for(final BundleEvent be : checkEvents) {
                sb.append(be);
                sb.append("\n");
            }
            sb.append("]\nreceived events=[\n");
            for(final BundleEvent be : this.events) {
                sb.append(be);
                sb.append("\n");
            }
            sb.append("]\n");
            fail(sb.toString());
        }

        public void assertNoBundleEvents(final String msg, final String symbolicName) {
            boolean found = false;
            synchronized ( this ) {
                if ( symbolicName == null ) {
                    found = this.events.size() > 0;
                } else {
                    for(BundleEvent e : this.events ) {
                        if ( symbolicName.equals(e.symbolicName) ) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            if ( found ) {
                fail(msg + " : Expected to receive no bundle events for bundle " + symbolicName);
            }
        }
    }
}
