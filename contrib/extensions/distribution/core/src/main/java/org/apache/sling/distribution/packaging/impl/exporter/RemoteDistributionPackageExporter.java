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
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExportException;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.core.DistributionTransport;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.distribution.transport.impl.MultipleEndpointDistributionTransport;
import org.apache.sling.distribution.transport.impl.SimpleHttpDistributionTransport;
import org.apache.sling.distribution.transport.impl.TransportEndpointStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link org.apache.sling.distribution.packaging.DistributionPackageExporter}
 */
public class RemoteDistributionPackageExporter implements DistributionPackageExporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DistributionPackageBuilder packageBuilder;
    private final DistributionTransportSecretProvider secretProvider;

    private DistributionTransport transportHandler;

    public RemoteDistributionPackageExporter(DistributionPackageBuilder packageBuilder,
                                             DistributionTransportSecretProvider secretProvider,
                                             String[] endpoints,
                                             TransportEndpointStrategyType transportEndpointStrategyType,
                                             int pullItems) {
        if (packageBuilder == null) {
            throw new IllegalArgumentException("packageBuilder is required");
        }

        if (secretProvider == null) {
            throw new IllegalArgumentException("distributionTransportSecretProvider is required");
        }


        this.packageBuilder = packageBuilder;
        this.secretProvider = secretProvider;

        List<DistributionTransport> transportHandlers = new ArrayList<DistributionTransport>();

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new SimpleHttpDistributionTransport(new DistributionEndpoint(endpoint), packageBuilder, pullItems));
            }
        }
        transportHandler = new MultipleEndpointDistributionTransport(transportHandlers,
                transportEndpointStrategyType);
    }

    @Nonnull
    public List<DistributionPackage> exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) throws DistributionPackageExportException {
        try {
            List<DistributionPackage> packages = new ArrayList<DistributionPackage>();
            for (DistributionPackage distributionPackage : transportHandler.retrievePackages(resourceResolver, distributionRequest, secretProvider.getSecret())) {
                packages.add(distributionPackage);
            }
            return packages;
        } catch (Exception e) {
            log.error("cannot export packages", e);
            throw new DistributionPackageExportException(e);
        }
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) {
        return packageBuilder.getPackage(resourceResolver, distributionPackageId);
    }
}
