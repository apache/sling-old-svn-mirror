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
package org.apache.sling.distribution.transport.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.transport.DistributionTransportHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link MultipleEndpointDistributionTransportHandler}
 */
public class MultipleEndpointDistributionTransportHandlerTest {

    @Test
    public void testDeliverPackageWithoutSubHandlers() throws Exception {
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(subHandlers, strategy);
            multipleEndpointdistributionTransportHandler.deliverPackage(resourceResolver, distributionPackage);
        }
    }

    @Test
    public void testRetrievePackagesWithoutSubHandlers() throws Exception {
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(subHandlers, strategy);
            List<DistributionPackage> distributionPackages = multipleEndpointdistributionTransportHandler.retrievePackages(resourceResolver, distributionRequest);
            assertNotNull(distributionPackages);
            assertTrue(distributionPackages.isEmpty());
        }
    }

    @Test
    public void testDeliverPackageWithSubHandlers() throws Exception {
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        subHandlers.add(mock(DistributionTransportHandler.class));
        subHandlers.add(mock(DistributionTransportHandler.class));
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(subHandlers, strategy);
            multipleEndpointdistributionTransportHandler.deliverPackage(resourceResolver, distributionPackage);
        }
    }

    @Test
    public void testRetrievePackagesWithSubHandlers() throws Exception {
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        subHandlers.add(mock(DistributionTransportHandler.class));
        subHandlers.add(mock(DistributionTransportHandler.class));
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(subHandlers, strategy);
            List<DistributionPackage> distributionPackages = multipleEndpointdistributionTransportHandler.retrievePackages(resourceResolver, distributionRequest);
            assertNotNull(distributionPackages);
            assertTrue(distributionPackages.isEmpty());
        }
    }

    @Test
    public void testRetrievePackagesWithOneReturningSubHandlerAndAllStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        DistributionTransportHandler handler1 = mock(DistributionTransportHandler.class);
        List<DistributionPackage> packages1 = new ArrayList<DistributionPackage>();
        packages1.add(mock(DistributionPackage.class));
        packages1.add(mock(DistributionPackage.class));
        when(handler1.retrievePackages(resourceResolver, distributionRequest)).thenReturn(packages1);
        subHandlers.add(handler1);
        subHandlers.add(mock(DistributionTransportHandler.class));
        MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(
                subHandlers, TransportEndpointStrategyType.All);
        List<DistributionPackage> distributionPackages = multipleEndpointdistributionTransportHandler.retrievePackages(resourceResolver, distributionRequest);
        assertNotNull(distributionPackages);
        assertFalse(distributionPackages.isEmpty());
        assertEquals(2, distributionPackages.size());
    }

    @Test
    public void testRetrievePackagesWithOneEmptyOneReturningSubHandlerAndOneStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        subHandlers.add(mock(DistributionTransportHandler.class));
        DistributionTransportHandler handler2 = mock(DistributionTransportHandler.class);
        List<DistributionPackage> packages2 = new ArrayList<DistributionPackage>();
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        when(handler2.retrievePackages(resourceResolver, distributionRequest)).thenReturn(packages2);
        subHandlers.add(handler2);
        MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(
                subHandlers, TransportEndpointStrategyType.One);
        List<DistributionPackage> distributionPackages = multipleEndpointdistributionTransportHandler.retrievePackages(resourceResolver, distributionRequest);
        assertNotNull(distributionPackages);
        assertTrue(distributionPackages.isEmpty());
    }

    @Test
    public void testRetrievePackagesWithTwoReturningSubHandlersAndAllStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        DistributionTransportHandler handler1 = mock(DistributionTransportHandler.class);
        List<DistributionPackage> packages1 = new ArrayList<DistributionPackage>();
        packages1.add(mock(DistributionPackage.class));
        packages1.add(mock(DistributionPackage.class));
        when(handler1.retrievePackages(resourceResolver, distributionRequest)).thenReturn(packages1);
        subHandlers.add(handler1);

        DistributionTransportHandler handler2 = mock(DistributionTransportHandler.class);
        List<DistributionPackage> packages2 = new ArrayList<DistributionPackage>();
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        when(handler2.retrievePackages(resourceResolver, distributionRequest)).thenReturn(packages2);
        subHandlers.add(handler2);

        MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(
                subHandlers, TransportEndpointStrategyType.All);
        List<DistributionPackage> distributionPackages = multipleEndpointdistributionTransportHandler.retrievePackages(resourceResolver, distributionRequest);
        assertNotNull(distributionPackages);
        assertFalse(distributionPackages.isEmpty());
        assertEquals(5, distributionPackages.size());
    }

    @Test
    public void testRetrievePackagesWithTwoReturningSubHandlersAndOneStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<DistributionTransportHandler> subHandlers = new ArrayList<DistributionTransportHandler>();
        DistributionTransportHandler handler1 = mock(DistributionTransportHandler.class);
        List<DistributionPackage> packages1 = new ArrayList<DistributionPackage>();
        packages1.add(mock(DistributionPackage.class));
        packages1.add(mock(DistributionPackage.class));
        when(handler1.retrievePackages(resourceResolver, distributionRequest)).thenReturn(packages1);
        subHandlers.add(handler1);

        DistributionTransportHandler handler2 = mock(DistributionTransportHandler.class);
        List<DistributionPackage> packages2 = new ArrayList<DistributionPackage>();
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        when(handler2.retrievePackages(resourceResolver, distributionRequest)).thenReturn(packages2);
        subHandlers.add(handler2);

        MultipleEndpointDistributionTransportHandler multipleEndpointdistributionTransportHandler = new MultipleEndpointDistributionTransportHandler(
                subHandlers, TransportEndpointStrategyType.One);
        List<DistributionPackage> distributionPackages = multipleEndpointdistributionTransportHandler.retrievePackages(resourceResolver, distributionRequest);
        assertNotNull(distributionPackages);
        assertFalse(distributionPackages.isEmpty());
        assertEquals(2, distributionPackages.size());
    }
}