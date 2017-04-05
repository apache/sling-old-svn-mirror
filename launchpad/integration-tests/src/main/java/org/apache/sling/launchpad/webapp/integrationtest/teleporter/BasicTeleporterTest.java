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
package org.apache.sling.launchpad.webapp.integrationtest.teleporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.UUID;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** Basic test of the teleporter mechanism */
public class BasicTeleporterTest {

    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");
    
    @Test
    public void testBundleContext() {
        final BundleContext bc = teleporter.getService(BundleContext.class);
        assertNotNull("Teleporter should provide a BundleContext", bc);
    }
    
    @Test
    public void testConfigAdmin() throws IOException {
        final String pid = "TEST_" + getClass().getName() + UUID.randomUUID();
        
        final ConfigurationAdmin ca = teleporter.getService(ConfigurationAdmin.class);
        assertNotNull("Teleporter should provide a ConfigurationAdmin", ca);
        
        final Configuration cfg = ca.getConfiguration(pid);
        assertNotNull("Expecting to get a Configuration", cfg);
        assertEquals("Expecting the correct pid", pid, cfg.getPid());
    }
}
