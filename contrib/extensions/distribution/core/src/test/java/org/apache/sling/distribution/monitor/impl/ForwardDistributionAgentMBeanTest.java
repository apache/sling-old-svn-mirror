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
package org.apache.sling.distribution.monitor.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.agent.DistributionAgentState;
import org.junit.Test;

/**
 * Test case for {@link ForwardDistributionAgentMBean}
 */
public class ForwardDistributionAgentMBeanTest {

    @Test
    public void verifyMBeanExposedValues() {
        DistributionAgent agent = mock(DistributionAgent.class);
        when(agent.getState()).thenReturn(DistributionAgentState.RUNNING);

        Map<String, Object> osgiConfiguration = new HashMap<String, Object>();
        osgiConfiguration.put("name", "#distributionagent");
        osgiConfiguration.put("title", "Just a test title");
        osgiConfiguration.put("details", "Just test details");
        osgiConfiguration.put("enabled", true);
        osgiConfiguration.put("serviceName", "@distributionagent");
        osgiConfiguration.put("log.level", "error");
        osgiConfiguration.put("allowed.roots", "admin");
        osgiConfiguration.put("queue.processing.enabled", true);
        osgiConfiguration.put("packageImporter.endpoints", "endpoints");
        osgiConfiguration.put("passiveQueues", "passiveQueues");
        osgiConfiguration.put("priorityQueues", "priorityQueues");
        osgiConfiguration.put("retry.strategy", "none");
        osgiConfiguration.put("retry.attempts", 30);
        osgiConfiguration.put("queue.provider", "jobsProvider");
        osgiConfiguration.put("async.delivery", true);

        ForwardDistributionAgentMBean mBean = new ForwardDistributionAgentMBeanImpl(agent, osgiConfiguration);

        assertEquals(osgiConfiguration.get("name"), mBean.getName());
        assertEquals(osgiConfiguration.get("title"), mBean.getTitle());
        assertEquals(osgiConfiguration.get("details"), mBean.getDetails());
        assertEquals(osgiConfiguration.get("enabled"), mBean.isEnabled());
        assertEquals(osgiConfiguration.get("serviceName"), mBean.getServiceName());
        assertEquals(osgiConfiguration.get("log.level"), mBean.getLogLevel());
        assertEquals(osgiConfiguration.get("allowed.roots"), mBean.getAllowedRoots());
        assertEquals(osgiConfiguration.get("queue.processing.enabled"), mBean.isQueueProcessingEnabled());
        assertEquals(osgiConfiguration.get("packageImporter.endpoints"), mBean.getPackageImporterEndpoints());
        assertEquals(osgiConfiguration.get("passiveQueues"), mBean.getPassiveQueues());
        assertEquals(osgiConfiguration.get("priorityQueues"), mBean.getPriorityQueues());
        assertEquals(osgiConfiguration.get("retry.strategy"), mBean.getRetryStrategy());
        assertEquals(osgiConfiguration.get("retry.attempts"), mBean.getRetryAttempts());
        assertEquals(osgiConfiguration.get("queue.provider"), mBean.getQueueProvider());
        assertEquals(osgiConfiguration.get("async.delivery"), mBean.isAsyncDelivery());
        assertEquals(agent.getState().name().toLowerCase(), mBean.getStatus());
    }

}
