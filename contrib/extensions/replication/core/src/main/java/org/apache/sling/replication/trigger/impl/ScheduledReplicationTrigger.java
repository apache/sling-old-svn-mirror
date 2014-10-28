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

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.trigger.ReplicationRequestHandler;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.trigger.ReplicationTriggerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.trigger.ReplicationTrigger} to schedule replications on a certain
 * {@link org.apache.sling.replication.agent.ReplicationAgent}
 */
public class ScheduledReplicationTrigger implements ReplicationTrigger {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationActionType replicationAction;
    private final String path;
    private final int secondsInterval;

    private final Scheduler scheduler;


    public ScheduledReplicationTrigger(String replicationActionName, String path, int secondsInterval, Scheduler scheduler) {
        this.replicationAction = ReplicationActionType.fromName(replicationActionName);
        this.path = path;
        this.secondsInterval = secondsInterval;
        this.scheduler = scheduler;
    }

    public void register(ReplicationRequestHandler requestHandler) throws ReplicationTriggerException {
        try {
            ScheduleOptions options = scheduler.NOW(-1, secondsInterval);
            options.name(requestHandler.toString());
            scheduler.schedule(new ScheduledReplication(requestHandler), options);
        } catch (Exception e) {
            throw new ReplicationTriggerException("unable to register handler " + requestHandler, e);
        }
    }

    public void unregister(ReplicationRequestHandler requestHandler) throws ReplicationTriggerException {
        scheduler.unschedule(requestHandler.toString());
    }

    private class ScheduledReplication implements Runnable {
        private final ReplicationRequestHandler requestHandler;

        public ScheduledReplication(ReplicationRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void run() {
            log.debug("agent {}: scheduling {} replication of {}", new Object[]{requestHandler, replicationAction, path});

            requestHandler.handle(new ReplicationRequest(System.currentTimeMillis(), replicationAction, path));
        }
    }
}
