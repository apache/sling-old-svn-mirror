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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.distribution.transport.impl.TransportEndpointStrategyType;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link RemoteDistributionPackageImporter}
 */
public class RemoteDistributionPackageImporterTest {

    @Test
    public void testDummyImport() throws Exception {
        TransportAuthenticationProvider authenticationProvider = mock(TransportAuthenticationProvider.class);
        String[] endpoints = new String[0];
        for (TransportEndpointStrategyType strategy : TransportEndpointStrategyType.values()) {
            RemoteDistributionPackageImporter remotedistributionPackageImporter = new RemoteDistributionPackageImporter(
                    authenticationProvider, endpoints, strategy.name());
            ResourceResolver resourceResolver = mock(ResourceResolver.class);
            DistributionPackage distributionPackage = mock(DistributionPackage.class);
            remotedistributionPackageImporter.importPackage(resourceResolver, distributionPackage);
        }
    }
}
