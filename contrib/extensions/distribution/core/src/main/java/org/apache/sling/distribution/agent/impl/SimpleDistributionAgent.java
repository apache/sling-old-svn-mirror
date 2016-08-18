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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentState;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.impl.CompositeDistributionResponse;
import org.apache.sling.distribution.impl.SimpleDistributionResponse;
import org.apache.sling.distribution.log.DistributionLog;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.impl.SimpleAgentDistributionQueue;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.util.impl.DistributionUtils;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * Basic implementation of a {@link org.apache.sling.distribution.agent.DistributionAgent}
 */
public class SimpleDistributionAgent implements DistributionAgent {
    private final static String DEFAULT_AGENT_SERVICE = "defaultAgentService";

    private final DistributionQueueProvider queueProvider;
    private final DistributionPackageImporter distributionPackageImporter;
    private final DistributionPackageExporter distributionPackageExporter;
    private final DistributionQueueDispatchingStrategy scheduleQueueStrategy;
    private final DistributionQueueDispatchingStrategy errorQueueStrategy;
    private final DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy;
    private final DefaultDistributionLog log;
    private final DistributionEventFactory distributionEventFactory;
    private final DistributionQueueProcessor queueProcessor;
    private AgentBasedRequestHandler agentBasedRequestHandler;

    private final String name;
    private final boolean queueProcessingEnabled;
    private final DistributionRequestType[] allowedRequests;
    private boolean active = false;
    private final Set<String> processingQueues;
    private final String[] allowedRoots;
    private final AtomicInteger nextRequestId = new AtomicInteger();

    private final SimpleDistributionAgentAuthenticationInfo agentAuthenticationInfo;

    public SimpleDistributionAgent(String name,
                                   boolean queueProcessingEnabled,
                                   Set<String> processingQueues,
                                   String subServiceName,
                                   DistributionPackageImporter distributionPackageImporter,
                                   DistributionPackageExporter distributionPackageExporter,
                                   DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy,
                                   DistributionQueueProvider queueProvider,
                                   DistributionQueueDispatchingStrategy scheduleQueueStrategy,
                                   DistributionQueueDispatchingStrategy errorQueueStrategy,
                                   DistributionEventFactory distributionEventFactory,
                                   ResourceResolverFactory resourceResolverFactory,
                                   SlingRepository slingRepository,
                                   DefaultDistributionLog log,
                                   DistributionRequestType[] allowedRequests,
                                   String[] allowedRoots,
                                   int retryAttempts) {
        this.log = log;
        this.allowedRequests = allowedRequests;
        this.processingQueues = processingQueues;

        validateConfiguration(name, queueProcessingEnabled, subServiceName, distributionPackageImporter, distributionPackageExporter, distributionRequestAuthorizationStrategy, queueProvider, scheduleQueueStrategy, distributionEventFactory, resourceResolverFactory);

        this.allowedRoots = SettingsUtils.removeEmptyEntries(allowedRoots);
        this.distributionRequestAuthorizationStrategy = distributionRequestAuthorizationStrategy;
        this.name = SettingsUtils.removeEmptyEntry(name);
        this.queueProcessingEnabled = queueProcessingEnabled;
        this.distributionPackageImporter = distributionPackageImporter;
        this.distributionPackageExporter = distributionPackageExporter;
        this.queueProvider = queueProvider;
        this.scheduleQueueStrategy = scheduleQueueStrategy;
        this.errorQueueStrategy = errorQueueStrategy;
        this.distributionEventFactory = distributionEventFactory;
        this.agentAuthenticationInfo = new SimpleDistributionAgentAuthenticationInfo(slingRepository, DEFAULT_AGENT_SERVICE, resourceResolverFactory, subServiceName);
        this.queueProcessor = new SimpleDistributionAgentQueueProcessor(distributionPackageExporter, distributionPackageImporter,
                retryAttempts, errorQueueStrategy, log, queueProvider, distributionEventFactory, agentAuthenticationInfo, name);
    }

    private void validateConfiguration(String name, boolean queueProcessingEnabled, String subServiceName, DistributionPackageImporter distributionPackageImporter, DistributionPackageExporter distributionPackageExporter, DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy, DistributionQueueProvider queueProvider, DistributionQueueDispatchingStrategy scheduleQueueStrategy, DistributionEventFactory distributionEventFactory, ResourceResolverFactory resourceResolverFactory) {
        // check configuration is valid
        if (name == null
                || (queueProcessingEnabled && distributionPackageImporter == null)
                || distributionPackageExporter == null
                || distributionRequestAuthorizationStrategy == null
                || queueProvider == null
                || scheduleQueueStrategy == null
                || distributionEventFactory == null
                || resourceResolverFactory == null) {

            String errorMessage = Arrays.toString(new Object[]{name,
                    distributionPackageImporter,
                    distributionPackageExporter,
                    subServiceName,
                    distributionRequestAuthorizationStrategy,
                    queueProvider,
                    scheduleQueueStrategy,
                    distributionEventFactory,
                    resourceResolverFactory});
            throw new IllegalArgumentException("all arguments are required: " + errorMessage);
        }
    }

