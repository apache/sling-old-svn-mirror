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
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.AssertingTopologyEventListener;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstance;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstanceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junitx.util.PrivateAccessor;

public class RepositoryDelaysTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Level logLevel;

    private FullJR2VirtualInstance instance1;

    private FullJR2VirtualInstance instance2;

    protected FullJR2VirtualInstanceBuilder newBuilder() {
        return new FullJR2VirtualInstanceBuilder();
    }

    @Before
    public void setUp() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.TRACE);        
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
     * SLING-5195 : simulate slow session.saves that block
     * the calling thread for non-trivial amounts of time,
     * typically for longer than the configured heartbeat
     * timeout
     */
    @Category(Slow.class) //TODO: test takes couple minutes!
    @Test
    public void testSlowSessionSaves() throws Exception {
        VirtualInstanceBuilder builder1 = newBuilder();
        instance1 = (FullJR2VirtualInstance) builder1
                .setDebugName("firstInstance")
                .newRepository("/var/discovery/impl/", true)
                .setMinEventDelay(0)
                .setConnectorPingInterval(1)
                .setConnectorPingTimeout(3)
                .build();
        VirtualInstanceBuilder builder2 = newBuilder();
        instance2 = (FullJR2VirtualInstance) builder2
                .setDebugName("secondInstance")
                .useRepositoryOf(instance1)
                .setMinEventDelay(0)
                .setConnectorPingInterval(1)
                .setConnectorPingTimeout(3)
                .build();
        
        instance1.setDelay("pre.commit", 12000);
        instance1.startViewChecker(1);
        instance2.startViewChecker(1);
        Thread.sleep(5000);
        // after 3 sec - without the 12sec pre-commit delay
        // the view would normally be established - but this time
        // round instance1 should still be pre-init and instance2
        // should be init but alone
        TopologyView t1 = instance1.getDiscoveryService().getTopology();
        assertFalse(t1.isCurrent());
        
        TopologyView t2 = instance2.getDiscoveryService().getTopology();
        assertTrue(t2.isCurrent());
        assertEquals(1, t2.getInstances().size());
        
        instance1.setDelay("pre.commit", -1);
        Thread.sleep(3000);

        TopologyView t1b = instance1.getDiscoveryService().getTopology();
        assertTrue(t1b.isCurrent());
        assertEquals(2, t1b.getInstances().size());
        
        TopologyView t2b = instance2.getDiscoveryService().getTopology();
        assertTrue(t2b.isCurrent());
        assertEquals(2, t2b.getInstances().size());
        
        instance1.setDelay("pre.commit", 59876);
        instance2.setDelay("pre.commit", 60000);
        logger.info("<main> both instances marked as delaying 1min - but with new background checks we should go changing within 3sec");
        Thread.sleep(8000);
        
        TopologyView t1c = instance1.getDiscoveryService().getTopology();
        assertFalse(t1c.isCurrent());
        
        TopologyView t2c = instance2.getDiscoveryService().getTopology();
        assertFalse(t2c.isCurrent());
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
        FullJR2VirtualInstanceBuilder builder = newBuilder();
        builder.setDebugName("firstInstanceA")
                .newRepository("/var/discovery/impl/", true)
                .setConnectorPingTimeout(3 /* heartbeat-timeout */)
                .setMinEventDelay(3 /*min event delay*/);
        instance1 = builder.fullBuild();
        instance1.stopVoting();
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
        l1.addExpected(Type.TOPOLOGY_CHANGING);
        FullJR2VirtualInstanceBuilder builder2 = newBuilder();
        builder2.setDebugName("secondInstanceB")
                .useRepositoryOf(instance1)
                .setConnectorPingTimeout(3)
                .setMinEventDelay(3);
        instance2 = builder2.fullBuild();
        instance2.stopVoting();
        
        logger.info("testOldView: instance2 created, now issuing one heartbeat with instance2 first, so that instance1 can take note of it");
        instance2.heartbeatsAndCheckView();
        logger.info("testOldView: now instance1 is also doing a heartbeat and should see that a new instance is there");
        instance1.heartbeatsAndCheckView();
        logger.info("testOldView: 500ms sleep...");
        Thread.sleep(500); // allow some time for CHANGING to be sent
        logger.info("testOldView: 500ms sleep done.");

        assertEquals(2, l1.getEvents().size()); // INIT and CHANGING
        assertEquals(0, l1.getRemainingExpectedCount()); // no remaining expected
        assertEquals(0, l1.getUnexpectedCount()); // and no unexpected
        t1 = instance1.getDiscoveryService().getTopology();
        assertFalse(t1.isCurrent()); // current it should not be
        assertEquals(1, t1.getInstances().size()); // but it should still contain the local instance from before
        
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
        
        // force instance1 to no longer analyze the votings
        // since stopVoting() only deactivates the listener, we also
        // have to set votingHandler of heartbeatHandler to null
        PrivateAccessor.setField(instance1.getHeartbeatHandler(), "votingHandler", null);
        l1.addExpected(Type.TOPOLOGY_CHANGING);
        for(int i=0; i<8; i++) {
            instance1.getHeartbeatHandler().heartbeatAndCheckView();
            Thread.sleep(500);
        }
        assertEquals(4, l1.getEvents().size()); // INIT, CHANGING, CHANGED and CHANGED
        assertEquals(0, l1.getRemainingExpectedCount()); // no remaining expected
        assertEquals(0, l1.getUnexpectedCount()); // and no unexpected
        t1 = instance1.getDiscoveryService().getTopology();
        assertFalse(t1.isCurrent()); // we should still be !current
        assertEquals(2, t1.getInstances().size()); // and contain both instances
    }
    
}
