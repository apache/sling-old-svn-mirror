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

import java.io.File;

import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link FileVaultDistributionPackage}
 */
public class FileVaultDistributionPackageTest {

    @Test
    public void testGetId() throws Exception {
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/file.txt");
        when(vaultPackage.getFile()).thenReturn(file);
        FileVaultDistributionPackage fileVaultdistributionPackage = new FileVaultDistributionPackage(vaultPackage);
        assertNotNull(fileVaultdistributionPackage.getId());
    }

    @Test
    public void testGetPaths() throws Exception {
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/file.txt");
        when(vaultPackage.getFile()).thenReturn(file);
        FileVaultDistributionPackage fileVaultdistributionPackage = new FileVaultDistributionPackage(vaultPackage);
        assertNull(fileVaultdistributionPackage.getInfo().getPaths());
    }

    @Test
    public void testCreateInputStream() throws Exception {
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = File.createTempFile("sample", "txt");
        when(vaultPackage.getFile()).thenReturn(file);
        FileVaultDistributionPackage fileVaultdistributionPackage = new FileVaultDistributionPackage(vaultPackage);
        assertNotNull(fileVaultdistributionPackage.createInputStream());
    }

    @Test
    public void testGetType() throws Exception {
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/file.txt");
        when(vaultPackage.getFile()).thenReturn(file);
        FileVaultDistributionPackage fileVaultdistributionPackage = new FileVaultDistributionPackage(vaultPackage);
        assertNotNull(fileVaultdistributionPackage.getType());
    }

    @Test
    public void testGetRequestType() throws Exception {
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/file.txt");
        when(vaultPackage.getFile()).thenReturn(file);
        FileVaultDistributionPackage fileVaultdistributionPackage = new FileVaultDistributionPackage(vaultPackage);
        assertNotNull(fileVaultdistributionPackage.getInfo().getRequestType());
    }

    @Test
    public void testClose() throws Exception {
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/file.txt");
        when(vaultPackage.getFile()).thenReturn(file);
        FileVaultDistributionPackage fileVaultdistributionPackage = new FileVaultDistributionPackage(vaultPackage);
        fileVaultdistributionPackage.close();
    }

    @Test
    public void testDelete() throws Exception {
        VaultPackage vaultPackage = mock(VaultPackage.class);
        File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/path/to/file.txt");
        when(vaultPackage.getFile()).thenReturn(file);
        FileVaultDistributionPackage fileVaultdistributionPackage = new FileVaultDistributionPackage(vaultPackage);
        fileVaultdistributionPackage.delete();
    }
}