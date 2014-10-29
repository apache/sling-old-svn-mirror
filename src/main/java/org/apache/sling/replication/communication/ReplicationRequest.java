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

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * A replication request
 */
public class ReplicationRequest {

    private final long time;

    private final ReplicationActionType action;

    private final String[] paths;

    public ReplicationRequest(@Nonnull ReplicationActionType action, @Nonnull String[] paths) {
        this(System.currentTimeMillis(), action, paths);
    }

    public ReplicationRequest(long time, @Nonnull ReplicationActionType action, @Nonnull String... paths) {
        this.time = time;
        this.action = action;
        this.paths = paths;
    }

    /**
     * get the time this replication request was created
     *
     * @return a <code>long</code> representing the replication request creation time e.g. as returend by {@code System#currentTimeMillis}
     */
    public long getTime() {
        return time;
    }

    /**
     * get the {@link org.apache.sling.replication.communication.ReplicationActionType} associated with this request
     *
     * @return the action as a <code>ReplicationActionType</code>
     */
    public ReplicationActionType getAction() {
        return action;
    }

    /**
     * get the paths for this replication request
     *
     * @return an array of <code>String</code>s representing the paths
     */
    public String[] getPaths() {
        return paths;
    }

    @Override
    public String toString() {
        return "ReplicationRequest{" +
                "time=" + time +
                ", action=" + action +
                ", paths=" + Arrays.toString(paths) +
                '}';
    }
}
