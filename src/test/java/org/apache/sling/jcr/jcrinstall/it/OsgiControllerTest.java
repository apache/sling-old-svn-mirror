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
package org.apache.sling.jcr.jcrinstall.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.osgi.installer.DictionaryInstallableData;
import org.apache.sling.osgi.installer.OsgiController;
import org.apache.sling.osgi.installer.OsgiControllerServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** Test the OsgiController running in the OSGi framework */
@RunWith(JUnit4TestRunner.class)
public class OsgiControllerTest {
	public final static String POM_VERSION = System.getProperty("jcrinstall.pom.version");
	
    @Inject
    protected BundleContext bundleContext;
    
    @SuppressWarnings("unchecked")
	protected <T> T getService(Class<T> clazz) {
    	final ServiceReference ref = bundleContext.getServiceReference(clazz.getName());
    	assertNotNull("getService(" + clazz.getName() + ") must find ServiceReference", ref);
    	final T result = (T)(bundleContext.getService(ref));
    	assertNotNull("getService(" + clazz.getName() + ") must find service", result);
    	return result;
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
    
    protected Bundle findBundle(String symbolicName) {
    	for(Bundle b : bundleContext.getBundles()) {
    		if(symbolicName.equals(b.getSymbolicName())) {
    			return b;
    		}
    	}
    	return null;
    }
    
    protected File getTestBundle(String bundleName) {
    	return new File(System.getProperty("jcrinstall.base.dir"), bundleName);
    }
    
    protected void waitForConfigAdmin(boolean shouldBePresent) throws InterruptedException {
    	final OsgiControllerServices svc = getService(OsgiControllerServices.class);
    	final int timeout = 5;
    	final long waitUntil = System.currentTimeMillis() + (timeout * 1000L);
    	do {
    		boolean isPresent = svc.getConfigurationAdmin() != null;
    		if(isPresent == shouldBePresent) {
    			return;
    		}
    		Thread.sleep(100L);
    	} while(System.currentTimeMillis() < waitUntil);
    	fail("ConfigurationAdmin service not available after waiting " + timeout + " seconds");
    }
    
    @Test
    public void testInstallAndRemoveConfig() throws Exception {
    	final OsgiController c = getService(OsgiController.class);
    	final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
    	cfgData.put("foo", "bar");
    	final String cfgPid = getClass().getName() + "." + System.currentTimeMillis();
    	
    	assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));
    	
    	c.scheduleInstallOrUpdate(cfgPid, new DictionaryInstallableData(cfgData));
    	assertNull("Config " + cfgPid + " must not be found right after scheduleInstall", findConfiguration(cfgPid));
    	c.executeScheduledOperations();
    	
    	final Configuration cfg = findConfiguration(cfgPid);
    	assertNotNull("Config " + cfgPid + " must be found right after executeScheduledOperations()", cfg);
    	final String value = (String)cfg.getProperties().get("foo");
    	assertEquals("Config value must match", "bar", value);
    	
