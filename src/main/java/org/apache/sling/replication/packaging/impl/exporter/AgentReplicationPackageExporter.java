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
package org.apache.sling.replication.packaging.impl.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.packaging.ReplicationPackage;
import org.apache.sling.replication.packaging.ReplicationPackageExporter;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueItem;
import org.apache.sling.replication.serialization.ReplicationPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentReplicationPackageExporter implements ReplicationPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String NAME = "agent";
    public static final String QUEUE_NAME = "queue";

    private ReplicationAgent agent;
    private final ReplicationPackageBuilder packageBuilder;
    private String queueName;

    public AgentReplicationPackageExporter(Map<String, Object> config, ReplicationAgent agent, ReplicationPackageBuilder packageBuilder) {
        this (PropertiesUtil.toString(config.get(QUEUE_NAME), ""), agent, packageBuilder);
    }

    public AgentReplicationPackageExporter(String queueName, ReplicationAgent agent, ReplicationPackageBuilder packageBuilder) {

        if (agent == null || packageBuilder == null) {
            throw new IllegalArgumentException("Agent and package builder are required");
        }
        this.queueName = queueName;
        this.agent = agent;
        this.packageBuilder = packageBuilder;
    }

    public List<ReplicationPackage> exportPackage(ResourceResolver resourceResolver, ReplicationRequest replicationRequest) {

        List<ReplicationPackage> result = new ArrayList<ReplicationPackage>();
        try {
            log.info("getting item from queue {}", queueName);

            ReplicationQueue queue = agent.getQueue(queueName);
            ReplicationQueueItem info = queue.getHead();
            ReplicationPackage replicationPackage;
            if (info != null) {
                replicationPackage = packageBuilder.getPackage(resourceResolver, info.getId());
                queue.remove(info.getId());
                if (replicationPackage != null) {
                    result.add(replicationPackage);
                }
            }

        } catch (Exception ex) {
            log.error("Error exporting package", ex);
        }

        return result;
    }

    public ReplicationPackage exportPackageById(ResourceResolver resourceResolver, String replicationPackageId) {
        return null;
    }
}
