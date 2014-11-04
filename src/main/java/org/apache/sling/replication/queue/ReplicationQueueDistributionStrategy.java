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

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.component.ReplicationComponent;
import org.apache.sling.replication.packaging.ReplicationPackage;

import java.util.List;

/**
 * a {@link ReplicationQueueDistributionStrategy} implements an algorithm for the distribution of
 * {@link org.apache.sling.replication.packaging.ReplicationPackage}s among the available queues
 */
@ConsumerType
public interface ReplicationQueueDistributionStrategy extends ReplicationComponent {
    String DEFAULT_QUEUE_NAME = "default";

    /**
     * synchronously distribute a {@link org.apache.sling.replication.packaging.ReplicationPackage}
     * to one or more {@link ReplicationQueue}s provided by the given {@link ReplicationQueueProvider}
     *
     * @param agentName     the name of a {@link ReplicationAgent}
     * @param replicationPackage          a {@link org.apache.sling.replication.packaging.ReplicationPackage} to distribute
     * @param queueProvider the {@link ReplicationQueueProvider} used to provide the queues to be used for the given package
     * @return a {@link ReplicationQueueItemState} representing the state of the package in the queue after its distribution
     * @throws ReplicationQueueException if distribution fails
     */
    @Nonnull
    boolean add(@Nonnull String agentName, @Nonnull ReplicationPackage replicationPackage,
                                  @Nonnull ReplicationQueueProvider queueProvider) throws ReplicationQueueException;


    /**
     * Returns the queue names available for this strategy.
     * @return a list of queue names
     */
    List<String> getQueueNames();

}
