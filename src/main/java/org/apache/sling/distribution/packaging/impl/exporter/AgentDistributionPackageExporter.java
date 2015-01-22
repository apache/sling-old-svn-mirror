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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuilderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentDistributionPackageExporter implements DistributionPackageExporter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DistributionPackageBuilderProvider packageBuilderProvider;


    private DistributionAgent agent;
    private String queueName;

    public AgentDistributionPackageExporter(String queueName, DistributionAgent agent, DistributionPackageBuilderProvider packageBuilderProvider) {
        this.packageBuilderProvider = packageBuilderProvider;

        if (agent == null || packageBuilderProvider == null) {
            throw new IllegalArgumentException("Agent and package builder are required");
        }
        this.queueName = queueName;
        this.agent = agent;
    }

    @Nonnull
    public List<DistributionPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) {

        List<DistributionPackage> result = new ArrayList<DistributionPackage>();
        try {
            log.debug("getting packages from queue {}", queueName);

            DistributionQueue queue = agent.getQueue(queueName);
            DistributionQueueItem info = queue.getHead();
            DistributionPackage distributionPackage;
            if (info != null) {
                DistributionPackageBuilder packageBuilder = packageBuilderProvider.getPackageBuilder(info.getType());

                if (packageBuilder != null) {
                    distributionPackage = packageBuilder.getPackage(resourceResolver, info.getId());
                    DistributionQueueItem item = queue.remove(info.getId());
                    log.info("item {} fetched and removed from the queue", item);
                    if (distributionPackage != null) {
                        result.add(distributionPackage);
                    }
                } else {
                    log.warn("cannot find package builder with type {}", info.getType());
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
