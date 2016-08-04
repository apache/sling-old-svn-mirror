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
package org.apache.sling.discovery.oak.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.base.its.AbstractDiscoveryServiceTest;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.AssertingTopologyEventListener;
import org.apache.sling.discovery.oak.OakDiscoveryService;
import org.apache.sling.discovery.oak.its.setup.OakVirtualInstanceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OakDiscoveryServiceTest extends AbstractDiscoveryServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Level logLevel;

    private VirtualInstance instance1;

    private VirtualInstance instance2;

    public OakVirtualInstanceBuilder newBuilder() {
        return new OakVirtualInstanceBuilder();
    }

    @Before
    public void setUp() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.INFO);        
    }
    
    @After
    public void teartDown() throws Throwable {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
        if (instance1!=null) {
            instance1.stopViewChecker();
            instance1.stop();
            instance1 = null;
        }
        if (instance2!=null) {
            instance2.stopViewChecker();
            instance2.stop();
            instance2 = null;
        }
    }
    
    /**
     * Tests whether the not-current view returned by getTopology()
     * matches what listeners get in TOPOLOGY_CHANGING - it should
     * basically be the same.
     */
    @Test
    public void testOldView() throws Throwable {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(Level.INFO); // info should do
        OakVirtualInstanceBuilder builder = newBuilder();
        builder.setDebugName("firstInstanceA")
                .newRepository("/var/discovery/oak/", true)
                .setConnectorPingTimeout(3 /* heartbeat-timeout */)
                .setMinEventDelay(3 /*min event delay*/);
        instance1 = builder.build();
        instance1.stopViewChecker();
//        instance1.stopVoting();
        TopologyView t1 = instance1.getDiscoveryService().getTopology();
        assertFalse(t1.isCurrent()); // current it should not be
        assertEquals(1, t1.getInstances().size()); // but it can as well contain the local instance
        AssertingTopologyEventListener l1 = new AssertingTopologyEventListener("instance1.l1");
        l1.addExpected(Type.TOPOLOGY_INIT);
        instance1.bindTopologyEventListener(l1);
        logger.info("testOldView: instance1 created, no events expected yet. slingId="+instance1.slingId);
        instance1.heartbeatsAndCheckView();
        Thread.sleep(200);
        instance1.heartbeatsAndCheckView();
        Thread.sleep(200);
        instance1.heartbeatsAndCheckView();
        Thread.sleep(200);
        logger.info("testOldView: 2nd/3rd heartbeat sent - now expecting a TOPOLOGY_INIT");
        instance1.dumpRepo();
        assertEquals(1, l1.getEvents().size()); // one event
        assertEquals(0, l1.getRemainingExpectedCount()); // the expected one
        assertEquals(0, l1.getUnexpectedCount());
        t1 = instance1.getDiscoveryService().getTopology();
        assertTrue(t1.isCurrent()); // current it should now be
        assertEquals(1, t1.getInstances().size()); // and it must contain the local instance
        
        logger.info("testOldView: creating instance2");
        builder.getSimulatedLeaseCollection().setFinal(false);
        l1.addExpected(Type.TOPOLOGY_CHANGING);
        VirtualInstanceBuilder builder2 = newBuilder();
        builder2.setDebugName("secondInstanceB")
                .useRepositoryOf(instance1)
                .setConnectorPingTimeout(3)
                .setMinEventDelay(3);
        instance2 = builder2.build();
        instance2.stopViewChecker();
//        instance2.stopVoting();
        
        logger.info("testOldView: instance2 created, now issuing one heartbeat with instance2 first, so that instance1 can take note of it");
        instance2.getViewChecker().checkView();
        OakDiscoveryService oakDisco1 = (OakDiscoveryService) instance2.getDiscoveryService();
        oakDisco1.checkForTopologyChange();
        logger.info("testOldView: now instance1 is also doing a heartbeat and should see that a new instance is there");
        instance1.getViewChecker().checkView();
        OakDiscoveryService oakDisco2 = (OakDiscoveryService) instance1.getDiscoveryService();
        oakDisco2.checkForTopologyChange();
        logger.info("testOldView: 500ms sleep...");
        Thread.sleep(500); // allow some time for CHANGING to be sent
        logger.info("testOldView: 500ms sleep done.");

        assertEquals(2, l1.getEvents().size()); // INIT and CHANGING
        assertEquals(0, l1.getRemainingExpectedCount()); // no remaining expected
        assertEquals(0, l1.getUnexpectedCount()); // and no unexpected
        t1 = instance1.getDiscoveryService().getTopology();
        assertFalse(t1.isCurrent()); // current it should not be
        assertEquals(1, t1.getInstances().size()); // but it should still contain the local instance from before
        
        builder.getSimulatedLeaseCollection().setFinal(true);
        l1.addExpected(Type.TOPOLOGY_CHANGED);
        logger.info("testOldView: now issuing 3 rounds of heartbeats/checks and expecting a TOPOLOGY_CHANGED then");

        instance2.heartbeatsAndCheckView();
//        instance2.analyzeVotings();
        instance1.heartbeatsAndCheckView();
//        instance1.analyzeVotings();
        Thread.sleep(1200);
        instance2.heartbeatsAndCheckView();
        instance1.heartbeatsAndCheckView();
        Thread.sleep(1200);
        instance2.heartbeatsAndCheckView();
        instance1.heartbeatsAndCheckView();
        Thread.sleep(1200);
        
        assertEquals(3, l1.getEvents().size()); // INIT, CHANGING and CHANGED
        assertEquals(0, l1.getRemainingExpectedCount()); // no remaining expected
        assertEquals(0, l1.getUnexpectedCount()); // and no unexpected
        t1 = instance1.getDiscoveryService().getTopology();
        assertTrue(t1.isCurrent()); // and we should be current again
        assertEquals(2, t1.getInstances().size()); // and contain both instances now
        
        // timeout is set to 3sec, so we now do heartbeats for 4sec with only instance1
        // to let instance2 crash
        builder.getSimulatedLeaseCollection().setFinal(false);
        l1.addExpected(Type.TOPOLOGY_CHANGING);
        for(int i=0; i<8; i++) {
            instance1.heartbeatsAndCheckView();
            Thread.sleep(500);
        }
        assertEquals(4, l1.getEvents().size()); // INIT, CHANGING, CHANGED and CHANGING
        assertEquals(0, l1.getRemainingExpectedCount()); // no remaining expected
        assertEquals(0, l1.getUnexpectedCount()); // and no unexpected
        t1 = instance1.getDiscoveryService().getTopology();
        assertFalse(t1.isCurrent()); // and we should be current again
        assertEquals(2, t1.getInstances().size()); // and contain both instances now
    }
    
}