    @Nonnull
    public DistributionResponse execute(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest)
            throws DistributionException {

        ResourceResolver agentResourceResolver = null;

        final String requestId = "DSTRQ" + nextRequestId.incrementAndGet();
        String callingUser = resourceResolver.getUserID();

        try {
            // check if the request type can be executed by this agent
            if (!isAcceptedRequestType(distributionRequest)) {
                log.debug("request type not accepted {}", distributionRequest.getRequestType());
                return new SimpleDistributionResponse(DistributionRequestState.DROPPED, "Request type not accepted");
            }

            // check if the request paths can be distributed via this agent
            if (!isAcceptedRequestRoot(distributionRequest)) {
                log.debug("request paths not accepted {}", Arrays.toString(distributionRequest.getPaths()));
                return new SimpleDistributionResponse(DistributionRequestState.DROPPED, "Request paths not accepted");
            }

            boolean silent = DistributionRequestType.PULL.equals(distributionRequest.getRequestType());

            log.info(silent, "REQUEST-START {}: {} paths={}, user={}", new Object[]{requestId,
                    distributionRequest.getRequestType(), distributionRequest.getPaths(), callingUser});

            // check permissions
            distributionRequestAuthorizationStrategy.checkPermission(resourceResolver, distributionRequest);

            agentResourceResolver = DistributionUtils.getResourceResolver(callingUser, agentAuthenticationInfo.getAgentService(),
                    agentAuthenticationInfo.getSlingRepository(), agentAuthenticationInfo.getSubServiceName(),
                    agentAuthenticationInfo.getResourceResolverFactory());

            // export packages
            CompositeDistributionResponse distributionResponse = exportPackages(agentResourceResolver, distributionRequest, callingUser, requestId);

            log.debug("REQUEST-STARTED {}: {} paths={}, success={}, state={}, exportTime={}ms, noPackages={}, size={}B, noQueues={}", new Object[]{
                    requestId,
                    distributionRequest.getRequestType(), distributionRequest.getPaths(),
                    distributionResponse.isSuccessful(), distributionResponse.getState(),
                    distributionResponse.getExportTime(),
                    distributionResponse.getPackagesCount(), distributionResponse.getPackagseSize(),
                    distributionResponse.getQueuesCount()
            });

            return distributionResponse;
        } catch (DistributionException e) {
            log.error("REQUEST-FAIL {}: {} paths={}, user={}, message={}", new Object[]{requestId,
                    distributionRequest.getRequestType(), distributionRequest.getPaths(), callingUser, e.getMessage()});
            throw e;
        } finally {
            DistributionUtils.ungetResourceResolver(agentResourceResolver);
        }
    }

    private boolean isPassive() {
        return !queueProcessingEnabled;
    }

    private CompositeDistributionResponse exportPackages(ResourceResolver agentResourceResolver, DistributionRequest distributionRequest, String callingUser, String requestId) throws DistributionException {
        final long startTime = System.currentTimeMillis();
        // callback function
        DistributionPackageExporterProcessor packageProcessor = new DistributionPackageExporterProcessor(callingUser, requestId, startTime,
                distributionEventFactory, scheduleQueueStrategy, queueProvider, log, name);

        // export packages
        distributionPackageExporter.exportPackages(agentResourceResolver, distributionRequest, packageProcessor);

        final long endTime = System.currentTimeMillis();

        generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_CREATED);

        // create response
        List<DistributionResponse> distributionResponses = packageProcessor.getAllResponses();
        int packagesCount = packageProcessor.getPackagesCount();
        long packagesSize = packageProcessor.getPackagesSize();

