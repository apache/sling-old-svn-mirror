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
package org.apache.sling.replication.agent;

import java.util.Collection;
import java.util.SortedSet;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationEndpoint;

/**
 * A manager for {@link ReplicationAgent}s
 */
public interface ReplicationAgentsManager {

    /**
     * get agents which can handle the given action on the given paths
     *
     * @param action a {@link ReplicationActionType}
     * @param paths  the paths such agents should be able to handle
     * @return a {@link SortedSet} of {@link ReplicationAgent}s
     */
    SortedSet<ReplicationAgent> getAgentsFor(ReplicationActionType action, String... paths);

    /**
     * get all the agents registered and active in the system
     *
     * @return a <code>Collection</code> of {@link ReplicationAgent}s
     */
    Collection<ReplicationAgent> getAllAvailableAgents();

    Collection<ReplicationAgent> getAgentsFor(ReplicationEndpoint endpoint);
}
