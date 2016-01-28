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
package org.apache.sling.distribution.serialization.impl.vlt;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link FileVaultDistributionPackageBuilder}
 */
public class FileVaultDistributionPackageBuilderTest {

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

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);


        FileVaultDistributionPackageBuilder fileVaultdistributionPackageBuilder = new FileVaultDistributionPackageBuilder("filevlt", packaging, null, null, null, null, null, false);
        DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        DistributionPackage distributionPackage = fileVaultdistributionPackageBuilder.createPackageForAdd(resourceResolver, request);
        assertNotNull(distributionPackage);
    }

    @Test
    public void testReadDummyPackageInternal() throws Exception {
        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(packaging.getPackageManager()).thenReturn(packageManager);

        FileVaultDistributionPackageBuilder fileVaultdistributionPackageBuilder = new FileVaultDistributionPackageBuilder("filevlt", packaging, null, null, null, null, null, false);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        InputStream stream = new ByteArrayInputStream("some binary".getBytes("UTF-8"));

        boolean throwsException  = false;
        try {
            DistributionPackage distributionPackage = fileVaultdistributionPackageBuilder.readPackageInternal(resourceResolver, stream);
        } catch (Exception e) {
            throwsException = true;
        }

        assertTrue(throwsException);
    }

    @Test
    public void testGetPackageInternalWithNonExistingId() throws Exception {
        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(packaging.getPackageManager()).thenReturn(packageManager);

        FileVaultDistributionPackageBuilder fileVaultdistributionPackageBuilder = new FileVaultDistributionPackageBuilder("filevlt", packaging, null, null, null, null, null, false);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String id = "some-id";
        DistributionPackage packageInternal = fileVaultdistributionPackageBuilder.getPackageInternal(resourceResolver, id);
        assertNull(packageInternal);
    }

    @Test
    public void testInstallPackageInternal() throws Exception {
        File tempFile = File.createTempFile("testInstallPackageInternal", "txt");
        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        VaultPackage vaultPackage = mock(VaultPackage.class);
        when(packageManager.open(tempFile)).thenReturn(vaultPackage);
        when(packaging.getPackageManager()).thenReturn(packageManager);

        FileVaultDistributionPackageBuilder fileVaultdistributionPackageBuilder = new FileVaultDistributionPackageBuilder("filevlt", packaging, null, null, null, null, null, false);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getId()).thenReturn(tempFile.getAbsolutePath());
        when(distributionPackage.getType()).thenReturn("filevlt");
        DistributionPackageInfo info = new DistributionPackageInfo("filevlt");
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/something"});
        when(distributionPackage.getInfo()).thenReturn(info);

        boolean success = fileVaultdistributionPackageBuilder.installPackage(resourceResolver, distributionPackage);
        assertTrue(success);
    }

    @Test
    public void testInstallWithCustomImportModeAndACLHandling() throws Exception {
        File tempFile = File.createTempFile("testInstallPackageInternal", "txt");
        Packaging packaging = mock(Packaging.class);
        PackageManager packageManager = mock(PackageManager.class);
        VaultPackage vaultPackage = mock(VaultPackage.class);
        when(packageManager.open(tempFile)).thenReturn(vaultPackage);
        when(packaging.getPackageManager()).thenReturn(packageManager);

        FileVaultDistributionPackageBuilder fileVaultdistributionPackageBuilder = new FileVaultDistributionPackageBuilder("filevlt", packaging,
                ImportMode.MERGE, AccessControlHandling.MERGE, null, null, null, false);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);
        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);
        when(distributionPackage.getId()).thenReturn(tempFile.getAbsolutePath());
        when(distributionPackage.getType()).thenReturn("filevlt");
        DistributionPackageInfo info = new DistributionPackageInfo("filevlt");
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.ADD);
        info.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, new String[]{"/something"});
        when(distributionPackage.getInfo()).thenReturn(info);

        boolean success = fileVaultdistributionPackageBuilder.installPackage(resourceResolver, distributionPackage);
        assertTrue(success);
    }
}