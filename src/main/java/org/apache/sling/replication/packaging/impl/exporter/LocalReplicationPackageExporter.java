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
package org.apache.sling.replication.packaging.impl.exporter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;

/**
 * {@link org.apache.sling.replication.packaging.ReplicationPackageExporter} implementation which creates a FileVault based
 * {@link org.apache.sling.replication.packaging.ReplicationPackage} locally.
 */
public class LocalReplicationPackageExporter implements ReplicationPackageExporter {

    private ReplicationPackageBuilder packageBuilder;

    public LocalReplicationPackageExporter(ReplicationPackageBuilder packageBuilder) {
        this.packageBuilder = packageBuilder;
    }

    @Nonnull
    public List<ReplicationPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        List<ReplicationPackage> result = new ArrayList<ReplicationPackage>();

        ReplicationPackage createdPackage = packageBuilder.createPackage(resourceResolver, replicationRequest);
        if (createdPackage != null) {
            result.add(createdPackage);
        }
        return result;
    }

    public ReplicationPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String replicationPackageId) {
        return packageBuilder.getPackage(resourceResolver, replicationPackageId);
    }
}
