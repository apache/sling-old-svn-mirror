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
package org.apache.sling.replication.serialization.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.serialization.impl.VoidReplicationPackageBuilder}
 */
public class VoidReplicationPackageBuilderTest {

    @Test
    public void testCreatePackage() throws Exception {
        VoidReplicationPackageBuilder voidReplicationPackageBuilder = new VoidReplicationPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String[] paths = new String[0];
        for (ReplicationActionType action : ReplicationActionType.values()) {
            ReplicationRequest request = mock(ReplicationRequest.class);
            when(request.getAction()).thenReturn(action);
            when(request.getPaths()).thenReturn(paths);
            when(request.getTime()).thenReturn(System.currentTimeMillis());
            ReplicationPackage replicationPackage = voidReplicationPackageBuilder.createPackage(resourceResolver, request);
            assertNotNull(replicationPackage);
        }
    }

    @Test
    public void testReadPackageWithEmptyStream() throws Exception {
        try {
            VoidReplicationPackageBuilder voidReplicationPackageBuilder = new VoidReplicationPackageBuilder();
            ResourceResolver resourceResolver = mock(ResourceResolver.class);
            InputStream stream = mock(InputStream.class);
            voidReplicationPackageBuilder.readPackage(resourceResolver, stream);
            fail("an empty stream should not generate a void package");
        } catch (ReplicationPackageReadingException e) {
            // expected to fail
        }
    }

    @Test
    public void testReadPackageWithAnotherStream() throws Exception {
        VoidReplicationPackageBuilder voidReplicationPackageBuilder = new VoidReplicationPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        InputStream stream = new ByteArrayInputStream("some words".getBytes("UTF-8"));
        ReplicationPackage replicationPackage = voidReplicationPackageBuilder.readPackage(resourceResolver, stream);
        assertNull(replicationPackage);
    }

    @Test
    public void testReadPackageWithValidStream() throws Exception {
        VoidReplicationPackageBuilder voidReplicationPackageBuilder = new VoidReplicationPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        for (ReplicationActionType action : ReplicationActionType.values()) {
            ReplicationRequest request = new ReplicationRequest(action, new String[]{"/"});
            InputStream stream = new VoidReplicationPackage(request).createInputStream();
            ReplicationPackage replicationPackage = voidReplicationPackageBuilder.readPackage(resourceResolver, stream);
            assertNotNull(replicationPackage);
        }
    }

    @Test
    public void testGetNonExistingPackage() throws Exception {
        VoidReplicationPackageBuilder voidReplicationPackageBuilder = new VoidReplicationPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationPackage replicationPackage = voidReplicationPackageBuilder.getPackage(resourceResolver, "missing-id");
        assertNull(replicationPackage);
    }

    @Test
    public void testGetMatchingIdPackage() throws Exception {
        VoidReplicationPackageBuilder voidReplicationPackageBuilder = new VoidReplicationPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String id = new VoidReplicationPackage(new ReplicationRequest(ReplicationActionType.DELETE, new String[]{"/"})).getId();
        ReplicationPackage replicationPackage = voidReplicationPackageBuilder.getPackage(resourceResolver, id);
        assertNotNull(replicationPackage);
    }

    @Test
    public void testInstallDummyPackage() throws Exception {
        VoidReplicationPackageBuilder voidReplicationPackageBuilder = new VoidReplicationPackageBuilder();
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        boolean success = voidReplicationPackageBuilder.installPackage(resourceResolver, replicationPackage);
        assertFalse(success);
    }
}