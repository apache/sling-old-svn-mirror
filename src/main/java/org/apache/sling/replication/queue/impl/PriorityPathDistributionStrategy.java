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

import java.util.Arrays;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.queue.*;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;

/**
 * Distribution algorithm which keeps one specific queue to handle specific paths and another queue
 * for handling all the other paths
 */
@Component(immediate = true, metatype = true)
@Service(value = ReplicationQueueDistributionStrategy.class)
@Property(name = "name", value = PriorityPathDistributionStrategy.NAME, propertyPrivate = true)
public class PriorityPathDistributionStrategy implements ReplicationQueueDistributionStrategy {

    public static final String NAME = "priority";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = { "/content" })
    private static final String PRIORITYPATHS = "priority.paths";

    private String[] priorityPaths;

    @Activate
    protected void activate(ComponentContext context) {
        priorityPaths = PropertiesUtil.toStringArray(context.getProperties().get(PRIORITYPATHS));
    }

    public ReplicationQueueItemState add(ReplicationQueueItem replicationPackage,
                    ReplicationAgent agent, ReplicationQueueProvider queueProvider)
                    throws ReplicationQueueException {
        if (log.isInfoEnabled()) {
            log.info("using path priority based queue distribution");
        }
        ReplicationQueueItemState state = new ReplicationQueueItemState();

        ReplicationQueue queue = getQueue(replicationPackage, agent, queueProvider);
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
            throw new ReplicationQueueException("could not get a queue for agent "
                            + agent.getName());
        }

    }

    private ReplicationQueue getQueue(ReplicationQueueItem replicationPackage,
                    ReplicationAgent agent, ReplicationQueueProvider queueProvider)
                    throws ReplicationQueueException {
        String[] paths = replicationPackage.getPaths();

        if (log.isInfoEnabled()) {
            log.info("calculating priority for paths {}", Arrays.toString(paths));
        }

        boolean usePriorityQueue = false;
        String pp = null;
        for (String path : paths) {
            for (String priorityPath : priorityPaths) {
                if (path.startsWith(priorityPath)) {
                    usePriorityQueue = true;
                    pp = priorityPath;
                    break;
                }
            }
        }

        ReplicationQueue queue;
        if (usePriorityQueue) {
            if (log.isInfoEnabled()) {
                log.info("using priority queue for path {}", pp);
            }
            queue = queueProvider.getQueue(agent, pp);
        } else {
            if (log.isInfoEnabled()) {
                log.info("using default queue");
            }
            queue = queueProvider.getDefaultQueue(agent);
        }
        return queue;
    }

    public boolean offer(ReplicationQueueItem replicationPackage, ReplicationAgent agent,
                         ReplicationQueueProvider queueProvider) throws ReplicationQueueException {
        ReplicationQueue queue = getQueue(replicationPackage, agent, queueProvider);
        if (queue != null) {
            return queue.add(replicationPackage);
        } else {
            throw new ReplicationQueueException("could not get a queue for agent "
                            + agent.getName());
        }

    }

}
