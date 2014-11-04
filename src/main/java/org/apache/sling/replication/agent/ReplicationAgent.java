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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.component.ReplicationComponent;
import org.apache.sling.replication.queue.ReplicationQueue;

/**
 * A replication agent is responsible for handling {@link org.apache.sling.replication.communication.ReplicationRequest}s.
 * <p/>
 * This means executing actions of e.g.: a specific {@link org.apache.sling.replication.communication.ReplicationActionType}s on
 * specific path(s) which will resume pulling resources from a certain Sling instance and / or pushing resources to
 * other instances.
 */
@ProviderType
public interface ReplicationAgent extends ReplicationComponent {


    /**
     * retrieves the names of the queues for this agent.
     * @return the list of queue names
     */
    Iterable<String> getQueueNames();

    /**
     * get the agent queue with the given name
     *
     * @param name a queue name
     * @return a {@link ReplicationQueue} with the given name bound to this agent, if it exists, <code>null</code> otherwise
     * @throws ReplicationAgentException if an error occurs in retrieving the queue
     */
    @CheckForNull
    ReplicationQueue getQueue(@Nullable String name) throws ReplicationAgentException;

    /**
     * executes a {@link ReplicationRequest}
     *
     * @param replicationRequest the replication request
     * @param resourceResolver   the resource resolver used for authenticating the request,
     * @return a {@link ReplicationResponse}
     * @throws ReplicationAgentException if any error happens during the execution of the request or if the authentication fails
     */
    @Nonnull
    ReplicationResponse execute(@Nonnull ResourceResolver resourceResolver, @Nonnull ReplicationRequest replicationRequest) throws ReplicationAgentException;

}
