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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a simple scheduled {@link SimpleReplicationQueue}s processor
 */
@Component(
    label = "In memory Replication Queues processor", 
    description = "Service that trigger processing of elements in memory resident replication queues.",
    metatype = true)
@Service(value = Runnable.class)
@Properties({
        @Property(name = "scheduler.period", longValue = 10, label = "Frequency", description = "Processing frequency in seconds"),
        @Property(name = "scheduler.concurrent", boolValue = false, propertyPrivate = true) })
public class ScheduledReplicationQueueProcessor implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(name = "ReplicationQueueProvider", target = "(name="
                    + SimpleReplicationQueueProvider.NAME + ")", policy = ReferencePolicy.DYNAMIC)
    private ReplicationQueueProvider replicationQueueProvider;

    public void run() {
        try {
            for (ReplicationQueue queue : replicationQueueProvider.getAllQueues()) {
                while (!queue.isEmpty()) {
                    // synchronized (queue) {
                    ReplicationPackage item = queue.getHead();
                    if (item != null) {
                        try {
                            if (((SimpleReplicationQueue)queue).getAgent().process(item)) {
                                queue.removeHead();
                            } else {
                                if (log.isWarnEnabled()) {
                                    log.warn("processing of item {} failed",
                                                    Arrays.toString(item.getPaths()));
                                }
                            }
                        } catch (AgentReplicationException e) {
                            if (log.isErrorEnabled()) {
                                log.error("an error happened while processing an item {}",
                                                Arrays.toString(item.getPaths()));
                            }
                        }
                    }
                    // }
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("error while processing queue {}", e);
            }
        }

    }
}
