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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.apache.sling.junit.rules.TeleporterRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** Test registering a service with a local interface in our teleported server-side test class. */
 public class LocalServiceTeleporterTest {

    private String value; 
    private ServiceRegistration reg;
    
    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");
    
    @Before
    public void setup() {
        value = UUID.randomUUID().toString();
        final BundleContext bc = teleporter.getService(BundleContext.class);
        
        final SomeService s = new SomeService() {
            @Override
            public String getValue() {
                return LocalServiceTeleporterTest.this.value;
            }
        };
        
        reg = bc.registerService(SomeService.class.getName(), s, null);
    }
    
    @After
    public void cleanup() {
        if(reg != null) {
            reg.unregister();
        }
    }
    
    @Test
    public void testLocalService() {
        final SomeService s = teleporter.getService(SomeService.class);
        assertNotNull("Expecting to get a SomeService instance", s);
        assertEquals(value, s.getValue());
    }
}