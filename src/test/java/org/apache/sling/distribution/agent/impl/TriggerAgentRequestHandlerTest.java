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
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.log.impl.DefaultDistributionLog;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TriggerAgentRequestHandler}
 */
public class TriggerAgentRequestHandlerTest {

    @Test
    public void testHandleActive() throws Exception {
        DistributionAgent agent = mock(DistributionAgent.class);
        SimpleDistributionAgentAuthenticationInfo authenticationInfo = mock(SimpleDistributionAgentAuthenticationInfo.class);
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        TriggerAgentRequestHandler triggerAgentRequestHandler = new TriggerAgentRequestHandler(agent,
                authenticationInfo, log, true);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest request = mock(DistributionRequest.class);
        triggerAgentRequestHandler.handle(resourceResolver, request);
    }

    @Test
    public void testHandlePassive() throws Exception {
        DistributionAgent agent = mock(DistributionAgent.class);
        SimpleDistributionAgentAuthenticationInfo authenticationInfo = mock(SimpleDistributionAgentAuthenticationInfo.class);
        DefaultDistributionLog log = mock(DefaultDistributionLog.class);
        TriggerAgentRequestHandler triggerAgentRequestHandler = new TriggerAgentRequestHandler(agent,
                authenticationInfo, log, false);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        DistributionRequest request = mock(DistributionRequest.class);
        triggerAgentRequestHandler.handle(resourceResolver, request);
    }
}