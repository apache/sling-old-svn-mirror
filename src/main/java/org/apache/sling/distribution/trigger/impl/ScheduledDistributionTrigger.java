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
package org.apache.sling.distribution.trigger.impl;

import javax.annotation.Nonnull;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionRequestType;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.distribution.trigger.DistributionTrigger} to schedule distributions on a certain
 * {@link org.apache.sling.distribution.agent.DistributionAgent}
 */
public class ScheduledDistributionTrigger implements DistributionTrigger {
    private final static String SCHEDULE_NAME = "scheduledEventTrigger";


    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionRequestType distributionAction;
    private final String path;
    private final int secondsInterval;

    private final Scheduler scheduler;


    public ScheduledDistributionTrigger(String distributionActionName, String path, int secondsInterval, Scheduler scheduler) {
        this.distributionAction = DistributionRequestType.fromName(distributionActionName);
        this.path = path;
        this.secondsInterval = secondsInterval;
        this.scheduler = scheduler;
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        try {
            ScheduleOptions options = scheduler.NOW(-1, secondsInterval);
            options.name(getJobName(requestHandler));
            scheduler.schedule(new ScheduledDistribution(requestHandler), options);
        } catch (Exception e) {
            throw new DistributionTriggerException("unable to register handler " + requestHandler, e);
        }
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionTriggerException {
        scheduler.unschedule(getJobName(requestHandler));
    }

    private class ScheduledDistribution implements Runnable {
        private final DistributionRequestHandler requestHandler;

        public ScheduledDistribution(DistributionRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void run() {
            log.debug("agent {}: scheduling {} distribution of {}", new Object[]{requestHandler, distributionAction, path});

            requestHandler.handle(new DistributionRequest(distributionAction, path));
        }
    }

    String getJobName(DistributionRequestHandler requestHandler) {
        return SCHEDULE_NAME + requestHandler.toString();
    }
}
