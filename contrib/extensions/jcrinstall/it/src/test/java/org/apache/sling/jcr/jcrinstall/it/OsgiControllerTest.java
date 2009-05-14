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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.jcr.jcrinstall.osgiworker.DictionaryInstallableData;
import org.apache.sling.jcr.jcrinstall.osgiworker.InstallableData;
import org.apache.sling.jcr.jcrinstall.osgiworker.OsgiController;
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
	// TODO System properties don't work as this runs in a separate process
	public final static String POM_VERSION = System.getProperty("maven.pom.version");
	
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
    	final Configuration[] cfgs = getService(ConfigurationAdmin.class).listConfigurations(null);
    	if(cfgs != null) {
	    	for(Configuration cfg : cfgs) {
	    		if(cfg.getPid().equals(pid)) {
	    			return cfg;
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
    	// TODO there must be a better way...but tests don't run in the project folder
    	// codebase returns a path like "...sling/contrib/extensions/jcrinstall/it/.$tail..."
    	final URL codebase = getClass().getProtectionDomain().getCodeSource().getLocation();
    	String path = codebase.getFile();
    	path = path.substring(0, path.indexOf(".$"));
    	return new File(path, "/target/testbundles/" + bundleName);
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
    public void testInstallAndRemoveBundle() throws Exception {
    	final File testBundle = getTestBundle("org.apache.felix.webconsole-1.2.8.jar");
    	
    	final String symbolicName = "org.apache.felix.webconsole";
    	final String uri = symbolicName + ".jar";
    	assertNull("Console bundle must not be present before test", findBundle(symbolicName));
    	
    	final OsgiController c = getService(OsgiController.class);
    	final InstallableData d = new SimpleFileInstallableData(testBundle);
    	c.scheduleInstallOrUpdate(uri, d);
    	assertNull("Console bundle must still be absent right after scheduleInstallOrUpdate", findBundle(symbolicName));
    	c.executeScheduledOperations();
    	assertNotNull("Console bundle must be present after executeScheduledOperations", findBundle(symbolicName));
    	
    	c.scheduleUninstall(uri);
    	assertNotNull("Console bundle must still be present after scheduleUninstall", findBundle(symbolicName));
    	c.executeScheduledOperations();
    	assertNull("Console bundle must be gone after executeScheduledOperations", findBundle(symbolicName));
    }


    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
    	final String paxDebugPort = System.getProperty("pax.exam.debug.port");
    	final String debugOptions = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + paxDebugPort; 
    	
        return options(
            mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.0.6"),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.0.10"),
            mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "2.0.2-incubator"),
        	mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcrinstall.osgiworker", POM_VERSION),
            felix(),
            vmOption(paxDebugPort != null && paxDebugPort.length() > 0 ? debugOptions : "-Dno.pax.debugging"),
            waitForFrameworkStartup()
        );
    }
}