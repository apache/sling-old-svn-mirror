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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link LocalDistributionPackageExporter}
 */
public class LocalDistributionPackageExporterTest {

    @Test
    public void testExport() throws Exception {
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        when(packageBuilder.createPackage(any(ResourceResolver.class), any(DistributionRequest.class))).thenReturn(mock(DistributionPackage.class));
        LocalDistributionPackageExporter localdistributionPackageExporter = new LocalDistributionPackageExporter(packageBuilder);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.ADD, "/");
        final List<DistributionPackage> distributionPackages = new ArrayList<DistributionPackage>();
        localdistributionPackageExporter.exportPackages(resourceResolver, distributionRequest, new DistributionPackageProcessor() {
            @Override
            public void process(DistributionPackage distributionPackage) {
                distributionPackages.add(distributionPackage);
            }
        });
        assertNotNull(distributionPackages);
        assertEquals(1, distributionPackages.size());
    }

    @Test
    public void testGetPackage() throws Exception {
        DistributionPackageBuilder packageBuilder = mock(DistributionPackageBuilder.class);
        when(packageBuilder.getPackage(any(ResourceResolver.class), any(String.class))).thenReturn(mock(DistributionPackage.class));
        LocalDistributionPackageExporter localdistributionPackageExporter = new LocalDistributionPackageExporter(packageBuilder);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionPackage distributionPackage = localdistributionPackageExporter.getPackage(resourceResolver, "123");
        assertNotNull(distributionPackage);
    }
}
