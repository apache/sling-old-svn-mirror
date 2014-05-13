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
package org.apache.sling.replication.queue.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.queue.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;

/**
 * The default strategy for delivering packages to queues. Each agent just manages a single queue,
 * no failure / stuck handling where each package is put regardless of anything.
 */
@Component(immediate = true, label = "Single Queue Distribution Strategy")
@Service(value = ReplicationQueueDistributionStrategy.class)
@Property(name = "name", value = SingleQueueDistributionStrategy.NAME)
public class SingleQueueDistributionStrategy implements ReplicationQueueDistributionStrategy {

    public static final String NAME = "single";

    private final Logger log = LoggerFactory.getLogger(getClass());

    public ReplicationQueueItemState add(String agentName, ReplicationQueueItem replicationPackage,
                                         ReplicationQueueProvider queueProvider)
                    throws ReplicationQueueException {
        log.info("using single queue distribution");

        ReplicationQueueItemState state = new ReplicationQueueItemState();
        ReplicationQueue queue = queueProvider.getDefaultQueue(agentName);
        log.info("obtained queue {}", queue);

        if (queue != null) {
            if (queue.add(replicationPackage)) {
                state = queue.getStatus(replicationPackage);
                log.info("replication status: {}", state);
            } else {
                log.error("could not add the item to the queue {}", queue);
                state.setItemState(ItemState.ERROR);
                state.setSuccessful(false);
            }
            return state;
        } else {
            throw new ReplicationQueueException("could not get a queue for agent " + agentName);
        }

    }

    public boolean offer(String agentName, ReplicationQueueItem replicationPackage,
                         ReplicationQueueProvider queueProvider) throws ReplicationQueueException {
        ReplicationQueue queue = queueProvider.getDefaultQueue(agentName);
        if (queue != null) {
            return queue.add(replicationPackage);
        } else {
            throw new ReplicationQueueException("could not get a queue for agent " + agentName);
        }

    }

}
