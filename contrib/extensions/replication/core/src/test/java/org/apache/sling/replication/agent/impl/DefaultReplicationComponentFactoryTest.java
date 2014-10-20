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
package org.apache.sling.replication.agent.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationComponentProvider;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.agent.impl.DefaultReplicationComponentFactory}
 */
public class DefaultReplicationComponentFactoryTest {

    @Test
    public void testDefaultCreateComponentForAgentByService() throws Exception {
        DefaultReplicationComponentFactory defaultReplicationComponentFactory = new DefaultReplicationComponentFactory();
        String name = "sample-agent";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        ReplicationComponentProvider provider = mock(ReplicationComponentProvider.class);
        ReplicationAgent agent = mock(ReplicationAgent.class);
        when(provider.getComponent(ReplicationAgent.class, name)).thenReturn(agent);
        ReplicationAgent component = defaultReplicationComponentFactory.createComponent(ReplicationAgent.class, properties, provider);
        assertNull(component); // agents cannot be referenced by service name using the factory
    }

    @Test
    public void testDefaultCreateComponentForTriggerByService() throws Exception {
        DefaultReplicationComponentFactory defaultReplicationComponentFactory = new DefaultReplicationComponentFactory();
        String name = "sample-trigger";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        ReplicationComponentProvider provider = mock(ReplicationComponentProvider.class);
        ReplicationTrigger trigger = mock(ReplicationTrigger.class);
        when(provider.getComponent(ReplicationTrigger.class, name)).thenReturn(trigger);
        ReplicationTrigger component = defaultReplicationComponentFactory.createComponent(ReplicationTrigger.class, properties, provider);
        assertNotNull(component);
    }

    @Test
    public void testDefaultCreateComponentForTransportAuthenticationProviderByService() throws Exception {
        DefaultReplicationComponentFactory defaultReplicationComponentFactory = new DefaultReplicationComponentFactory();
        String name = "sample-auth";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        ReplicationComponentProvider provider = mock(ReplicationComponentProvider.class);
        TransportAuthenticationProvider authenticationProvider = mock(TransportAuthenticationProvider.class);
        when(provider.getComponent(TransportAuthenticationProvider.class, name)).thenReturn(authenticationProvider);
        TransportAuthenticationProvider component = defaultReplicationComponentFactory.createComponent(TransportAuthenticationProvider.class, properties, provider);
        assertNotNull(component);
    }

    @Test
    public void testDefaultCreateComponentForImporterByService() throws Exception {
        DefaultReplicationComponentFactory defaultReplicationComponentFactory = new DefaultReplicationComponentFactory();
        String name = "sample-importer";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        ReplicationComponentProvider provider = mock(ReplicationComponentProvider.class);
        ReplicationPackageImporter importer = mock(ReplicationPackageImporter.class);
        when(provider.getComponent(ReplicationPackageImporter.class, name)).thenReturn(importer);
        ReplicationPackageImporter component = defaultReplicationComponentFactory.createComponent(ReplicationPackageImporter.class, properties, provider);
        assertNotNull(component);
    }

    @Test
    public void testDefaultCreateComponentForExporterByService() throws Exception {
        DefaultReplicationComponentFactory defaultReplicationComponentFactory = new DefaultReplicationComponentFactory();
        String name = "sample-exporter";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        ReplicationComponentProvider provider = mock(ReplicationComponentProvider.class);
        ReplicationPackageExporter exporter = mock(ReplicationPackageExporter.class);
        when(provider.getComponent(ReplicationPackageExporter.class, name)).thenReturn(exporter);
        ReplicationPackageExporter component = defaultReplicationComponentFactory.createComponent(ReplicationPackageExporter.class, properties, provider);
        assertNotNull(component);
    }

}