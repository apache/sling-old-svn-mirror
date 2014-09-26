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
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.communication.ReplicationResponse;
import org.apache.sling.replication.event.ReplicationEventFactory;
import org.apache.sling.replication.event.ReplicationEventType;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.packaging.ReplicationPackageImporter;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.trigger.ReplicationTriggerRequestHandler;
import org.apache.sling.replication.trigger.ReplicationTrigger;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageReadingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of a {@link ReplicationAgent}
 */
public class SimpleReplicationAgent implements ReplicationAgent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ReplicationQueueProvider queueProvider;

    private final boolean passive;
    private final ReplicationPackageImporter replicationPackageImporter;
    private final ReplicationPackageExporter replicationPackageExporter;

    private final ReplicationQueueDistributionStrategy queueDistributionStrategy;

    private final ReplicationEventFactory replicationEventFactory;

    private final List<ReplicationTrigger> triggers;

    private final String name;

    private final boolean useAggregatePaths;

    public SimpleReplicationAgent(String name,
                                  boolean useAggregatePaths,
                                  boolean passive,
                                  ReplicationPackageImporter replicationPackageImporter,
                                  ReplicationPackageExporter replicationPackageExporter,
                                  ReplicationQueueProvider queueProvider,
                                  ReplicationQueueDistributionStrategy queueDistributionHandler,
                                  ReplicationEventFactory replicationEventFactory,
                                  List<ReplicationTrigger> triggers) {
        this.name = name;
        this.passive = passive;
        this.replicationPackageImporter = replicationPackageImporter;
        this.replicationPackageExporter = replicationPackageExporter;
        this.queueProvider = queueProvider;
        this.queueDistributionStrategy = queueDistributionHandler;
        this.useAggregatePaths = useAggregatePaths;
        this.replicationEventFactory = replicationEventFactory;
        this.triggers = triggers == null ? new ArrayList<ReplicationTrigger>() : triggers;
    }

    public ReplicationResponse execute(ReplicationRequest replicationRequest)
            throws AgentReplicationException {
        try {
            return schedule(buildPackages(replicationRequest));
        } catch (ReplicationPackageBuildingException e) {
            log.error("Error building packages", e);
            throw new AgentReplicationException(e);
        }

    }

    public boolean isPassive() {
        return passive;
    }

    private List<ReplicationPackage> buildPackages(ReplicationRequest replicationRequest) throws ReplicationPackageBuildingException {

        List<ReplicationPackage> replicationPackages = new ArrayList<ReplicationPackage>();

        if (useAggregatePaths) {
            List<ReplicationPackage> exportedPackages = replicationPackageExporter.exportPackage(replicationRequest);
            replicationPackages.addAll(exportedPackages);
        } else {
            for (String path : replicationRequest.getPaths()) {
                ReplicationRequest splitReplicationRequest = new ReplicationRequest(replicationRequest.getTime(),
                        replicationRequest.getAction(),
                        path);
                List<ReplicationPackage> exportedPackages = replicationPackageExporter.exportPackage(splitReplicationRequest);
                replicationPackages.addAll(exportedPackages);
            }
        }

        return replicationPackages;
    }

    private ReplicationResponse schedule(List<ReplicationPackage> replicationPackages) {
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
                replicationPackage.getType());

        // send the replication package to the queue distribution handler
        try {
            ReplicationQueueItemState state = queueDistributionStrategy.add(getName(), replicationQueueItem,
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

    public String getName() {
        return name;
    }

    public ReplicationQueue getQueue(String name) throws ReplicationQueueException {
        ReplicationQueue queue;
        if (name != null && name.length() > 0) {
            queue = queueProvider.getQueue(getName(), name);
        } else {
            queue = queueProvider.getDefaultQueue(getName());
        }
        return queue;
    }


    public void enable() {
        log.info("enabling agent");
        // register triggers if any

        for (int i=0; i < triggers.size(); i++) {
            ReplicationTrigger trigger = triggers.get(i);
            String handlerId = getName() + "-" + i;
            trigger.register(handlerId, new AgentBasedTriggerRequestHandler(this));
        }

        if (!isPassive()) {
            queueProvider.enableQueueProcessing(getName(), new PackageQueueProcessor());
        }
    }

    public void disable() {
        log.info("disabling agent");
        for (int i=0; i < triggers.size(); i++) {
            ReplicationTrigger trigger = triggers.get(i);
            String handlerId = getName() + "-" + i;
            trigger.unregister(handlerId);
        }

        if (!isPassive()) {
            queueProvider.disableQueueProcessing(getName());
        }
    }

    private boolean processTransportQueue(ReplicationQueueItem queueItem) {
        boolean success = false;
        log.debug("reading package with id {}", queueItem.getId());
        try {
            ReplicationPackage replicationPackage = replicationPackageExporter.exportPackageById(queueItem.getId());
            if (replicationPackage != null) {
                replicationPackageImporter.importPackage(replicationPackage);

                Dictionary<Object, Object> properties = new Properties();
                properties.put("replication.package.paths", replicationPackage.getPaths());
                properties.put("replication.agent.name", name);
                replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_REPLICATED, properties);

                replicationPackage.delete();
                success = true;
            }
        } catch (ReplicationPackageReadingException e) {
            log.error("could not process transport queue", e);
        }
        return success;
    }

    class PackageQueueProcessor implements ReplicationQueueProcessor {
        public boolean process(String queueName, ReplicationQueueItem packageInfo) {
            log.info("running package queue processor");
            return processTransportQueue(packageInfo);
        }
    }

    public class AgentBasedTriggerRequestHandler implements ReplicationTriggerRequestHandler {
        private final ReplicationAgent agent;

        public AgentBasedTriggerRequestHandler(ReplicationAgent agent) {
            this.agent = agent;
        }

        public void handle(ReplicationRequest request) {
            try {
                agent.execute(request);
            }
            catch (AgentReplicationException e) {
                log.error("Error executing handler", e);
            }
        }
    }
}
