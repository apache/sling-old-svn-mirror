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
package org.apache.sling.distribution.packaging.impl;

import java.io.InputStream;
import java.util.HashMap;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ReferencePackage}
 */
public class ReferencePackageTest {

    @Test
    public void testAcquire() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        referencePackage.acquire("queue1", "queue2");
    }

    @Test
    public void testRelease() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        referencePackage.release("queue1", "queue2");
    }

    @Test
    public void testCreateInputStream() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        InputStream inputStream = referencePackage.createInputStream();
        assertNotNull(inputStream);
    }

    @Test
    public void testGetSize() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        when(distributionPackage.getSize()).thenReturn(10L);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        long size = referencePackage.getSize();
        assertTrue(referencePackage.getSize() == size);
    }

    @Test
    public void testClose() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        referencePackage.close();
    }

    @Test
    public void testDelete() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        referencePackage.delete();
    }

    @Test
    public void testGetType() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        assertEquals(type, referencePackage.getType());
    }

    @Test
    public void testGetId() throws Exception {
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        String type = "dummy";
        when(distributionPackage.getType()).thenReturn(type);
        DistributionPackageInfo info = new DistributionPackageInfo(type, new HashMap<String, Object>());
        when(distributionPackage.getInfo()).thenReturn(info);
        ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
        assertNotNull(referencePackage);
        String id = referencePackage.getId();
        assertNotNull(id);
    }

    @Test
    public void testIsReference() throws Exception {
        assertFalse(ReferencePackage.isReference("1231231312"));
        assertTrue(ReferencePackage.isReference(ReferencePackage.REFERENCE_PREFIX + "12312312"));
    }

    @Test
    public void testIdFromReference() throws Exception {
        String reference = "132231231";
        String id = ReferencePackage.idFromReference(reference);
        assertNull(id);
        id = ReferencePackage.idFromReference(ReferencePackage.REFERENCE_PREFIX + reference);
        assertNotNull(id);
        assertEquals(reference, id);
    }

}