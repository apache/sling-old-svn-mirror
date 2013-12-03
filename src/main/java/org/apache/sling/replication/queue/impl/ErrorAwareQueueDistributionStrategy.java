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
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackage;

/**
 * The error strategy for delivering packages to queues. Each agent manages a single queue for
 * delivering packages with an error queue which can be used when an item is stuck in the default
 * queue for too much time, then the stuck item is moved to the error queue or dropped.
 */
@Component(immediate = true, metatype = true)
@Service(value = ReplicationQueueDistributionStrategy.class)
@Property(name = "name", value = ErrorAwareQueueDistributionStrategy.NAME, propertyPrivate = true)
public class ErrorAwareQueueDistributionStrategy implements ReplicationQueueDistributionStrategy {

    private static final String ERROR = "ERROR";

    public static final String NAME = "error";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property
    private static final String ATTEMPTS_THRESHOLD = "attempts.threshold";

    @Property(name = "Stuck Queue Handling", options = {
            @PropertyOption(name = ERROR, value = "Error"),
            @PropertyOption(name = "DROP", value = "Drop") })
    private static final String STUCK_HANDLING = "stuck.handling";

    private String stuckQueueHandling;

    private Integer attemptsThreshold;

    protected void activate(final ComponentContext ctx) {
        stuckQueueHandling = PropertiesUtil
                        .toString(ctx.getProperties().get(STUCK_HANDLING), ERROR);
        attemptsThreshold = PropertiesUtil.toInteger(ctx.getProperties().get(ATTEMPTS_THRESHOLD),
                        100);
    }

    public ReplicationQueueItemState add(ReplicationPackage replicationPackage,
                    ReplicationAgent agent, ReplicationQueueProvider queueProvider)
                    throws ReplicationQueueException {
        try {
            if (log.isInfoEnabled()) {
                log.info("using single queue distribution");
            }
            ReplicationQueueItemState state = new ReplicationQueueItemState();
            ReplicationQueue queue = queueProvider.getOrCreateDefaultQueue(agent);
            if (log.isInfoEnabled()) {
                log.info("obtained queue {}", queue);
            }
            if (queue != null) {
                if (queue.add(replicationPackage)) {
                    if (log.isInfoEnabled()) {
                        log.info("replication status: {}", state);
                    }
                    state = queue.getStatus(replicationPackage);
                } else {
                    if (log.isErrorEnabled()) {
                        log.error("could not add the item to the queue {}", queue);
                    }
                    state.setItemState(ItemState.ERROR);
                    state.setSuccessfull(false);
                }
                return state;
            } else {
                throw new ReplicationQueueException("could not get a queue for agent "
                                + agent.getName());
            }
        } finally {
            checkAndRemoveStuckItems(agent, queueProvider);
        }
    }

    public void offer(ReplicationPackage replicationPackage, ReplicationAgent agent,
                    ReplicationQueueProvider queueProvider) throws ReplicationQueueException {
        ReplicationQueue queue = queueProvider.getOrCreateDefaultQueue(agent);
        if (queue != null) {
            queue.add(replicationPackage);
        } else {
            throw new ReplicationQueueException("could not get a queue for agent "
                            + agent.getName());
        }
        checkAndRemoveStuckItems(agent, queueProvider);
    }

    private void checkAndRemoveStuckItems(ReplicationAgent agent,
                    ReplicationQueueProvider queueProvider) throws ReplicationQueueException {
        ReplicationQueue defaultQueue = queueProvider.getOrCreateDefaultQueue(agent);
        // get first item in the queue with its status
        ReplicationPackage firstItem = defaultQueue.getHead();
        if (firstItem != null) {
            ReplicationQueueItemState status = defaultQueue.getStatus(firstItem);
            // if item is still in the queue after a max no. of attempts, move it to the error queue
            int attempts = status.getAttempts();
            if (log.isInfoEnabled()) {
                log.info("attemps for item {}: {}", firstItem.getId(), attempts);
            }
            if (attempts > attemptsThreshold) {
                if (ERROR.equals(stuckQueueHandling)) {
                    if (log.isWarnEnabled()) {
                        log.warn("item moved to the error queue");
                    }
                    ReplicationQueue errorQueue = queueProvider.getOrCreateQueue(agent, "-error");
                    errorQueue.add(firstItem);
                }
                if (log.isWarnEnabled()) {
                    log.warn("item dropped from the default queue");
                }
                defaultQueue.removeHead();
            }
        }
    }

}
