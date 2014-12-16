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
package org.apache.sling.distribution.transport.core;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.transport.DistributionTransportSecret;

/**
 * A transport layer implementation to transport data between two (or eventually more) Sling instances.
 * <p/>
 * Each implementation is meant to be stateful in the sense that it will hide the details about the sending / receiving
 * endpoints of the transport.
 */
public interface DistributionTransport {

    /**
     * Deliver a {@link org.apache.sling.distribution.packaging.DistributionPackage} to a target instance using this
     * transport layer implementation.
     *
     * @param resourceResolver    a resolver used to eventually access local resources needed by the transport algorithm
     * @param distributionPackage a {@link org.apache.sling.distribution.packaging.DistributionPackage} to transport
     * @param secret              the {@link org.apache.sling.distribution.transport.DistributionTransportSecret} used to authenticate
     *                            against the target instance according to an authentication algorithm implemented by the transport.
     * @throws DistributionTransportException if the {@link org.apache.sling.distribution.packaging.DistributionPackage}
     *                                        fails to be delivered to the target instance (e.g. because of network, I/O issues)
     */
    void deliverPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage
            distributionPackage, @Nonnull DistributionTransportSecret secret) throws DistributionTransportException;

    /**
     * Retrieve {@link org.apache.sling.distribution.packaging.DistributionPackage}s from a target Sling instance, which
     * will create them according to {@link org.apache.sling.distribution.DistributionRequest}.
     *
     * @param resourceResolver a resolver used to eventually access local resources needed by the transport algorithm
     * @param request          a {@link org.apache.sling.distribution.DistributionRequest} to be forwarded to the target
     *                         instance
     * @param secret           the {@link org.apache.sling.distribution.transport.DistributionTransportSecret} used to authenticate
     *                         against the target instance according to an authentication algorithm implemented by the transport.
     * @return an {@link java.lang.Iterable} of {@link org.apache.sling.distribution.packaging.DistributionPackage}s fetched
     * from the target instance.
     * @throws DistributionTransportException if the {@link org.apache.sling.distribution.packaging.DistributionPackage}s
     *                                        fail to be retrieved from the target instance
     */
    @Nonnull
    Iterable<DistributionPackage> retrievePackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest
            request, @Nonnull DistributionTransportSecret secret) throws DistributionTransportException;

}
