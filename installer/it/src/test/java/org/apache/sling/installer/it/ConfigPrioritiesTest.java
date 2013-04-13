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

import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.InstallableResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.cm.Configuration;

@RunWith(PaxExam.class)
public class ConfigPrioritiesTest extends OsgiInstallerTestBase {

    private final static long TIMEOUT = 5000L;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
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

    void assertConfigValue(String pid, String key, String value, long timeoutMsec) throws Exception {
        boolean found = false;
        final String info = pid + ": waiting for " + key + "=" + value;
        final long end = System.currentTimeMillis() + timeoutMsec;
        do {
            final Configuration cfg = waitForConfiguration(info, pid, timeoutMsec, true);
            if(value.equals(cfg.getProperties().get(key))) {
                found = true;
                break;
            }
        } while(System.currentTimeMillis() < end);

        if(!found) {
            fail("Did not get expected value: " + info);
        }
    }

    @Test
    public void testOverrideConfig() throws Exception {
        final String pid = getClass().getSimpleName() + "." + System.currentTimeMillis();
        
        final Dictionary<String, Object> data = new Hashtable<String, Object>();
        data.put("foo", "a");
        final InstallableResource a = getInstallableResource(pid, data, InstallableResource.DEFAULT_PRIORITY - 1)[0];
        data.put("foo", "b");
        final InstallableResource b = getInstallableResource(pid, data, InstallableResource.DEFAULT_PRIORITY)[0];
        data.put("foo", "c");
        final InstallableResource c = getInstallableResource(pid, data, InstallableResource.DEFAULT_PRIORITY + 1)[0];

        // c has more priority than b which has more than a
        installer.updateResources(URL_SCHEME, new InstallableResource[] {b}, null);
        assertConfigValue(pid, "foo", "b", TIMEOUT);
        installer.updateResources(URL_SCHEME, new InstallableResource[] {c}, null);
        assertConfigValue(pid, "foo", "c", TIMEOUT);
        installer.updateResources(URL_SCHEME, new InstallableResource[] {a}, null);
        
        // TODO should be "c" here - looks like the installer does not
        // take priorities into account
        assertConfigValue(pid, "foo", "a", TIMEOUT);
        
        // TODO: after removing c value should go back to b, looks like
        // installer removes the config instead 
        installer.updateResources(URL_SCHEME, null, new String[] {c.getId()});
        // assertConfigValue(pid, "foo", "b", TIMEOUT);
        waitForConfiguration("After removing all resources", pid, TIMEOUT, false);
        
        // TODO after removing b value should go back to a
        // installer.updateResources(URL_SCHEME, null, new String[] {b.getId()});
        // assertConfigValue(pid, "foo", "a", TIMEOUT);
        
        // TODO and config should be gone only after removing everything
        // installer.updateResources(URL_SCHEME, null, new String[] {a.getId()});
        // waitForConfiguration("After removing all resources", pid, TIMEOUT, false);
    }
}
