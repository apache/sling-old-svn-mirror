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

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;

/**
 * Authentication information required by a {@link SimpleDistributionAgent} to perform its tasks.
 */
public class SimpleDistributionAgentAuthenticationInfo {

    private final SlingRepository slingRepository;
    private final String agentService;

    private final ResourceResolverFactory resourceResolverFactory;
    private final String subServiceName;

    public SimpleDistributionAgentAuthenticationInfo(@Nonnull SlingRepository slingRepository, @Nonnull String agentService,
                                                     @Nonnull ResourceResolverFactory resourceResolverFactory,
                                                     @Nullable String subServiceName) {
        this.slingRepository = slingRepository;
        this.agentService = agentService;
        this.resourceResolverFactory = resourceResolverFactory;
        this.subServiceName = subServiceName;
    }

    public SlingRepository getSlingRepository() {
        return slingRepository;
    }

    public String getAgentService() {
        return agentService;
    }

    public ResourceResolverFactory getResourceResolverFactory() {
        return resourceResolverFactory;
    }

    public String getSubServiceName() {
        return subServiceName;
    }
}
