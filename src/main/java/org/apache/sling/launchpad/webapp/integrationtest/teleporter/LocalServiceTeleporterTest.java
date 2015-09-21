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

import org.apache.sling.junit.rules.TeleporterRule;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/** Test registering a service with a local interface in our teleported server-side test class. */
 public class LocalServiceTeleporterTest {

    private final long value = System.currentTimeMillis(); 
    
    private final SomeService serviceImpl = new SomeService() {
        @Override
        public long getValue() {
            return LocalServiceTeleporterTest.this.value;
        }
    };
     
    @Rule
    public final TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "Launchpad");
    
    @Test
    public void testLocalService() {
        final BundleContext bc = teleporter.getService(BundleContext.class);
        final ServiceRegistration reg = bc.registerService(SomeService.class.getName(), serviceImpl, null);
        try {
            final SomeService s = teleporter.getService(SomeService.class);
            assertEquals(value, s.getValue());
        } finally {
            reg.unregister();
        }
    }
}