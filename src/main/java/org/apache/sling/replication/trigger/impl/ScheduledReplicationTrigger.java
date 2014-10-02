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
package org.apache.sling.replication.trigger.impl;

import java.util.Map;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.ReplicationTriggerRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.trigger.ReplicationTrigger} to schedule replications on a certain
 * {@link org.apache.sling.replication.agent.ReplicationAgent}
 */
public class ScheduledReplicationTrigger implements ReplicationTrigger {

    public static final String TYPE = "scheduledEvent";

    public static final String ACTION = "action";
    public static final String PATH = "path";
    public static final String SECONDS = "seconds";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationActionType replicationAction;
    private final String path;
    private final int secondsInterval;

    private final Scheduler scheduler;

    public ScheduledReplicationTrigger(Map<String, Object> config, Scheduler scheduler) {
        this(ReplicationActionType.fromName(PropertiesUtil.toString(config.get(ACTION), ReplicationActionType.POLL.name())),
                PropertiesUtil.toString(config.get(PATH), "/"),
                PropertiesUtil.toInteger(config.get(SECONDS), 30),
                scheduler);
    }

    public ScheduledReplicationTrigger(ReplicationActionType replicationAction, String path, int secondsInterval, Scheduler scheduler) {
        this.replicationAction = replicationAction;
        this.path = path;
        this.secondsInterval = secondsInterval;
        this.scheduler = scheduler;
    }

    public void register(String handlerId, ReplicationTriggerRequestHandler requestHandler) {
        ScheduleOptions options = scheduler.NOW(-1, secondsInterval);
        options.name(handlerId);
        scheduler.schedule(new ScheduledReplication(requestHandler), options);
    }

    public void unregister(String handlerId) {
        scheduler.unschedule(handlerId);
    }

    private class ScheduledReplication implements Runnable {
        private final ReplicationTriggerRequestHandler requestHandler;


        public ScheduledReplication(ReplicationTriggerRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void run() {
            log.debug("agent {}: scheduling {} replication of {}", new Object[]{requestHandler, replicationAction, path});

            requestHandler.handle(new ReplicationRequest(System.currentTimeMillis(), replicationAction, path));
        }
    }
}