        return new CompositeDistributionResponse(distributionResponses, packagesCount, packagesSize, endTime - startTime);
    }

    @Nonnull
    public Set<String> getQueueNames() {
        Set<String> queueNames = new TreeSet<String>();
        queueNames.addAll(scheduleQueueStrategy.getQueueNames());
        if (errorQueueStrategy != null) {
            queueNames.addAll(errorQueueStrategy.getQueueNames());
        }
        return queueNames;
    }

    public DistributionQueue getQueue(@Nonnull final String queueName) {
        Set<String> queues = getQueueNames();
        if (!queues.contains(queueName)) {
            return null;
        }

        DistributionQueue queue = null;

        try {
            queue = queueProvider.getQueue(queueName);
        } catch (DistributionException e) {
            log.error("cannot get queue", e);
        }

        if (queue != null) {
            boolean isPausedQueue = !queueProcessingEnabled && (processingQueues != null && processingQueues.contains(queueName));
            queue = new SimpleAgentDistributionQueue(queue, isPausedQueue, name);
        }

        return queue;
    }

    @Nonnull
    public DistributionLog getLog() {
        return log;
    }

    @Nonnull
    public DistributionAgentState getState() {
        DistributionAgentState agentState = DistributionAgentState.IDLE;

        // if it is passive and it is not a queueing agent
        if (isPassive() && distributionPackageImporter != null) {
            return DistributionAgentState.PAUSED;
        }

        for (String queueName : getQueueNames()) {
            DistributionQueue queue = getQueue(queueName);

            DistributionQueueState state = queue.getStatus().getState();
            if (DistributionQueueState.BLOCKED == state) {
                return DistributionAgentState.BLOCKED;
            }

            if (DistributionQueueState.RUNNING == state) {
                agentState = DistributionAgentState.RUNNING;
            }
        }
        return agentState;
    }

    public void enable() {
        log.info("enabling agent");

        // register triggers if any
        agentBasedRequestHandler = new AgentBasedRequestHandler(this);

        if (!isPassive()) {
            try {
                queueProvider.enableQueueProcessing(queueProcessor, processingQueues.toArray(new String[processingQueues.size()]));
            } catch (DistributionException e) {
                log.error("cannot enable queue processing", e);
            }
        }

        active = true;
    }

    public void enableTrigger(DistributionTrigger trigger) {
        if (!active || agentBasedRequestHandler == null) {
            return;
        }

        try {
            log.info("enabling trigger {}", trigger);

            trigger.register(agentBasedRequestHandler);
        } catch (DistributionException e) {
            log.error("could not register handler from trigger {} {}", trigger, e);
        }

    }

    public void disableTrigger(DistributionTrigger trigger) {
        if (!active || agentBasedRequestHandler == null) {
            return;
        }

        try {
            log.info("disabling trigger {}", trigger);

            trigger.unregister(agentBasedRequestHandler);
        } catch (DistributionException e) {
            log.error("could not unregister handler from trigger {} {}", trigger, e);
        }
    }

    public void disable() {
        log.info("disabling agent");
        active = false;

        agentBasedRequestHandler = null;

        if (!isPassive()) {

            try {
                queueProvider.disableQueueProcessing();
            } catch (DistributionException e) {
                log.error("cannot disable queue processing", e);
            }
        }

    }

    private void generatePackageEvent(String topic, DistributionPackage... distributionPackages) {
        for (DistributionPackage distributionPackage : distributionPackages) {
            distributionEventFactory.generatePackageEvent(topic, DistributionComponentKind.AGENT, name, distributionPackage.getInfo());
        }
    }

    /**
     * check whether the given request type can be handled by the current agent
     * @param request a distribution request
     * @return {@code true} if request can be executed, {@code false} otherwise.
     */
    private boolean isAcceptedRequestType(DistributionRequest request) {
        if (allowedRequests == null) {
            return true;
        }

        if (DistributionRequestType.TEST.equals(request.getRequestType())) {
            return true;
        }

        for (DistributionRequestType requestType : allowedRequests) {
            if (requestType.equals(request.getRequestType())) {
                return true;
            }
        }

        return false;
    }

    /**
     * check whether a certain request paths can be distributed via this agent
     * @param request a distribution request
     * @return {@code true} if request can be executed, {@code false} otherwise.
     */
    private boolean isAcceptedRequestRoot(DistributionRequest request) {
        if (allowedRoots == null || allowedRoots.length == 0) {
            return true;
        }

        if (!DistributionRequestType.ADD.equals(request.getRequestType()) && !DistributionRequestType.DELETE.equals(request.getRequestType())) {
            return true;
        }

        for (String path : request.getPaths()) {
            boolean allowed = false;
            for (String allowedRoot : allowedRoots) {
                if (allowedRoot != null && allowedRoot.trim().length() != 0
                        && path.startsWith(allowedRoot)) {
                    allowed = true;
                }
            }

            if (!allowed) {
                return false;
            }
        }

        return true;
    }

    public class AgentBasedRequestHandler implements DistributionRequestHandler {
        private final DistributionAgent agent;

        public AgentBasedRequestHandler(DistributionAgent agent) {
            this.agent = agent;
        }

        public void handle(@Nullable ResourceResolver resourceResolver, @Nonnull DistributionRequest request) {

            if (!active) {
                log.warn("skipping agent handler as agent is disabled");
                return;
            }

            if (resourceResolver != null) {
                try {
                    agent.execute(resourceResolver, request);
                } catch (Throwable t) {
                    log.error("Error executing handler {}", request, t);
                }
            } else {
                ResourceResolver agentResourceResolver = null;

                try {
                    agentResourceResolver = DistributionUtils.getResourceResolver(null, agentAuthenticationInfo.getAgentService(),
                            agentAuthenticationInfo.getSlingRepository(), agentAuthenticationInfo.getSubServiceName(),
                            agentAuthenticationInfo.getResourceResolverFactory());

                    agent.execute(agentResourceResolver, request);
                } catch (Throwable e) {
                    log.error("Error executing handler {}", request, e);
                } finally {
                    DistributionUtils.ungetResourceResolver(agentResourceResolver);
                }
            }

        }
    }


}
