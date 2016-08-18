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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.common.RecoverableDistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.util.impl.DistributionUtils;

/**
 * A processor of agent queue entries, each entry's underlying package is fecthed and passed to the {@link DistributionPackageImporter} for import.
 */
class SimpleDistributionAgentQueueProcessor implements DistributionQueueProcessor {

    private final DistributionPackageExporter distributionPackageExporter;
    private final DistributionPackageImporter distributionPackageImporter;
    private final int retryAttempts;
    private final DistributionQueueDispatchingStrategy errorQueueStrategy;
    private final DefaultDistributionLog log;
    private final DistributionQueueProvider queueProvider;
    private final DistributionEventFactory distributionEventFactory;
    private final SimpleDistributionAgentAuthenticationInfo authenticationInfo;
    private final String agentName;

    public SimpleDistributionAgentQueueProcessor(DistributionPackageExporter distributionPackageExporter,
                                                 DistributionPackageImporter distributionPackageImporter, int retryAttempts,
                                                 DistributionQueueDispatchingStrategy errorQueueStrategy, DefaultDistributionLog log,
                                                 DistributionQueueProvider queueProvider, DistributionEventFactory distributionEventFactory,
                                                 SimpleDistributionAgentAuthenticationInfo authenticationInfo, String agentName) {
        this.distributionPackageExporter = distributionPackageExporter;

        this.distributionPackageImporter = distributionPackageImporter;
        this.retryAttempts = retryAttempts;
        this.errorQueueStrategy = errorQueueStrategy;
        this.log = log;
        this.queueProvider = queueProvider;
        this.distributionEventFactory = distributionEventFactory;
        this.authenticationInfo = authenticationInfo;
        this.agentName = agentName;
    }

    @Override
    public boolean process(@Nonnull String queueName, @Nonnull DistributionQueueEntry queueEntry) {
        DistributionQueueItem queueItem = queueEntry.getItem();

        try {
            final long startTime = System.currentTimeMillis();

            log.debug("[{}] ITEM-PROCESS processing item={}", queueName, queueItem);

            boolean success = processQueueItem(queueName, queueEntry);

            final long endTime = System.currentTimeMillis();

            log.debug("[{}] ITEM-PROCESSED item={}, status={}, processingTime={}ms", queueName, queueItem, success, endTime - startTime);

            return success;

        } catch (Throwable t) {
            log.error("[{}] ITEM-FAIL item={}", queueName, queueItem, t);
            return false;
        }
    }

    private boolean processQueueItem(String queueName, DistributionQueueEntry queueEntry) throws DistributionException {
        boolean removeItemFromQueue = false;
        ResourceResolver agentResourceResolver = null;
        DistributionPackage distributionPackage = null;
        DistributionQueueItem queueItem = queueEntry.getItem();
        DistributionQueueItemStatus queueItemStatus = queueEntry.getStatus();
        try {

            String callingUser = queueItem.get(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_USER, String.class);
            String requestId = queueItem.get(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_ID, String.class);
            Long globalStartTime = queueItem.get(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_START_TIME, Long.class);

            agentResourceResolver = DistributionUtils.getResourceResolver(callingUser, authenticationInfo.getAgentService(),
                    authenticationInfo.getSlingRepository(), authenticationInfo.getSubServiceName(),
                    authenticationInfo.getResourceResolverFactory());

            final long startTime = System.currentTimeMillis();

            distributionPackage = distributionPackageExporter.getPackage(agentResourceResolver, queueItem.getPackageId());

            if (distributionPackage != null) {
                final long packageSize = distributionPackage.getSize();
                DistributionPackageUtils.mergeQueueEntry(distributionPackage.getInfo(), queueEntry);

                final DistributionRequestType requestType = distributionPackage.getInfo().getRequestType();
                final String[] paths = distributionPackage.getInfo().getPaths();

                try {
                    // import package
                    distributionPackageImporter.importPackage(agentResourceResolver, distributionPackage);

                    // generated event
                    distributionEventFactory.generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED,
                            DistributionComponentKind.AGENT, agentName, distributionPackage.getInfo());

                    removeItemFromQueue = true;
                    final long endTime = System.currentTimeMillis();

                    log.info("[{}] PACKAGE-DELIVERED {}: {} paths={}, importTime={}ms, execTime={}ms, size={}B", new Object[]{
                            queueName, requestId,
                            requestType, paths,
                            endTime - startTime, endTime - globalStartTime,
                            packageSize
                    });
                } catch (RecoverableDistributionException e) {
                    log.error("[{}] PACKAGE-FAIL {}: could not deliver {}, {}", queueName, requestId, distributionPackage.getId(), e.getMessage());
                    log.debug("could not deliver package {}", distributionPackage.getId(), e);
                } catch (Throwable e) {
                    log.error("[{}] PACKAGE-FAIL {}: could not deliver package {} {}", queueName, requestId, distributionPackage.getId(), e.getMessage(), e);

                    if (errorQueueStrategy != null && queueItemStatus.getAttempts() > retryAttempts) {
                        removeItemFromQueue = reEnqueuePackage(distributionPackage);
                        log.info("[{}] PACKAGE-QUEUED {}: distribution package {} was enqueued to an error queue", queueName, requestId, distributionPackage.getId());
                    }
                }
            } else {
                removeItemFromQueue = true; // return success if package does not exist in order to clear the queue.
                log.error("distribution package with id {} does not exist. the package will be skipped.", queueItem.getPackageId());
            }
        } finally {
            if (removeItemFromQueue) {
                DistributionPackageUtils.releaseOrDelete(distributionPackage, queueName);
            } else {
                DistributionPackageUtils.closeSafely(distributionPackage);
            }
            DistributionUtils.ungetResourceResolver(agentResourceResolver);
        }

        // return true if item should be removed from queue
        return removeItemFromQueue;
    }

    private boolean reEnqueuePackage(DistributionPackage distributionPackage) {

        if (errorQueueStrategy == null) {
            return false;
        }

        try {
            errorQueueStrategy.add(distributionPackage, queueProvider);
        } catch (DistributionException e) {
            log.error("could not reenqueue package {}", distributionPackage.getId(), e);
            return false;
        }

        return true;
    }

}
