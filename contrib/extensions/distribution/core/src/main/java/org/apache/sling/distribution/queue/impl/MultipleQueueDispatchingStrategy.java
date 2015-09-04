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

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.SharedDistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * The default strategy for delivering packages to queues. Each package can be dispatched to multiple queues.
 */
public class MultipleQueueDispatchingStrategy implements DistributionQueueDispatchingStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String[] queueNames;

    public MultipleQueueDispatchingStrategy(String[] queueNames) {

        this.queueNames = queueNames;
    }

    public Iterable<DistributionQueueItemStatus> add(@Nonnull DistributionPackage distributionPackage, @Nonnull DistributionQueueProvider queueProvider) throws DistributionQueueException {


        if (!(distributionPackage instanceof SharedDistributionPackage) && queueNames.length > 1) {
            throw new DistributionQueueException("distribution package must be a shared package to be added in multiple queues");
        }

        DistributionQueueItem queueItem = getItem(distributionPackage);
        List<DistributionQueueItemStatus> result = new ArrayList<DistributionQueueItemStatus>();

        // acquire the package temporarily until all queues are filled
        String tempQueueName = "temp" + UUID.randomUUID();
        DistributionPackageUtils.acquire(distributionPackage, tempQueueName);

        try {
            for (String queueName: queueNames) {
                DistributionQueue queue = queueProvider.getQueue(queueName);
                DistributionQueueItemStatus status = new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, queue.getName());

                DistributionPackageUtils.acquire(distributionPackage, queueName);
                if (queue.add(queueItem)) {
                    status = queue.getItem(queueItem.getId()).getStatus();
                } else {
                    DistributionPackageUtils.releaseOrDelete(distributionPackage, queueName);
                }

                result.add(status);
            }
        } finally {
            DistributionPackageUtils.releaseOrDelete(distributionPackage, tempQueueName);
        }

        return result;

    }

    @Nonnull
    public List<String> getQueueNames() {

        return Arrays.asList(queueNames);
    }


    private DistributionQueueItem getItem(DistributionPackage distributionPackage) {
        DistributionQueueItem distributionQueueItem = DistributionPackageUtils.toQueueItem(distributionPackage);

        return distributionQueueItem;
    }

}
