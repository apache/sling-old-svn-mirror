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
package org.apache.sling.distribution.agent.impl;

import java.util.Arrays;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.simple.SimpleDistributionQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link SimpleDistributionAgent}
 */
public class SimpleDistributionAgentTest {

    @Test
    public void testDistributionWithFailingDistributionStrategy() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy distributionHandler = mock(DistributionQueueDispatchingStrategy.class);
        Iterable<DistributionQueueItemStatus> states = Arrays.asList(new DistributionQueueItemStatus(DistributionQueueItemStatus.ItemState.ERROR, DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME));
        when(distributionHandler.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(states);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, mock(DefaultDistributionLog.class), null);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo());
        when(packageExporter.exportPackages(any(ResourceResolver.class), any(DistributionRequest.class)))
                .thenReturn(Arrays.asList(distributionPackage));
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));
        DistributionResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("ERROR", response.getMessage());
        assertEquals(DistributionRequestState.DROPPED, response.getState());
    }

    @Test
    public void testDistributionWithWorkingDistributionStrategy() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy distributionHandler = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "subServiceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider,
                distributionHandler, distributionEventFactory, resolverFactory, mock(DefaultDistributionLog.class), null);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo());
        Iterable<DistributionQueueItemStatus> states = Arrays.asList(new DistributionQueueItemStatus(DistributionQueueItemStatus.ItemState.QUEUED,
                DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME));
        when(distributionHandler.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(states);
        when(packageExporter.exportPackages(any(ResourceResolver.class), any(DistributionRequest.class)))
                .thenReturn(Arrays.asList(distributionPackage));
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));
        DistributionResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("QUEUED", response.getMessage());
        assertEquals(DistributionRequestState.ACCEPTED, response.getState());
    }

    @Test
    public void testDistribution() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy distributionHandler = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, mock(DefaultDistributionLog.class), null);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo();
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(packageExporter.exportPackages(resourceResolver, request)).thenReturn(Arrays.asList(distributionPackage));
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));

        agent.execute(resourceResolver, request);
    }

    @Test
    public void testGetDefaultQueue() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);

        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy distributionHandler = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, mock(DefaultDistributionLog.class), null);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME))
                .thenReturn(queue);
        assertNotNull(agent.getQueue(""));
    }

    @Test
    public void testGetExistingNamedQueue() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);

        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy distributionHandler = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, mock(DefaultDistributionLog.class), null);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue("priority")).thenReturn(queue);
        assertNotNull(agent.getQueue("priority"));
    }

    @Test
    public void testGetNonExistingNamedQueue() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);

        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy distributionHandler = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, mock(DefaultDistributionLog.class), null);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue("priority")).thenReturn(queue);
        assertNull(agent.getQueue("weird"));
    }
}
