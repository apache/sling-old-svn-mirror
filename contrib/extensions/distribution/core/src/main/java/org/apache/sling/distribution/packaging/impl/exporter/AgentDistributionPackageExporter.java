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
package org.apache.sling.distribution.packaging.impl.exporter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentDistributionPackageExporter implements DistributionPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String QUEUE_NAME = "queue";

    private DistributionAgent agent;
    private final DistributionPackageBuilder packageBuilder;
    private String queueName;

    public AgentDistributionPackageExporter(Map<String, Object> config, DistributionAgent agent, DistributionPackageBuilder packageBuilder) {
        this (PropertiesUtil.toString(config.get(QUEUE_NAME), ""), agent, packageBuilder);
    }

    private AgentDistributionPackageExporter(String queueName, DistributionAgent agent, DistributionPackageBuilder packageBuilder) {

        if (agent == null || packageBuilder == null) {
            throw new IllegalArgumentException("Agent and package builder are required");
        }
        this.queueName = queueName;
        this.agent = agent;
        this.packageBuilder = packageBuilder;
    }

    @Nonnull
    public List<DistributionPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) {

        List<DistributionPackage> result = new ArrayList<DistributionPackage>();
        try {
            log.info("getting packages from queue {}", queueName);

            DistributionQueue queue = agent.getQueue(queueName);
            DistributionQueueItem info = queue.getHead();
            DistributionPackage distributionPackage;
            if (info != null) {
                distributionPackage = packageBuilder.getPackage(resourceResolver, info.getId());
                DistributionQueueItem item = queue.remove(info.getId());
                log.info("item {} fetched and removed from the queue", item);
                if (distributionPackage != null) {
                    result.add(distributionPackage);
                }
            }

        } catch (Exception ex) {
            log.error("Error exporting package", ex);
        }

        return result;
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) {
        return null;
    }
}
