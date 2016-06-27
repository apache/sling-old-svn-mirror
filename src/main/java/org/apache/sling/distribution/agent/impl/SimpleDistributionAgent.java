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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentState;
import org.apache.sling.distribution.common.RecoverableDistributionException;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.impl.CompositeDistributionResponse;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.impl.SimpleDistributionResponse;
import org.apache.sling.distribution.log.DistributionLog;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.queue.impl.SimpleAgentDistributionQueue;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;

import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.util.impl.DistributionUtils;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;


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
    private AgentBasedRequestHandler agentBasedRequestHandler;

    private final SlingRepository slingRepository;
    private final ResourceResolverFactory resourceResolverFactory;

    private final String name;
    private final boolean queueProcessingEnabled;
    private final DistributionRequestType[] allowedRequests;
    private final String subServiceName;
    private boolean active = false;
    private final Set<String> processingQueues;
    private final int retryAttempts;
    private final boolean impersonateUser;
    private final String[] allowedRoots;
    private final AtomicInteger nextRequestId = new AtomicInteger();

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
        this.slingRepository = slingRepository;
        this.log = log;
        this.allowedRequests = allowedRequests;
        this.processingQueues = processingQueues;
        this.retryAttempts = retryAttempts;

        validateConfiguration(name, queueProcessingEnabled, subServiceName, distributionPackageImporter, distributionPackageExporter, distributionRequestAuthorizationStrategy, queueProvider, scheduleQueueStrategy, distributionEventFactory, resourceResolverFactory);

        this.allowedRoots = SettingsUtils.removeEmptyEntries(allowedRoots);
        this.subServiceName = SettingsUtils.removeEmptyEntry(subServiceName);
        this.impersonateUser = this.subServiceName == null;
        this.distributionRequestAuthorizationStrategy = distributionRequestAuthorizationStrategy;
        this.resourceResolverFactory = resourceResolverFactory;
        this.name = SettingsUtils.removeEmptyEntry(name);
        this.queueProcessingEnabled = queueProcessingEnabled;
        this.distributionPackageImporter = distributionPackageImporter;
        this.distributionPackageExporter = distributionPackageExporter;
        this.queueProvider = queueProvider;
        this.scheduleQueueStrategy = scheduleQueueStrategy;
        this.errorQueueStrategy = errorQueueStrategy;
        this.distributionEventFactory = distributionEventFactory;
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

        final String requestId = "DSTRQ"+ nextRequestId.incrementAndGet();
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

            log.info(silent, "REQUEST-START {}: {} paths={}, user={}",  new Object[]{ requestId,
                    distributionRequest.getRequestType(), distributionRequest.getPaths(), callingUser});

            // check permissions
            distributionRequestAuthorizationStrategy.checkPermission(resourceResolver, distributionRequest);

            agentResourceResolver = getAgentResourceResolver(callingUser);

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
            ungetAgentResourceResolver(agentResourceResolver);
        }
    }

    private boolean isPassive() {
        return !queueProcessingEnabled;
    }

    private CompositeDistributionResponse exportPackages(ResourceResolver agentResourceResolver, DistributionRequest distributionRequest, String callingUser, String requestId) throws DistributionException {
        final long startTime = System.currentTimeMillis();
        // callback function
        PackageExporterProcessor packageProcessor = new PackageExporterProcessor(callingUser, requestId, startTime);

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


    private Collection<SimpleDistributionResponse> scheduleImportPackage(DistributionPackage distributionPackage, String callingUser, String requestId, long startTime) {
        Collection<SimpleDistributionResponse> distributionResponses = new LinkedList<SimpleDistributionResponse>();

        // dispatch the distribution package to the queue distribution handler
        try {
            // add metadata to packages
            distributionPackage.getInfo().put(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_USER, callingUser);
            distributionPackage.getInfo().put(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_ID, requestId);
            distributionPackage.getInfo().put(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_START_TIME, startTime);

            // put the packages in the queue
            Iterable<DistributionQueueItemStatus> states = scheduleQueueStrategy.add(distributionPackage, queueProvider);
            for (DistributionQueueItemStatus state : states) {
                DistributionRequestState requestState = getRequestStateFromQueueState(state.getItemState());
                distributionResponses.add(new SimpleDistributionResponse(requestState, state.getItemState().toString()));
            }

            generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_QUEUED, distributionPackage);
        } catch (DistributionException e) {
            log.error("an error happened during dispatching items to the queue(s)", e);
            distributionResponses.add(new SimpleDistributionResponse(DistributionRequestState.DROPPED, e.toString()));
        }

        return distributionResponses;
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
            if (DistributionQueueState.BLOCKED.equals(state)) {
                return DistributionAgentState.BLOCKED;
            }

            if (DistributionQueueState.RUNNING.equals(state)) {
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
                queueProvider.enableQueueProcessing(new PackageQueueProcessor(), processingQueues.toArray(new String[processingQueues.size()]));
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

    private boolean processQueueItem(String queueName, DistributionQueueEntry queueEntry) throws DistributionException {
        boolean removeItemFromQueue = false;
        ResourceResolver agentResourceResolver = null;
        DistributionPackage distributionPackage = null;
        DistributionQueueItem queueItem = queueEntry.getItem();
        DistributionQueueItemStatus queueItemStatus = queueEntry.getStatus();
        try {

            String callingUser = queueItem.get(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_USER, String.class);
            String requestId = queueItem.get(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_ID, String.class);
            long globalStartTime = queueItem.get(DistributionPackageUtils.PACKAGE_INFO_PROPERTY_REQUEST_START_TIME, Long.class);

            agentResourceResolver = getAgentResourceResolver(callingUser);

            final long startTime = System.currentTimeMillis();

            distributionPackage = distributionPackageExporter.getPackage(agentResourceResolver, queueItem.getPackageId());

            if (distributionPackage != null) {
                final long packageSize = distributionPackage.getSize();
                DistributionPackageUtils.mergeQueueEntry(distributionPackage.getInfo(), queueEntry);

                final DistributionRequestType requestType = distributionPackage.getInfo().getRequestType();
                final String[] paths = distributionPackage.getInfo().getPaths();

                try {
                    distributionPackageImporter.importPackage(agentResourceResolver, distributionPackage);
                    generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED, distributionPackage);
                    removeItemFromQueue = true;
                    final long endTime = System.currentTimeMillis();

                    log.info("[{}] PACKAGE-DELIVERED {}: {} paths={}, importTime={}ms, execTime={}ms, size={}B", new Object[] {
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
            ungetAgentResourceResolver(agentResourceResolver);
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

    private ResourceResolver getAgentResourceResolver(String user) throws DistributionException {
        ResourceResolver resourceResolver;

        try {
            Map<String, Object> authenticationInfo = new HashMap<String, Object>();

            if (impersonateUser && user != null) {
                Session session = slingRepository.impersonateFromService(DEFAULT_AGENT_SERVICE, new SimpleCredentials(user, new char[0]), null);
                authenticationInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
                resourceResolver = resourceResolverFactory.getResourceResolver(authenticationInfo);
            } else {
                authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, subServiceName);
                resourceResolver = resourceResolverFactory.getServiceResourceResolver(authenticationInfo);
            }

            return resourceResolver;
        } catch (LoginException le) {
            throw new DistributionException(le);
        } catch (RepositoryException re) {
            throw new DistributionException(re);
        }
    }

    private void ungetAgentResourceResolver(ResourceResolver resourceResolver) {

        if (resourceResolver != null) {
            try {
                if (resourceResolver.hasChanges()) {
                    resourceResolver.commit();
                }
            } catch (PersistenceException e) {
                log.error("cannot commit changes to resource resolver", e);
            } finally {
                DistributionUtils.safelyLogout(resourceResolver);
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

    /**
     * processor for items in the queue
     */
    class PackageQueueProcessor implements DistributionQueueProcessor {
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
    }

    /**
     * The package exporter callback function to process the exported packages.
     * The exported packages are scheduled for import.
     */
    class PackageExporterProcessor implements DistributionPackageProcessor {

        private final String callingUser;
        private final String requestId;
        private final long requestStartTime;
        private final AtomicInteger packagesCount = new AtomicInteger();
        private final AtomicLong packagesSize = new AtomicLong();
        private final List<DistributionResponse> allResponses = new ArrayList<DistributionResponse>();

        public List<DistributionResponse> getAllResponses() {
            return allResponses;
        }

        public int getPackagesCount() {
            return packagesCount.get();
        }

        public long getPackagesSize() {
            return packagesSize.get();
        }

        PackageExporterProcessor(String callingUser, String requestId, long requestStartTime) {
            this.callingUser = callingUser;
            this.requestId = requestId;
            this.requestStartTime = requestStartTime;
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
                    agentResourceResolver = getAgentResourceResolver(null);

                    agent.execute(agentResourceResolver, request);
                } catch (Throwable e) {
                    log.error("Error executing handler {}", request, e);
                } finally {
                    ungetAgentResourceResolver(agentResourceResolver);
                }
            }

        }
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
