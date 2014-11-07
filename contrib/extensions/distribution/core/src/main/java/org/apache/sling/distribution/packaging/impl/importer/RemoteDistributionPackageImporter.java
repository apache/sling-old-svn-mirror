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
import org.apache.sling.distribution.transport.DistributionTransportHandler;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.distribution.transport.impl.DistributionEndpoint;
import org.apache.sling.distribution.transport.impl.MultipleEndpointDistributionTransportHandler;
import org.apache.sling.distribution.transport.impl.SimpleHttpDistributionTransportHandler;
import org.apache.sling.distribution.transport.impl.TransportEndpointStrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote implementation of {@link org.apache.sling.distribution.packaging.DistributionPackageImporter}
 */
public class RemoteDistributionPackageImporter implements DistributionPackageImporter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DistributionTransportHandler transportHandler;


    public RemoteDistributionPackageImporter(TransportAuthenticationProvider transportAuthenticationProvider,
                                             String[] endpoints,
                                             String transportEndpointStrategyName) {

        if (transportAuthenticationProvider == null) {
            throw new IllegalArgumentException("transportAuthenticationProviderFactory is required");
        }

        TransportEndpointStrategyType transportEndpointStrategyType = TransportEndpointStrategyType.valueOf(transportEndpointStrategyName);


        List<DistributionTransportHandler> transportHandlers = new ArrayList<DistributionTransportHandler>();

        for (String endpoint : endpoints) {
            if (endpoint != null && endpoint.length() > 0) {
                transportHandlers.add(new SimpleHttpDistributionTransportHandler(transportAuthenticationProvider,
                        new DistributionEndpoint(endpoint), null, -1));
            }
        }
        transportHandler = new MultipleEndpointDistributionTransportHandler(transportHandlers,
                transportEndpointStrategyType);

    }

    public boolean importPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) {
        boolean result = false;
        try {
            transportHandler.deliverPackage(resourceResolver, distributionPackage);
            result = true;
        } catch (Exception e) {
            log.error("failed in importing package {} ", distributionPackage, e);
        }
        return result;
    }

    public DistributionPackage importStream(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageImportException {
        throw new DistributionPackageImportException("not supported");
    }

}
