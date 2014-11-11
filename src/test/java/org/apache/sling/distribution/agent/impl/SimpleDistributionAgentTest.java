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
import org.apache.sling.distribution.agent.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.communication.DistributionActionType;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionResponse;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueDistributionStrategy;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.simple.SimpleDistributionQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
        DistributionQueueDistributionStrategy distributionHandler = mock(DistributionQueueDistributionStrategy.class);
        when(distributionHandler.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(false);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory,  null);
        DistributionRequest request = new DistributionRequest(System.nanoTime(),
                DistributionActionType.ADD, "/");
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(distributionPackage.getPaths()).thenReturn(new String[]{"/"});
        when(packageExporter.exportPackages(any(ResourceResolver.class), any(DistributionRequest.class)))
                .thenReturn(Arrays.asList(distributionPackage));
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));
        DistributionResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    public void testDistributionWithWorkingDistributionStrategy() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDistributionStrategy distributionHandler = mock(DistributionQueueDistributionStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);
        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "subServiceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider,
                distributionHandler, distributionEventFactory, resolverFactory, null);
        DistributionRequest request = new DistributionRequest(System.nanoTime(),
                DistributionActionType.ADD, "/");
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(distributionPackage.getPaths()).thenReturn(new String[]{"/"});
        when(distributionHandler.add(any(DistributionPackage.class), eq(queueProvider))).thenReturn(true);
        when(packageExporter.exportPackages(any(ResourceResolver.class), any(DistributionRequest.class)))
                .thenReturn(Arrays.asList(distributionPackage));
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));
        DistributionResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("QUEUED", response.getStatus());
    }

    @Test
    public void testDistribution() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDistributionStrategy distributionHandler = mock(DistributionQueueDistributionStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, null);
        DistributionRequest request = new DistributionRequest(System.nanoTime(),
                DistributionActionType.ADD, "/");
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(distributionPackage.getPaths()).thenReturn(new String[]{"/"});
        when(packageExporter.exportPackages(resourceResolver, request)).thenReturn(Arrays.asList(distributionPackage));
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
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
        DistributionQueueDistributionStrategy distributionHandler = mock(DistributionQueueDistributionStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, null);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue(DistributionQueueDistributionStrategy.DEFAULT_QUEUE_NAME))
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
        DistributionQueueDistributionStrategy distributionHandler = mock(DistributionQueueDistributionStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, null);
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
        DistributionQueueDistributionStrategy distributionHandler = mock(DistributionQueueDistributionStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler,
                distributionEventFactory, resolverFactory, null);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue("priority")).thenReturn(queue);
        assertNull(agent.getQueue("weird"));
    }
}
