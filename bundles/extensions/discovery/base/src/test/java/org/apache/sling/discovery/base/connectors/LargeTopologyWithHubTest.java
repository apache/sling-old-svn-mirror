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
package org.apache.sling.discovery.base.connectors;

import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.base.its.setup.TopologyHelper;
import org.apache.sling.discovery.base.its.setup.VirtualConnector;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.testing.tools.sling.TimeoutsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LargeTopologyWithHubTest {

    private static final Logger logger = LoggerFactory.getLogger(LargeTopologyWithHubTest.class);

    private static List<VirtualInstance> instances;
    private static VirtualInstance hub;
    private static List<String> slingIds;
    private static final int TEST_SIZE = 50;
    
    @Rule
    public final RetryRule retryRule = new RetryRule();

    private VirtualInstanceBuilder newBuilder() {
        return new DummyVirtualInstanceBuilder();
    }
    
    @Before
    public void setup() throws Throwable {
        instances = new LinkedList<VirtualInstance>();
        final int defaultHeartbeatTimeout = 3600; // 1 hour should be enough, really
        final int heartbeatTimeout = TimeoutsProvider.getInstance().getTimeout(defaultHeartbeatTimeout);
        VirtualInstanceBuilder hubBuilder = newBuilder()
                .newRepository("/var/discovery/impl/", true)
                .setDebugName("hub")
                .setConnectorPingInterval(5)
                .setConnectorPingTimeout(heartbeatTimeout);
        hub = hubBuilder.build();
        instances.add(hub);
        hub.getConfig().setViewCheckTimeout(heartbeatTimeout);
//        hub.installVotingOnHeartbeatHandler();
        hub.heartbeatsAndCheckView();
        hub.heartbeatsAndCheckView();
        assertNotNull(hub.getClusterViewService().getLocalClusterView());
        hub.startViewChecker(1);
        hub.dumpRepo();
        
        slingIds = new LinkedList<String>();
        slingIds.add(hub.getSlingId());
        logger.info("setUp: using heartbeatTimeout of "+heartbeatTimeout+"sec "
                + "(default: "+defaultHeartbeatTimeout+")");
        for(int i=0; i<TEST_SIZE; i++) {
            logger.info("setUp: creating instance"+i);
            VirtualInstanceBuilder builder2 = newBuilder()
                    .newRepository("/var/discovery/impl/", false)
                    .setDebugName("instance"+i)
                    .setConnectorPingInterval(5)
                    .setConnectorPingTimeout(heartbeatTimeout);
            VirtualInstance instance = builder2.build();
            instances.add(instance);
            instance.getConfig().setViewCheckTimeout(heartbeatTimeout);
//            instance.installVotingOnHeartbeatHandler();
            instance.heartbeatsAndCheckView();
            instance.heartbeatsAndCheckView();
            ClusterView clusterView = instance.getClusterViewService().getLocalClusterView();
            assertNotNull(clusterView);
            new VirtualConnector(instance, hub);
            slingIds.add(instance.getSlingId());
        }
    }
    
    @After
    public void tearDown() throws Exception {
        for (Iterator<VirtualInstance> it = instances.iterator(); it.hasNext();) {
            final VirtualInstance instance = it.next();
            instance.stop();
        }
    }
    
    @Test
    @Retry(timeoutMsec=30000, intervalMsec=500)
    public void testLargeTopologyWithHub() throws Exception {
        hub.dumpRepo();
        final TopologyView tv = hub.getDiscoveryService().getTopology();
        assertNotNull(tv);
        logger.info(
                "testLargeTopologyWithHub: checking if all connectors are registered, TopologyView has {} Instances", 
                tv.getInstances().size());
        TopologyHelper.assertTopologyConsistsOf(tv, slingIds.toArray(new String[slingIds.size()]));
        logger.info("testLargeTopologyWithHub: test passed");
    }
}