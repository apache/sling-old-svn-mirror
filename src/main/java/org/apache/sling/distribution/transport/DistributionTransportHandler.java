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
package org.apache.sling.distribution.transport;

import javax.annotation.Nonnull;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.communication.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;

/**
 * A {@link DistributionTransportHandler} is responsible for implementing the transport of a
 * {@link org.apache.sling.distribution.packaging.DistributionPackage}s to / from another instance described by a {@link org.apache.sling.distribution.transport.impl.DistributionEndpoint}.
 * {@link DistributionTransportHandler} implementations are meant to be stateful,
 * so all the information regarding the target endpoint of the transport algorithm is part of its state and thus not exposed
 * in the API.
 */
public interface DistributionTransportHandler {

    /**
     * Delivers a given {@link org.apache.sling.distribution.packaging.DistributionPackage}
     *
     * @param resourceResolver   used to eventually access local resources needed by the transport algorithm
     * @param distributionPackage a {@link org.apache.sling.distribution.packaging.DistributionPackage} to transport
     * @throws DistributionTransportException if any error occurs during the transport
     */
    void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionTransportException;

    /**
     * Retrieves a list of {@link org.apache.sling.distribution.packaging.DistributionPackage}
     *
     * @param resourceResolver   used to eventually access local resources needed by the transport algorithm
     * @param distributionRequest the distribution request
     * @throws DistributionTransportException if any error occurs during the transport
     */
    @Nonnull
    List<DistributionPackage> retrievePackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionTransportException;

}