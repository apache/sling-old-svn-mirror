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

import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;

public abstract class AbstractReplicationPackageBuilder implements ReplicationPackageBuilder {

    public ReplicationPackage createPackage(ReplicationRequest request)
                    throws ReplicationPackageBuildingException {
        ReplicationPackage replicationPackage = null;
        if (ReplicationActionType.ACTIVATE.equals(request.getAction())) {
            replicationPackage = createPackageForActivation(request);
        } else if (ReplicationActionType.DEACTIVATE.equals(request.getAction())) {
            replicationPackage = createPackageForDeactivation(request);
        } else {
            throw new ReplicationPackageBuildingException("unknown action type "
                            + request.getAction());
        }
        return replicationPackage;
    }

    protected abstract ReplicationPackage createPackageForDeactivation(
                    final ReplicationRequest request);

    protected abstract ReplicationPackage createPackageForActivation(ReplicationRequest request)
                    throws ReplicationPackageBuildingException;

    public ReplicationPackage readPackage(ReplicationRequest request, InputStream stream,
                    boolean install) throws ReplicationPackageBuildingException {
        ReplicationPackage replicationPackage = null;
        if (ReplicationActionType.ACTIVATE.equals(request.getAction())) {
            replicationPackage = readPackageForActivation(request, stream, install);
        } else if (ReplicationActionType.DEACTIVATE.equals(request.getAction())) {
            replicationPackage = readPackageForDeactivation(request, stream, install);
        } else {
            throw new ReplicationPackageBuildingException("unknown action type "
                            + request.getAction());
        }
        return replicationPackage;
    }

    protected abstract ReplicationPackage readPackageForDeactivation(ReplicationRequest request,
                    InputStream stream, boolean install) throws ReplicationPackageBuildingException;

    protected abstract ReplicationPackage readPackageForActivation(ReplicationRequest request,
                    InputStream stream, boolean install) throws ReplicationPackageBuildingException;

}
