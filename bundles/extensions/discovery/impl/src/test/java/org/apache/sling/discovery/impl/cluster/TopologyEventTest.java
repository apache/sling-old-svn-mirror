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

import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
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
     * @throws Throwable 
     */
    @Test
    public void testDelayedInitEvent() throws Throwable {
        logger.info("testDelayedInitEvent: start");
        instance1 = Instance.newStandaloneInstance("/var/discovery/impl/", 
                "firstInstanceA", true, 20 /* heartbeat-timeout */, 3 /*min event delay*/,
                UUID.randomUUID().toString(), true /*delayInitEventUntilVoted*/);
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
                "secondInstanceB", instance1, false, 20, 3, UUID.randomUUID().toString(), true);
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
        
        // one heartbeat doesn't change the history yet
        logger.info("testDelayedInitEvent: an additional heartbeat shouldn't trigger any event for now");
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        assertEquals(1, l1.getEvents().size()); // one event
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(0, l2.getEvents().size());
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(1, l1Two.getEvents().size());
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
    
    /**
     * Tests the fact that the INIT event is NOT delayed (until voting has succeeded)
     * when the config is set accordingly (ie: config test).
     * @throws Throwable 
     */
    @Test
    public void testNonDelayedInitEvent() throws Throwable {
        logger.info("testNonDelayedInitEvent: start");
        instance1 = Instance.newStandaloneInstance("/var/discovery/impl/", 
                "firstInstanceB", true, 20 /* heartbeat-timeout */, 5 /*min event delay*/,
                UUID.randomUUID().toString(), false /*delayInitEventUntilVoted*/);
        AssertingTopologyEventListener l1 = new AssertingTopologyEventListener("instance1.l1") {
            private volatile boolean firstEvent = false;
            @Override
            public void handleTopologyEvent(TopologyEvent event) {
                super.handleTopologyEvent(event);
                if (firstEvent) {
                    // only handle the first event - that one must be INIT with isCurrent==false
                    assertFalse(event.getNewView().isCurrent());
                    firstEvent = false;
                }
            }
        };
        l1.addExpected(Type.TOPOLOGY_INIT);
        instance1.bindTopologyEventListener(l1);

        Thread.sleep(500); // SLING-4755: async event sending requires some minimal wait time nowadays
        
        // when delayInitEventUntilVoted is disabled, the INIT event is sent immediately
        assertEquals(1, l1.getEvents().size());
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(0, l1.getRemainingExpectedCount());

        instance1.runHeartbeatOnce();
        Thread.sleep(1000);
        // no further event now:
        assertEquals(1, l1.getEvents().size());
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(0, l1.getRemainingExpectedCount());
        
        instance1.runHeartbeatOnce();
        // still no further event, only INIT
        assertEquals(1, l1.getEvents().size());
        assertEquals(0, l1.getRemainingExpectedCount());
        assertEquals(0, l1.getUnexpectedCount());
        
        instance2 = Instance.newClusterInstance("/var/discovery/impl/", 
                "secondInstanceB", instance1, false, 20, 5, UUID.randomUUID().toString(), false);
        AssertingTopologyEventListener l2 = new AssertingTopologyEventListener("instance2.l2");
        l2.addExpected(Type.TOPOLOGY_INIT);
        instance2.bindTopologyEventListener(l2);
        AssertingTopologyEventListener l1Two = new AssertingTopologyEventListener("instance1.l1Two");
        l1Two.addExpected(Type.TOPOLOGY_INIT);
        instance1.bindTopologyEventListener(l1Two);
        
        Thread.sleep(500); // SLING-4755: async event sending requires some minimal wait time nowadays

        // just because instance2 is started doesn't kick off any events yet 
        // since instance2 didn't send heartbeats yet
        assertEquals(1, l1.getEvents().size()); // one event
        assertEquals(0, l1.getRemainingExpectedCount()); // the expected one
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(1, l2.getEvents().size());
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(1, l1Two.getEvents().size());
        assertEquals(0, l1Two.getRemainingExpectedCount()); // the expected one
        assertEquals(0, l1Two.getUnexpectedCount());
        
        // one heartbeat doesn't change the history yet
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        assertEquals(1, l1.getEvents().size()); // one event
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(1, l2.getEvents().size());
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(1, l1Two.getEvents().size());
        assertEquals(0, l1Two.getUnexpectedCount());
        
        // the second & third heartbeat though triggers the voting etc
        l1.addExpected(Type.TOPOLOGY_CHANGING);
        l1Two.addExpected(Type.TOPOLOGY_CHANGING);
        l2.addExpected(Type.TOPOLOGY_CHANGING);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(1500);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(1500);
        logger.info("testNonDelayedInitEvent: instance1: "+instance1.slingId);
        logger.info("testNonDelayedInitEvent: instance2: "+instance2.slingId);
        instance1.dumpRepo();
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(2, l1.getEvents().size());
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(2, l2.getEvents().size());
        assertEquals(0, l1Two.getUnexpectedCount());
        assertEquals(2, l1Two.getEvents().size());
        
        // now meanwhile - for SLING-4638 : register a listener 'late':
        // this one should get an INIT with a newView that has isCurrent()==false
        AssertingTopologyEventListener late = new AssertingTopologyEventListener("instance1.late") {
            @Override
            public void handleTopologyEvent(TopologyEvent event) {
                super.handleTopologyEvent(event);
                // also check if the newView has isCurrent==false
                assertFalse(event.getNewView().isCurrent());
                // plus lets now directly ask the discovery service for getTopology and check that
                assertFalse(instance1.getDiscoveryService().getTopology().isCurrent());
            }
        };
        late.addExpected(Type.TOPOLOGY_INIT);
        instance1.bindTopologyEventListener(late);

        // wait until CHANGED is sent - which is 3 sec after CHANGING
        l1.addExpected(Type.TOPOLOGY_CHANGED);
        l1Two.addExpected(Type.TOPOLOGY_CHANGED);
        l2.addExpected(Type.TOPOLOGY_CHANGED);
        Thread.sleep(4000);
        assertEquals(0, l1.getUnexpectedCount());
        assertEquals(3, l1.getEvents().size()); // one event
        assertEquals(0, l2.getUnexpectedCount());
        assertEquals(3, l2.getEvents().size());
        assertEquals(0, l1Two.getUnexpectedCount());
        assertEquals(3, l1Two.getEvents().size());
        logger.info("testNonDelayedInitEvent: end");
    }
    
}
