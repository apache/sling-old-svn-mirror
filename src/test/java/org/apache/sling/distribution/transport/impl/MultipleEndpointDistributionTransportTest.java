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
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionRequestType;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.transport.DistributionTransport;
import org.apache.sling.distribution.transport.DistributionTransport;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link MultipleEndpointDistributionTransport}
 */
public class MultipleEndpointDistributionTransportTest {

    @Test
    public void testDeliverPackageWithoutSubHandlers() throws Exception {
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransport multipleEndpointDistributionTransport = new MultipleEndpointDistributionTransport(subHandlers, strategy);
            multipleEndpointDistributionTransport.deliverPackage(resourceResolver, distributionPackage, secret);
        }
    }

    @Test
    public void testRetrievePackagesWithoutSubHandlers() throws Exception {
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransport multipleEndpointdistributionTransport = new MultipleEndpointDistributionTransport(subHandlers, strategy);
            List<DistributionPackage> distributionPackages = multipleEndpointdistributionTransport.retrievePackages(resourceResolver, distributionRequest, secret);
            assertNotNull(distributionPackages);
            assertTrue(distributionPackages.isEmpty());
        }
    }

    @Test
    public void testDeliverPackageWithSubHandlers() throws Exception {
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        DistributionTransport first = mock(DistributionTransport.class);
        subHandlers.add(first);
        DistributionTransport second = mock(DistributionTransport.class);
        subHandlers.add(second);
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransport multipleEndpointdistributionTransport = new MultipleEndpointDistributionTransport(subHandlers, strategy);
            multipleEndpointdistributionTransport.deliverPackage(resourceResolver, distributionPackage, secret);
        }
    }

    @Test
    public void testRetrievePackagesWithSubHandlers() throws Exception {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        DistributionTransport first = mock(DistributionTransport.class);
        Iterable<DistributionPackage> packages = Collections.emptyList();
        when(first.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages);
        subHandlers.add(first);
        DistributionTransport second = mock(DistributionTransport.class);
        when(second.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages);
        subHandlers.add(second);
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            MultipleEndpointDistributionTransport multipleEndpointDistributionTransport = new MultipleEndpointDistributionTransport(subHandlers, strategy);
            List<DistributionPackage> distributionPackages = multipleEndpointDistributionTransport.retrievePackages(resourceResolver, distributionRequest, secret);
            assertNotNull(distributionPackages);
            assertTrue(distributionPackages.isEmpty());
        }
    }

    @Test
    public void testRetrievePackagesWithOneReturningSubHandlerAndAllStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        DistributionTransport handler1 = mock(DistributionTransport.class);
        List<DistributionPackage> packages1 = new ArrayList<DistributionPackage>();
        packages1.add(mock(DistributionPackage.class));
        packages1.add(mock(DistributionPackage.class));
        when(handler1.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages1);
        subHandlers.add(handler1);
        MultipleEndpointDistributionTransport multipleEndpointDistributionTransport = new MultipleEndpointDistributionTransport(
                subHandlers, TransportEndpointStrategyType.All);
        List<DistributionPackage> distributionPackages = multipleEndpointDistributionTransport.retrievePackages(resourceResolver, distributionRequest, secret);
        assertNotNull(distributionPackages);
        assertFalse(distributionPackages.isEmpty());
        assertEquals(2, distributionPackages.size());
    }

    @Test
    public void testRetrievePackagesWithOneEmptyOneReturningSubHandlerAndOneStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        DistributionTransport handler = mock(DistributionTransport.class);
        when(handler.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(new ArrayList<DistributionPackage>());
        subHandlers.add(handler);
        DistributionTransport handler2 = mock(DistributionTransport.class);
        List<DistributionPackage> packages2 = new ArrayList<DistributionPackage>();
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        when(handler2.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages2);
        subHandlers.add(handler2);
        MultipleEndpointDistributionTransport multipleEndpointDistributionTransport = new MultipleEndpointDistributionTransport(
                subHandlers, TransportEndpointStrategyType.One);
        List<DistributionPackage> distributionPackages = multipleEndpointDistributionTransport.retrievePackages(resourceResolver, distributionRequest, secret);
        assertNotNull(distributionPackages);
        assertTrue(distributionPackages.isEmpty());
    }

    @Test
    public void testRetrievePackagesWithTwoReturningSubHandlersAndAllStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        DistributionTransport handler1 = mock(DistributionTransport.class);
        List<DistributionPackage> packages1 = new ArrayList<DistributionPackage>();
        packages1.add(mock(DistributionPackage.class));
        packages1.add(mock(DistributionPackage.class));
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        when(handler1.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages1);
        subHandlers.add(handler1);

        DistributionTransport handler2 = mock(DistributionTransport.class);
        List<DistributionPackage> packages2 = new ArrayList<DistributionPackage>();
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        when(handler2.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages2);
        subHandlers.add(handler2);

        MultipleEndpointDistributionTransport multipleEndpointDistributionTransport = new MultipleEndpointDistributionTransport(
                subHandlers, TransportEndpointStrategyType.All);
        List<DistributionPackage> distributionPackages = multipleEndpointDistributionTransport.retrievePackages(resourceResolver, distributionRequest, secret);
        assertNotNull(distributionPackages);
        assertFalse(distributionPackages.isEmpty());
        assertEquals(5, distributionPackages.size());
    }

    @Test
    public void testRetrievePackagesWithTwoReturningSubHandlersAndOneStrategy() throws Exception {
        DistributionRequest distributionRequest = new DistributionRequest(DistributionRequestType.ADD, "/");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        List<DistributionTransport> subHandlers = new ArrayList<DistributionTransport>();
        DistributionTransport handler1 = mock(DistributionTransport.class);
        List<DistributionPackage> packages1 = new ArrayList<DistributionPackage>();
        packages1.add(mock(DistributionPackage.class));
        packages1.add(mock(DistributionPackage.class));
        DistributionTransportSecret secret = mock(DistributionTransportSecret.class);
        when(handler1.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages1);
        subHandlers.add(handler1);

        DistributionTransport handler2 = mock(DistributionTransport.class);
        List<DistributionPackage> packages2 = new ArrayList<DistributionPackage>();
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        packages2.add(mock(DistributionPackage.class));
        when(handler2.retrievePackages(resourceResolver, distributionRequest, secret)).thenReturn(packages2);
        subHandlers.add(handler2);

        MultipleEndpointDistributionTransport multipleEndpointDistributionTransport = new MultipleEndpointDistributionTransport(
                subHandlers, TransportEndpointStrategyType.One);
        List<DistributionPackage> distributionPackages = multipleEndpointDistributionTransport.retrievePackages(resourceResolver, distributionRequest, secret);
        assertNotNull(distributionPackages);
        assertFalse(distributionPackages.isEmpty());
        assertEquals(2, distributionPackages.size());
    }
}