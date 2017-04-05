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
package org.apache.sling.testing.samples.bundlewit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.util.UUID;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** Basic teleported server-side test, demonstrates how
 *  these work.
 *  This is a general Sling test, it does not test anything 
 *  from this bundle.
 */
public class BasicTeleportedIT {
    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "BWIT_Teleporter");

    @Test
    public void testConfigAdmin() throws IOException {
        final String pid = "TEST_" + getClass().getName() + UUID.randomUUID();

        // demonstrate that we can access OSGi services from such 
        // teleported tests
        final ConfigurationAdmin ca = teleporter.getService(ConfigurationAdmin.class);
        assertNotNull("Teleporter should provide a ConfigurationAdmin", ca);

        final Configuration cfg = ca.getConfiguration(pid);
        assertNotNull("Expecting to get a Configuration", cfg);
        assertEquals("Expecting the correct pid", pid, cfg.getPid());
    }    
}