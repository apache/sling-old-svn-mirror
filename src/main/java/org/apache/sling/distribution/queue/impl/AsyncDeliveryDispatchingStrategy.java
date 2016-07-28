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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.packaging.impl.ReferencePackage;
import org.apache.sling.distribution.packaging.impl.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatching strategy that delivers items to their target queue unless the queue contains too many items, in such a case
 * a reference package is created and put into that queue while the actual package is sent using a different queue for
 * storage at the receiving side so that it'll be installed only once the reference is processed.
 * This guarantees delivery order while maximizing the network throughput when there're many items in the queues.
 */
public class AsyncDeliveryDispatchingStrategy implements DistributionQueueDispatchingStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<String> queues;
    private final Map<String, String> deliveryMappings;

    /**
     * create an async delivery strategy
     * @param deliveryMappings the mappings (by name) between the default queue for a certain endpoint and its respective delivery queue
     */
    public AsyncDeliveryDispatchingStrategy(Map<String, String> deliveryMappings) {
        this.deliveryMappings = deliveryMappings;
        this.queues = new LinkedList<String>();
        queues.addAll(deliveryMappings.keySet());
        queues.addAll(deliveryMappings.values());
    }

    @Override
    public Iterable<DistributionQueueItemStatus> add(@Nonnull DistributionPackage distributionPackage,
                                                     @Nonnull DistributionQueueProvider queueProvider) throws DistributionException {

        if (!(distributionPackage instanceof SharedDistributionPackage) && deliveryMappings.size() > 1) {
            throw new DistributionException("distribution package must be a shared package to be added in multiple queues");
        }

        String distributionPackageId = distributionPackage.getId();

        List<DistributionQueueItemStatus> result = new LinkedList<DistributionQueueItemStatus>();

        for (String referenceQueueName : deliveryMappings.keySet()) {
            DistributionQueue queue = queueProvider.getQueue(referenceQueueName);

            if (queue.getStatus().getItemsCount() > 30) {
                // too many items in the queue, let's send actual packages and references separately

                distributionPackage.getInfo().put("reference-required", true);
                DistributionQueueItem item = getItem(distributionPackage);

                // create and acquire reference package
                ReferencePackage referencePackage = new ReferencePackage(distributionPackage);
                DistributionPackageUtils.acquire(referencePackage, referenceQueueName);

                // acquire actual package
                String deliveryQueueName = deliveryMappings.get(referenceQueueName);
                DistributionPackageUtils.acquire(distributionPackage, deliveryQueueName);

                // add the actual package to the delivery queue
                DistributionQueue deliveryQueue = queueProvider.getQueue(deliveryQueueName);
                DistributionQueueEntry deliveryQueueEntry = deliveryQueue.add(item);
                if (deliveryQueueEntry != null) {
                    DistributionQueueItemStatus status = deliveryQueueEntry.getStatus();
                    log.debug("item {} added to delivery queue: {}", item, status);
                    result.add(status);
                } else {
                    DistributionPackageUtils.release(distributionPackage, deliveryQueueName);
                    log.error("cannot add package {} to delivery queue {}", distributionPackageId, deliveryQueueName);
                    result.add(new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, deliveryQueue.getName()));
                }

                // add the reference package to the reference queue
                DistributionQueue referenceQueue = queueProvider.getQueue(referenceQueueName);
                DistributionQueueItem referenceQueueItem = getItem(referencePackage);
                DistributionQueueEntry referenceQueueEntry = referenceQueue.add(referenceQueueItem);
                if (referenceQueueEntry != null) {
                    DistributionQueueItemStatus status = referenceQueueEntry.getStatus();
                    log.debug("item {} added to reference queue: {}", referenceQueueItem, status);
                } else {
                    DistributionPackageUtils.release(referencePackage, referenceQueueName);
                    log.error("cannot add package {} to reference queue {}", distributionPackageId, referenceQueueName);
                    result.add(new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, referenceQueue.getName()));
                }

            } else {
                // normal queueing

                DistributionQueueItem item = getItem(distributionPackage);

                DistributionQueueItemStatus status = new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, queue.getName());
                DistributionPackageUtils.acquire(distributionPackage, referenceQueueName);

                DistributionQueueEntry queueEntry = queue.add(item);

                if (queueEntry != null) {
                    status = queueEntry.getStatus();
                    log.debug("item {} added to queue: {}", item, status);
                } else {
                    DistributionPackageUtils.release(distributionPackage, referenceQueueName);
                    log.error("cannot add package {} to queue {}", distributionPackageId, referenceQueueName);
                }

                result.add(status);
            }
        }

        return result;

    }

    @Nonnull
    public List<String> getQueueNames() {
        return queues;
    }

    private DistributionQueueItem getItem(DistributionPackage distributionPackage) {
        return DistributionPackageUtils.toQueueItem(distributionPackage);
    }
}
