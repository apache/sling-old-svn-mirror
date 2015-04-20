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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentException;

import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.agent.DistributionAgentState;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.event.DistributionEventTopics;
import org.apache.sling.distribution.impl.CompositeDistributionResponse;
import org.apache.sling.distribution.impl.SimpleDistributionResponse;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.log.DistributionLog;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExportException;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueState;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;

import static org.apache.sling.distribution.queue.DistributionQueueItemStatus.ItemState;

/**
 * Basic implementation of a {@link org.apache.sling.distribution.agent.DistributionAgent}
 */
public class SimpleDistributionAgent implements DistributionAgent {

    private final DistributionQueueProvider queueProvider;

    private final boolean queueProcessingEnabled;
    private final DistributionPackageImporter distributionPackageImporter;
    private final DistributionPackageExporter distributionPackageExporter;

    private final DistributionQueueDispatchingStrategy queueDistributionStrategy;

    private final DistributionEventFactory distributionEventFactory;

    private final String name;

    private final DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy;
    private final ResourceResolverFactory resourceResolverFactory;
    private final String subServiceName;
    private AgentBasedRequestHandler agentBasedRequestHandler;
    private boolean active = false;
    private final DefaultDistributionLog log;
    private final DistributionRequestType[] allowedRequests;
    private final String[] allowedRoots;
    private final String[] passiveQueues;

    public SimpleDistributionAgent(String name,
                                   boolean queueProcessingEnabled,
                                   String[] passiveQueues,
                                   String subServiceName,
                                   DistributionPackageImporter distributionPackageImporter,
                                   DistributionPackageExporter distributionPackageExporter,
                                   DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy,
                                   DistributionQueueProvider queueProvider,
                                   DistributionQueueDispatchingStrategy queueDistributionStrategy,
                                   DistributionEventFactory distributionEventFactory,
                                   ResourceResolverFactory resourceResolverFactory,
                                   DefaultDistributionLog log,
                                   DistributionRequestType[] allowedRequests,
                                   String[] allowedRoots) {
        this.log = log;
        this.allowedRequests = allowedRequests;
        this.allowedRoots = allowedRoots;
        this.passiveQueues = passiveQueues;

        // check configuration is valid
        if (name == null
                || (queueProcessingEnabled && distributionPackageImporter == null)
                || distributionPackageExporter == null
                || subServiceName == null
                || distributionRequestAuthorizationStrategy == null
                || queueProvider == null
                || queueDistributionStrategy == null
                || distributionEventFactory == null
                || resourceResolverFactory == null) {

            String errorMessage = Arrays.toString(new Object[]{name,
                    distributionPackageImporter,
                    distributionPackageExporter,
                    subServiceName,
                    distributionRequestAuthorizationStrategy,
                    queueProvider,
                    queueDistributionStrategy,
                    distributionEventFactory,
                    resourceResolverFactory});
            throw new IllegalArgumentException("all arguments are required: " + errorMessage);
        }

        this.subServiceName = subServiceName;
        this.distributionRequestAuthorizationStrategy = distributionRequestAuthorizationStrategy;
        this.resourceResolverFactory = resourceResolverFactory;
        this.name = name;
        this.queueProcessingEnabled = queueProcessingEnabled;
        this.distributionPackageImporter = distributionPackageImporter;
        this.distributionPackageExporter = distributionPackageExporter;
        this.queueProvider = queueProvider;
        this.queueDistributionStrategy = queueDistributionStrategy;
        this.distributionEventFactory = distributionEventFactory;
    }

    @Nonnull
    public DistributionResponse execute(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest)
            throws DistributionAgentException {

        ResourceResolver agentResourceResolver = null;

        try {

            if (!isAcceptedRequestType(distributionRequest)) {
                log.debug("request type not accepted {}", distributionRequest.getRequestType());
                return new SimpleDistributionResponse(DistributionRequestState.DROPPED, "Request type not accepted");
            }

            if (!isAcceptedRequestRoot(distributionRequest)) {
                log.debug("request paths not accepted {}", Arrays.toString(distributionRequest.getPaths()));
                return new SimpleDistributionResponse(DistributionRequestState.DROPPED, "Request paths not accepted");
            }

            boolean silent = DistributionRequestType.PULL.equals(distributionRequest.getRequestType());

            log.info(silent, "starting request {}", distributionRequest);

            agentResourceResolver = getAgentResourceResolver();

            distributionRequestAuthorizationStrategy.checkPermission(resourceResolver, distributionRequest);

            List<DistributionPackage> distributionPackages = exportPackages(agentResourceResolver, distributionRequest);

            log.debug("exported packages {}", distributionPackages.size());

            DistributionResponse distributionResponse = scheduleImportPackages(distributionPackages);

            log.info(silent, "returning response {}", distributionResponse);

            return distributionResponse;
        } catch (DistributionRequestAuthorizationException e) {
            return new SimpleDistributionResponse(DistributionRequestState.DROPPED, "Request is not authorized");
        } catch (LoginException e) {
            log.error("Error executing distribution request {} {}", distributionRequest, e);
            throw new DistributionAgentException(e);
        } catch (DistributionPackageExportException e) {
            log.error("Error executing distribution request {} {}", distributionRequest, e);
            throw new DistributionAgentException(e);
        } finally {
            ungetAgentResourceResolver(agentResourceResolver);
        }

    }

