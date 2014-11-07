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
package org.apache.sling.distribution.queue.impl;

import javax.annotation.Nonnull;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueDistributionStrategy;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * The default strategy for delivering packages to queues. Each agent just manages a single queue,
 * no failure / stuck handling where each package is put regardless of anything.
 */
public class SingleQueueDistributionStrategy implements DistributionQueueDistributionStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean add(@Nonnull DistributionPackage distributionPackage, @Nonnull DistributionQueueProvider queueProvider) throws DistributionQueueException {
        DistributionQueueItem queueItem = getItem(distributionPackage);
        DistributionQueue queue = queueProvider.getQueue(DEFAULT_QUEUE_NAME);
        return queue.add(queueItem);
    }

    @Nonnull
    public List<String> getQueueNames() {
        return Arrays.asList(new String[] { DEFAULT_QUEUE_NAME });
    }


    private DistributionQueueItem getItem(DistributionPackage distributionPackage) {
        DistributionQueueItem distributionQueueItem = new DistributionQueueItem(distributionPackage.getId(),
                distributionPackage.getPaths(),
                distributionPackage.getAction(),
                distributionPackage.getType(),
                distributionPackage.getInfo());

        return distributionQueueItem;
    }

}
