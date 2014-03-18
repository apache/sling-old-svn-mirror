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

import java.util.Calendar;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The error strategy for delivering packages to queues. Each agent manages a single queue for
 * delivering packages with an error queue which can be used when an item is stuck in the default
 * queue for too much time, then the stuck item is moved to the error queue or dropped.
 */
@Component(immediate = true, metatype = true, label = "Error Aware Queue Distribution Strategy")
@Service(value = ReplicationQueueDistributionStrategy.class)
@Property(name = "name", value = ErrorAwareQueueDistributionStrategy.NAME, propertyPrivate = true)
public class ErrorAwareQueueDistributionStrategy implements ReplicationQueueDistributionStrategy {

    private static final String ERROR = "ERROR";

    public static final String NAME = "error";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(intValue = 100, label = "no. of maximum queue item processing attempts")
    private static final String ATTEMPTS_THRESHOLD = "attempts.threshold";

    @Property(intValue = 600000, label = "no. of maximum amount of milliseconds in the queue")
    private static final String TIME_THRESHOLD = "time.threshold";

    @Property(label = "Stuck Queue Handling", options = {
            @PropertyOption(name = "ERROR", value = "Error"),
            @PropertyOption(name = "DROP", value = "Drop")}, value = ERROR)
    private static final String STUCK_HANDLING = "stuck.handling";

    private String stuckQueueHandling;

    private Integer attemptsThreshold;

    private Integer timeThreshold;

    @Activate
    protected void activate(final ComponentContext ctx) {
        stuckQueueHandling = PropertiesUtil
                .toString(ctx.getProperties().get(STUCK_HANDLING), ERROR);
        attemptsThreshold = PropertiesUtil.toInteger(ctx.getProperties().get(ATTEMPTS_THRESHOLD), 100);
        timeThreshold = PropertiesUtil.toInteger(ctx.getProperties().get(TIME_THRESHOLD), 600000);
    }

    public ReplicationQueueItemState add(String agentName, ReplicationQueueItem replicationPackage,
                                         ReplicationQueueProvider queueProvider)
            throws ReplicationQueueException {
        try {
            if (log.isInfoEnabled()) {
                log.info("using error aware queue distribution");
            }
            ReplicationQueueItemState state = new ReplicationQueueItemState();
            ReplicationQueue queue = queueProvider.getDefaultQueue(agentName);
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
                    state.setSuccessful(false);
                }
                return state;
            } else {
                throw new ReplicationQueueException("could not get a queue for agent " + agentName);
            }
        } finally {
            checkAndRemoveStuckItems(agentName, queueProvider);
        }
    }

    public boolean offer(String agentName, ReplicationQueueItem replicationPackage,
                         ReplicationQueueProvider queueProvider) throws ReplicationQueueException {
        boolean added;
        ReplicationQueue queue = queueProvider.getDefaultQueue(agentName);
        if (queue != null) {
            added = queue.add(replicationPackage);
        } else {
            throw new ReplicationQueueException("could not get a queue for agent " + agentName);
        }
        checkAndRemoveStuckItems(agentName, queueProvider);
        return added;
    }

    private void checkAndRemoveStuckItems(String agent,
                                          ReplicationQueueProvider queueProvider) throws ReplicationQueueException {
        ReplicationQueue defaultQueue = queueProvider.getDefaultQueue(agent);
        // get first item in the queue with its status
        ReplicationQueueItem firstItem = defaultQueue.getHead();
        if (firstItem != null) {
            ReplicationQueueItemState status = defaultQueue.getStatus(firstItem);
            // if item is still in the queue after a max no. of attempts, move it to the error queue
            int attempts = status.getAttempts();
            Calendar entered = status.getEntered();
            log.info("item {} entered {} was attempted {} times", new Object[]{firstItem, entered, attempts});
            if (attempts > attemptsThreshold || (entered != null && Calendar.getInstance().getTimeInMillis() - entered.getTimeInMillis() > timeThreshold)) {
                if (ERROR.equals(stuckQueueHandling)) {
                    log.warn("item {} moved to the error queue", firstItem);

                    ReplicationQueue errorQueue = queueProvider.getQueue(agent, "-error");
                    if (!errorQueue.add(firstItem)) {
                        log.error("failed to move item {} the queue {}", firstItem, errorQueue);
                        throw new ReplicationQueueException("could not move an item to the error queue");
                    }
                }
                log.warn("item {} dropped from the default queue", firstItem);
                defaultQueue.removeHead();
            }
        }
    }

}
