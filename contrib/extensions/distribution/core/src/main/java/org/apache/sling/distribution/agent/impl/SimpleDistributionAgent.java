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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentException;

import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.impl.SimpleDistributionResponse;
import org.apache.sling.distribution.event.DistributionEventType;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExportException;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.impl.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.distribution.queue.DistributionQueueItemStatus.ItemState;

/**
 * Basic implementation of a {@link org.apache.sling.distribution.agent.DistributionAgent}
 */
public class SimpleDistributionAgent implements DistributionAgent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionQueueProvider queueProvider;

    private final boolean passive;
    private final DistributionPackageImporter distributionPackageImporter;
    private final DistributionPackageExporter distributionPackageExporter;

    private final DistributionQueueDispatchingStrategy queueDistributionStrategy;

    private final DistributionEventFactory distributionEventFactory;

    private final List<DistributionTrigger> triggers;

    private final String name;

    private final DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy;
    private final ResourceResolverFactory resourceResolverFactory;
    private final String subServiceName;
    private AgentBasedRequestHandler agentBasedRequestHandler;
    private boolean active = false;

    public SimpleDistributionAgent(String name,
                                   boolean passive,
                                   String subServiceName,
                                   DistributionPackageImporter distributionPackageImporter,
                                   DistributionPackageExporter distributionPackageExporter,
                                   DistributionRequestAuthorizationStrategy distributionRequestAuthorizationStrategy,
                                   DistributionQueueProvider queueProvider,
                                   DistributionQueueDispatchingStrategy queueDistributionStrategy,
                                   DistributionEventFactory distributionEventFactory,
                                   ResourceResolverFactory resourceResolverFactory,
                                   List<DistributionTrigger> triggers) {

        // check configuration is valid
        if (name == null
                || (!passive && distributionPackageImporter == null)
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
        this.passive = passive;
        this.distributionPackageImporter = distributionPackageImporter;
        this.distributionPackageExporter = distributionPackageExporter;
        this.queueProvider = queueProvider;
        this.queueDistributionStrategy = queueDistributionStrategy;
        this.distributionEventFactory = distributionEventFactory;
        this.triggers = triggers == null ? new ArrayList<DistributionTrigger>() : triggers;
    }

    @Nonnull
    public DistributionResponse execute(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest)
            throws DistributionAgentException {

        ResourceResolver agentResourceResolver = null;

        try {
            agentResourceResolver = getAgentResourceResolver();

            distributionRequestAuthorizationStrategy.checkPermission(resourceResolver, distributionRequest);

            List<DistributionPackage> distributionPackages = exportPackages(agentResourceResolver, distributionRequest);

            return scheduleImport(distributionPackages);
        } catch (Exception e) {
            log.error("Error executing distribution request {} in agent " + name, distributionRequest, e);
            throw new DistributionAgentException(e);
        } finally {
            ungetAgentResourceResolver(agentResourceResolver);
        }

    }

    boolean isPassive() {
        return passive;
    }

    private List<DistributionPackage> exportPackages(ResourceResolver agentResourceResolver, DistributionRequest distributionRequest) throws DistributionPackageExportException {
        List<DistributionPackage> distributionPackages = distributionPackageExporter.exportPackages(agentResourceResolver, distributionRequest);
        for (DistributionPackage distributionPackage : distributionPackages) {
            distributionEventFactory.generateAgentPackageEvent(DistributionEventType.AGENT_PACKAGE_CREATED, name, distributionPackage.getInfo());
        }

        return distributionPackages;
    }

    private DistributionResponse scheduleImport(List<DistributionPackage> distributionPackages) {
        List<DistributionResponse> distributionResponses = new LinkedList<DistributionResponse>();

        for (DistributionPackage distributionPackage : distributionPackages) {
            distributionResponses.addAll(schedule(distributionPackage));
        }
        return distributionResponses.size() == 1 ? distributionResponses.get(0) : new CompositeDistributionResponse(distributionResponses);
    }

    private Collection<DistributionResponse> schedule(DistributionPackage distributionPackage) {
        Collection<DistributionResponse> distributionResponses = new LinkedList<DistributionResponse>();
        log.info("scheduling distribution of package {}", distributionPackage);

        // dispatch the distribution package to the queue distribution handler
        try {
            Iterable<DistributionQueueItemStatus> states = queueDistributionStrategy.add(distributionPackage, queueProvider);
            for (DistributionQueueItemStatus state : states) {
                DistributionRequestState requestState = getRequestStateFromQueueState(state.getItemState());
                distributionResponses.add(new SimpleDistributionResponse(requestState, state.getItemState().toString()));
            }

            distributionEventFactory.generateAgentPackageEvent(DistributionEventType.AGENT_PACKAGE_QUEUED, name, distributionPackage.getInfo());
        } catch (Exception e) {
            log.error("an error happened during dispatching items to the queue(s)", e);
            distributionResponses.add(new SimpleDistributionResponse(DistributionRequestState.DROPPED, e.toString()));
        }

        return distributionResponses;
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


    public void enable() {
        log.info("enabling agent");

        // register triggers if any
        agentBasedRequestHandler = new AgentBasedRequestHandler(this);

        for (DistributionTrigger trigger : triggers) {
            try {
                trigger.register(agentBasedRequestHandler);
            } catch (DistributionTriggerException e) {
                log.error("could not register handler from trigger {} {}", trigger, e);
            }
        }

        if (!isPassive()) {
            try {
                queueProvider.enableQueueProcessing(new PackageQueueProcessor());
            } catch (DistributionQueueException e) {
                log.error("cannot enable queue processing", e);
            }
        }

        active = true;
    }

    public void enableTrigger(DistributionTrigger trigger) {
        if (!active) {
            return;
        }

        try {
            trigger.register(agentBasedRequestHandler);
        } catch (DistributionTriggerException e) {
            log.error("could not register handler from trigger {} {}", trigger, e);
        }

    }

    public void disableTrigger(DistributionTrigger trigger) {
        if (!active) {
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


        for (DistributionTrigger trigger : triggers) {
            try {
                trigger.unregister(agentBasedRequestHandler);
            } catch (DistributionTriggerException e) {
                log.error("could not unregister handler trigger {} {}", trigger, e);
            }
        }

        agentBasedRequestHandler = null;


        if (!isPassive()) {

            try {
                queueProvider.disableQueueProcessing();
            } catch (DistributionQueueException e) {
                log.error("cannot disable queue processing", e);
            }
        }

    }

    private boolean processQueue(String queueName, DistributionQueueItem queueItem) {
        boolean success = false;
        log.debug("reading package with id {}", queueItem.getId());
        ResourceResolver agentResourceResolver = null;
        try {

            agentResourceResolver = getAgentResourceResolver();

            DistributionPackage distributionPackage = distributionPackageExporter.getPackage(agentResourceResolver, queueItem.getId());

            if (distributionPackage != null) {
                distributionPackage.getInfo().fillInfo(queueItem.getPackageInfo());
                distributionPackage.getInfo().setQueue(queueName);

                distributionPackageImporter.importPackage(agentResourceResolver, distributionPackage);
                distributionEventFactory.generateAgentPackageEvent(DistributionEventType.AGENT_PACKAGE_DISTRIBUTED, name, distributionPackage.getInfo());

                if (distributionPackage instanceof SharedDistributionPackage) {
                    ((SharedDistributionPackage) distributionPackage).release(queueName);
                } else {
                    distributionPackage.delete();
                }
                success = true;
            } else {
                success = true; // return success if package does not exist in order to clear the queue.
                log.error("distribution package with id {} does not exist", queueItem.getId());
            }

        } catch (DistributionPackageImportException e) {
            log.error("could not process transport queue", e);
        } catch (LoginException e) {
            log.info("cannot obtain resource resolver", e);
        } finally {
            ungetAgentResourceResolver(agentResourceResolver);
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

    class PackageQueueProcessor implements DistributionQueueProcessor {
        public boolean process(@Nonnull String queueName, @Nonnull DistributionQueueItem packageInfo) {
            log.info("running package queue processor for queue {}", queueName);
            return processQueue(queueName, packageInfo);
        }
    }

    public class AgentBasedRequestHandler implements DistributionRequestHandler {
        private final DistributionAgent agent;

        public AgentBasedRequestHandler(DistributionAgent agent) {
            this.agent = agent;
        }

        public void handle(@Nonnull DistributionRequest request) {

            if (!active) {
                log.debug("skipping agent handler as agent {} is disabled", name);
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

    private class CompositeDistributionResponse extends SimpleDistributionResponse {

        private DistributionRequestState state;

        private String message;

        public CompositeDistributionResponse(List<DistributionResponse> distributionResponses) {
            super(DistributionRequestState.DROPPED, null);
            if (distributionResponses.isEmpty()) {
                state = DistributionRequestState.DROPPED;
                message = "empty response";
            } else {
                state = DistributionRequestState.DISTRIBUTED;
                StringBuilder messageBuilder = new StringBuilder("[");
                for (DistributionResponse response : distributionResponses) {
                    state = aggregatedState(state, response.getState());
                    messageBuilder.append(response.getMessage()).append(", ");
                }
                int lof = messageBuilder.lastIndexOf(", ");
                messageBuilder.replace(lof, lof + 2, "]");
                message = messageBuilder.toString();
            }
        }

        @Override
        public boolean isSuccessful() {
            return !DistributionRequestState.DROPPED.equals(state);
        }

        @Nonnull
        @Override
        public DistributionRequestState getState() {
            return state;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    /* Provide the aggregated state of two {@link org.apache.sling.distribution.DistributionRequestState}s */
    private DistributionRequestState aggregatedState(DistributionRequestState first, DistributionRequestState second) {
        DistributionRequestState aggregatedState;
        switch (second) {
            case DISTRIBUTED:
                aggregatedState = first;
                break;
            case DROPPED:
                aggregatedState = DistributionRequestState.DISTRIBUTED;
                break;
            case ACCEPTED:
                if (first.equals(DistributionRequestState.DISTRIBUTED)) {
                    aggregatedState = DistributionRequestState.ACCEPTED;
                } else {
                    aggregatedState = first;
                }
                break;
            default:
                aggregatedState = DistributionRequestState.DROPPED;
        }
        return aggregatedState;
    }
}
