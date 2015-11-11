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
package org.apache.sling.distribution.transport.impl;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.component.impl.SettingsUtils;
import org.apache.sling.distribution.impl.DistributionException;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageInfo;
import org.apache.sling.distribution.transport.core.DistributionTransport;

/**
 * {@link org.apache.sling.distribution.transport.core.DistributionTransport} supporting delivery / retrieval from multiple
 * endpoints.
 */
public class MultipleEndpointDistributionTransport implements DistributionTransport {

    private final Map<String, DistributionTransport> transportHelpers;
    private final TransportEndpointStrategyType endpointStrategyType;

    public MultipleEndpointDistributionTransport(Map<String, DistributionTransport> transportHelpers,
                                                 TransportEndpointStrategyType endpointStrategyType) {
        this.transportHelpers = new TreeMap<String, DistributionTransport>();
        this.transportHelpers.putAll(transportHelpers);
        this.endpointStrategyType = endpointStrategyType;
    }

    public MultipleEndpointDistributionTransport(List<DistributionTransport> transportHelpers,
                                                 TransportEndpointStrategyType endpointStrategyType) {
        this(SettingsUtils.toMap(transportHelpers, "endpoint"), endpointStrategyType);
    }

    public void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionException {

        if (endpointStrategyType.equals(TransportEndpointStrategyType.One)) {
            DistributionPackageInfo info = distributionPackage.getInfo();
            String queueName = DistributionPackageUtils.getQueueName(info);

            DistributionTransport distributionTransport = getDefaultTransport();
            if (queueName != null) {
                distributionTransport = transportHelpers.get(queueName);
            }

            if (distributionTransport != null) {
                distributionTransport.deliverPackage(resourceResolver, distributionPackage);
            }

        } else if (endpointStrategyType.equals(TransportEndpointStrategyType.All)) {
            for (DistributionTransport distributionTransport : transportHelpers.values()) {
                distributionTransport.deliverPackage(resourceResolver, distributionPackage);

            }

        }
    }

    @Nonnull
    public List<DistributionPackage> retrievePackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionException {
        List<DistributionPackage> result = new ArrayList<DistributionPackage>();


        if (endpointStrategyType.equals(TransportEndpointStrategyType.One)) {

            DistributionTransport distributionTransport = getDefaultTransport();

            if (distributionTransport != null) {
                Iterable<DistributionPackage> retrievedPackages = distributionTransport.retrievePackages(resourceResolver, distributionRequest);

                for (DistributionPackage retrievedPackage : retrievedPackages) {
                    result.add(retrievedPackage);
                }
            }


        } else if (endpointStrategyType.equals(TransportEndpointStrategyType.All)) {
            for (DistributionTransport distributionTransport : transportHelpers.values()) {
                Iterable<DistributionPackage> retrievedPackages = distributionTransport.retrievePackages(resourceResolver, distributionRequest);

                for (DistributionPackage retrievedPackage : retrievedPackages) {
                    result.add(retrievedPackage);
                }

            }
        }

        return result;
    }

    DistributionTransport getDefaultTransport() {
        java.util.Collection<DistributionTransport> var = transportHelpers.values();
        DistributionTransport[] handlers = var.toArray(new DistributionTransport[var.size()]);

        if (handlers.length > 0) {
            return handlers[0];
        }

        return null;
    }


}
