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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.util.impl.DistributionUtils;
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
    private final String serviceName;
    private final ResourceResolverFactory resourceResolverFactory;

    private final Set<String> registeredJobs = Collections.synchronizedSet(new HashSet<String>());


    public ScheduledDistributionTrigger(String distributionActionName, String path, int secondsInterval, String serviceName, Scheduler scheduler, ResourceResolverFactory resourceResolverFactory) {
        this.serviceName = serviceName;
        this.resourceResolverFactory = resourceResolverFactory;
        this.distributionAction = DistributionRequestType.fromName(distributionActionName);
        this.path = path;
        this.secondsInterval = secondsInterval;
        this.scheduler = scheduler;

        if (distributionAction == null) {
            throw new IllegalArgumentException("unsupported action " + distributionActionName);
        }

        if (path == null &&
                (DistributionRequestType.ADD.equals(distributionAction)
                        || DistributionRequestType.DELETE.equals(distributionAction))) {

            throw new IllegalArgumentException("path is required for action " + distributionActionName);
        }
    }

    public void register(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        try {
            ScheduleOptions options = scheduler.NOW(-1, secondsInterval);
            String jobName = getJobName(requestHandler);

            options.name(jobName);
            options.canRunConcurrently(false);
            options.onLeaderOnly(true);
            boolean success = scheduler.schedule(new ScheduledDistribution(requestHandler), options);

            if (success) {
                registeredJobs.add(jobName);

            }
            log.info("handler registered {} {}", jobName, success);

        } catch (Exception e) {
            throw new DistributionException("unable to register handler " + requestHandler, e);
        }
    }

    public void unregister(@Nonnull DistributionRequestHandler requestHandler) throws DistributionException {
        String jobName = getJobName(requestHandler);

        boolean success = scheduler.unschedule(jobName);

        if (success) {
            registeredJobs.remove(jobName);
        }
        log.info("handler unregistered {} {}", jobName, success);


    }

    public void disable() {
        for (String jobName : registeredJobs) {
            boolean result = scheduler.unschedule(jobName);
            log.info("handler unregistered {} {}", jobName, result);

        }
    }

    private class ScheduledDistribution implements Runnable {
        private final DistributionRequestHandler requestHandler;

        public ScheduledDistribution(DistributionRequestHandler requestHandler) {
            this.requestHandler = requestHandler;
        }

        public void run() {
            log.debug("agent {}: scheduling {} distribution of {}", new Object[]{requestHandler, distributionAction, path});

            if (serviceName == null) {
                requestHandler.handle(null, new SimpleDistributionRequest(distributionAction, path));
            } else {
                ResourceResolver resourceResolver = null;
                try {
                    resourceResolver = DistributionUtils.loginService(resourceResolverFactory, serviceName);
                    requestHandler.handle(resourceResolver, new SimpleDistributionRequest(distributionAction, path));
                } catch (LoginException le) {
                    log.error("cannot obtain resource resolver for {}", serviceName);
                } finally {
                    DistributionUtils.safelyLogout(resourceResolver);
                }
            }
        }
    }

    private String getJobName(DistributionRequestHandler requestHandler) {
        return SCHEDULE_NAME + requestHandler.toString();
    }
}
