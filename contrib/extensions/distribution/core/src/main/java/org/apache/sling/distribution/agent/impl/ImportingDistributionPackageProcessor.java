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

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.impl.SimpleDistributionResponse;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageProcessor;
import org.apache.sling.distribution.util.impl.DistributionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link DistributionPackageProcessor} that directly imports {@link DistributionPackage}s (skipping queues).
 */
class ImportingDistributionPackageProcessor implements DistributionPackageProcessor {

    // required components
    private final DistributionPackageImporter distributionPackageImporter;
    private final SimpleDistributionAgentAuthenticationInfo authenticationInfo;

    // request info
    private final String callingUser;
    private final String requestId;
    private final DefaultDistributionLog log;

    // stats
    private final AtomicInteger packagesCount = new AtomicInteger();
    private final AtomicLong packagesSize = new AtomicLong();
    private final List<DistributionResponse> allResponses = new LinkedList<DistributionResponse>();


    public ImportingDistributionPackageProcessor(DistributionPackageImporter distributionPackageImporter,
                                                 SimpleDistributionAgentAuthenticationInfo authenticationInfo,
                                                 String callingUser, String requestId, DefaultDistributionLog log) {
        this.distributionPackageImporter = distributionPackageImporter;
        this.authenticationInfo = authenticationInfo;
        this.callingUser = callingUser;
        this.requestId = requestId;
        this.log = log;
    }

    @Override
    public void process(DistributionPackage distributionPackage) {
        final long startTime = System.currentTimeMillis();
        try {
            // set up original calling RR
            ResourceResolver resourceResolver = DistributionUtils.getResourceResolver(callingUser, authenticationInfo.getAgentService(),
                    authenticationInfo.getSlingRepository(), authenticationInfo.getSubServiceName(),
                    authenticationInfo.getResourceResolverFactory());

            // perform importing
            distributionPackageImporter.importPackage(resourceResolver, distributionPackage);

            // collect stats
            packagesSize.addAndGet(distributionPackage.getSize());
            packagesCount.incrementAndGet();

            DistributionResponse response = new SimpleDistributionResponse(DistributionRequestState.ACCEPTED, "package imported");
            allResponses.add(response);

            final long endTime = System.currentTimeMillis();
            log.debug("PACKAGE-IMPORTED {}: packageId={}, paths={}, responses={}", requestId, distributionPackage.getId(),
                    distributionPackage.getInfo().getPaths(), endTime - startTime, response);

        } catch (DistributionException e) {
            log.error("an error happened during package import", e);
            allResponses.add(new SimpleDistributionResponse(DistributionRequestState.DROPPED, e.toString()));
        }
    }

    @Override
    public List<DistributionResponse> getAllResponses() {
        return allResponses;
    }

    @Override
    public int getPackagesCount() {
        return packagesCount.get();
    }

    @Override
    public long getPackagesSize() {
        return packagesSize.get();
    }
}
