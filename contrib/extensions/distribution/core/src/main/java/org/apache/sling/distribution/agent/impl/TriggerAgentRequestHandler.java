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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.apache.sling.distribution.util.impl.DistributionUtils;

/**
 * A {@link DistributionRequestHandler} to trigger an agent.
 */
class TriggerAgentRequestHandler implements DistributionRequestHandler {
    private final SimpleDistributionAgentAuthenticationInfo authenticationInfo;
    private final DefaultDistributionLog log;
    private final boolean active;
    private SimpleDistributionAgent simpleDistributionAgent;
    private final DistributionAgent agent;

    public TriggerAgentRequestHandler(@Nonnull DistributionAgent agent,
                                      @Nonnull SimpleDistributionAgentAuthenticationInfo authenticationInfo,
                                      @Nonnull DefaultDistributionLog log,
                                      boolean active) {
        this.authenticationInfo = authenticationInfo;
        this.log = log;
        this.active = active;
        this.simpleDistributionAgent = simpleDistributionAgent;
        this.agent = agent;
    }

    public void handle(@Nullable ResourceResolver resourceResolver, @Nonnull DistributionRequest request) {

        if (!active) {
            log.warn("skipping agent handler as agent is disabled");
            return;
        }

        if (resourceResolver != null) {
            try {
                agent.execute(resourceResolver, request);
            } catch (Throwable t) {
                log.error("Error executing handler {}", request, t);
            }
        } else {
            ResourceResolver agentResourceResolver = null;

            try {
                agentResourceResolver = DistributionUtils.getResourceResolver(null, authenticationInfo.getAgentService(),
                        authenticationInfo.getSlingRepository(), authenticationInfo.getSubServiceName(),
                        authenticationInfo.getResourceResolverFactory());

                agent.execute(agentResourceResolver, request);
            } catch (Throwable e) {
                log.error("Error executing handler {}", request, e);
            } finally {
                DistributionUtils.ungetResourceResolver(agentResourceResolver);
            }
        }

    }
}
