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
package org.apache.sling.replication.serialization.impl.exporter;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.impl.vlt.FileVaultReplicationPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.apache.sling.replication.serialization.ReplicationPackageExporter} implementation which creates a FileVault based
 * {@link org.apache.sling.replication.serialization.ReplicationPackage} locally.
 */
@Component(label = "Local Replication Package Exporter")
@Service(value = ReplicationPackageExporter.class)
@Property(name = "name", value = LocalReplicationPackageExporter.NAME)
public class LocalReplicationPackageExporter implements ReplicationPackageExporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String NAME = "local";

    @Reference(name = "ReplicationPackageBuilder", target = "(name=" + FileVaultReplicationPackageBuilder.NAME + ")",
            policy = ReferencePolicy.STATIC)
    private ReplicationPackageBuilder packageBuilder;

    public List<ReplicationPackage> exportPackage(ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        List<ReplicationPackage> result = new ArrayList<ReplicationPackage>();

        ReplicationPackage createdPackage = packageBuilder.createPackage(replicationRequest);
        result.add(createdPackage);

        return result;
    }

    public ReplicationPackage exportPackageById(String replicationPackageId) {
        return packageBuilder.getPackage(replicationPackageId);
    }
}
