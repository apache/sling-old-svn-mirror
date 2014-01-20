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

import java.net.URI;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.serialization.ReplicationPackage;

/**
 * A replication agent is responsible for delivering content to another instance
 */
public interface ReplicationAgent {

    /**
     * get agent name
     *
     * @return the agent name as a <code>String</code>
     */
    String getName();

    /**
     * get the agent queue with the given name
     *
     * @param name a queue name as a <code>String</code>
     * @return a {@link ReplicationQueue} with the given name bound to this agent, if it exists, <code>null</code> otherwise
     * @throws ReplicationQueueException
     */
    ReplicationQueue getQueue(String name) throws ReplicationQueueException;

    /**
     * get the rules defined for this {@link ReplicationAgent}
     *
     * @return an <code>Array</code> of <code>String</code>s for this agent's rules
     */
    String[] getRules();

    /**
     * Synchronously sends a {@link ReplicationRequest} waiting for a {@link ReplicationResponse}
     *
     * @param replicationRequest the replication request
     * @return a {@link ReplicationResponse}
     * @throws AgentReplicationException
     */
    ReplicationResponse execute(ReplicationRequest replicationRequest)
            throws AgentReplicationException;

    /**
     * Asynchronously sends a {@link ReplicationRequest} without waiting for any response
     *
     * @param replicationRequest the replication request
     * @throws AgentReplicationException
     */
    void send(ReplicationRequest replicationRequest) throws AgentReplicationException;

    /**
     * get the agent configured endpoint
     *
     * @return an <code>URI</code> specifying its endpoint
     */
    URI getEndpoint();


    /**
     * removes a package from the top of the queue
     * @param queueName
     *          the name of a {@link ReplicationQueue} bound tothis agent
     * @return
     * @throws ReplicationQueueException
     */
    ReplicationPackage removeHead(String queueName) throws ReplicationQueueException;
}
