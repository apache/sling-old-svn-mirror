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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.junit.Test;

/**
 * Testcase for {@link RemoteDistributionPackageExporter}
 */
public class RemoteDistributionPackageExporterTest {

    @Test
    public void testNothingExported() throws Exception {
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        DistributionTransportSecretProvider distributionTransportSecretProvider = mock(DistributionTransportSecretProvider.class);
        String[] endpoints = new String[0];
        RemoteDistributionPackageExporter remotedistributionPackageExporter = new RemoteDistributionPackageExporter(mock(DefaultDistributionLog.class),
                packageBuilder, distributionTransportSecretProvider, endpoints, 1);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        final List<DistributionPackage> distributionPackages = new ArrayList<DistributionPackage>();
        remotedistributionPackageExporter.exportPackages(resourceResolver, distributionRequest, new DistributionPackageProcessor() {
            @Override
            public void process(DistributionPackage distributionPackage) {
                distributionPackages.add(distributionPackage);
            }
        });
        assertNotNull(distributionPackages);
        assertTrue(distributionPackages.isEmpty());
    }

    @Test
    public void testFailedPackageRetrieval() throws Exception {
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        DistributionTransportSecretProvider distributionTransportSecretProvider = mock(DistributionTransportSecretProvider.class);
        String[] endpoints = new String[0];
        RemoteDistributionPackageExporter remotedistributionPackageExporter = new RemoteDistributionPackageExporter(mock(DefaultDistributionLog.class),
                packageBuilder, distributionTransportSecretProvider, endpoints, 1);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = remotedistributionPackageExporter.getPackage(resourceResolver, "123");
        assertNull(distributionPackage);
    }
}
