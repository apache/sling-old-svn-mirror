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
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.event.InstallationEvent;
import org.apache.sling.installer.api.event.InstallationListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

@RunWith(PaxExam.class)

public class ConfigInstallTest extends OsgiInstallerTestBase implements ConfigurationListener {

    private final static long TIMEOUT = 5000L;
    private List<ConfigurationEvent> events = new LinkedList<ConfigurationEvent>();
    private List<ServiceRegistration<?>> serviceRegistrations = new ArrayList<ServiceRegistration<?>>();
    private int installationEvents = 0;
    private static final AtomicInteger counter = new AtomicInteger();

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }
    
    @Before
    public void setUp() {
        installationEvents = 0;
        setupInstaller();
        events.clear();
        serviceRegistrations.clear();
        serviceRegistrations.add(bundleContext.registerService(ConfigurationListener.class.getName(), this, null));
        
        final InstallationListener il = new InstallationListener() {
            public void onEvent(InstallationEvent event) {
                installationEvents++;
            }
        };
        serviceRegistrations.add(bundleContext.registerService(InstallationListener.class.getName(), il, null));
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        for(ServiceRegistration<?> reg : serviceRegistrations) {
        	reg.unregister();
        }
        serviceRegistrations.clear();
    }
    
    private String uniqueID() {
        return counter.incrementAndGet() + "_" + System.currentTimeMillis();
    }
    
    /**
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    public void configurationEvent(ConfigurationEvent e) {
        synchronized ( events ) {
            events.add(e);
        }
	}

	@Test
    public void testInstallAndRemoveConfig() throws Exception {
        final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
        cfgData.put("foo", "bar");
        final String cfgPid = getClass().getSimpleName() + "." + uniqueID();
        assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));

        // install config
        final InstallableResource[] rsrc = getInstallableResource(cfgPid, cfgData);
        installer.updateResources(URL_SCHEME, rsrc, null);

        Configuration cfg = waitForConfiguration("After installing", cfgPid, TIMEOUT, true);
        assertEquals("Config value must match", "bar", cfg.getProperties().get("foo"));

        // remove resource
        installer.updateResources(URL_SCHEME, null, new String[] {rsrc[0].getId()});
        waitForConfiguration("After removing", cfgPid, TIMEOUT, false);

        // Reinstalling with same digest must work
        installer.updateResources(URL_SCHEME, rsrc, null);
        cfg = waitForConfiguration("After reinstalling", cfgPid, TIMEOUT, true);
        assertEquals("Config value must match", "bar", cfg.getProperties().get("foo"));

        // remove again
        installer.updateResources(URL_SCHEME, null, new String[] {rsrc[0].getId()});
        waitForConfiguration("After removing for the second time", cfgPid, TIMEOUT, false);

    }

    @Test
    public void testDeferredConfigInstall() throws Exception {
        // get config admin bundle and wait for service
    	final Bundle configAdmin = this.getConfigAdminBundle();
    	assertNotNull("ConfigAdmin bundle must be found", configAdmin);
    	waitForConfigAdmin(true);

    	// check that configuration is not available
        final String cfgPid = getClass().getSimpleName() + ".deferred." + uniqueID();
        assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));
    	// create new configuration object
    	final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
    	cfgData.put("foo", "bar");

    	// Configuration installs must be deferred if ConfigAdmin service is stopped
        configAdmin.stop();
    	waitForConfigAdmin(false);
    	
    	// add new configuration
        final InstallableResource[] rsrc = getInstallableResource(cfgPid, cfgData);
        installationEvents = 0;
        installer.updateResources(URL_SCHEME, rsrc, null);
        waitForInstallationEvents(2);
        configAdmin.start();
    	waitForConfigAdmin(true);
        waitForConfiguration("Config must be installed once ConfigurationAdmin restarts",
                cfgPid, TIMEOUT, true);

        // Remove config and check
        installer.updateResources(URL_SCHEME, null, new String[] {rsrc[0].getId()});
        waitForConfiguration("Config must be removed once ConfigurationAdmin restarts",
                cfgPid, TIMEOUT, false);
    }

    @Test
    public void testDeferredConfigRemove() throws Exception {
        // get config admin bundle and wait for service
        final Bundle configAdmin = this.getConfigAdminBundle();
        assertNotNull("ConfigAdmin bundle must be found", configAdmin);
        waitForConfigAdmin(true);

        // check that configuration is not available
        final String cfgPid = getClass().getSimpleName() + ".deferred." + uniqueID();
        assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));
        
        // create and install new configuration object, verify
        final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
        cfgData.put("foo", "bar");
        final InstallableResource[] rsrc = getInstallableResource(cfgPid, cfgData);
        installationEvents = 0;
        installer.updateResources(URL_SCHEME, rsrc, null);
        waitForInstallationEvents(2);
        waitForConfiguration("Config must be installed before stopping ConfigurationAdmin",
                cfgPid, TIMEOUT, true);

        // Configuration uninstalls must be deferred if ConfigAdmin service is stopped
        configAdmin.stop();
        waitForConfigAdmin(false);
        
        // remove configuration
        installationEvents = 0;
        installer.updateResources(URL_SCHEME, null, new String[] {rsrc[0].getId()});
        waitForInstallationEvents(2);
        configAdmin.start();
        waitForConfigAdmin(true);
        waitForConfiguration("Config must be removed once ConfigurationAdmin restarts",
                cfgPid, TIMEOUT, false);
    }

    @Test
    public void testReinstallSameConfig() throws Exception {
    	final Dictionary<String, Object> cfgData = new Hashtable<String, Object>();
    	cfgData.put("foo", "bar");
    	final String cfgPid = getClass().getSimpleName() + ".reinstall." + uniqueID();
    	assertNull("Config " + cfgPid + " must not be found before test", findConfiguration(cfgPid));

    	// Install config directly
    	ConfigurationAdmin ca = waitForConfigAdmin(true);
    	final Configuration c = ca.getConfiguration(cfgPid);
    	c.update(cfgData);
        waitForConfigValue("After manual installation", cfgPid, TIMEOUT, "foo", "bar");
        waitForCondition("Expected one ConfigurationEvents since beginning of test", TIMEOUT, new ConfigCondition(cfgPid, 1));

        installer.updateResources(URL_SCHEME, getInstallableResource(cfgPid, cfgData), null);

    	// Reinstalling with a change must be executed
        cfgData.put("foo", "changed");
        installer.updateResources(URL_SCHEME, getInstallableResource(cfgPid, cfgData), null);
        waitForConfigValue("After changing value", cfgPid, TIMEOUT, "foo", "changed");
        waitForCondition("Expected two ConfigurationEvents since beginning of test", TIMEOUT, new ConfigCondition(cfgPid, 2));
    }

    protected final class ConfigCondition extends Condition {

        private final String pid;

        private final int maxCount;

        public ConfigCondition(final String pid, final int count) {
            this.pid = pid;
            this.maxCount = count;
        }

        @Override
        boolean isTrue() throws Exception {
            int count = 0;
            synchronized ( events ) {
                for(final ConfigurationEvent e : events) {
                    if ( pid.equals(e.getPid()) ) {
                        count++;
                    }
                }
            }
            return count == maxCount;
        }

        @Override
        String additionalInfo() {
            final StringBuilder sb = new StringBuilder("Expected ");
            sb.append(maxCount);
            sb.append(" events for ");
            sb.append(pid);
            sb.append(". Received events: [");
            boolean first = true;
            synchronized ( events ) {
                for(final ConfigurationEvent e : events) {
                    if ( !first) {
                        sb.append(", ");
                    }
                    first = false;
                    sb.append(e.getPid());
                    sb.append(':');
                    sb.append(e.getType());
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }
    
    private void waitForInstallationEvents(final int howMany) throws Exception {
        final Condition c = new Condition() {
            @Override
            boolean isTrue() throws Exception {
                return installationEvents >= howMany;
            }
        };
        waitForCondition("Wait for " + howMany + " installation events", TIMEOUT, c);
    }
}
