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
package org.apache.sling.replication.transport.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.transport.ReplicationTransportHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.transport.impl.MultipleEndpointReplicationTransportHandler}
 */
public class MultipleEndpointReplicationTransportHandlerTest {

    @Test
    public void testDeliverPackageWithoutSubHandlers() throws Exception {
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(subHandlers, strategy);
            multipleEndpointReplicationTransportHandler.deliverPackage(resourceResolver, replicationPackage);
        }
    }

    @Test
    public void testRetrievePackagesWithoutSubHandlers() throws Exception {
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationRequest replicationRequest = mock(ReplicationRequest.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(subHandlers, strategy);
            List<ReplicationPackage> replicationPackages = multipleEndpointReplicationTransportHandler.retrievePackages(resourceResolver, replicationRequest);
            assertNotNull(replicationPackages);
            assertTrue(replicationPackages.isEmpty());
        }
    }

    @Test
    public void testDeliverPackageWithSubHandlers() throws Exception {
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        subHandlers.add(mock(ReplicationTransportHandler.class));
        subHandlers.add(mock(ReplicationTransportHandler.class));
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(subHandlers, strategy);
            multipleEndpointReplicationTransportHandler.deliverPackage(resourceResolver, replicationPackage);
        }
    }

    @Test
    public void testRetrievePackagesWithSubHandlers() throws Exception {
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        subHandlers.add(mock(ReplicationTransportHandler.class));
        subHandlers.add(mock(ReplicationTransportHandler.class));
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationRequest replicationRequest = mock(ReplicationRequest.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(subHandlers, strategy);
            List<ReplicationPackage> replicationPackages = multipleEndpointReplicationTransportHandler.retrievePackages(resourceResolver, replicationRequest);
            assertNotNull(replicationPackages);
            assertTrue(replicationPackages.isEmpty());
        }
    }

    @Test
    public void testRetrievePackagesWithOneReturningSubHandlerAndAllStrategy() throws Exception {
        ReplicationRequest replicationRequest = mock(ReplicationRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        ReplicationTransportHandler handler1 = mock(ReplicationTransportHandler.class);
        List<ReplicationPackage> packages1 = new ArrayList<ReplicationPackage>();
        packages1.add(mock(ReplicationPackage.class));
        packages1.add(mock(ReplicationPackage.class));
        when(handler1.retrievePackages(resourceResolver, replicationRequest)).thenReturn(packages1);
        subHandlers.add(handler1);
        subHandlers.add(mock(ReplicationTransportHandler.class));
        MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(
                subHandlers, TransportEndpointStrategyType.All);
        List<ReplicationPackage> replicationPackages = multipleEndpointReplicationTransportHandler.retrievePackages(resourceResolver, replicationRequest);
        assertNotNull(replicationPackages);
        assertFalse(replicationPackages.isEmpty());
        assertEquals(2, replicationPackages.size());
    }

    @Test
    public void testRetrievePackagesWithOneEmptyOneReturningSubHandlerAndOneStrategy() throws Exception {
        ReplicationRequest replicationRequest = mock(ReplicationRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        subHandlers.add(mock(ReplicationTransportHandler.class));
        ReplicationTransportHandler handler2 = mock(ReplicationTransportHandler.class);
        List<ReplicationPackage> packages2 = new ArrayList<ReplicationPackage>();
        packages2.add(mock(ReplicationPackage.class));
        packages2.add(mock(ReplicationPackage.class));
        when(handler2.retrievePackages(resourceResolver, replicationRequest)).thenReturn(packages2);
        subHandlers.add(handler2);
        MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(
                subHandlers, TransportEndpointStrategyType.One);
        List<ReplicationPackage> replicationPackages = multipleEndpointReplicationTransportHandler.retrievePackages(resourceResolver, replicationRequest);
        assertNotNull(replicationPackages);
        assertTrue(replicationPackages.isEmpty());
    }

    @Test
    public void testRetrievePackagesWithTwoReturningSubHandlersAndAllStrategy() throws Exception {
        ReplicationRequest replicationRequest = mock(ReplicationRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        ReplicationTransportHandler handler1 = mock(ReplicationTransportHandler.class);
        List<ReplicationPackage> packages1 = new ArrayList<ReplicationPackage>();
        packages1.add(mock(ReplicationPackage.class));
        packages1.add(mock(ReplicationPackage.class));
        when(handler1.retrievePackages(resourceResolver, replicationRequest)).thenReturn(packages1);
        subHandlers.add(handler1);

        ReplicationTransportHandler handler2 = mock(ReplicationTransportHandler.class);
        List<ReplicationPackage> packages2 = new ArrayList<ReplicationPackage>();
        packages2.add(mock(ReplicationPackage.class));
        packages2.add(mock(ReplicationPackage.class));
        packages2.add(mock(ReplicationPackage.class));
        when(handler2.retrievePackages(resourceResolver, replicationRequest)).thenReturn(packages2);
        subHandlers.add(handler2);

        MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(
                subHandlers, TransportEndpointStrategyType.All);
        List<ReplicationPackage> replicationPackages = multipleEndpointReplicationTransportHandler.retrievePackages(resourceResolver, replicationRequest);
        assertNotNull(replicationPackages);
        assertFalse(replicationPackages.isEmpty());
        assertEquals(5, replicationPackages.size());
    }

    @Test
    public void testRetrievePackagesWithTwoReturningSubHandlersAndOneStrategy() throws Exception {
        ReplicationRequest replicationRequest = mock(ReplicationRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<ReplicationTransportHandler> subHandlers = new ArrayList<ReplicationTransportHandler>();
        ReplicationTransportHandler handler1 = mock(ReplicationTransportHandler.class);
        List<ReplicationPackage> packages1 = new ArrayList<ReplicationPackage>();
        packages1.add(mock(ReplicationPackage.class));
        packages1.add(mock(ReplicationPackage.class));
        when(handler1.retrievePackages(resourceResolver, replicationRequest)).thenReturn(packages1);
        subHandlers.add(handler1);

        ReplicationTransportHandler handler2 = mock(ReplicationTransportHandler.class);
        List<ReplicationPackage> packages2 = new ArrayList<ReplicationPackage>();
        packages2.add(mock(ReplicationPackage.class));
        packages2.add(mock(ReplicationPackage.class));
        packages2.add(mock(ReplicationPackage.class));
        when(handler2.retrievePackages(resourceResolver, replicationRequest)).thenReturn(packages2);
        subHandlers.add(handler2);

        MultipleEndpointReplicationTransportHandler multipleEndpointReplicationTransportHandler = new MultipleEndpointReplicationTransportHandler(
                subHandlers, TransportEndpointStrategyType.One);
        List<ReplicationPackage> replicationPackages = multipleEndpointReplicationTransportHandler.retrievePackages(resourceResolver, replicationRequest);
        assertNotNull(replicationPackages);
        assertFalse(replicationPackages.isEmpty());
        assertEquals(2, replicationPackages.size());
    }
}