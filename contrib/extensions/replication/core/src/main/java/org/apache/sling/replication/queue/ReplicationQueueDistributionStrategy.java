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

import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.serialization.ReplicationPackage;

/**
 * a {@link ReplicationQueueDistributionStrategy} implements an algorithm for the distribution of
 * replication packages among the available queues for a certain agent
 */
public interface ReplicationQueueDistributionStrategy {

    /**
     * synchronously distribute a {@link ReplicationPackage} to a {@link ReplicationAgent} to a {@link ReplicationQueue}
     * provided by the given {@link ReplicationQueueProvider}
     *
     * @param agentName the name of a {@link ReplicationAgent}
     * @param replicationPackage a {@link org.apache.sling.replication.serialization.ReplicationPackage} to distribute
     * @param queueProvider      the {@link ReplicationQueueProvider} used to provide the queue to be used for the given package
     * @return a {@link ReplicationQueueItemState} representing the state of the package in the queue after its distribution
     * @throws ReplicationQueueException
     */
    ReplicationQueueItemState add(String agentName, ReplicationQueueItem replicationPackage,
                                  ReplicationQueueProvider queueProvider) throws ReplicationQueueException;

    /**
     * asynchronously distribute a {@link ReplicationPackage} to a {@link ReplicationAgent} to a {@link ReplicationQueue}
     * provided by the given {@link ReplicationQueueProvider}
     *
     * @param agentName the name of a {@link ReplicationAgent}
     * @param replicationPackage a {@link org.apache.sling.replication.serialization.ReplicationPackage} to distribute
     * @param queueProvider      the {@link ReplicationQueueProvider} used to provide the queue to be used for the given package
     * @return <code>true</code> if the package could be distributed to a {@link ReplicationQueue}, <code>false</code> otherwise
     * @throws ReplicationQueueException
     */
    boolean offer(String agentName, ReplicationQueueItem replicationPackage,
                  ReplicationQueueProvider queueProvider) throws ReplicationQueueException;

}
