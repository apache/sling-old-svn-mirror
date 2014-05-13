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

import java.io.InputStream;
import java.lang.reflect.Field;

import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuilderProvider;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.serialization.impl.DefaultReplicationPackageImporter}
 */
public class DefaultReplicationPackageImporterTest {

    @Test
    public void testSynchronousImportWithoutServices() throws Exception {
        DefaultReplicationPackageImporter importer = new DefaultReplicationPackageImporter();
        InputStream stream = mock(InputStream.class);
        assertFalse(importer.importStream(stream, "some-type"));
        assertFalse(importer.importStream(stream, null));
    }

    @Test
    public void testSynchronousImportWithNotExistingType() throws Exception {
        DefaultReplicationPackageImporter importer = new DefaultReplicationPackageImporter();
        Field replicationEventFactoryField = importer.getClass().getDeclaredField("replicationEventFactory");
        replicationEventFactoryField.setAccessible(true);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        replicationEventFactoryField.set(importer, replicationEventFactory);

        Field replicationPackageBuilderProviderField = importer.getClass().getDeclaredField("replicationPackageBuilderProvider");
        replicationPackageBuilderProviderField.setAccessible(true);
        ReplicationPackageBuilderProvider replicationPackageBuilderProvider = mock(ReplicationPackageBuilderProvider.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(packageBuilder.readPackage(any(InputStream.class), eq(true))).thenReturn(replicationPackage);
        when(replicationPackageBuilderProvider.getReplicationPackageBuilder("void")).thenReturn(packageBuilder);
        replicationPackageBuilderProviderField.set(importer, replicationPackageBuilderProvider);

        InputStream stream = mock(InputStream.class);
        assertFalse(importer.importStream(stream, "some-type"));
        assertFalse(importer.importStream(stream, null));
    }

    @Test
    public void testSynchronousImportWithTypeParameter() throws Exception {
        DefaultReplicationPackageImporter importer = new DefaultReplicationPackageImporter();
        Field replicationEventFactoryField = importer.getClass().getDeclaredField("replicationEventFactory");
        replicationEventFactoryField.setAccessible(true);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        replicationEventFactoryField.set(importer, replicationEventFactory);

        Field replicationPackageBuilderProviderField = importer.getClass().getDeclaredField("replicationPackageBuilderProvider");
        replicationPackageBuilderProviderField.setAccessible(true);
        ReplicationPackageBuilderProvider replicationPackageBuilderProvider = mock(ReplicationPackageBuilderProvider.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationPackage replicationPackage = new VoidReplicationPackage(new ReplicationRequest(System.currentTimeMillis(), ReplicationActionType.DELETE, "/content"), "void");
        when(packageBuilder.readPackage(any(InputStream.class), eq(true))).thenReturn(replicationPackage);
        when(replicationPackageBuilderProvider.getReplicationPackageBuilder("void")).thenReturn(packageBuilder);
        replicationPackageBuilderProviderField.set(importer, replicationPackageBuilderProvider);

        InputStream stream = mock(InputStream.class);
        assertTrue(importer.importStream(stream, "void"));
    }

    @Test
    public void testSynchronousImportWithoutTypeParameter() throws Exception {
        DefaultReplicationPackageImporter importer = new DefaultReplicationPackageImporter();
        Field replicationEventFactoryField = importer.getClass().getDeclaredField("replicationEventFactory");
        replicationEventFactoryField.setAccessible(true);
        ReplicationEventFactory replicationEventFactory = mock(ReplicationEventFactory.class);
        replicationEventFactoryField.set(importer, replicationEventFactory);

        Field replicationPackageBuilderProviderField = importer.getClass().getDeclaredField("replicationPackageBuilderProvider");
        replicationPackageBuilderProviderField.setAccessible(true);
        ReplicationPackageBuilderProvider replicationPackageBuilderProvider = mock(ReplicationPackageBuilderProvider.class);
        ReplicationPackageBuilder packageBuilder = mock(ReplicationPackageBuilder.class);
        ReplicationPackage replicationPackage = new VoidReplicationPackage(new ReplicationRequest(System.currentTimeMillis(), ReplicationActionType.DELETE, "/content"), "void");
        when(packageBuilder.readPackage(any(InputStream.class), eq(true))).thenReturn(replicationPackage);
        replicationPackageBuilderProviderField.set(importer, replicationPackageBuilderProvider);

        InputStream stream = mock(InputStream.class);
        assertFalse(importer.importStream(stream, null));
    }



}
