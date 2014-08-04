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
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.queue.ReplicationQueueItemState;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.apache.sling.replication.rule.ReplicationRuleEngine;
import org.apache.sling.replication.serialization.ReplicationPackage;
import org.apache.sling.replication.serialization.ReplicationPackageBuildingException;
import org.apache.sling.replication.serialization.ReplicationPackageExporter;
import org.apache.sling.replication.serialization.ReplicationPackageImporter;
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

    private final String name;

    private final String[] rules;

    private final boolean useAggregatePaths;

    private final ReplicationRuleEngine ruleEngine;

    public SimpleReplicationAgent(String name, String[] rules,
                                  boolean useAggregatePaths,
                                  boolean passive,
                                  ReplicationPackageImporter replicationPackageImporter,
                                  ReplicationPackageExporter replicationPackageExporter,
                                  ReplicationQueueProvider queueProvider,
                                  ReplicationQueueDistributionStrategy queueDistributionHandler,
                                  ReplicationEventFactory replicationEventFactory, ReplicationRuleEngine ruleEngine) {
        this.name = name;
        this.rules = rules;
        this.passive = passive;
        this.replicationPackageImporter = replicationPackageImporter;
        this.replicationPackageExporter = replicationPackageExporter;
        this.queueProvider = queueProvider;
        this.queueDistributionStrategy = queueDistributionHandler;
        this.useAggregatePaths = useAggregatePaths;
        this.replicationEventFactory = replicationEventFactory;
        this.ruleEngine = ruleEngine;
    }

    public ReplicationResponse execute(ReplicationRequest replicationRequest)
            throws AgentReplicationException {

        // create packages from request
        ReplicationPackage[] replicationPackages = buildPackages(replicationRequest);

        return schedule(replicationPackages, false);
    }

    public void send(ReplicationRequest replicationRequest) throws AgentReplicationException {
        // create packages from request
        ReplicationPackage[] replicationPackages = buildPackages(replicationRequest);

        schedule(replicationPackages, true);
    }

    public boolean isPassive() {
        return passive;
    }


    private ReplicationPackage buildPackage(ReplicationRequest replicationRequest) throws AgentReplicationException {
        // create package from request
        ReplicationPackage replicationPackage;
        try {
            replicationPackage = replicationPackageExporter.exportPackage(replicationRequest);
        } catch (ReplicationPackageBuildingException e) {
            throw new AgentReplicationException(e);
        }

        return replicationPackage;
    }

    private ReplicationPackage[] buildPackages(ReplicationRequest replicationRequest) throws AgentReplicationException {

        List<ReplicationPackage> packages = new ArrayList<ReplicationPackage>();

        if (useAggregatePaths) {
            ReplicationPackage replicationPackage = buildPackage(replicationRequest);
            packages.add(replicationPackage);
        } else {
            for (String path : replicationRequest.getPaths()) {
                ReplicationPackage replicationPackage = buildPackage(new ReplicationRequest(replicationRequest.getTime(),
                        replicationRequest.getAction(),
                        path));

                packages.add(replicationPackage);
            }
        }

        return packages.toArray(new ReplicationPackage[packages.size()]);
    }

    // offer option throws an exception at first error
    private ReplicationResponse schedule(ReplicationPackage[] packages, boolean offer) throws AgentReplicationException {
        ReplicationResponse replicationResponse = new ReplicationResponse();

        for (ReplicationPackage replicationPackage : packages) {
            ReplicationResponse currentReplicationResponse = schedule(replicationPackage, offer);

            replicationResponse.setSuccessful(currentReplicationResponse.isSuccessful());
            replicationResponse.setStatus(currentReplicationResponse.getStatus());
        }

        return replicationResponse;
    }

    private ReplicationResponse schedule(ReplicationPackage replicationPackage, boolean offer) throws AgentReplicationException {
        ReplicationResponse replicationResponse = new ReplicationResponse();
        ReplicationQueueItem replicationQueueItem = new ReplicationQueueItem(replicationPackage.getId(),
                replicationPackage.getPaths(),
                replicationPackage.getAction(),
                replicationPackage.getType());

        if (offer) {
            try {
                queueDistributionStrategy.offer(getName(), replicationQueueItem, queueProvider);
                if (isPassive()) {
                    generatePackageQueuedEvent(replicationQueueItem);
                }
            } catch (ReplicationQueueException e) {
                replicationResponse.setSuccessful(false);
                throw new AgentReplicationException(e);
            }
        } else {
            // send the replication package to the queue distribution handler
            try {
                ReplicationQueueItemState state = queueDistributionStrategy.add(getName(), replicationQueueItem,
                        queueProvider);
                if (isPassive()) {
                    generatePackageQueuedEvent(replicationQueueItem);
                }
                if (state != null) {
                    replicationResponse.setStatus(state.getItemState().toString());
                    replicationResponse.setSuccessful(state.isSuccessful());
                } else {
                    replicationResponse.setStatus(ReplicationQueueItemState.ItemState.ERROR.toString());
                    replicationResponse.setSuccessful(false);
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("an error happened during queue processing", e);
                }
                replicationResponse.setSuccessful(false);
            }
        }
        return replicationResponse;
    }

    private void generatePackageQueuedEvent(ReplicationQueueItem replicationQueueItem) {
        Dictionary<Object, Object> properties = new Properties();
        properties.put("replication.package.paths", replicationQueueItem.getPaths());
        properties.put("replication.agent.name", name);
        replicationEventFactory.generateEvent(ReplicationEventType.PACKAGE_QUEUED, properties);
    }

    public ReplicationPackage removeHead(String queueName) throws ReplicationQueueException {
        ReplicationPackage replicationPackage = null;
        if (isPassive()) {
            ReplicationQueue queue = getQueue(queueName);
            ReplicationQueueItem info = queue.getHead();
            if (info != null) {
                queue.removeHead();
                replicationPackage = replicationPackageExporter.exportPackageById(info.getId());
            }
            return replicationPackage;
        } else {
            throw new ReplicationQueueException("cannot explicitly fetch items from not-passive agents");
        }
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
        // apply rules if any
        if (rules.length > 0) {
            ruleEngine.applyRules(this, rules);
        }

        if (!isPassive()) {
            queueProvider.enableQueueProcessing(getName(), new PackageQueueProcessor());
        }
    }

    public void disable() {
        log.info("disabling agent");
        if (rules != null) {
            ruleEngine.unapplyRules(this, rules);
        }

        if (!isPassive()) {
            queueProvider.disableQueueProcessing(getName());
        }
    }

    private boolean processTransportQueue(ReplicationQueueItem queueItem) {
        log.debug("reading package from id {}", queueItem.getId());
        try {
            ReplicationPackage replicationPackage = replicationPackageExporter.exportPackageById(queueItem.getId());
            replicationPackageImporter.importPackage(replicationPackage);
            replicationPackage.delete();
            return true;
        } catch (ReplicationPackageReadingException e) {
            return false;
        }
    }

    class PackageQueueProcessor implements ReplicationQueueProcessor {
        public boolean process(String queueName, ReplicationQueueItem packageInfo) {
            log.info("running package queue processor");

            return processTransportQueue(packageInfo);
        }
    }
}
