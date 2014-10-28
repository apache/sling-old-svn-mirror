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
package org.apache.sling.replication.agent.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentException;
import org.apache.sling.replication.agent.ReplicationRequestAuthorizationStrategy;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.component.ManagedReplicationComponent;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImportException;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.apache.sling.replication.trigger.ReplicationRequestHandler;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of a {@link ReplicationAgent}
 */
public class SimpleReplicationAgent implements ReplicationAgent, ManagedReplicationComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationQueueProvider queueProvider;

    private final boolean passive;
    private final ReplicationPackageImporter replicationPackageImporter;
    private final ReplicationPackageExporter replicationPackageExporter;

    private final ReplicationQueueDistributionStrategy queueDistributionStrategy;

    private final ReplicationEventFactory replicationEventFactory;

    private final List<ReplicationTrigger> triggers;

    private final String name;

    private final ReplicationRequestAuthorizationStrategy replicationRequestAuthorizationStrategy;
    private final ResourceResolverFactory resourceResolverFactory;
    private final String subServiceName;
    private AgentBasedRequestHandler agentBasedRequestHandler;

    public SimpleReplicationAgent(String name,
                                  boolean passive,
                                  String subServiceName,
                                  ReplicationPackageImporter replicationPackageImporter,
                                  ReplicationPackageExporter replicationPackageExporter,
                                  ReplicationRequestAuthorizationStrategy replicationRequestAuthorizationStrategy,
                                  ReplicationQueueProvider queueProvider,
                                  ReplicationQueueDistributionStrategy queueDistributionStrategy,
                                  ReplicationEventFactory replicationEventFactory,
                                  ResourceResolverFactory resourceResolverFactory,
                                  List<ReplicationTrigger> triggers) {


        // check configuration is valid
        if (name == null
                || replicationPackageImporter == null
                || replicationPackageExporter == null
                || subServiceName == null
                || replicationRequestAuthorizationStrategy == null
                || queueProvider == null
                || queueDistributionStrategy == null
                || replicationEventFactory == null
                || resourceResolverFactory == null) {

            String errorMessage = Arrays.toString(new Object[]{name,
                    replicationPackageImporter,
                    replicationPackageExporter,
                    subServiceName,
                    replicationRequestAuthorizationStrategy,
                    queueProvider,
                    queueDistributionStrategy,
                    replicationEventFactory,
                    resourceResolverFactory});
            throw new IllegalArgumentException("all arguments are required: " + errorMessage);
        }

        this.subServiceName = subServiceName;
        this.replicationRequestAuthorizationStrategy = replicationRequestAuthorizationStrategy;
        this.resourceResolverFactory = resourceResolverFactory;
        this.name = name;
        this.passive = passive;
        this.replicationPackageImporter = replicationPackageImporter;
        this.replicationPackageExporter = replicationPackageExporter;
        this.queueProvider = queueProvider;
        this.queueDistributionStrategy = queueDistributionStrategy;
        this.replicationEventFactory = replicationEventFactory;
        this.triggers = triggers == null ? new ArrayList<ReplicationTrigger>() : triggers;
    }

    public ReplicationResponse execute(ResourceResolver resourceResolver, ReplicationRequest replicationRequest)
            throws ReplicationAgentException {



        ResourceResolver agentResourceResolver = null;

        try {
            agentResourceResolver = getAgentResourceResolver();

            replicationRequestAuthorizationStrategy.checkPermission(resourceResolver, replicationRequest);


            List<ReplicationPackage> replicationPackages = buildPackages(agentResourceResolver, replicationRequest);

            return schedule(agentResourceResolver, replicationPackages);
        } catch (Exception e) {
            log.error("Error executing replication request {}", replicationRequest, e);
            throw new ReplicationAgentException(e);
        }
        finally {
            ungetAgentResourceResolver(agentResourceResolver);

        }

    }

    public boolean isPassive() {
        return passive;
    }


    private List<ReplicationPackage> buildPackages(ResourceResolver agentResourceResolver, ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {
        List<ReplicationPackage> replicationPackages = replicationPackageExporter.exportPackages(agentResourceResolver, replicationRequest);

        return replicationPackages;
    }

    private ReplicationResponse schedule(ResourceResolver agentResourceResolver, List<ReplicationPackage> replicationPackages) {
        // TODO : create a composite replication response otherwise only the last response will be returned
        ReplicationResponse replicationResponse = new ReplicationResponse();

        for (ReplicationPackage replicationPackage : replicationPackages) {
            ReplicationResponse currentReplicationResponse = schedule(replicationPackage);

            replicationResponse.setSuccessful(currentReplicationResponse.isSuccessful());
            replicationResponse.setStatus(currentReplicationResponse.getStatus());
        }

        return replicationResponse;
    }

    private ReplicationResponse schedule(ReplicationPackage replicationPackage) {
        ReplicationResponse replicationResponse = new ReplicationResponse();
        log.info("scheduling replication of package {}", replicationPackage);

        ReplicationQueueItem replicationQueueItem = new ReplicationQueueItem(replicationPackage.getId(),
                replicationPackage.getPaths(),
                replicationPackage.getAction(),
                replicationPackage.getType(),
                replicationPackage.getInfo());

        // dispatch the replication package to the queue distribution handler
        try {
            ReplicationQueueItemState state = queueDistributionStrategy.add(name, replicationQueueItem,
                    queueProvider);

            Dictionary<Object, Object> properties = new Properties();
            properties.put("replication.package.paths", replicationQueueItem.getPaths());
            properties.put("replication.agent.name", name);
            replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_QUEUED, properties);

            if (state != null) {
                replicationResponse.setStatus(state.getItemState().toString());
                replicationResponse.setSuccessful(state.isSuccessful());
            } else {
                replicationResponse.setStatus(ReplicationQueueItemState.ItemState.ERROR.toString());
                replicationResponse.setSuccessful(false);
            }
        } catch (Exception e) {
            log.error("an error happened during queue processing", e);

            replicationResponse.setSuccessful(false);
        }

        return replicationResponse;
    }

    public ReplicationQueue getQueue(String queueName) throws ReplicationAgentException {
        ReplicationQueue queue;
        try {
            if (queueName != null && queueName.length() > 0) {
                queue = queueProvider.getQueue(this.name, queueName);
            } else {
                queue = queueProvider.getDefaultQueue(this.name);
            }
        } catch (ReplicationQueueException e) {
            throw new ReplicationAgentException(e);
        }
        return queue;
    }


    public void enable() {
        log.info("enabling agent");

        // register triggers if any
        agentBasedRequestHandler = new AgentBasedRequestHandler(this);

        for (ReplicationTrigger trigger : triggers) {
            trigger.register(agentBasedRequestHandler);
        }

        if (!isPassive()) {
            queueProvider.enableQueueProcessing(name, new PackageQueueProcessor());
        }
    }

    public void disable() {
        log.info("disabling agent");

        for (ReplicationTrigger trigger : triggers) {
            trigger.unregister(agentBasedRequestHandler);
        }

        agentBasedRequestHandler = null;

        if (!isPassive()) {
            queueProvider.disableQueueProcessing(name);
        }
    }

    private boolean processQueue(ReplicationQueueItem queueItem) {
        boolean success = false;
        log.debug("reading package with id {}", queueItem.getId());
        ResourceResolver agentResourceResolver = null;
        try {

            agentResourceResolver = getAgentResourceResolver();

            ReplicationPackage replicationPackage = replicationPackageExporter.getPackage(agentResourceResolver, queueItem.getId());


            if (replicationPackage != null) {
                replicationPackage.getInfo().fillInfo(queueItem.getPackageInfo());

                replicationPackageImporter.importPackage(agentResourceResolver, replicationPackage);

                Dictionary<Object, Object> properties = new Properties();
                properties.put("replication.package.paths", replicationPackage.getPaths());
                properties.put("replication.agent.name", name);
                replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_REPLICATED, properties);

                replicationPackage.delete();
                success = true;
            } else {
                log.warn("replication package with id {} does not exist", queueItem.getId());
            }

        } catch (ReplicationPackageImportException e) {
            log.error("could not process transport queue", e);
        } catch (LoginException e) {
            log.error("cannot obtain resource resolver", e);
        } finally {
            ungetAgentResourceResolver(agentResourceResolver);
        }
        return success;
    }

    private ResourceResolver getAgentResourceResolver() throws LoginException {
        ResourceResolver resourceResolver = null;

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

    class PackageQueueProcessor implements ReplicationQueueProcessor {
        public boolean process(String queueName, ReplicationQueueItem packageInfo) {
            log.info("running package queue processor for queue {}", queueName);
            return processQueue(packageInfo);
        }
    }

    public class AgentBasedRequestHandler implements ReplicationRequestHandler {
        private final ReplicationAgent agent;

        public AgentBasedRequestHandler(ReplicationAgent agent) {
            this.agent = agent;
        }

        public void handle(ReplicationRequest request) {
            ResourceResolver agentResourceResolver = null;
            try {
                agentResourceResolver = getAgentResourceResolver();
                agent.execute(agentResourceResolver, request);
            } catch (ReplicationAgentException e) {
                log.error("Error executing handler", e);
            } catch (LoginException e) {
                log.error("Cannot obtain resource resolver");
            }
            finally {
                ungetAgentResourceResolver(agentResourceResolver);
            }
        }
    }
}
