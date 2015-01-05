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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.installer.api.InstallableResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class ConfigPrioritiesTest extends OsgiInstallerTestBase {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return defaultConfiguration();
    }

    @Before
    public void setUp() {
        setupInstaller();
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
    }

   @Test
    public void testOverrideConfig() throws Exception {
        final String pid = getClass().getSimpleName() + "." + System.currentTimeMillis();

        final Dictionary<String, Object> data = new Hashtable<String, Object>();
        data.put("foo", "a");
        final InstallableResource a = getInstallableResource("a/" + pid, data, InstallableResource.DEFAULT_PRIORITY - 1)[0];
        data.put("foo", "b");
        final InstallableResource b = getInstallableResource("b/" + pid, data, InstallableResource.DEFAULT_PRIORITY)[0];
        data.put("foo", "c");
        final InstallableResource c = getInstallableResource("c/" + pid, data, InstallableResource.DEFAULT_PRIORITY + 1)[0];

        // c has more priority than b which has more than a
        installer.updateResources(URL_SCHEME, new InstallableResource[] {b}, null);
        waitForConfigValue(null, pid, "foo", "b");
        installer.updateResources(URL_SCHEME, new InstallableResource[] {c}, null);
        waitForConfigValue(null, pid, "foo", "c");
        installer.updateResources(URL_SCHEME, new InstallableResource[] {a}, null);

        // highest prio should be active (c)
        waitForConfigValue(null, pid, "foo", "c");

        // removing c, second highest prio should be active (b)
        installer.updateResources(URL_SCHEME, null, new String[] {c.getId()});
        waitForConfigValue(null, pid, "foo", "b");

        // removing b, a should be active
        installer.updateResources(URL_SCHEME, null, new String[] {b.getId()});
        waitForConfigValue(null, pid, "foo", "a");

        // and config should be gone only after removing everything
        installer.updateResources(URL_SCHEME, null, new String[] {a.getId()});
        waitForConfiguration("After removing all resources", pid, false);
    }
}
