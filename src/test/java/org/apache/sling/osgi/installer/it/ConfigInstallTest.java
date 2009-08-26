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
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.osgi.installer.InstallableResource;
import org.apache.sling.osgi.installer.OsgiInstaller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;

@RunWith(JUnit4TestRunner.class)

public class ConfigInstallTest extends OsgiInstallerTestBase {
    
    private final static long TIMEOUT = 5000L;
    
    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration() {
        return defaultConfiguration();
    }
    
    @Before
    public void setUp() {
        setupInstaller();
    }
    
    @After
    public void tearDown() {
        super.tearDown();
    }
    
    @Test
    public void testInstallAndRemoveConfig() throws Exception {
        final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
        cfgData.put("foo", "bar");
        final String cfgPid = getClass().getSimpleName() + "." + System.currentTimeMillis();
        final InstallableResource r = getInstallableResource(cfgPid, cfgData); 
        
        assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));
        
        resetCounters();
        installer.addResource(r);
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        
        final Configuration cfg = waitForConfiguration("After installing", cfgPid, TIMEOUT, true);
        assertNotNull("Config " + cfgPid + " must be found after installing", cfg);
        final String value = (String)cfg.getProperties().get("foo");
        assertEquals("Config value must match", "bar", value);
        
        resetCounters();
        installer.removeResource(new InstallableResource(r.getUrl()));
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        
        waitForConfiguration("After removing", cfgPid, TIMEOUT, false);
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
    	
    	final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
    	cfgData.put("foo", "bar");
    	final String cfgPid = getClass().getSimpleName() + ".deferred." + System.currentTimeMillis();
    	assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));

    	// Config installs must be deferred if ConfigAdmin service is stopped
    	configAdmin.stop();
    	waitForConfigAdmin(false);
        resetCounters();
        final InstallableResource r = getInstallableResource(cfgPid, cfgData);
        installer.addResource(r);
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        sleep(1000L);
        configAdmin.start();
    	waitForConfigAdmin(true);
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        waitForConfiguration("Config must be installed once ConfigurationAdmin restarts", 
                cfgPid, TIMEOUT, true);
    	
        configAdmin.stop();
        waitForConfigAdmin(false);
        resetCounters();
        installer.removeResource(new InstallableResource(r.getUrl()));
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        sleep(1000L);
        resetCounters();
        configAdmin.start();
        waitForConfigAdmin(true);
        waitForInstallerAction(OsgiInstaller.WORKER_THREAD_BECOMES_IDLE_COUNTER, 1);
        waitForConfiguration("Config must be removed once ConfigurationAdmin restarts", 
                cfgPid, TIMEOUT, false);
    }
}
