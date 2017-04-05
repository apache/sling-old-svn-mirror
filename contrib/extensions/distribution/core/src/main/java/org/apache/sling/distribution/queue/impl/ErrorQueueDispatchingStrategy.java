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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The error strategy for delivering packages to queues. The strategy delivers the packages in a queue named error-queueName
 * where the queueName is the name of the original queue the package was in.
 */
public class ErrorQueueDispatchingStrategy implements DistributionQueueDispatchingStrategy {
    private final Logger log = LoggerFactory.getLogger(getClass());


    public final static String ERROR_PREFIX = "error-";
    private final Set<String> queueNames = new TreeSet<String>();

    public ErrorQueueDispatchingStrategy(String[] queueNames) {
        this.queueNames.addAll(Arrays.asList(queueNames));
    }

    @Override
    public Iterable<DistributionQueueItemStatus> add(@Nonnull DistributionPackage distributionPackage, @Nonnull DistributionQueueProvider queueProvider) throws DistributionException {

        List<DistributionQueueItemStatus> result = new ArrayList<DistributionQueueItemStatus>();
        String originQueue = DistributionPackageUtils.getQueueName(distributionPackage.getInfo());

        if (!queueNames.contains(originQueue)) {
            return result;
        }

        String errorQueueName = ERROR_PREFIX + originQueue;

        DistributionQueue errorQueue = queueProvider.getQueue(errorQueueName);

        DistributionQueueItemStatus status = new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, errorQueueName);

        DistributionQueueItem queueItem = DistributionPackageUtils.toQueueItem(distributionPackage);
        DistributionPackageUtils.acquire(distributionPackage, errorQueueName);
        DistributionQueueEntry queueEntry = errorQueue.add(queueItem);

        if (queueEntry != null) {
            status = queueEntry.getStatus();
        } else {
            DistributionPackageUtils.releaseOrDelete(distributionPackage, errorQueueName);
            log.error("cannot add package {} to queue {}", distributionPackage.getId(), errorQueueName);
        }

        result.add(status);

        return result;
    }

    @Nonnull
    @Override
    public List<String> getQueueNames() {
        List<String> errorQueueNames = new ArrayList<String>();
        for (String queueName : queueNames) {
            errorQueueNames.add(ERROR_PREFIX + queueName);
        }
        return errorQueueNames;
    }
}
