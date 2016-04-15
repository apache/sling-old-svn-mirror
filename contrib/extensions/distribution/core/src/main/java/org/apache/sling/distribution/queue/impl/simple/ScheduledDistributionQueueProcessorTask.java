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
package org.apache.sling.distribution.queue.impl.simple;

import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a simple scheduled {@link SimpleDistributionQueue}s processor
 */
class ScheduledDistributionQueueProcessorTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final SimpleDistributionQueueProvider queueProvider;
    private final DistributionQueueProcessor queueProcessor;

    public ScheduledDistributionQueueProcessorTask(SimpleDistributionQueueProvider queueProvider,
                                                   DistributionQueueProcessor queueProcessor) {
        this.queueProvider = queueProvider;
        this.queueProcessor = queueProcessor;
    }

    public void run() {
        try {
            for (DistributionQueue queue : queueProvider.getQueues()) {
                DistributionQueueEntry entry;
                while ((entry = queue.getHead()) != null) {

                    if (queueProcessor.process(queue.getName(), entry)) {
                        if (queue.remove(entry.getId()) != null) {
                            log.debug("item {} processed and removed from the queue", entry.getItem());
                        }
                    } else {
                        log.warn("processing of item {} failed", entry.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("error while processing queue {}", e);
        }

    }
}
