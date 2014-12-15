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
package org.apache.sling.distribution.serialization.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionRequestType;
import org.apache.sling.distribution.communication.SimpleDistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageReadingException;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link VoidDistributionPackageBuilder}
 */
public class VoidDistributionPackageBuilderTest {

    @Test
    public void testCreatePackage() throws Exception {
        VoidDistributionPackageBuilder voiddistributionPackageBuilder = new VoidDistributionPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String[] paths = new String[0];
        for (DistributionRequestType action : DistributionRequestType.values()) {
            DistributionRequest distributionRequest = new SimpleDistributionRequest(action, paths);
            DistributionPackage distributionPackage = voiddistributionPackageBuilder.createPackage(resourceResolver, distributionRequest);
            assertNotNull(distributionPackage);
        }
    }

    @Test
    public void testReadPackageWithEmptyStream() throws Exception {
        try {
            VoidDistributionPackageBuilder voiddistributionPackageBuilder = new VoidDistributionPackageBuilder();
            ResourceResolver resourceResolver = mock(ResourceResolver.class);
            InputStream stream = mock(InputStream.class);
            voiddistributionPackageBuilder.readPackage(resourceResolver, stream);
            fail("an empty stream should not generate a void package");
        } catch (DistributionPackageReadingException e) {
            // expected to fail
        }
    }

    @Test
    public void testReadPackageWithAnotherStream() throws Exception {
        VoidDistributionPackageBuilder voiddistributionPackageBuilder = new VoidDistributionPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        InputStream stream = new ByteArrayInputStream("some words".getBytes("UTF-8"));
        DistributionPackage distributionPackage = voiddistributionPackageBuilder.readPackage(resourceResolver, stream);
        assertNull(distributionPackage);
    }

    @Test
    public void testReadPackageWithValidStream() throws Exception {
        VoidDistributionPackageBuilder voiddistributionPackageBuilder = new VoidDistributionPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        for (DistributionRequestType action : DistributionRequestType.values()) {
            DistributionRequest request = new SimpleDistributionRequest(action, new String[]{"/"});
            InputStream stream = new VoidDistributionPackage(request).createInputStream();
            DistributionPackage distributionPackage = voiddistributionPackageBuilder.readPackage(resourceResolver, stream);
            assertNotNull(distributionPackage);
        }
    }

    @Test
    public void testGetNonExistingPackage() throws Exception {
        VoidDistributionPackageBuilder voiddistributionPackageBuilder = new VoidDistributionPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = voiddistributionPackageBuilder.getPackage(resourceResolver, "missing-id");
        assertNull(distributionPackage);
    }

    @Test
    public void testGetMatchingIdPackage() throws Exception {
        VoidDistributionPackageBuilder voiddistributionPackageBuilder = new VoidDistributionPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String id = new VoidDistributionPackage(new SimpleDistributionRequest(DistributionRequestType.DELETE, new String[]{"/"})).getId();
        DistributionPackage distributionPackage = voiddistributionPackageBuilder.getPackage(resourceResolver, id);
        assertNotNull(distributionPackage);
    }

    @Test
    public void testInstallDummyPackage() throws Exception {
        VoidDistributionPackageBuilder voiddistributionPackageBuilder = new VoidDistributionPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        boolean success = voiddistributionPackageBuilder.installPackage(resourceResolver, distributionPackage);
        assertTrue(success);
    }
}