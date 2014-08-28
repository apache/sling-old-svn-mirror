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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;

/**
 * A void {@link ReplicationPackage} is used for deletion of certain paths on the target instance
 */
public class VoidReplicationPackage implements ReplicationPackage {

    private static final String TYPE = "VOID";

    private final String type;

    private final String[] paths;

    private final String id;

    private final String action;

    public VoidReplicationPackage(ReplicationRequest request) {
        this(request, TYPE);
    }

    public VoidReplicationPackage(ReplicationRequest request, String type) {
        this.type = type;
        this.paths = request.getPaths();
        this.action = request.getAction().toString();
        this.id = request.getAction().toString()
                + ':' + Arrays.toString(request.getPaths()).replaceAll("\\[", "").replaceAll("\\]", "")
                + ':' + request.getTime()
                + ':' + type;
    }

    public static VoidReplicationPackage fromStream(InputStream stream) throws IOException {
        String streamString = IOUtils.toString(stream);

        String[] parts = streamString.split(":");

        if (parts.length < 4) return null;

        String actionString = parts[0];
        String pathsString = parts[1];
        String timeString = parts[2];
        String typeString = parts[3];

        ReplicationActionType replicationActionType = ReplicationActionType.fromName(actionString);

        VoidReplicationPackage replicationPackage = null;
        if (replicationActionType != null) {
            pathsString = Text.unescape(pathsString);
            String[] paths = pathsString.split(", ");

            ReplicationRequest request = new ReplicationRequest(Long.valueOf(timeString),
                    replicationActionType, paths);
            replicationPackage = new VoidReplicationPackage(request, typeString);
        }

        return replicationPackage;
    }


    private static final long serialVersionUID = 1L;

    public String getType() {
        return type;
    }

    public String[] getPaths() {
        return paths;
    }

    public long getLength() {
        try {
            return id.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported UTF-8 encoding");
        }
    }

    public InputStream createInputStream() throws IOException {
        return new ByteArrayInputStream(id.getBytes("UTF-8"));
    }

    public String getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public void close() {
    }

    public void delete() {

    }

    @Override
    public String toString() {
        return "VoidReplicationPackage{" +
                "type='" + type + '\'' +
                ", paths=" + Arrays.toString(paths) +
                ", id='" + id + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
