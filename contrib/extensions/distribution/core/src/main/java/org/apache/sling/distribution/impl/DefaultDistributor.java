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

package org.apache.sling.distribution.impl;

import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestState;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.impl.DistributionComponentProvider;
import org.apache.sling.distribution.common.DistributionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of Distributor interface that dispatches the request to available agents.
 */
@Component
@Service(Distributor.class)
public class DefaultDistributor implements Distributor {

    private final Logger log = LoggerFactory.getLogger(getClass());


    @Reference
    private
    DistributionComponentProvider componentProvider;

    @Nonnull
    public DistributionResponse distribute(@Nonnull String agentName, @Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest distributionRequest) {
        DistributionAgent agent = componentProvider.getService(DistributionAgent.class, agentName);

        if (agent == null) {
            return new SimpleDistributionResponse(DistributionRequestState.NOT_EXECUTED, "Agent is not available");
        }

        try {
            return agent.execute(resourceResolver, distributionRequest);
        } catch (DistributionException e) {
            log.error("cannot execute", e);
            return new SimpleDistributionResponse(DistributionRequestState.DROPPED, "Cannot execute request " + e.getMessage());
        }
    }
}
