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

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base abstract implementation of a {@link ReplicationPackageBuilder}
 */
public abstract class AbstractReplicationPackageBuilder implements ReplicationPackageBuilder {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public ReplicationPackage createPackage(ReplicationRequest request)
            throws ReplicationPackageBuildingException {
        ReplicationPackage replicationPackage;
        if (ReplicationActionType.ADD.equals(request.getAction())) {
            replicationPackage = createPackageForAdd(request);
        } else if (ReplicationActionType.DELETE.equals(request.getAction())) {
            replicationPackage = new VoidReplicationPackage(request, getName());
        } else if (ReplicationActionType.POLL.equals(request.getAction())) {
            replicationPackage = new VoidReplicationPackage(request, getName()); // TODO : change this
        } else {
            throw new ReplicationPackageBuildingException("unknown action type "
                    + request.getAction());
        }
        return replicationPackage;
    }

    protected abstract ReplicationPackage createPackageForAdd(ReplicationRequest request)
            throws ReplicationPackageBuildingException;

    public ReplicationPackage readPackage(InputStream stream,
                                          boolean install) throws ReplicationPackageReadingException {
        ReplicationPackage replicationPackage = null;
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        try {
            stream.mark(6);
            byte[] buffer = new byte[6];
            int bytesRead = stream.read(buffer, 0, 6);
            stream.reset();
            String s = new String(buffer);
            if (log.isInfoEnabled()) {
                log.info("read {} bytes as {}", bytesRead, s);
            }
            if (bytesRead > 0 && buffer[0] > 0 && s.startsWith("DEL")) {
                replicationPackage = readPackageForDelete(stream);
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("{}", e);
            }
        }
        stream.mark(-1);
        if (replicationPackage == null) {
            replicationPackage = readPackageForAdd(stream, install);
        }
        return replicationPackage;
    }

    private ReplicationPackage readPackageForDelete(InputStream stream) throws ReplicationPackageReadingException {
        ReplicationPackage replicationPackage = null;
        Session session = null;
        try {
            VoidReplicationPackage voidReplicationPackage = VoidReplicationPackage.fromStream(stream);
            if (voidReplicationPackage != null) {
                session = getSession();
                if (session != null) {
                    for (String path : voidReplicationPackage.getPaths()) {
                        if (session.itemExists(path)) {
                            session.removeItem(path);
                        }
                    }
                    session.save();
                    ReplicationRequest request = new ReplicationRequest(System.currentTimeMillis(),
                            ReplicationActionType.DELETE, voidReplicationPackage.getPaths());
                    replicationPackage = new VoidReplicationPackage(request, getName());
                }
            }
            return replicationPackage;
        } catch (Exception e) {
            throw new ReplicationPackageReadingException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

    }

    protected abstract String getName();

    protected abstract Session getSession() throws RepositoryException;

    protected abstract ReplicationPackage readPackageForAdd(InputStream stream, boolean install)
            throws ReplicationPackageReadingException;

}