    boolean isPassive() {
        return !queueProcessingEnabled;
    }

    private List<DistributionPackage> exportPackages(ResourceResolver agentResourceResolver, DistributionRequest distributionRequest) throws DistributionPackageExportException {
        List<DistributionPackage> distributionPackages = distributionPackageExporter.exportPackages(agentResourceResolver, distributionRequest);

        generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_CREATED);

        return distributionPackages;
    }

    private DistributionResponse scheduleImportPackages(List<DistributionPackage> distributionPackages) {
        List<DistributionResponse> distributionResponses = new LinkedList<DistributionResponse>();

        for (DistributionPackage distributionPackage : distributionPackages) {
            Collection<SimpleDistributionResponse> distributionResponsesForPackage = scheduleImportPackage(distributionPackage);
            distributionResponses.addAll(distributionResponsesForPackage);
        }
        return distributionResponses.size() == 1 ? distributionResponses.get(0) : new CompositeDistributionResponse(distributionResponses);
    }

    private Collection<SimpleDistributionResponse> scheduleImportPackage(DistributionPackage distributionPackage) {
        Collection<SimpleDistributionResponse> distributionResponses = new LinkedList<SimpleDistributionResponse>();
        log.debug("scheduling distribution of package {} {}", distributionPackage.getId(), distributionPackage);

        // dispatch the distribution package to the queue distribution handler
        try {
            Iterable<DistributionQueueItemStatus> states = queueDistributionStrategy.add(distributionPackage, queueProvider);
            for (DistributionQueueItemStatus state : states) {
                DistributionRequestState requestState = getRequestStateFromQueueState(state.getItemState());
                distributionResponses.add(new SimpleDistributionResponse(requestState, state.getItemState().toString()));
            }

            generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_QUEUED, distributionPackage);
        } catch (Exception e) {
            log.error("an error happened during dispatching items to the queue(s)", e);
            distributionResponses.add(new SimpleDistributionResponse(DistributionRequestState.DROPPED, e.toString()));
        } finally {
            if (distributionPackage != null) {
                distributionPackage.close();
            }
        }

        return distributionResponses;
    }


    @Nonnull
    public Iterable<String> getQueueNames() {
        return queueDistributionStrategy.getQueueNames();
    }

    public DistributionQueue getQueue(@Nonnull String queueName) throws DistributionAgentException {
        DistributionQueue queue;
        try {
            if (queueName.length() > 0) {
                queue = queueProvider.getQueue(queueName);
            } else {
                queue = queueProvider.getQueue(DistributionQueueDispatchingStrategy.DEFAULT_QUEUE_NAME);
            }
        } catch (DistributionQueueException e) {
            throw new DistributionAgentException(e);
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
            DistributionQueue queue = null;
            try {
                 queue = getQueue(queueName);
            } catch (DistributionAgentException e) {

            }

            if (queue != null) {
                DistributionQueueState state = queue.getState();
                if (DistributionQueueState.BLOCKED.equals(state)) {
                    return DistributionAgentState.BLOCKED;
                }

                if (DistributionQueueState.RUNNING.equals(state)) {
                    agentState = DistributionAgentState.RUNNING;
                }
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

                Set<String> allQueues = new HashSet<String>(queueDistributionStrategy.getQueueNames());

                if (passiveQueues != null) {
                    Set<String> passiveQueues = new HashSet<String>(Arrays.asList(this.passiveQueues));
                    allQueues.removeAll(passiveQueues);
                }

                queueProvider.enableQueueProcessing(new PackageQueueProcessor(), allQueues.toArray(new String[0]));
            } catch (DistributionQueueException e) {
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
            trigger.register(agentBasedRequestHandler);
        } catch (DistributionTriggerException e) {
            log.error("could not register handler from trigger {} {}", trigger, e);
        }

    }

    public void disableTrigger(DistributionTrigger trigger) {
        if (!active || agentBasedRequestHandler == null) {
            return;
        }

        try {
            trigger.unregister(agentBasedRequestHandler);
        } catch (DistributionTriggerException e) {
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
            } catch (DistributionQueueException e) {
                log.error("cannot disable queue processing", e);
            }
        }

    }

    private boolean processQueueItem(String queueName, DistributionQueueItem queueItem) {
        boolean success = false;
        ResourceResolver agentResourceResolver = null;
        DistributionPackage distributionPackage = null;
        try {

            agentResourceResolver = getAgentResourceResolver();

            distributionPackage = distributionPackageExporter.getPackage(agentResourceResolver, queueItem.getId());

            if (distributionPackage != null) {
                distributionPackage.getInfo().fillInfo(queueItem.getPackageInfo());
                distributionPackage.getInfo().setQueue(queueName);

                distributionPackageImporter.importPackage(agentResourceResolver, distributionPackage);

                DistributionPackageUtils.releaseOrDelete(distributionPackage, queueName);

                generatePackageEvent(DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED, distributionPackage);
                success = true;
                log.info("distribution package {} was delivered", queueItem.getId());
            } else {
                success = true; // return success if package does not exist in order to clear the queue.
                log.error("distribution package with id {} does not exist. the package will be skipped.", queueItem.getId());
            }

        } catch (DistributionPackageImportException e) {
            log.error("could not deliver package {}", queueItem.getId(), e);
        } catch (LoginException e) {
            log.info("cannot obtain resource resolver", e);
        } finally {
            ungetAgentResourceResolver(agentResourceResolver);
            if (distributionPackage != null) {
                distributionPackage.close();
            }
        }
        return success;
    }

    private ResourceResolver getAgentResourceResolver() throws LoginException {
        ResourceResolver resourceResolver;

        Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, subServiceName);
        resourceResolver = resourceResolverFactory.getServiceResourceResolver(authenticationInfo);

        return resourceResolver;
    }

    private void ungetAgentResourceResolver(ResourceResolver resourceResolver) {

        if (resourceResolver != null) {
            try {
                resourceResolver.commit();
            } catch (PersistenceException e) {
                log.error("cannot commit changes to resource resolver", e);
            }
            finally {
                resourceResolver.close();
            }
        }

    }

    private void generatePackageEvent(String topic, DistributionPackage... distributionPackages) {
        for (DistributionPackage distributionPackage : distributionPackages) {
            distributionEventFactory.generatePackageEvent(topic, DistributionComponentKind.AGENT, name, distributionPackage.getInfo());
        }
    }

    boolean isAcceptedRequestType(DistributionRequest request) {
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

    boolean isAcceptedRequestRoot(DistributionRequest request) {
        if (allowedRoots == null || allowedRoots.length == 0) {
            return true;
        }

        if (!DistributionRequestType.ADD.equals(request.getRequestType()) && !DistributionRequestType.DELETE.equals(request.getRequestType())) {
            return true;
        }

        for (String path : request.getPaths()) {
            boolean allowed = false;
            for (String allowedRoot: allowedRoots) {
                if(allowedRoot != null && allowedRoot.trim().length() != 0
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

    class PackageQueueProcessor implements DistributionQueueProcessor {
        public boolean process(@Nonnull String queueName, @Nonnull DistributionQueueItem queueItem) {
            try {
                log.debug("queue {} processing item {}", queueName, queueItem);

                boolean success = processQueueItem(queueName, queueItem);

                log.debug("queue {} processing item {} ended with status {}", queueName, queueItem, success);

                return success;

            } catch (Throwable e) {
                log.error("queue {} error while processing item {}", queueName, queueItem);
                return false;
            }
        }
    }

    public class AgentBasedRequestHandler implements DistributionRequestHandler {
        private final DistributionAgent agent;

        public AgentBasedRequestHandler(DistributionAgent agent) {
            this.agent = agent;
        }

        public void handle(@Nonnull DistributionRequest request) {

            if (!active) {
                log.warn("skipping agent handler as agent is disabled");
                return;
            }

            ResourceResolver agentResourceResolver = null;
            try {
                agentResourceResolver = getAgentResourceResolver();
                agent.execute(agentResourceResolver, request);
            } catch (DistributionAgentException e) {
                log.error("Error executing handler", e);
            } catch (LoginException e) {
                log.info("Cannot obtain resource resolver", e);
            } finally {
                ungetAgentResourceResolver(agentResourceResolver);
            }
        }
    }



    /* Convert the state of a certain item in the queue into a request state */
    private DistributionRequestState getRequestStateFromQueueState(ItemState itemState) {
        DistributionRequestState requestState;
        switch (itemState) {
            case QUEUED:
                requestState = DistributionRequestState.ACCEPTED;
                break;
            case ACTIVE:
                requestState = DistributionRequestState.ACCEPTED;
                break;
            case SUCCEEDED:
                requestState = DistributionRequestState.DISTRIBUTED;
                break;
            case STOPPED:
                requestState = DistributionRequestState.DROPPED;
                break;
            case GIVEN_UP:
                requestState = DistributionRequestState.DROPPED;
                break;
            case ERROR:
                requestState = DistributionRequestState.DROPPED;
                break;
            case DROPPED:
                requestState = DistributionRequestState.DROPPED;
                break;
            default:
                requestState = DistributionRequestState.DROPPED;
                break;
        }
        return requestState;
    }

}
