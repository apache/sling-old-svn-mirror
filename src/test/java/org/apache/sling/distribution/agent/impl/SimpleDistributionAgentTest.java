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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.simple.SimpleDistributionQueue;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
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
        Iterable<DistributionQueueItemStatus> states = Collections.singletonList(new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME));
        when(distributionHandler.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(states);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, null, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler, null,
                distributionEventFactory, resolverFactory, mock(SlingRepository.class),  mock(DefaultDistributionLog.class), null, null, 0);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        final DistributionPackage distributionPackage = mock(DistributionPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo("type"));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                ((DistributionPackageProcessor) args[2]).process(distributionPackage);
                return null;
            }
        }).when(packageExporter).exportPackages(any(ResourceResolver.class), any(DistributionRequest.class), any(DistributionPackageProcessor.class));


        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));
        DistributionResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("[ERROR]", response.getMessage());
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
                false, null, "subServiceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider,
                distributionHandler, null, distributionEventFactory, resolverFactory, mock(SlingRepository.class),
                mock(DefaultDistributionLog.class), null, null, 0);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        final DistributionPackage distributionPackage = mock(DistributionPackage.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(distributionPackage.getInfo()).thenReturn(new DistributionPackageInfo("type"));
        Iterable<DistributionQueueItemStatus> states = Collections.singletonList(new DistributionQueueItemStatus(DistributionQueueItemState.QUEUED,
                DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME));
        when(distributionHandler.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(states);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                ((DistributionPackageProcessor) args[2]).process(distributionPackage);
                return null;
            }
        }).when(packageExporter).exportPackages(any(ResourceResolver.class), any(DistributionRequest.class), any(DistributionPackageProcessor.class));
        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));
        DistributionResponse response = agent.execute(resourceResolver, request);
        assertNotNull(response);
        assertEquals("[QUEUED]", response.getMessage());
        assertEquals(DistributionRequestState.ACCEPTED, response.getState());
    }

    @Test
    public void testDistribution() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy requestAuthorizationStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy dispatchingStrategy = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        when(dispatchingStrategy.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(new ArrayList<DistributionQueueItemStatus>());

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, null, "serviceName", packageImporter,
                packageExporter, requestAuthorizationStrategy,
                queueProvider, dispatchingStrategy, null,
                distributionEventFactory, resolverFactory, mock(SlingRepository.class),
                mock(DefaultDistributionLog.class), null, null, 0);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        final DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                ((DistributionPackageProcessor) args[2]).process(distributionPackage);
                return null;
            }
        }).when(packageExporter).exportPackages(any(ResourceResolver.class), any(DistributionRequest.class), any(DistributionPackageProcessor.class));        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));

        agent.execute(resourceResolver, request);
    }

    @Test
    public void testGetExistingNamedQueue() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy authorizationStrategy = mock(DistributionRequestAuthorizationStrategy.class);

        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy dispatchingStrategy = mock(DistributionQueueDispatchingStrategy.class);
        when(dispatchingStrategy.getQueueNames()).thenReturn(Arrays.asList(new String[]{ "priority" }));
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, null, "serviceName", packageImporter,
                packageExporter, authorizationStrategy,
                queueProvider, dispatchingStrategy, null,
                distributionEventFactory, resolverFactory, mock(SlingRepository.class),
                mock(DefaultDistributionLog.class), null, null, 0);
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
                false, null, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, distributionHandler, null,
                distributionEventFactory, resolverFactory, mock(SlingRepository.class),
                mock(DefaultDistributionLog.class), null, null, 0);
        DistributionQueue queue = mock(DistributionQueue.class);
        when(queueProvider.getQueue("priority")).thenReturn(queue);
        assertNull(agent.getQueue("weird"));
    }



    @Test
    public void testDistributionWithAllowedRoot() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy queueDistributionStrategy = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);


        when(queueDistributionStrategy.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(Collections.singletonList(
                new DistributionQueueItemStatus(DistributionQueueItemState.QUEUED, "default")
        ));

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, null, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, queueDistributionStrategy, null,
                distributionEventFactory, resolverFactory, mock(SlingRepository.class),
                mock(DefaultDistributionLog.class), null, new String[] { "/content" }, 0);

        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/content");
        final DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        queueDistributionStrategy.add(distributionPackage, queueProvider);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                ((DistributionPackageProcessor) args[2]).process(distributionPackage);
                return null;
            }
        }).when(packageExporter).exportPackages(any(ResourceResolver.class), any(DistributionRequest.class), any(DistributionPackageProcessor.class));        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));

        DistributionResponse response = agent.execute(resourceResolver, request);

        assertTrue(response.isSuccessful());
    }

    @Test
    public void testDistributionWithDisallowedRoot() throws Exception {
        String name = "sample-agent";
        DistributionPackageImporter packageImporter = mock(DistributionPackageImporter.class);
        DistributionPackageExporter packageExporter = mock(DistributionPackageExporter.class);
        DistributionRequestAuthorizationStrategy packageExporterStrategy = mock(DistributionRequestAuthorizationStrategy.class);
        DistributionQueueProvider queueProvider = mock(DistributionQueueProvider.class);
        DistributionQueueDispatchingStrategy queueDistributionStrategy = mock(DistributionQueueDispatchingStrategy.class);
        DistributionEventFactory distributionEventFactory = mock(DistributionEventFactory.class);
        ResourceResolverFactory resolverFactory = mock(ResourceResolverFactory.class);


        when(queueDistributionStrategy.add(any(DistributionPackage.class), any(DistributionQueueProvider.class))).thenReturn(Collections.singletonList(
                new DistributionQueueItemStatus(DistributionQueueItemState.QUEUED, "default")
        ));

        SimpleDistributionAgent agent = new SimpleDistributionAgent(name,
                false, null, "serviceName", packageImporter,
                packageExporter, packageExporterStrategy,
                queueProvider, queueDistributionStrategy, null,
                distributionEventFactory, resolverFactory, mock(SlingRepository.class),
                mock(DefaultDistributionLog.class), null, new String[] { "/content" }, 0);

        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/home");
        final DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionPackageInfo packageInfo = new DistributionPackageInfo("type");
        when(distributionPackage.getInfo()).thenReturn(packageInfo);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        queueDistributionStrategy.add(distributionPackage, queueProvider);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                ((DistributionPackageProcessor) args[2]).process(distributionPackage);
                return null;
            }
        }).when(packageExporter).exportPackages(any(ResourceResolver.class), any(DistributionRequest.class), any(DistributionPackageProcessor.class));        when(queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME)).thenReturn(
                new SimpleDistributionQueue(name, "name"));

        DistributionResponse response = agent.execute(resourceResolver, request);

        assertFalse(response.isSuccessful());
    }
}
