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
package org.apache.sling.distribution.agent.impl;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ImportingDistributionPackageProcessor}
 */
public class ImportingDistributionPackageProcessorTest {

    @Test
    public void testProcess() throws Exception {
        DistributionPackageImporter importer = mock(DistributionPackageImporter.class);
        SlingRepository repository = mock(SlingRepository.class);
        String agentService = "agentService";
        ResourceResolverFactory resourceResolverFactory = mock(ResourceResolverFactory.class);
        String subServiceName = "ssn";
        SimpleDistributionAgentAuthenticationInfo authInfo = new SimpleDistributionAgentAuthenticationInfo(repository,
                agentService, resourceResolverFactory, subServiceName);
        String callingUser = "foo";
        String requestId = "123";
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        ImportingDistributionPackageProcessor processor = new ImportingDistributionPackageProcessor(importer, authInfo,
                callingUser, requestId, log);
        DistributionPackage distributionPackage = mock(DistributionPackage.class);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DistributionPackageInfo.PROPERTY_REQUEST_PATHS, "/");
        map.put(DistributionPackageInfo.PROPERTY_REQUEST_TYPE, DistributionRequestType.TEST);

        DistributionPackageInfo info = new DistributionPackageInfo("foo", map);
        when(distributionPackage.getInfo()).thenReturn(info);
        processor.process(distributionPackage);
    }

    @Test
    public void testGetAllResponses() throws Exception {
        DistributionPackageImporter importer = mock(DistributionPackageImporter.class);
        SlingRepository repository = mock(SlingRepository.class);
        String agentService = "agentService";
        ResourceResolverFactory resourceResolverFactory = mock(ResourceResolverFactory.class);
        String subServiceName = "ssn";
        SimpleDistributionAgentAuthenticationInfo authInfo = new SimpleDistributionAgentAuthenticationInfo(repository,
                agentService, resourceResolverFactory, subServiceName);
        String callingUser = "foo";
        String requestId = "123";
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        ImportingDistributionPackageProcessor processor = new ImportingDistributionPackageProcessor(importer, authInfo,
                callingUser, requestId, log);
        assertNotNull(processor.getAllResponses());
        assertTrue(processor.getAllResponses().isEmpty());

    }

    @Test
    public void testGetPackagesCount() throws Exception {
        DistributionPackageImporter importer = mock(DistributionPackageImporter.class);
        SlingRepository repository = mock(SlingRepository.class);
        String agentService = "agentService";
        ResourceResolverFactory resourceResolverFactory = mock(ResourceResolverFactory.class);
        String subServiceName = "ssn";
        SimpleDistributionAgentAuthenticationInfo authInfo = new SimpleDistributionAgentAuthenticationInfo(repository,
                agentService, resourceResolverFactory, subServiceName);
        String callingUser = "foo";
        String requestId = "123";
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        ImportingDistributionPackageProcessor processor = new ImportingDistributionPackageProcessor(importer, authInfo,
                callingUser, requestId, log);
        assertEquals(0, processor.getPackagesCount());
    }

    @Test
    public void testGetPackagesSize() throws Exception {
        DistributionPackageImporter importer = mock(DistributionPackageImporter.class);
        SlingRepository repository = mock(SlingRepository.class);
        String agentService = "agentService";
        ResourceResolverFactory resourceResolverFactory = mock(ResourceResolverFactory.class);
        String subServiceName = "ssn";
        SimpleDistributionAgentAuthenticationInfo authInfo = new SimpleDistributionAgentAuthenticationInfo(repository,
                agentService, resourceResolverFactory, subServiceName);
        String callingUser = "foo";
        String requestId = "123";
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        ImportingDistributionPackageProcessor processor = new ImportingDistributionPackageProcessor(importer, authInfo,
                callingUser, requestId, log);
        assertEquals(0, processor.getPackagesSize());
    }
}