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
package org.apache.sling.distribution.packaging.impl.importer;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImportException;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.transport.DistributionTransport;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.distribution.transport.impl.MultipleEndpointDistributionTransport;
import org.apache.sling.distribution.transport.impl.SimpleHttpDistributionTransport;
import org.apache.sling.distribution.transport.impl.TransportEndpointStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote implementation of {@link org.apache.sling.distribution.packaging.DistributionPackageImporter}
 */
public class RemoteDistributionPackageImporter implements DistributionPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DistributionTransport transportHandler;
    private DistributionTransportSecretProvider distributionTransportSecretProvider;


    public RemoteDistributionPackageImporter(DistributionTransportSecretProvider distributionTransportSecretProvider,
                                             String[] endpoints,
                                             String transportEndpointStrategyName) {
        this.distributionTransportSecretProvider = distributionTransportSecretProvider;

        if (distributionTransportSecretProvider == null) {
            throw new IllegalArgumentException("distributionTransportSecretProvider is required");
        }

        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(transportEndpointStrategyName);


        List<DistributionTransport> transportHandlers = new ArrayList<DistributionTransport>();

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new SimpleHttpDistributionTransport(new DistributionEndpoint(endpoint), null, -1));
            }
        }
        transportHandler = new MultipleEndpointDistributionTransport(transportHandlers,
                transportEndpointStrategyType);

    }

    public void importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageImportException {
        try {
            transportHandler.deliverPackage(resourceResolver, distributionPackage, distributionTransportSecretProvider.getSecret());
        } catch (Exception e) {
            throw new DistributionPackageImportException("failed in importing package " + distributionPackage);
        }
    }

    public DistributionPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageImportException {
        throw new DistributionPackageImportException("not supported");
    }

}
