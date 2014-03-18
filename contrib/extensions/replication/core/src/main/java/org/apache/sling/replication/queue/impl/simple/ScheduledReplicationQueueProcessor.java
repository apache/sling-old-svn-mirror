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
package org.apache.sling.replication.queue.impl.simple;

import java.util.Arrays;

import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a simple scheduled {@link SimpleReplicationQueue}s processor
 */
public class ScheduledReplicationQueueProcessor implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ReplicationQueueProvider queueProvider;
    private final ReplicationQueueProcessor queueProcessor;

    public ScheduledReplicationQueueProcessor(ReplicationQueueProvider queueProvider,
                                              ReplicationQueueProcessor queueProcessor){

        this.queueProvider = queueProvider;
        this.queueProcessor = queueProcessor;
    }


    public void run() {
        try {
            for (ReplicationQueue queue : queueProvider.getAllQueues()) {
                while (!queue.isEmpty()) {
                    // synchronized (queue) {
                    ReplicationQueueItem item = queue.getHead();
                    if (item != null) {
                        if (queueProcessor.process(queue.getName(), item)) {
                            queue.removeHead();
                        } else {
                            log.warn("processing of item {} failed",
                                    Arrays.toString(item.getPaths()));

                        }
                    }
                    // }
                }
            }
        } catch (Exception e) {
            log.error("error while processing queue {}", e);
        }

    }
}
