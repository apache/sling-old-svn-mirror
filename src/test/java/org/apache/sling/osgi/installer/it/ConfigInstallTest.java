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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class ConfigInstallTest extends OsgiInstallerTestBase {
    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
    	return defaultConfiguration();
    }
    
    @Test
    public void TODOReactivateTests() {
    }
    
    /**
    @Test
    public void testInstallAndRemoveConfig() throws Exception {
    	final OsgiInstaller c = getService(OsgiInstaller.class);
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
    	
    	final OsgiInstaller c = getService(OsgiInstaller.class);
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
    */    
}
