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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.transport.core.DistributionTransport;
import org.apache.sling.distribution.transport.core.DistributionTransportException;
import org.apache.sling.distribution.transport.DistributionTransportSecret;

/**
 * {@link org.apache.sling.distribution.transport.core.DistributionTransport} supporting delivery / retrieval from multiple
 * endpoints.
 */
public class MultipleEndpointDistributionTransport implements DistributionTransport {

    private final List<DistributionTransport> transportHelpers;
    private final TransportEndpointStrategyType endpointStrategyType;
    private int lastSuccessfulEndpointId = 0;

    public MultipleEndpointDistributionTransport(List<DistributionTransport> transportHelpers,
                                                 TransportEndpointStrategyType endpointStrategyType) {
        this.transportHelpers = transportHelpers;
        this.endpointStrategyType = endpointStrategyType;
    }

    private List<DistributionPackage> doTransport(ResourceResolver resourceResolver, DistributionRequest distributionRequest,
                                                  DistributionPackage distributionPackage, DistributionTransportSecret secret) throws DistributionTransportException {

        int offset = 0;
        if (endpointStrategyType.equals(TransportEndpointStrategyType.One)) {
            offset = lastSuccessfulEndpointId;
        }

        int length = transportHelpers.size();
        List<DistributionPackage> result = new ArrayList<DistributionPackage>();

        for (int i = 0; i < length; i++) {
            int currentId = (offset + i) % length;

            DistributionTransport transportHelper = transportHelpers.get(currentId);
            if (distributionPackage != null) {
                transportHelper.deliverPackage(resourceResolver, distributionPackage, secret);
            } else if (distributionRequest != null) {
                Iterable<DistributionPackage> distributionPackages = transportHelper.retrievePackages(resourceResolver, distributionRequest, secret);
                for (DistributionPackage retrievedPackage : distributionPackages) {
                    result.add(retrievedPackage);
                }
            }

            lastSuccessfulEndpointId = currentId;
            if (endpointStrategyType.equals(TransportEndpointStrategyType.One))
                break;
        }

        return result;
    }

    public void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage,
                                                      @Nonnull DistributionTransportSecret secret) throws DistributionTransportException {
        doTransport(resourceResolver, null, distributionPackage, secret);
    }

    @Nonnull
    public List<DistributionPackage> retrievePackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest,
                                                      @Nonnull DistributionTransportSecret secret) throws DistributionTransportException {
        return doTransport(resourceResolver, distributionRequest, null, secret);
    }


}
