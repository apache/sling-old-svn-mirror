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
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.util.Text;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;

/**
 * A void {@link ReplicationPackage} is used for deletion of certain paths on the target instance
 */
public class VoidReplicationPackage implements ReplicationPackage {

    private final String type;

    private final String[] paths;

    private final String id;

    public VoidReplicationPackage(ReplicationRequest request, String type) {
        this.type = type;
        this.paths = request.getPaths();
        this.id = ReplicationActionType.DELETE.toString() + ':' + Arrays.toString(request.getPaths()) + ':' + request.getTime();
    }

    public static VoidReplicationPackage fromStream(InputStream stream) throws IOException {
        VoidReplicationPackage replicationPackage = null;
        String streamString = IOUtils.toString(stream);
        int beginIndex = streamString.indexOf(':');
        int endIndex = streamString.lastIndexOf(':');
        if (beginIndex >= 0 && endIndex > beginIndex && streamString.startsWith(ReplicationActionType.DELETE.toString())) {
            String pathsArrayString = Text.unescape(streamString.substring(beginIndex + 1, endIndex - 1));
            String[] paths = pathsArrayString.replaceAll("\\[", "").replaceAll("\\]", "").split(", ");
            ReplicationRequest request = new ReplicationRequest(Long.valueOf(streamString.substring(streamString.lastIndexOf(':') + 1)),
                    ReplicationActionType.DELETE, paths);
            replicationPackage = new VoidReplicationPackage(request, "VOID");
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
        return id.getBytes().length;
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(id.getBytes());
    }

    public String getId() {
        return id;
    }

    public String getAction() {
        return ReplicationActionType.DELETE.toString();
    }

}
