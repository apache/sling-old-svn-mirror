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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.serialization.ReplicationPackage;

/**
 * A void {@link ReplicationPackage} used mainly for deactivation
 */
public class VoidReplicationPackage implements ReplicationPackage {

    private String type;

    private String[] paths;

    private String id;

    public VoidReplicationPackage(ReplicationRequest request, String type) {
        this.type = type;
        this.paths = request.getPaths();
        this.id = String.valueOf(request.getTime());
    }

    private static final long serialVersionUID = 1L;

    public String getType() {
        return type;
    }

    public String[] getPaths() {
        return paths;
    }

    public long getLength() {
        return 0;
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream("".getBytes());
    }

    public String getId() {
        return id;
    }

    public String getAction() {
        return ReplicationActionType.DEACTIVATE.toString();
    }

}
