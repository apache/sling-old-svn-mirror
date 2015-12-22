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
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageExporter;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.core.DistributionContext;
import org.apache.sling.distribution.transport.core.DistributionTransport;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.distribution.transport.core.DistributionPackageProxy;
import org.apache.sling.distribution.transport.impl.SimpleHttpDistributionTransport;

/**
 * Default implementation of {@link org.apache.sling.distribution.packaging.DistributionPackageExporter}
 */
public class RemoteDistributionPackageExporter implements DistributionPackageExporter {


    private final DistributionPackageBuilder packageBuilder;
    private final DistributionTransportSecretProvider secretProvider;
    private final DefaultDistributionLog log;
    private final int maxPullItems;

    private List<DistributionTransport> transportHandlers = new ArrayList<DistributionTransport>();

    public RemoteDistributionPackageExporter(DefaultDistributionLog log, DistributionPackageBuilder packageBuilder,
                                             DistributionTransportSecretProvider secretProvider,
                                             String[] endpoints,
                                             int maxPullItems) {
        this.log = log;
        this.maxPullItems = maxPullItems;
        if (packageBuilder == null) {
            throw new IllegalArgumentException("packageBuilder is required");
        }

        if (secretProvider == null) {
            throw new IllegalArgumentException("distributionTransportSecretProvider is required");
        }


        this.packageBuilder = packageBuilder;
        this.secretProvider = secretProvider;

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new SimpleHttpDistributionTransport(log, new DistributionEndpoint(endpoint), packageBuilder, secretProvider));
            }
        }
    }

    @Nonnull
    public void exportPackages(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest, @Nonnull DistributionPackageProcessor packageProcessor) throws DistributionException {
        int maxNumberOfPackages = DistributionRequestType.PULL.equals(distributionRequest.getRequestType()) ? maxPullItems : 1;
        for (DistributionTransport distributionTransport : transportHandlers) {
            int noPackages = 0;

            DistributionContext distributionContext = new DistributionContext();
            DistributionPackageProxy retrievedPackage;
            while (noPackages < maxNumberOfPackages && ((retrievedPackage = distributionTransport.retrievePackage(resourceResolver, distributionRequest, distributionContext)) != null)) {


                DistributionPackage distributionPackage = retrievedPackage.getPackage();

                try {
                    packageProcessor.process(distributionPackage);

                    retrievedPackage.deletePackage();

                } finally {
                    DistributionPackageUtils.closeSafely(distributionPackage);
                }

                noPackages++;
            }
        }
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) throws DistributionException {
        return packageBuilder.getPackage(resourceResolver, distributionPackageId);
    }
}
