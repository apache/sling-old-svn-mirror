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
package org.apache.sling.replication.serialization.impl.vlt;

import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder}
 */
public class FileVaultReplicationPackageBuilderTest {

    @Test
    public void testCreatePackageForAdd() throws Exception {

        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/file");
        when(vaultPackage.getFile()).thenReturn(file);
        MetaInf inf = mock(MetaInf.class);
        when(vaultPackage.getMetaInf()).thenReturn(inf);
        when(packageManager.assemble(any(Session.class), any(ExportOptions.class), any(File.class))).thenReturn(vaultPackage);
        when(packaging.getPackageManager()).thenReturn(packageManager);

        ReplicationEventFactory eventFactory = mock(ReplicationEventFactory.class);

        FileVaultReplicationPackageBuilder fileVaultReplicationPackageBuilder = new FileVaultReplicationPackageBuilder(
                packaging, eventFactory);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationRequest request = new ReplicationRequest(ReplicationActionType.ADD, new String[]{"/"});
        ReplicationPackage replicationPackage = fileVaultReplicationPackageBuilder.createPackageForAdd(resourceResolver, request);
        assertNotNull(replicationPackage);
    }

    @Test
    public void testReadDummyPackageInternal() throws Exception {
        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(packaging.getPackageManager()).thenReturn(packageManager);

        ReplicationEventFactory eventFactory = mock(ReplicationEventFactory.class);

        FileVaultReplicationPackageBuilder fileVaultReplicationPackageBuilder = new FileVaultReplicationPackageBuilder(
                packaging, eventFactory);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        InputStream stream = new ByteArrayInputStream("some binary".getBytes("UTF-8"));
        ReplicationPackage replicationPackage = fileVaultReplicationPackageBuilder.readPackageInternal(resourceResolver, stream);
        assertNull(replicationPackage);
    }

    @Test
    public void testGetPackageInternalWithNonExistingId() throws Exception {
        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(packaging.getPackageManager()).thenReturn(packageManager);
        ReplicationEventFactory eventFactory = mock(ReplicationEventFactory.class);

        FileVaultReplicationPackageBuilder fileVaultReplicationPackageBuilder = new FileVaultReplicationPackageBuilder(
                packaging, eventFactory);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String id = "some-id";
        ReplicationPackage packageInternal = fileVaultReplicationPackageBuilder.getPackageInternal(resourceResolver, id);
        assertNull(packageInternal);
    }

    @Test
    public void testInstallPackageInternal() throws Exception {
        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(packaging.getPackageManager()).thenReturn(packageManager);
        ReplicationEventFactory eventFactory = mock(ReplicationEventFactory.class);

        FileVaultReplicationPackageBuilder fileVaultReplicationPackageBuilder = new FileVaultReplicationPackageBuilder(
                packaging, eventFactory);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ReplicationPackage replicationPackage = mock(ReplicationPackage.class);
        when(replicationPackage.getId()).thenReturn("/path/to/file");
        boolean success = fileVaultReplicationPackageBuilder.installPackage(resourceResolver, replicationPackage);
        assertFalse(success);
    }
}