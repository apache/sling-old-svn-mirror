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
package org.apache.sling.replication.packaging;


import java.util.List;

import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;

/**
 * A {@link org.apache.sling.replication.packaging.ReplicationPackageExporter) is responsible of exporting
 * {@link org.apache.sling.replication.packaging.ReplicationPackage}s to be then imported by a {@link org.apache.sling.replication.agent.ReplicationAgent}
 * (via a {@link org.apache.sling.replication.packaging.ReplicationPackageImporter}).
 */
public interface ReplicationPackageExporter {

    /**
     * Exports the {@link org.apache.sling.replication.packaging.ReplicationPackage}s built from the
     * passed {@link org.apache.sling.replication.communication.ReplicationRequest}.
     *
     * @return a <code>List</code> of {@link org.apache.sling.replication.packaging.ReplicationPackage}s
     */
    List<ReplicationPackage> exportPackage(ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException;

    /**
     * Exports a {@link org.apache.sling.replication.packaging.ReplicationPackage} given its 'id', if it already exists.
     *
     * @return a {@link org.apache.sling.replication.packaging.ReplicationPackage} if available, <code>null</code> otherwise
     */
    ReplicationPackage exportPackageById(String replicationPackageId);
}
