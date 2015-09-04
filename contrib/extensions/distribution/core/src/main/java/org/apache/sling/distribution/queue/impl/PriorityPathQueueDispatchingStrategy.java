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
import java.util.Arrays;
import java.util.List;

import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Distribution algorithm which keeps one specific queue to handle specific paths and another queue
 * for handling all the other paths
 */
public class PriorityPathQueueDispatchingStrategy implements DistributionQueueDispatchingStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String[] priorityPaths;

    public PriorityPathQueueDispatchingStrategy(String[] priorityPaths) {
        this.priorityPaths = priorityPaths;

    }

    private DistributionQueue getQueue(DistributionQueueItem queueItem, DistributionQueueProvider queueProvider)
            throws DistributionQueueException {
        DistributionPackageInfo packageInfo = DistributionPackageUtils.fromQueueItem(queueItem);
        String[] paths = packageInfo.getPaths();

        String pp = null;

        if (paths != null) {
            log.info("calculating priority for paths {}", Arrays.toString(paths));

            for (String path : paths) {
                for (String priorityPath : priorityPaths) {
                    if (path.startsWith(priorityPath)) {
                        pp = priorityPath;
                        break;
                    }
                }
            }
        }

        DistributionQueue queue;
        if (pp != null) {
            log.info("using priority queue for path {}", pp);
            queue = queueProvider.getQueue(pp);
        } else {
            log.info("using default queue");
            queue = queueProvider.getQueue(DEFAULT_QUEUE_NAME);
        }
        return queue;
    }

    public Iterable<DistributionQueueItemStatus> add(@Nonnull DistributionPackage distributionPackage, @Nonnull DistributionQueueProvider queueProvider) throws DistributionQueueException {
        DistributionQueueItem queueItem = getItem(distributionPackage);
        DistributionQueue queue = getQueue(queueItem, queueProvider);
        if (queue.add(queueItem)) {
            return Arrays.asList(queue.getItem(queueItem.getId()).getStatus());
        } else {
            return Arrays.asList(new DistributionQueueItemStatus(DistributionQueueItemState.ERROR, queue.getName()));
        }
    }


    @Nonnull
    public List<String> getQueueNames() {
        List<String> paths = Arrays.asList(priorityPaths);
        paths.add(DEFAULT_QUEUE_NAME);

        return paths;
    }

    private DistributionQueueItem getItem(DistributionPackage distributionPackage) {
        DistributionQueueItem distributionQueueItem = DistributionPackageUtils.toQueueItem(distributionPackage);

        return distributionQueueItem;
    }


}
