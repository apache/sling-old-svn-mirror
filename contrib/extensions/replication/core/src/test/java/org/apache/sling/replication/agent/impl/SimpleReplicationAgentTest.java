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

import java.util.Arrays;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.replication.agent.ReplicationRequestAuthorizationStrategy;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.event.impl.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.queue.impl.simple.SimpleReplicationQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleReplicationAgent}
 */
public class SimpleReplicationAgentTest {

    @Test
    public void testReplicationWithFailingDistributionStrategy() throws Exception {
        String name = "sample-agent";
        ReplicationPackageImporter packageImporter = mock(ReplicationPackageImporter.class);
        ReplicationPackageExporter packageExporter = mock(ReplicationPackageExporter.class);
        ReplicationRequestAuthorizationStrategy packageExporterStrategy = mock(ReplicationRequestAuthorizationStrategy.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        when(distributionHandler.add(any(String.class), any(ReplicationPackage.class), any(ReplicationQueueProvider.class))).thenReturn(false);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleReplicationAgent agent = new SimpleReplicationAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                replicationEventFactory, resolverFactory,  null);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                ReplicationActionType.ADD, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(replicationPackage.getPaths()).thenReturn(new String[]{"/"});
        when(packageExporter.exportPackages(any(ResourceResolver.class), any(ReplicationRequest.class)))
                .thenReturn(Arrays.asList(replicationPackage));
        when(queueProvider.getDefaultQueue(name)).thenReturn(
                new SimpleReplicationQueue(name, "name"));
        ReplicationResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    public void testReplicationWithWorkingDistributionStrategy() throws Exception {
        String name = "sample-agent";
        ReplicationPackageImporter packageImporter = mock(ReplicationPackageImporter.class);
        ReplicationPackageExporter packageExporter = mock(ReplicationPackageExporter.class);
        ReplicationRequestAuthorizationStrategy packageExporterStrategy = mock(ReplicationRequestAuthorizationStrategy.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
        SimpleReplicationAgent agent = new SimpleReplicationAgent(name,
                false, "subServiceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider,
                distributionHandler, replicationEventFactory, resolverFactory, null);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                ReplicationActionType.ADD, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(replicationPackage.getPaths()).thenReturn(new String[]{"/"});
        when(distributionHandler.add(any(String.class), any(ReplicationPackage.class), eq(queueProvider))).thenReturn(true);
        when(packageExporter.exportPackages(any(ResourceResolver.class), any(ReplicationRequest.class)))
                .thenReturn(Arrays.asList(replicationPackage));
        when(queueProvider.getDefaultQueue(name)).thenReturn(
                new SimpleReplicationQueue(name, "name"));
        ReplicationResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("QUEUED", response.getStatus());
    }

    @Test
    public void testReplication() throws Exception {
        String name = "sample-agent";
        ReplicationPackageImporter packageImporter = mock(ReplicationPackageImporter.class);
        ReplicationPackageExporter packageExporter = mock(ReplicationPackageExporter.class);
        ReplicationRequestAuthorizationStrategy packageExporterStrategy = mock(ReplicationRequestAuthorizationStrategy.class);
        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleReplicationAgent agent = new SimpleReplicationAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                replicationEventFactory, resolverFactory, null);
        ReplicationRequest request = new ReplicationRequest(System.nanoTime(),
                ReplicationActionType.ADD, "/");
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(replicationPackage.getPaths()).thenReturn(new String[]{"/"});
        when(packageExporter.exportPackages(resourceResolver, request)).thenReturn(Arrays.asList(replicationPackage));
        when(queueProvider.getDefaultQueue(name)).thenReturn(
                new SimpleReplicationQueue(name, "name"));

        agent.execute(resourceResolver, request);
    }

    @Test
    public void testGetDefaultQueue() throws Exception {
        String name = "sample-agent";
        ReplicationPackageImporter packageImporter = mock(ReplicationPackageImporter.class);
        ReplicationPackageExporter packageExporter = mock(ReplicationPackageExporter.class);
        ReplicationRequestAuthorizationStrategy packageExporterStrategy = mock(ReplicationRequestAuthorizationStrategy.class);

        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleReplicationAgent agent = new SimpleReplicationAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                replicationEventFactory, resolverFactory, null);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getDefaultQueue(name)).thenReturn(queue);
        assertNotNull(agent.getQueue(null));
    }

    @Test
    public void testGetExistingNamedQueue() throws Exception {
        String name = "sample-agent";
        ReplicationPackageImporter packageImporter = mock(ReplicationPackageImporter.class);
        ReplicationPackageExporter packageExporter = mock(ReplicationPackageExporter.class);
        ReplicationRequestAuthorizationStrategy packageExporterStrategy = mock(ReplicationRequestAuthorizationStrategy.class);

        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleReplicationAgent agent = new SimpleReplicationAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                replicationEventFactory, resolverFactory, null);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getQueue(name, "priority")).thenReturn(queue);
        assertNotNull(agent.getQueue("priority"));
    }

    @Test
    public void testGetNonExistingNamedQueue() throws Exception {
        String name = "sample-agent";
        ReplicationPackageImporter packageImporter = mock(ReplicationPackageImporter.class);
        ReplicationPackageExporter packageExporter = mock(ReplicationPackageExporter.class);
        ReplicationRequestAuthorizationStrategy packageExporterStrategy = mock(ReplicationRequestAuthorizationStrategy.class);

        ReplicationQueueProvider queueProvider = mock(ReplicationQueueProvider.class);
        ReplicationQueueDistributionStrategy distributionHandler = mock(ReplicationQueueDistributionStrategy.class);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleReplicationAgent agent = new SimpleReplicationAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                replicationEventFactory, resolverFactory, null);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        when(queueProvider.getQueue(name, "priority")).thenReturn(queue);
        assertNull(agent.getQueue("weird"));
    }
}