    	c.scheduleUninstall(cfgPid);
    	assertNotNull("Config " + cfgPid + " must still be found right after scheduleUninstall", cfg);
    	c.executeScheduledOperations();
    	assertNull("Config " + cfgPid + " must be gone after executeScheduledOperations", findConfiguration(cfgPid));
    }
    
    @Test
    public void testDeferredConfigInstall() throws Exception {
    	
    	final String cfgName = "org.apache.felix.configadmin";
    	Bundle configAdmin = null;
    	for(Bundle b : bundleContext.getBundles()) {
    		if(b.getSymbolicName().equals(cfgName)) {
    			configAdmin = b;
    			break;
    		}
    	}
    	assertNotNull(cfgName + " bundle must be found", configAdmin);
    	waitForConfigAdmin(true);
    	
    	final OsgiController c = getService(OsgiController.class);
    	final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
    	cfgData.put("foo", "bar");
    	final String cfgPid = getClass().getName() + ".deferred." + System.currentTimeMillis();
    	assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));
    	
    	c.scheduleInstallOrUpdate(cfgPid, new DictionaryInstallableData(cfgData));
    	assertNull("Config " + cfgPid + " must not be found right after scheduleInstall", findConfiguration(cfgPid));
    	
    	// Config installs must be deferred if ConfigAdmin service is stopped
    	configAdmin.stop();
    	waitForConfigAdmin(false);
    	c.executeScheduledOperations();
    	configAdmin.start();
    	waitForConfigAdmin(true);
    	assertNull("Config " + cfgPid + " must not be installed if ConfigAdmin was stopped", findConfiguration(cfgPid));
    	
    	// with configadmin back, executeScheduledOperations must install deferred configs
    	c.executeScheduledOperations();
    	assertNotNull("Config " + cfgPid + " must be installed after restarting ConfigAdmin", findConfiguration(cfgPid));
    	findConfiguration(cfgPid).delete();
    	assertNull("Config " + cfgPid + " must be gone after test", findConfiguration(cfgPid));
    }
    
    @Test
    public void testInstallUpgradeDowngradeBundle() throws Exception {
    	final String symbolicName = "jcrinstall-testbundle";
    	final String uri = symbolicName + ".jar";
    	final String BUNDLE_VERSION = "Bundle-Version";
    	
    	assertNull("Test bundle must not be present before test", findBundle(symbolicName));
    	
    	// Install first test bundle and check version
    	long bundleId = 0;
    	final OsgiController c = getService(OsgiController.class);
    	{
        	c.scheduleInstallOrUpdate(uri, new SimpleFileInstallableData(getTestBundle("org.apache.sling.jcr.jcrinstall.it-" + POM_VERSION + "-testbundle-1.1.jar")));
        	assertNull("Test bundle must be absent right after scheduleInstallOrUpdate", findBundle(symbolicName));
        	c.executeScheduledOperations();
        	final Bundle b = findBundle(symbolicName);
        	bundleId = b.getBundleId();
        	assertNotNull("Test bundle 1.1 must be found after executeScheduledOperations", b);
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.1", "1.1", b.getHeaders().get(BUNDLE_VERSION));
    	}
    	
    	// Upgrade to later version, verify
    	{
        	c.scheduleInstallOrUpdate(uri, new SimpleFileInstallableData(getTestBundle("org.apache.sling.jcr.jcrinstall.it-" + POM_VERSION + "-testbundle-1.2.jar")));
        	c.executeScheduledOperations();
        	final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.2 must be found after executeScheduledOperations", b);
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.2 after upgrade", "1.2", b.getHeaders().get(BUNDLE_VERSION));
        	assertEquals("Bundle ID must not change after upgrade", bundleId, b.getBundleId());
    	}
    	
    	// Downgrade to lower version, installed bundle must not change
    	{
        	c.scheduleInstallOrUpdate(uri, new SimpleFileInstallableData(getTestBundle("org.apache.sling.jcr.jcrinstall.it-" + POM_VERSION + "-testbundle-1.0.jar")));
        	c.executeScheduledOperations();
        	final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.2 must be found after executeScheduledOperations", b);
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.2 after ignored downgrade", "1.2", b.getHeaders().get(BUNDLE_VERSION));
        	assertEquals("Bundle ID must not change after downgrade", bundleId, b.getBundleId());
    	}
    	
    	// Uninstall
    	{
        	c.scheduleUninstall(uri);
        	c.executeScheduledOperations();
        	final Bundle b = findBundle(symbolicName);
        	assertNull("Test bundle 1.2 must be gone", b);
    	}
    	
    	// Install lower version, must work
    	{
        	c.scheduleInstallOrUpdate(uri, new SimpleFileInstallableData(getTestBundle("org.apache.sling.jcr.jcrinstall.it-" + POM_VERSION + "-testbundle-1.0.jar")));
        	c.executeScheduledOperations();
        	final Bundle b = findBundle(symbolicName);
        	assertNotNull("Test bundle 1.0 must be found after executeScheduledOperations", b);
        	assertEquals("Installed bundle must be started", Bundle.ACTIVE, b.getState());
        	assertEquals("Version must be 1.0 after uninstall and downgrade", "1.0", b.getHeaders().get(BUNDLE_VERSION));
        	assertFalse("Bundle ID must have changed after uninstall and reinstall", bundleId == b.getBundleId());
    	}
    }

    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
    	String vmOpt = "-Djrcinstall.testing";
    	
    	// This runs in the VM that runs the build, but the tests run in another one.
    	// Make all jcrinstall.* system properties available to OSGi framework VM
    	for(Object o : System.getProperties().keySet()) {
    		final String key = (String)o;
    		if(key.startsWith("jcrinstall.")) {
    			vmOpt += " -D" + key + "=" + System.getProperty(key);
    		}
    	}

    	// optional debugging
    	final String paxDebugPort = System.getProperty("pax.exam.debug.port");
    	if(paxDebugPort != null && paxDebugPort.length() > 0) {
        	vmOpt += " -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + paxDebugPort; 
    	}
    	
        return options(
                felix(),
                vmOption(vmOpt),
                waitForFrameworkStartup(),
        		provision(
        				// TODO use latest scr?
        	            mavenBundle("org.apache.felix", "org.apache.felix.scr"),
        	            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
        	            mavenBundle("org.apache.sling", "org.apache.sling.commons.log"),
        	        	mavenBundle("org.apache.sling", "org.apache.sling.osgi.installer", POM_VERSION)
        		)
        );
    }
}