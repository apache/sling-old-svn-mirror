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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentException;
import org.apache.sling.distribution.agent.DistributionRequestAuthorizationStrategy;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.communication.DistributionResponse;
import org.apache.sling.distribution.component.ManagedDistributionComponent;
import org.apache.sling.distribution.event.DistributionEventType;
import org.apache.sling.distribution.event.impl.DistributionEventFactory;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExportException;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.SharedDistributionPackage;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueDispatchingStrategy;
import org.apache.sling.distribution.queue.DistributionQueueException;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemState;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.trigger.DistributionTrigger;
import org.apache.sling.distribution.trigger.DistributionTriggerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of a {@link org.apache.sling.distribution.agent.DistributionAgent}
 */
public class SimpleDistributionAgent implements DistributionAgent, ManagedDistributionComponent {

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
                || distributionPackageImporter == null
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

            return scheduleImport(exportPackages(agentResourceResolver, distributionRequest));
        } catch (Exception e) {
            log.error("Error executing distribution request {}", distributionRequest, e);
            throw new DistributionAgentException(e);
        } finally {
            ungetAgentResourceResolver(agentResourceResolver);

        }

    }

    boolean isPassive() {
        return passive;
    }

    private List<DistributionPackage> exportPackages(ResourceResolver agentResourceResolver, DistributionRequest distributionRequest) throws DistributionPackageExportException {
        return distributionPackageExporter.exportPackages(agentResourceResolver, distributionRequest);
    }

    private DistributionResponse scheduleImport(List<DistributionPackage> distributionPackages) {
        List<DistributionResponse> distributionResponses = new LinkedList<DistributionResponse>();

        for (DistributionPackage distributionPackage : distributionPackages) {
            distributionResponses.add(schedule(distributionPackage));
        }
        return distributionResponses.size() == 1 ? distributionResponses.get(0) : new CompositeDistributionResponse(distributionResponses);
    }

    private DistributionResponse schedule(DistributionPackage distributionPackage) {
        DistributionResponse distributionResponse;
        log.info("scheduling distribution of package {}", distributionPackage);



        // dispatch the distribution package to the queue distribution handler
        try {
            boolean success = queueDistributionStrategy.add(distributionPackage, queueProvider);

            Dictionary<Object, Object> properties = new Properties();
            properties.put("distribution.package.paths", distributionPackage.getPaths());
            properties.put("distribution.agent.name", name);
            distributionEventFactory.generateEvent(DistributionEventType.PACKAGE_QUEUED, properties);

            distributionResponse = new DistributionResponse(success? DistributionQueueItemState.ItemState.QUEUED.toString() :
                    DistributionQueueItemState.ItemState.ERROR.toString(), success);
        } catch (Exception e) {
            log.error("an error happened during queue processing", e);
            distributionResponse = new DistributionResponse(e.toString(), false);
        }

        return distributionResponse;
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
                log.error("could not register handler {} from trigger {}", agentBasedRequestHandler, trigger);
            }
        }

        if (!isPassive()) {
            try {
                queueProvider.enableQueueProcessing(new PackageQueueProcessor());
            } catch (DistributionQueueException e) {
                log.error("cannot enable queue processing", e);
            }
        }
    }

    public void disable() {
        log.info("disabling agent");

        for (DistributionTrigger trigger : triggers) {
            try {
                trigger.unregister(agentBasedRequestHandler);
            } catch (DistributionTriggerException e) {
                log.error("could not unregister handler {} from trigger {}", agentBasedRequestHandler, trigger);
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

                distributionPackageImporter.importPackage(agentResourceResolver, distributionPackage);

                Dictionary<Object, Object> properties = new Properties();
                properties.put("distribution.package.paths", distributionPackage.getPaths());
                properties.put("distribution.agent.name", name);
                distributionEventFactory.generateEvent(DistributionEventType.PACKAGE_DISTRIBUTED, properties);

                if (distributionPackage instanceof SharedDistributionPackage) {
                    ((SharedDistributionPackage) distributionPackage).release(queueName);
                }
                else {
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
            log.error("cannot obtain resource resolver", e);
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
            resourceResolver.close();
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
            ResourceResolver agentResourceResolver = null;
            try {
                agentResourceResolver = getAgentResourceResolver();
                agent.execute(agentResourceResolver, request);
            } catch (DistributionAgentException e) {
                log.error("Error executing handler", e);
            } catch (LoginException e) {
                log.error("Cannot obtain resource resolver");
            } finally {
                ungetAgentResourceResolver(agentResourceResolver);
            }
        }
    }

    private class CompositeDistributionResponse extends DistributionResponse {

        private boolean successful;

        private String status;

        public CompositeDistributionResponse(List<DistributionResponse> distributionResponses) {
            super("", false);
            if (distributionResponses.isEmpty()) {
                successful = false;
                status = "empty response";
            } else {
                successful = true;
                StringBuilder statusBuilder = new StringBuilder("[");
                for (DistributionResponse response : distributionResponses) {
                    successful &= response.isSuccessful();
                    statusBuilder.append(response.getStatus()).append(", ");
                }
                int lof = statusBuilder.lastIndexOf(", ");
                statusBuilder.replace(lof, lof + 2, "]");
                status = statusBuilder.toString();
            }
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }

        @Override
        public String getStatus() {
            return status;
        }
    }
}
