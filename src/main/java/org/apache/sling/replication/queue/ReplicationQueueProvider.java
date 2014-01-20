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

import java.util.Collection;

import org.apache.sling.replication.agent.ReplicationAgent;

/**
 * A provider for {@link ReplicationQueue}s
 */
public interface ReplicationQueueProvider {

    /**
     * provide the queue to be used for a certain agent and package or creates it if it doesn't
     * exist
     * 
     * @param agent
     *            the replication agent needing the queue
     * @param name
     *            the name of the queue to retrieve
     * @return a replication queue to be used for the given parameters
     * @throws ReplicationQueueException
     */
    ReplicationQueue getQueue(ReplicationAgent agent, String name)
                    throws ReplicationQueueException;


    /**
     * get the default queue to be used for a certain agent
     * 
     * @param agent
     *            a replication agent
     * @return the default replication queue for the given agent
     * @throws ReplicationQueueException
     */
    ReplicationQueue getDefaultQueue(ReplicationAgent agent)
                    throws ReplicationQueueException;

    /**
     * get all the available queues from this provider
     * 
     * @return a collection of replication queues
     * @throws ReplicationQueueException
     */
    Collection<ReplicationQueue> getAllQueues();

    /**
     * removes an existing queue owned by this provider
     * 
     * @param queue
     *            a replication queue to be removed
     * @throws ReplicationQueueException
     */
    void removeQueue(ReplicationQueue queue) throws ReplicationQueueException;

    /**
     * enables queue driven processing for an agent.
     * @param agent
     *          a replication agent
     * @param queueProcessor
     *          the callback that is called when an item needs processing
     */
    void enableQueueProcessing(ReplicationAgent agent, ReplicationQueueProcessor queueProcessor);


    /**
     * disables queue driven processing for an agent
     * @param agent
     *          a replication agent
     */
    void disableQueueProcessing(ReplicationAgent agent);
}
