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
package org.apache.sling.discovery.impl.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.cluster.helpers.AssertingTopologyEventListener;
import org.apache.sling.discovery.impl.setup.Instance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class covering correct sending of TopologyEvents
 * in various scenarios (which are not covered in other tests already).
 */
public class TopologyEventTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Instance instance1;
    private Instance instance2;

    private Level logLevel;
    
    @Before
    public void setup() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.DEBUG);
    }
    
    @After
    public void tearDown() throws Throwable {
        if (instance1!=null) {
            instance1.stopHeartbeats();
            instance1.stop();
            instance1 = null;
        }
        if (instance2!=null) {
            instance2.stopHeartbeats();
            instance2.stop();
            instance2 = null;
        }
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
    }
    
    /**
     * Tests the fact that the INIT event is delayed until voting has succeeded
     * (which is the default with SLIGN-5030 and SLING-4959
     * @throws Throwable 
     */
    @Test
    public void testDelayedInitEvent() throws Throwable {
        logger.info("testDelayedInitEvent: start");
        instance1 = Instance.newStandaloneInstance("/var/discovery/impl/", 
                "firstInstanceA", true, 20 /* heartbeat-timeout */, 3 /*min event delay*/,
                UUID.randomUUID().toString());
        AssertingTopologyEventListener l1 = new AssertingTopologyEventListener("instance1.l1");
        instance1.bindTopologyEventListener(l1);
        logger.info("testDelayedInitEvent: instance1 created, no events expected yet. slingId="+instance1.slingId);
        
        // should not have received any events yet
        assertEquals(0, l1.getEvents().size());
        assertEquals(0, l1.getUnexpectedCount());

        // one heartbeat doesn't make a day yet
        instance1.runHeartbeatOnce();
        Thread.sleep(1000);
        logger.info("testDelayedInitEvent: even after 500ms no events expected, as it needs more than 1 heartbeat");
        // should not have received any events yet
        assertEquals(0, l1.getEvents().size());
        assertEquals(0, l1.getUnexpectedCount());
        
        // but two are a good start
        l1.addExpected(Type.TOPOLOGY_INIT);
        instance1.runHeartbeatOnce();
        Thread.sleep(1000);
        instance1.runHeartbeatOnce();
        Thread.sleep(1000);
        logger.info("testDelayedInitEvent: 2nd/3rd heartbeat sent - now expecting a TOPOLOGY_INIT");
        instance1.dumpRepo();
        assertEquals(1, l1.getEvents().size()); // one event
        assertEquals(0, l1.getRemainingExpectedCount()); // the expected one
        assertEquals(0, l1.getUnexpectedCount());
        
        logger.info("testDelayedInitEvent: creating instance2");
        instance2 = Instance.newClusterInstance("/var/discovery/impl/", 
                "secondInstanceB", instance1, false, 20, 3, UUID.randomUUID().toString());
        logger.info("testDelayedInitEvent: instance2 created with slingId="+instance2.slingId);
        AssertingTopologyEventListener l2 = new AssertingTopologyEventListener("instance2.l2");
        instance2.bindTopologyEventListener(l2);
        logger.info("testDelayedInitEvent: listener instance2.l2 added - it should not get any events though");
        AssertingTopologyEventListener l1Two = new AssertingTopologyEventListener("instance1.l1Two");
        l1Two.addExpected(Type.TOPOLOGY_INIT);
        logger.info("testDelayedInitEvent: listener instance1.l1Two added - it expects an INIT now");
        instance1.bindTopologyEventListener(l1Two);
        
        Thread.sleep(500); // SLING-4755: async event sending requires some minimal wait time nowadays

        // just because instance2 is started doesn't kick off any events yet 
        // since instance2 didn't send heartbeats yet
        assertEquals(1, l1.getEvents().size()); // one event
        assertEquals(0, l1.getRemainingExpectedCount()); // the expected one
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(0, l2.getEvents().size());
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(1, l1Two.getEvents().size());
        assertEquals(0, l1Two.getRemainingExpectedCount()); // the expected one
        assertEquals(0, l1Two.getUnexpectedCount());
        
        
        // the second & third heartbeat though triggers the voting etc
        logger.info("testDelayedInitEvent: two more heartbeats should trigger events");
        l1.addExpected(Type.TOPOLOGY_CHANGING);
        l1Two.addExpected(Type.TOPOLOGY_CHANGING);
        Thread.sleep(500);
        l2.addExpected(Type.TOPOLOGY_INIT);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        logger.info("testDelayedInitEvent: instance1: "+instance1.slingId);
        logger.info("testDelayedInitEvent: instance2: "+instance2.slingId);
        instance1.dumpRepo();
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(2, l1.getEvents().size());
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(1, l2.getEvents().size());
        assertEquals(0, l1Two.getUnexpectedCount());
        assertEquals(2, l1Two.getEvents().size());

        // wait until CHANGED is sent - which is 3 sec after CHANGING
        l1.addExpected(Type.TOPOLOGY_CHANGED);
        l1Two.addExpected(Type.TOPOLOGY_CHANGED);
        Thread.sleep(4000);
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(3, l1.getEvents().size()); // one event
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(1, l2.getEvents().size());
        assertEquals(0, l1Two.getUnexpectedCount());
        assertEquals(3, l1Two.getEvents().size());
        logger.info("testDelayedInitEvent: end");
    }
    
    @Test
    public void testGetDuringDelay() throws Throwable {
        instance1 = Instance.newStandaloneInstance("/var/discovery/impl/", 
                "firstInstanceA", true, 20 /* heartbeat-timeout */, 6 /*min event delay*/,
                UUID.randomUUID().toString());
        AssertingTopologyEventListener l1 = new AssertingTopologyEventListener("instance1.l1");
        l1.addExpected(TopologyEvent.Type.TOPOLOGY_INIT);
        instance1.bindTopologyEventListener(l1);
        
        TopologyView earlyTopo = instance1.getDiscoveryService().getTopology();
        assertNotNull(earlyTopo);
        assertFalse(earlyTopo.isCurrent());
        assertEquals(1, earlyTopo.getInstances().size());
        
        for(int i=0; i<4; i++) {
            instance1.runHeartbeatOnce();
            Thread.sleep(125);
        }
        TopologyView secondTopo = instance1.getDiscoveryService().getTopology();
        assertEquals(1, secondTopo.getInstances().size());
        assertEquals(instance1.getSlingId(), secondTopo.getInstances().iterator().next().getSlingId());
        assertTrue(secondTopo.isCurrent());
        instance1.dumpRepo();
        assertEquals(earlyTopo.getLocalInstance().getClusterView().getId(),
                secondTopo.getLocalInstance().getClusterView().getId());

        Thread.sleep(500);
        // should have gotten the INIT, hence 0 remaining expected events
        assertEquals(0, l1.getRemainingExpectedCount());
        assertEquals(0, l1.getUnexpectedCount());
        
        l1.addExpected(TopologyEvent.Type.TOPOLOGY_CHANGING);
        instance2 = Instance.newClusterInstance("/var/discovery/impl/", 
                "secondInstanceB", instance1, false, 20, 1, UUID.randomUUID().toString());
        AssertingTopologyEventListener l2 = new AssertingTopologyEventListener("instance2.l1");
        l2.addExpected(TopologyEvent.Type.TOPOLOGY_INIT);
        instance2.bindTopologyEventListener(l2);

        for(int i=0; i<4; i++) {
            instance2.runHeartbeatOnce();
            instance1.runHeartbeatOnce();
            Thread.sleep(750);
        }
        
        assertEquals(0, l1.getUnexpectedCount());
        TopologyView topo2 = instance2.getDiscoveryService().getTopology();
        assertTrue(topo2.isCurrent());
        assertEquals(2, topo2.getInstances().size());
        TopologyView topo1 = instance1.getDiscoveryService().getTopology();
        assertTrue(topo1.isCurrent());
        assertEquals(2, topo1.getInstances().size());
        
        l1.addExpected(TopologyEvent.Type.TOPOLOGY_CHANGED);
        Thread.sleep(5000);
        assertEquals(0, l1.getRemainingExpectedCount());
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(0, l2.getRemainingExpectedCount());
        assertEquals(0, l2.getUnexpectedCount());
        assertTrue(instance2.getDiscoveryService().getTopology().isCurrent());
        assertEquals(2, instance2.getDiscoveryService().getTopology().getInstances().size());
        assertTrue(instance1.getDiscoveryService().getTopology().isCurrent());
        assertEquals(2, instance1.getDiscoveryService().getTopology().getInstances().size());
    }
    
}