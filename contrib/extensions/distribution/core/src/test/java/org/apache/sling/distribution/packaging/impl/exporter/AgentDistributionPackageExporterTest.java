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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageBuilderProvider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class AgentDistributionPackageExporterTest {

    @Test
    public void testTestExport() throws Exception {
        AgentDistributionPackageExporter distributionPackageExporter = new AgentDistributionPackageExporter(null,
                mock(DistributionAgent.class), mock(DistributionPackageBuilderProvider.class), null);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String[] args = new String[0]; // vargarg doesn't match and causes compiler warning
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.TEST, args);
        final List<DistributionPackage> distributionPackages = new ArrayList<DistributionPackage>();
        distributionPackageExporter.exportPackages(resourceResolver, distributionRequest, new DistributionPackageProcessor() {
            @Override
            public void process(DistributionPackage distributionPackage) {
                distributionPackages.add(distributionPackage);
            }

            @Override
            public List<DistributionResponse> getAllResponses() {
                return null;
            }

            @Override
            public int getPackagesCount() {
                return 0;
            }

            @Override
            public long getPackagesSize() {
                return 0;
            }
        });
        assertNotNull(distributionPackages);

        assertEquals(1, distributionPackages.size());

    }
}

