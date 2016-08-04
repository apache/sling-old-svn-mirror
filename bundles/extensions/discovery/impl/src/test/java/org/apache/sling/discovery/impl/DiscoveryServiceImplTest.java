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
package org.apache.sling.discovery.impl;

import static org.junit.Assert.assertEquals;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.base.its.AbstractDiscoveryServiceTest;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.commons.providers.base.DummyListener;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstanceBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryServiceImplTest extends AbstractDiscoveryServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public VirtualInstanceBuilder newBuilder() {
        return new FullJR2VirtualInstanceBuilder();
    }

    @Test
    public void testLocalClusterSyncTokenIdChange() throws Exception {
        logger.info("testLocalClusterSyncTokenIdChange: start");
        logger.info("testLocalClusterSyncTokenIdChange: creating instance1...");
        FullJR2VirtualInstanceBuilder builder1 = 
                (FullJR2VirtualInstanceBuilder) new FullJR2VirtualInstanceBuilder()
                .setDebugName("instance1")
                .newRepository("/var/testLocalClusterSyncTokenIdChange/", true)
                .setConnectorPingInterval(999)
                .setConnectorPingTimeout(999)
                .setMinEventDelay(0);
        VirtualInstance instance1 = builder1.build();
        logger.info("testLocalClusterSyncTokenIdChange: creating instance2...");
        FullJR2VirtualInstanceBuilder builder2 = 
                (FullJR2VirtualInstanceBuilder) new FullJR2VirtualInstanceBuilder()
                .setDebugName("instance2")
                .useRepositoryOf(instance1)
                .setConnectorPingInterval(999)
                .setConnectorPingTimeout(999)
                .setMinEventDelay(0);
        VirtualInstance instance2 = builder2.build();
        
        logger.info("testLocalClusterSyncTokenIdChange: registering listener...");
        DummyListener listener = new DummyListener();
        DiscoveryServiceImpl discoveryService = (DiscoveryServiceImpl) instance1.getDiscoveryService();
        discoveryService.bindTopologyEventListener(listener);
        
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(0, listener.countEvents());
        
        logger.info("testLocalClusterSyncTokenIdChange: doing some heartbeating...");
        instance1.heartbeatsAndCheckView();
        instance2.heartbeatsAndCheckView();
        Thread.sleep(1000);
        instance1.heartbeatsAndCheckView();
        instance2.heartbeatsAndCheckView();
        Thread.sleep(2000);

        logger.info("testLocalClusterSyncTokenIdChange: expecting to have received the INIT...");
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(1, listener.countEvents());
        
        ResourceResolverFactory factory = instance1.getResourceResolverFactory();
        ResourceResolver resolver = factory.getAdministrativeResourceResolver(null);
        
        instance1.heartbeatsAndCheckView();
        instance2.heartbeatsAndCheckView();
        Thread.sleep(1000);

        logger.info("testLocalClusterSyncTokenIdChange: after another heartbeat nothing more should have been triggered...");
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(1, listener.countEvents());
        
        // simulate a change in the establishedView's viewId - which can be
        // achieved by triggering a revoting - which should result with the
        // same view cos the instances have not changed
        HeartbeatHandler heartbeatHandler = (HeartbeatHandler) instance1.getViewChecker();
        logger.info("testLocalClusterSyncTokenIdChange: forcing a new voting to start...");
        heartbeatHandler.startNewVoting();
        
        logger.info("testLocalClusterSyncTokenIdChange: doing some heartbeats to finish the voting...");
        instance1.heartbeatsAndCheckView();
        instance2.heartbeatsAndCheckView();
        Thread.sleep(1000);
        instance1.heartbeatsAndCheckView();
        instance2.heartbeatsAndCheckView();
        Thread.sleep(3000);

        logger.info("testLocalClusterSyncTokenIdChange: now we should have gotten a CHANGING/CHANGED pair additionally...");
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(3, listener.countEvents());
    }
}
