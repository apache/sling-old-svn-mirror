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

import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueItemState.ItemState;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The default strategy for delivering packages to queues. Each agent just manages a single queue,
 * no failure / stuck handling where each package is put regardless of anything.
 */
public class SingleQueueDistributionStrategy implements ReplicationQueueDistributionStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean add(ReplicationPackage replicationPackage, ReplicationQueueProvider queueProvider) throws ReplicationQueueException {
        ReplicationQueueItem queueItem = getItem(replicationPackage);
        ReplicationQueue queue = queueProvider.getQueue(DEFAULT_QUEUE_NAME);
        return queue.add(queueItem);
    }

    public List<String> getQueueNames() {
        return Arrays.asList(new String[] { DEFAULT_QUEUE_NAME });
    }


    private ReplicationQueueItem getItem(ReplicationPackage replicationPackage) {
        ReplicationQueueItem replicationQueueItem = new ReplicationQueueItem(replicationPackage.getId(),
                replicationPackage.getPaths(),
                replicationPackage.getAction(),
                replicationPackage.getType(),
                replicationPackage.getInfo());

        return replicationQueueItem;
    }

}
