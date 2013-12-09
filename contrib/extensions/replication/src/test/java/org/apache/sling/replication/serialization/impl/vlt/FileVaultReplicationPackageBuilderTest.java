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

import java.lang.reflect.Field;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link FileVaultReplicationPackageBuilder}
 */
public class FileVaultReplicationPackageBuilderTest {

    @Test
    public void testCreatePackageForAddWithoutPermissions() throws Exception {
        try {
            SlingRepository repository = mock(SlingRepository.class);
            FileVaultReplicationPackageBuilder fileVaultReplicationPackageBuilder = new FileVaultReplicationPackageBuilder();
            Field repositoryField = fileVaultReplicationPackageBuilder.getClass().getDeclaredField("repository");
            repositoryField.setAccessible(true);
            repositoryField.set(fileVaultReplicationPackageBuilder, repository);

            Packaging packaging = mock(Packaging.class);
            Field packagingField = fileVaultReplicationPackageBuilder.getClass().getDeclaredField("packaging");
            packagingField.setAccessible(true);
            packagingField.set(fileVaultReplicationPackageBuilder, packaging);

            ReplicationRequest request = mock(ReplicationRequest.class);
            fileVaultReplicationPackageBuilder.createPackageForAdd(request);
            fail("cannot create a package without supplying needed credentials");
        } catch (Throwable t) {
            // expected to fail
        }
    }
}
