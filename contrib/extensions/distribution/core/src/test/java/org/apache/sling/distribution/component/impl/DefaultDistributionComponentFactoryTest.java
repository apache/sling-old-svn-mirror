/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.component.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.DistributionComponentProvider;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.impl.PersistingJcrEventDistributionTrigger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link DefaultDistributionComponentFactory}
 */
public class DefaultDistributionComponentFactoryTest {

    @Test
    public void testDefaultCreateComponentForAgentByService() throws Exception {
        DefaultDistributionComponentFactory defaultdistributionComponentFactory = new DefaultDistributionComponentFactory();
        String name = "sample-agent";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        DistributionComponentProvider provider = mock(DistributionComponentProvider.class);
        DistributionAgent agent = mock(DistributionAgent.class);
        when(provider.getComponent(DistributionAgent.class, name)).thenReturn(agent);
        try {
            DistributionAgent component = defaultdistributionComponentFactory.createComponent(DistributionAgent.class, properties, provider);

            fail("agents cannot be referenced by service name using the factory");

        }
        catch (IllegalArgumentException e) {
            // expect to fail
        }
    }

    @Test
    public void testDefaultCreateComponentForTriggerByService() throws Exception {
        DefaultDistributionComponentFactory defaultdistributionComponentFactory = new DefaultDistributionComponentFactory();
        String name = "sample-trigger";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        DistributionComponentProvider provider = mock(DistributionComponentProvider.class);
        DistributionTrigger trigger = mock(DistributionTrigger.class);
        when(provider.getComponent(DistributionTrigger.class, name)).thenReturn(trigger);
        DistributionTrigger component = defaultdistributionComponentFactory.createComponent(DistributionTrigger.class, properties, provider);
        assertNotNull(component);
    }

    @Test
    public void testDefaultCreateComponentForTransportAuthenticationProviderByService() throws Exception {
        DefaultDistributionComponentFactory defaultdistributionComponentFactory = new DefaultDistributionComponentFactory();
        String name = "sample-auth";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        DistributionComponentProvider provider = mock(DistributionComponentProvider.class);
        TransportAuthenticationProvider authenticationProvider = mock(TransportAuthenticationProvider.class);
        when(provider.getComponent(TransportAuthenticationProvider.class, name)).thenReturn(authenticationProvider);
        TransportAuthenticationProvider component = defaultdistributionComponentFactory.createComponent(TransportAuthenticationProvider.class, properties, provider);
        assertNotNull(component);
    }

    @Test
    public void testDefaultCreateComponentForImporterByService() throws Exception {
        DefaultDistributionComponentFactory defaultdistributionComponentFactory = new DefaultDistributionComponentFactory();
        String name = "sample-importer";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        DistributionComponentProvider provider = mock(DistributionComponentProvider.class);
        DistributionPackageImporter importer = mock(DistributionPackageImporter.class);
        when(provider.getComponent(DistributionPackageImporter.class, name)).thenReturn(importer);
        DistributionPackageImporter component = defaultdistributionComponentFactory.createComponent(DistributionPackageImporter.class, properties, provider);
        assertNotNull(component);
    }

    @Test
    public void testDefaultCreateComponentForExporterByService() throws Exception {
        DefaultDistributionComponentFactory defaultdistributionComponentFactory = new DefaultDistributionComponentFactory();
        String name = "sample-exporter";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        DistributionComponentProvider provider = mock(DistributionComponentProvider.class);
        DistributionPackageExporter exporter = mock(DistributionPackageExporter.class);
        when(provider.getComponent(DistributionPackageExporter.class, name)).thenReturn(exporter);
        DistributionPackageExporter component = defaultdistributionComponentFactory.createComponent(DistributionPackageExporter.class, properties, provider);
        assertNotNull(component);
    }

    @Test
    public void testPersistingJcrEventTriggerCreation() throws Exception {
        DefaultDistributionComponentFactory defaultdistributionComponentFactory = new DefaultDistributionComponentFactory();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", "persistedJcrEvent");
        properties.put("path", "/content/persistedEvents");
        properties.put("servicename", "distributionService");
        DistributionComponentProvider componentProvider = mock(DistributionComponentProvider.class);
        DistributionTrigger trigger = defaultdistributionComponentFactory.createTrigger(properties, componentProvider);
        assertNotNull(trigger);
        assertEquals(PersistingJcrEventDistributionTrigger.class, trigger.getClass());
    }
}