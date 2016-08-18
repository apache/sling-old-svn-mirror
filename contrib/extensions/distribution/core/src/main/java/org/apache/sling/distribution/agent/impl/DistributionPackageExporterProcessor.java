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
package org.apache.sling.distribution.agent.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.impl.SimpleDistributionResponse;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;

/**
 * The package exporter callback function is responsible to process the exported packages.
 * The exported packages are scheduled for import by passing them to a {@link DistributionQueueDispatchingStrategy}.
 */
class DistributionPackageExporterProcessor implements DistributionPackageProcessor {

    private final String callingUser;
    private final String requestId;
    private final long requestStartTime;
    private final AtomicInteger packagesCount = new AtomicInteger();
    private final AtomicLong packagesSize = new AtomicLong();
    private final List<DistributionResponse> allResponses = new LinkedList<DistributionResponse>();

    private final DistributionEventFactory distributionEventFactory;
    private final DistributionQueueDispatchingStrategy scheduleQueueStrategy;
    private final DistributionQueueProvider queueProvider;
    private final DefaultDistributionLog log;
    private final String agentName;

    public List<DistributionResponse> getAllResponses() {
        return allResponses;
    }

    public int getPackagesCount() {
        return packagesCount.get();
    }

    public long getPackagesSize() {
        return packagesSize.get();
    }

    DistributionPackageExporterProcessor(@Nullable String callingUser, @Nonnull String requestId, long requestStartTime,
                                         @Nonnull DistributionEventFactory distributionEventFactory,
                                         @Nonnull DistributionQueueDispatchingStrategy scheduleQueueStrategy,
                                         @Nonnull DistributionQueueProvider queueProvider, @Nonnull DefaultDistributionLog log,
                                         @Nonnull String agentName) {
        this.callingUser = callingUser;
        this.requestId = requestId;
        this.requestStartTime = requestStartTime;
        this.distributionEventFactory = distributionEventFactory;
        this.scheduleQueueStrategy = scheduleQueueStrategy;
        this.queueProvider = queueProvider;
        this.log = log;
        this.agentName = agentName;
    }

    @Override
    public void process(DistributionPackage distributionPackage) {
        final long startTime = System.currentTimeMillis();

        Collection<SimpleDistributionResponse> responses = scheduleImportPackage(distributionPackage, callingUser,
                requestId, requestStartTime);
        packagesCount.incrementAndGet();
        packagesSize.addAndGet(distributionPackage.getSize());
        allResponses.addAll(responses);

        final long endTime = System.currentTimeMillis();

        log.debug("PACKAGE-QUEUED {}: packageId={}, paths={}, queueTime={}ms, responses={}", requestId, distributionPackage.getId(),
                distributionPackage.getInfo().getPaths(), endTime - startTime, responses.size());
    }

    private Collection<SimpleDistributionResponse> scheduleImportPackage(DistributionPackage distributionPackage, String callingUser, String requestId, long startTime) {
        Collection<SimpleDistributionResponse> distributionResponses = new LinkedList<SimpleDistributionResponse>();

        // dispatch the distribution package to one or more queues
        try {
            // add metadata to the package
            distributionPackage.getInfo().put(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_USER, callingUser);
            distributionPackage.getInfo().put(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_ID, requestId);
            distributionPackage.getInfo().put(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_START_TIME, startTime);

            // put the package in the queue
            Iterable<DistributionQueueItemStatus> states = scheduleQueueStrategy.add(distributionPackage, queueProvider);
            for (DistributionQueueItemStatus state : states) {
                DistributionRequestState requestState = getRequestStateFromQueueState(state.getItemState());
                distributionResponses.add(new SimpleDistributionResponse(requestState, state.getItemState().toString()));
            }

            distributionEventFactory.generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_QUEUED,
                    DistributionComponentKind.AGENT, agentName, distributionPackage.getInfo());
        } catch (DistributionException e) {
            log.error("an error happened during dispatching items to the queue(s)", e);
            distributionResponses.add(new SimpleDistributionResponse(DistributionRequestState.DROPPED, e.toString()));
        }

        return distributionResponses;
    }

    /* Convert the state of a certain item in the queue into a request state */
    private DistributionRequestState getRequestStateFromQueueState(DistributionQueueItemState itemState) {
        DistributionRequestState requestState;
        switch (itemState) {
            case QUEUED:
                requestState = DistributionRequestState.ACCEPTED;
                break;
            case ERROR:
                requestState = DistributionRequestState.DROPPED;
                break;
            default:
                requestState = DistributionRequestState.DROPPED;
                break;
        }
        return requestState;
    }
}
