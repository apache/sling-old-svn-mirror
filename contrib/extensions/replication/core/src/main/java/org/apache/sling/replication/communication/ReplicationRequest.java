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
package org.apache.sling.replication.communication;

/**
 * A replication request
 */
public class ReplicationRequest {

    private final long time;

    private final ReplicationActionType action;

    private final String[] paths;

    public ReplicationRequest(long time, ReplicationActionType action, String... paths) {
        this.time = time;
        this.action = action;
        this.paths = paths;
    }

    public long getTime() {
        return time;
    }

    public ReplicationActionType getAction() {
        return action;
    }

    public String[] getPaths() {
        return paths;
    }
}
