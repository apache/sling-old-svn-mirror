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
package org.apache.sling.replication.queue;

/**
 * An item in a {@link org.apache.sling.replication.queue.ReplicationQueue}
 */
public class ReplicationQueueItem {

    private final String id;

    private final String[] paths;

    private final String action;

    private final String type;

    private final byte[] bytes;

    private ReplicationQueueItem(String id, String[] paths, String action, String type, byte[] bytes) {
        this.id = id;
        this.paths = paths;
        this.action = action;
        this.type = type;
        this.bytes = bytes;
    }

    public ReplicationQueueItem(String id, String[] paths, String action, String type) {
        this(id, paths, action, type, null);
    }

    public ReplicationQueueItem(String[] paths, String action, String type, byte[] bytes) {
        this(null, paths, action, type, bytes);
    }

    public String getId() {
        return id;
    }

    public String[] getPaths() {
        return paths;
    }

    public String getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean isTransient(){
        return id == null;
    }
}
