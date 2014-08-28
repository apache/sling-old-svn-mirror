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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
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

    public ReplicationPackage readPackage(InputStream stream) throws ReplicationPackageReadingException {
        ReplicationPackage replicationPackage = null;
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        try {
            stream.mark(6);
            byte[] buffer = new byte[6];
            int bytesRead = stream.read(buffer, 0, 6);
            stream.reset();
            String s = new String(buffer, "UTF-8");
            log.info("read {} bytes as {}", bytesRead, s);

            if (bytesRead > 0 && buffer[0] > 0 && s.startsWith("DEL")) {
                replicationPackage = VoidReplicationPackage.fromStream(stream);
            }
        } catch (Exception e) {
            log.warn("cannot parse stream", e);
        }
        stream.mark(-1);
        if (replicationPackage == null) {
            replicationPackage = readPackageInternal(stream);
        }
        return replicationPackage;
    }


    public boolean installPackage(ReplicationPackage replicationPackage) throws ReplicationPackageReadingException {
        ReplicationActionType actionType = ReplicationActionType.fromName(replicationPackage.getAction());
        if (ReplicationActionType.DELETE.equals(actionType)) {
            return installDeletePackage(replicationPackage);
        }
        return installPackageInternal(replicationPackage);

    }

    private boolean installDeletePackage(ReplicationPackage replicationPackage) throws ReplicationPackageReadingException {
        Session session = null;
        try {
            if (replicationPackage != null) {
                session = getSession();
                for (String path : replicationPackage.getPaths()) {
                    if (session.itemExists(path)) {
                        session.removeItem(path);
                    }
                }
                session.save();
                return true;
            }
        } catch (Exception e) {
            throw new ReplicationPackageReadingException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

        return false;
    }

    public ReplicationPackage getPackage(String id) {
        ReplicationPackage replicationPackage = null;
        try {
            replicationPackage = VoidReplicationPackage.fromStream(new ByteArrayInputStream(id.getBytes("UTF-8")));
        } catch (IOException ex) {
            // not a void package
        }

        if (replicationPackage == null) {
            replicationPackage = getPackageInternal(id);
        }
        return replicationPackage;
    }

    protected abstract String getName();

    protected abstract Session getSession() throws RepositoryException;

    protected abstract ReplicationPackage readPackageInternal(InputStream stream)
            throws ReplicationPackageReadingException;


    protected abstract boolean installPackageInternal(ReplicationPackage replicationPackage)
            throws ReplicationPackageReadingException;

    protected abstract ReplicationPackage getPackageInternal(String id);

}
