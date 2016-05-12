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
package org.apache.sling.discovery.impl.common.heartbeat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Property;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.junit.categories.Slow;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHelper;
import org.apache.sling.discovery.impl.cluster.voting.VotingView;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstance;
import org.apache.sling.discovery.impl.setup.FullJR2VirtualInstanceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junitx.util.PrivateAccessor;

public class HeartbeatTest {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    class SimpleTopologyEventListener implements TopologyEventListener {

        private List<TopologyEvent> events = new LinkedList<TopologyEvent>();
        private TopologyEvent lastEvent;
        private int eventCount;
        private final String name;

        public SimpleTopologyEventListener(String name) {
            this.name = name;
        }
        
        @Override
        public void handleTopologyEvent(TopologyEvent event) {
            events.add(event);
            String msg = event.toString();
            TopologyView newView = event.getNewView();
            switch(event.getType()) {
                case PROPERTIES_CHANGED: {
                    msg = String.valueOf(TopologyEvent.Type.PROPERTIES_CHANGED);
                    break;
                }
                case TOPOLOGY_CHANGED: {
                    msg = TopologyEvent.Type.TOPOLOGY_CHANGED + ", newView contains "+newView.getInstances().size()+", newView.isCurrent="+newView.isCurrent();
                    break;
                }
                case TOPOLOGY_CHANGING: {
                    msg = TopologyEvent.Type.TOPOLOGY_CHANGING + ", oldView contained "+event.getOldView().getInstances().size()+", oldView.isCurrent="+event.getOldView().isCurrent();
                    break;
                }
                case TOPOLOGY_INIT: {
                    if (newView==null) {
                        msg = TopologyEvent.Type.TOPOLOGY_INIT + ", newView contains null: "+newView;
                    } else if (newView.getInstances()==null) {
                        msg = TopologyEvent.Type.TOPOLOGY_INIT + ", newView contains no instances:"+newView+", newView.isCurrent="+newView.isCurrent();
                    } else {
                        msg = TopologyEvent.Type.TOPOLOGY_INIT + ", newView contains "+newView.getInstances().size()+", newView.isCurrent="+newView.isCurrent();
                    }
                    break;
                }
            }
            LoggerFactory.getLogger(this.getClass()).info("handleTopologyEvent["+name+"]: "+msg);
            lastEvent = event;
            eventCount++;
        }
        
        public int getEventCount() {
            return eventCount;
        }
        
        public TopologyEvent getLastEvent() {
            return lastEvent;
        }
        
    }
    
    Set<VirtualInstance> instances = new HashSet<VirtualInstance>();
    private Level logLevel;
    
    @Before
    public void setup() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.TRACE);
    }
    
    @After
    public void tearDown() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);

        Iterator<VirtualInstance> it = instances.iterator();
        while(it.hasNext()) {
            VirtualInstance i = it.next();
            i.stop();
        }
    }

    @Category(Slow.class) //TODO: takes env 40sec
    @Test
    public void testPartitioning() throws Throwable {
        doTestPartitioning(true);
    }
    
    @Category(Slow.class) //TODO: takes env 35sec
    @Test
    public void testPartitioningWithFailingScheduler() throws Throwable {
        doTestPartitioning(false);
    }
    
    public FullJR2VirtualInstanceBuilder newBuilder() {
        return new FullJR2VirtualInstanceBuilder();
    }
    
    public void doTestPartitioning(boolean scheduler) throws Throwable {
        logger.info("doTestPartitioning: creating slowMachine...");
        FullJR2VirtualInstanceBuilder builder = newBuilder();
        builder.setDebugName("slow")
                .newRepository("/var/discovery/impl/", true)
                .setConnectorPingTimeout(10 /* 10 sec timeout */)
                .setConnectorPingInterval(999 /* 999 sec interval: to disable it */)
                .setMinEventDelay(0)
                .withFailingScheduler(!scheduler);
        FullJR2VirtualInstance slowMachine = builder.fullBuild();
        assertEquals(1, slowMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(slowMachine.getSlingId(), slowMachine.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(slowMachine);
        Thread.sleep(10); // wait 10ms to ensure 'slowMachine' has the lowerst leaderElectionId (to become leader)
        SimpleTopologyEventListener slowListener = new SimpleTopologyEventListener("slow");
        slowMachine.bindTopologyEventListener(slowListener);
        
        logger.info("doTestPartitioning: creating fastMachine1...");
        FullJR2VirtualInstanceBuilder fastBuilder1 = newBuilder();
        fastBuilder1.setDebugName("fast1")
                .useRepositoryOf(slowMachine)
                .setConnectorPingTimeout(10)
                .setConnectorPingInterval(1)
                .setMinEventDelay(0)
                .withFailingScheduler(!scheduler);
        FullJR2VirtualInstance fastMachine1 = fastBuilder1.fullBuild();
        assertEquals(1, fastMachine1.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine1.getSlingId(), fastMachine1.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine1);
        Thread.sleep(10); // wait 10ms to ensure 'fastMachine1' has the 2nd lowerst leaderElectionId (to become leader during partitioning)
        SimpleTopologyEventListener fastListener1 = new SimpleTopologyEventListener("fast1");
        fastMachine1.bindTopologyEventListener(fastListener1);

        logger.info("doTestPartitioning: creating fastMachine2...");
        FullJR2VirtualInstanceBuilder fullBuilder2 = newBuilder();
        fullBuilder2.setDebugName("fast2")
                .useRepositoryOf(slowMachine)
                .setConnectorPingTimeout(10)
                .setConnectorPingInterval(1)
                .setMinEventDelay(0)
                .withFailingScheduler(!scheduler);
        FullJR2VirtualInstance fastMachine2 = fullBuilder2.fullBuild();
        assertEquals(1, fastMachine2.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine2.getSlingId(), fastMachine2.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine2);
        SimpleTopologyEventListener fastListener2 = new SimpleTopologyEventListener("fast2");
        fastMachine2.bindTopologyEventListener(fastListener2);

        logger.info("doTestPartitioning: creating fastMachine3...");
        FullJR2VirtualInstanceBuilder fullBuilder3 = newBuilder();
        fullBuilder3.setDebugName("fast3")
                .useRepositoryOf(slowMachine)
                .setConnectorPingTimeout(10)
                .setConnectorPingInterval(1)
                .setMinEventDelay(0)
                .withFailingScheduler(!scheduler);
        FullJR2VirtualInstance fastMachine3 = fullBuilder3.fullBuild();
        assertEquals(1, fastMachine3.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine3.getSlingId(), fastMachine3.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine3);
        SimpleTopologyEventListener fastListener3 = new SimpleTopologyEventListener("fast3");
        fastMachine3.bindTopologyEventListener(fastListener3);

        logger.info("doTestPartitioning: creating fastMachine4...");
        FullJR2VirtualInstanceBuilder fullBuilder4 = newBuilder();
        fullBuilder4.setDebugName("fast4")
                .useRepositoryOf(slowMachine)
                .setConnectorPingTimeout(10)
                .setConnectorPingInterval(1)
                .setMinEventDelay(0)
                .withFailingScheduler(!scheduler);
        FullJR2VirtualInstance fastMachine4 = fullBuilder4.fullBuild();
        assertEquals(1, fastMachine4.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine4.getSlingId(), fastMachine4.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine4);
        SimpleTopologyEventListener fastListener4 = new SimpleTopologyEventListener("fast4");
        fastMachine4.bindTopologyEventListener(fastListener4);
        
        logger.info("doTestPartitioning: --------------------------------");
        logger.info("doTestPartitioning: letting heartbeats be sent by all instances for a few loops...");
        logger.info("doTestPartitioning: --------------------------------");
        HeartbeatHandler hhSlow = slowMachine.getHeartbeatHandler();
        for(int i=0; i<5; i++) {
            logger.info("doTestPartitioning: --------------------------------");
            logger.info("doTestPartitioning: doing pinging with hhSlow now...");
            logger.info("doTestPartitioning: --------------------------------");
            synchronized(lock(hhSlow)) {
                hhSlow.issueHeartbeat();
                hhSlow.doCheckView();
            }
            if (!scheduler) {
                logger.info("doTestPartitioning: --------------------------------");
                logger.info("doTestPartitioning: doing pinging with fastMachine1 now...");
                logger.info("doTestPartitioning: --------------------------------");
                synchronized(lock(fastMachine1.getHeartbeatHandler())) {
                    fastMachine1.getHeartbeatHandler().issueHeartbeat();
                    fastMachine1.getHeartbeatHandler().doCheckView();
                }
                logger.info("doTestPartitioning: --------------------------------");
                logger.info("doTestPartitioning: doing pinging with fastMachine2 now...");
                logger.info("doTestPartitioning: --------------------------------");
                synchronized(lock(fastMachine2.getHeartbeatHandler())) {
                    fastMachine2.getHeartbeatHandler().issueHeartbeat();
                    fastMachine2.getHeartbeatHandler().doCheckView();
                }
                logger.info("doTestPartitioning: --------------------------------");
                logger.info("doTestPartitioning: doing pinging with fastMachine3 now...");
                logger.info("doTestPartitioning: --------------------------------");
                synchronized(lock(fastMachine3.getHeartbeatHandler())) {
                    fastMachine3.getHeartbeatHandler().issueHeartbeat();
                    fastMachine3.getHeartbeatHandler().doCheckView();
                }
                logger.info("doTestPartitioning: --------------------------------");
                logger.info("doTestPartitioning: doing pinging with fastMachine4 now...");
                logger.info("doTestPartitioning: --------------------------------");
                synchronized(lock(fastMachine4.getHeartbeatHandler())) {
                    fastMachine4.getHeartbeatHandler().issueHeartbeat();
                    fastMachine4.getHeartbeatHandler().doCheckView();
                }
            }
            Thread.sleep(1000);
        }
        
        // at this stage the 4 fast plus the slow instance should all see each other
        logger.info("doTestPartitioning: all 4 instances should have agreed on seeing each other");
        assertNotNull(fastListener1.getLastEvent());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, fastListener1.getLastEvent().getType());
        assertEquals(5, fastListener1.getLastEvent().getNewView().getInstances().size());
        assertFalse(fastListener1.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertNotNull(fastListener2.getLastEvent());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, fastListener2.getLastEvent().getType());
        assertEquals(5, fastListener2.getLastEvent().getNewView().getInstances().size());
        assertFalse(fastListener2.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertNotNull(fastListener3.getLastEvent());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, fastListener3.getLastEvent().getType());
        assertEquals(5, fastListener3.getLastEvent().getNewView().getInstances().size());
        assertFalse(fastListener3.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertNotNull(fastListener4.getLastEvent());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, fastListener4.getLastEvent().getType());
        assertEquals(5, fastListener4.getLastEvent().getNewView().getInstances().size());
        assertFalse(fastListener4.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertNotNull(slowListener.getLastEvent());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, slowListener.getLastEvent().getType());
        assertEquals(5, slowListener.getLastEvent().getNewView().getInstances().size());
        assertTrue(slowListener.getLastEvent().getNewView().getLocalInstance().isLeader());
        
        // after 12sec the slow instance' heartbeat should have timed out
        logger.info("doTestPartitioning: letting slowMachine NOT send any heartbeats for 12sec, only the fast ones do...");
        for(int i=0; i<12; i++) {
            if (!scheduler) {
                synchronized(lock(fastMachine1.getHeartbeatHandler())) {
                    fastMachine1.getHeartbeatHandler().issueHeartbeat();
                    fastMachine1.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine2.getHeartbeatHandler())) {
                    fastMachine2.getHeartbeatHandler().issueHeartbeat();
                    fastMachine2.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine3.getHeartbeatHandler())) {
                    fastMachine3.getHeartbeatHandler().issueHeartbeat();
                    fastMachine3.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine4.getHeartbeatHandler())) {
                    fastMachine4.getHeartbeatHandler().issueHeartbeat();
                    fastMachine4.getHeartbeatHandler().doCheckView();
                }
            }
            Thread.sleep(1000);
        }
        logger.info("doTestPartitioning: this should now have decoupled slowMachine from the other 4...");
        
        // so the fast listeners should only see 4 instances remaining
        for(int i=0; i<7; i++) {
            logger.info("doTestPartitioning: sleeping 2sec...");
            Thread.sleep(2000);
            logger.info("doTestPartitioning: the 4 fast machines should all just see themselves...");
            assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener1.getLastEvent().getType());
            assertEquals(4, fastListener1.getLastEvent().getNewView().getInstances().size());
            assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener2.getLastEvent().getType());
            assertEquals(4, fastListener2.getLastEvent().getNewView().getInstances().size());
            assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener3.getLastEvent().getType());
            assertEquals(4, fastListener3.getLastEvent().getNewView().getInstances().size());
            assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener4.getLastEvent().getType());
            assertEquals(4, fastListener4.getLastEvent().getNewView().getInstances().size());
            
            assertTrue(fastListener1.getLastEvent().getNewView().getLocalInstance().isLeader());
            assertFalse(fastListener2.getLastEvent().getNewView().getLocalInstance().isLeader());
            assertFalse(fastListener3.getLastEvent().getNewView().getLocalInstance().isLeader());
            assertFalse(fastListener4.getLastEvent().getNewView().getLocalInstance().isLeader());

            // and the slow instance should be isolated
            assertFalse(slowMachine.getDiscoveryService().getTopology().isCurrent());
            // however we can't really make any assertions on how many instances a non-current topology contains...
            // assertEquals(5, slowMachine.getDiscoveryService().getTopology().getInstances().size());
            if (i==0) {
                assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, slowListener.getLastEvent().getType());
            } else {
                assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, slowListener.getLastEvent().getType());
            }
            //TODO but only after 'handlePotentialTopologyChange' is called
            // which either happens via handleTopologyChanged (via the TopologyChangeHandler)
            // or via updateProperties
            DiscoveryServiceImpl slowDisco = (DiscoveryServiceImpl) slowMachine.getDiscoveryService();
            slowDisco.updateProperties();
            // that should have triggered an async event - which takes a little moment
            Thread.sleep(500);
            assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, slowListener.getLastEvent().getType());
            assertEquals(2, slowListener.getEventCount());
            TopologyView slowTopo = slowMachine.getDiscoveryService().getTopology();
            assertNotNull(slowTopo);
            assertFalse(slowTopo.isCurrent());
            // again, can't make any assertion on how many instances the slow non-current view contains...
            //assertEquals(5, slowTopo.getInstances().size());
            if (!scheduler) {
                synchronized(lock(fastMachine1.getHeartbeatHandler())) {
                    fastMachine1.getHeartbeatHandler().issueHeartbeat();
                    fastMachine1.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine2.getHeartbeatHandler())) {
                    fastMachine2.getHeartbeatHandler().issueHeartbeat();
                    fastMachine2.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine3.getHeartbeatHandler())) {
                    fastMachine3.getHeartbeatHandler().issueHeartbeat();
                    fastMachine3.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine4.getHeartbeatHandler())) {
                    fastMachine4.getHeartbeatHandler().issueHeartbeat();
                    fastMachine4.getHeartbeatHandler().doCheckView();
                }
            }
        }

        for(int i=0; i<4; i++) {
            hhSlow.issueHeartbeat();
            hhSlow.doCheckView();
            if (!scheduler) {
                synchronized(lock(fastMachine1.getHeartbeatHandler())) {
                    fastMachine1.getHeartbeatHandler().issueHeartbeat();
                    fastMachine1.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine2.getHeartbeatHandler())) {
                    fastMachine2.getHeartbeatHandler().issueHeartbeat();
                    fastMachine2.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine3.getHeartbeatHandler())) {
                    fastMachine3.getHeartbeatHandler().issueHeartbeat();
                    fastMachine3.getHeartbeatHandler().doCheckView();
                }
                synchronized(lock(fastMachine4.getHeartbeatHandler())) {
                    fastMachine4.getHeartbeatHandler().issueHeartbeat();
                    fastMachine4.getHeartbeatHandler().doCheckView();
                }
            }
            Thread.sleep(1000);
        }
        
        // now all should be in one cluster again
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener1.getLastEvent().getType());
        assertEquals(5, fastListener1.getLastEvent().getNewView().getInstances().size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener2.getLastEvent().getType());
        assertEquals(5, fastListener2.getLastEvent().getNewView().getInstances().size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener3.getLastEvent().getType());
        assertEquals(5, fastListener3.getLastEvent().getNewView().getInstances().size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener4.getLastEvent().getType());
        assertEquals(5, fastListener4.getLastEvent().getNewView().getInstances().size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, slowListener.getLastEvent().getType());
        assertEquals(5, slowListener.getLastEvent().getNewView().getInstances().size());
        
        // SLING-5030 part 2 : after rejoin-after-partitioning the slowMachine1 should again be leader
        slowMachine.dumpRepo();
        assertFalse(slowListener.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertTrue(fastListener1.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertFalse(fastListener2.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertFalse(fastListener3.getLastEvent().getNewView().getLocalInstance().isLeader());
        assertFalse(fastListener4.getLastEvent().getNewView().getLocalInstance().isLeader());
    }
    
    /**
     * This tests the case where one machine is slow with sending heartbeats
     * and should thus trigger the second, fast machine to kick it out of the topology.
     * But the slow one should also get a TOPOLOGY_CHANGING but just not get a
     * TOPOLOGY_CHANGED until it finally sends heartbeats again and the voting can 
     * happen again.
     */
    @Category(Slow.class) //TODO: takes env 25sec
    @Test
    public void testSlowAndFastMachine() throws Throwable {
        doTestSlowAndFastMachine(false);
    }

    @Category(Slow.class) //TODO: takes env 25sec
    @Test
    public void testSlowAndFastMachineWithFailingScheduler() throws Throwable {
        doTestSlowAndFastMachine(true);
    }

    public void doTestSlowAndFastMachine(boolean withFailingScheduler) throws Throwable {
        logger.info("doTestSlowAndFastMachine: creating slowMachine... (w/o heartbeat runner)");
        FullJR2VirtualInstanceBuilder slowBuilder = newBuilder();
        slowBuilder.setDebugName("slow")
                .newRepository("/var/discovery/impl/", true)
                .setConnectorPingTimeout(5 /*5 sec timeout */)
                .setConnectorPingInterval(999 /* 999sec interval: to disable it */)
                .setMinEventDelay(0)
                .withFailingScheduler(withFailingScheduler);
        FullJR2VirtualInstance slowMachine = slowBuilder.fullBuild();
        instances.add(slowMachine);
        SimpleTopologyEventListener slowListener = new SimpleTopologyEventListener("slow");
        slowMachine.bindTopologyEventListener(slowListener);
        logger.info("doTestSlowAndFastMachine: creating fastMachine... (w/o heartbeat runner)");
        FullJR2VirtualInstanceBuilder fastBuilder = newBuilder();
        fastBuilder.setDebugName("fast")
                .useRepositoryOf(slowMachine)
                .setConnectorPingTimeout(5)
                .setConnectorPingInterval(999)
                .setMinEventDelay(0)
                .withFailingScheduler(withFailingScheduler);
        FullJR2VirtualInstance fastMachine = fastBuilder.fullBuild();
        instances.add(fastMachine);
        SimpleTopologyEventListener fastListener = new SimpleTopologyEventListener("fast");
        fastMachine.bindTopologyEventListener(fastListener);
        HeartbeatHandler hhSlow = slowMachine.getHeartbeatHandler();
        HeartbeatHandler hhFast = fastMachine.getHeartbeatHandler();
        
        Thread.sleep(1000);
        logger.info("doTestSlowAndFastMachine: no event should have been triggered yet");
        assertFalse(fastMachine.getDiscoveryService().getTopology().isCurrent());
        assertFalse(slowMachine.getDiscoveryService().getTopology().isCurrent());
        assertNull(fastListener.getLastEvent());
        assertNull(slowListener.getLastEvent());

        // make few rounds of heartbeats so that the two instances see each other
        logger.info("doTestSlowAndFastMachine: send a couple of heartbeats to connect the two..");
        for(int i=0; i<5; i++) {
            synchronized(lock(hhSlow)) {
                hhSlow.issueHeartbeat();
                hhSlow.doCheckView();
            }
            synchronized(lock(hhFast)) {
                hhFast.issueHeartbeat();
                hhFast.doCheckView();
            }
            Thread.sleep(100);
        }
        logger.info("doTestSlowAndFastMachine: now the two instances should be connected.");
        slowMachine.dumpRepo();
        
        assertEquals(2, slowMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(2, fastMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, fastListener.getLastEvent().getType());
        assertEquals(1, fastListener.getEventCount());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, slowListener.getLastEvent().getType());
        assertEquals(1, slowListener.getEventCount());
        
        // now let the slow machine be slow while the fast one updates as expected
        logger.info("doTestSlowAndFastMachine: last heartbeat of slowMachine.");
        synchronized(lock(hhSlow)) {
            hhSlow.issueHeartbeat();
        }
        logger.info("doTestSlowAndFastMachine: while the fastMachine still sends heartbeats...");
        for(int i=0; i<6; i++) {
            Thread.sleep(1500);
            synchronized(lock(hhFast)) {
                hhFast.issueHeartbeat();
                hhFast.doCheckView();
            }
        }
        logger.info("doTestSlowAndFastMachine: now the fastMachine should have decoupled the slow one");
        fastMachine.dumpRepo();
        hhFast.doCheckView(); // one more for the start of the vote
        fastMachine.dumpRepo();
        hhFast.doCheckView(); // and one for the promotion

        // after 9 sec hhSlow's heartbeat will have timed out, so hhFast will not see hhSlow anymore
        fastMachine.dumpRepo();
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener.getLastEvent().getType());
        assertEquals(3, fastListener.getEventCount());
        assertEquals(1, fastMachine.getDiscoveryService().getTopology().getInstances().size());
        
        TopologyView topo = slowMachine.getDiscoveryService().getTopology();
        assertFalse(topo.isCurrent());
        
        // after those 6 sec, hhSlow does the check (6sec between heartbeat and check)
        logger.info("doTestSlowAndFastMachine: slowMachine is going to do a checkView next - and will detect being decoupled");
        hhSlow.doCheckView();
        slowMachine.dumpRepo();
        logger.info("doTestSlowAndFastMachine: slowMachine is going to also do a heartbeat next");
        synchronized(lock(hhSlow)) {
            hhSlow.issueHeartbeat();
        }
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener.getLastEvent().getType());
        assertEquals(3, fastListener.getEventCount());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, slowListener.getLastEvent().getType());
        assertEquals(2, slowListener.getEventCount());
        Thread.sleep(8000);
        // even after 8 sec the slow lsitener did not send a TOPOLOGY_CHANGED yet
        logger.info("doTestSlowAndFastMachine: after another 8 sec of silence from slowMachine, it should still remain in CHANGING state");
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, slowListener.getLastEvent().getType());
        assertFalse(slowMachine.getDiscoveryService().getTopology().isCurrent());
        assertEquals(2, slowListener.getEventCount());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener.getLastEvent().getType());
        assertEquals(1, fastMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(3, fastListener.getEventCount());
        
        // make few rounds of heartbeats so that the two instances see each other again
        logger.info("doTestSlowAndFastMachine: now let both fast and slow issue heartbeats...");
        for(int i=0; i<4; i++) {
            synchronized(lock(hhFast)) {
                hhFast.issueHeartbeat();
                hhFast.doCheckView();
            }
            synchronized(lock(hhSlow)) {
                hhSlow.issueHeartbeat();
                hhSlow.doCheckView();
            }
            Thread.sleep(1000);
        }
        logger.info("doTestSlowAndFastMachine: by now the two should have joined");
        
        // this should have put the two together again
        // even after 8 sec the slow lsitener did not send a TOPOLOGY_CHANGED yet
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener.getLastEvent().getType());
        assertTrue(fastMachine.getDiscoveryService().getTopology().isCurrent());
        assertEquals(2, fastMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(5, fastListener.getEventCount());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, slowListener.getLastEvent().getType());
        assertTrue(slowMachine.getDiscoveryService().getTopology().isCurrent());
        assertEquals(2, slowMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(3, slowListener.getEventCount());
    }
    
    private Object lock(HeartbeatHandler heartbeatHandler) throws NoSuchFieldException {
        //TODO: refactor HeartbeatHandler to provide such a synchronized method
        // rather having the test rely on this
        return PrivateAccessor.getField(heartbeatHandler, "lock");
    }

    /**
     * SLING-5027 : test to reproduce the voting loop
     * (and verify that it's fixed)
     */
    @Test
    public void testVotingLoop() throws Throwable {
        logger.info("testVotingLoop: creating slowMachine1...");
        FullJR2VirtualInstanceBuilder slowBuilder1 = newBuilder();
        slowBuilder1.setDebugName("slow1")
            .newRepository("/var/discovery/impl/", true)
            .setConnectorPingTimeout(600 /* 600 sec timeout */)
            .setConnectorPingInterval(999 /* 999 sec interval: to disable it */)
            .setMinEventDelay(0);
        FullJR2VirtualInstance slowMachine1 = slowBuilder1.fullBuild();
        instances.add(slowMachine1);
        SimpleTopologyEventListener slowListener1 = new SimpleTopologyEventListener("slow1");
        slowMachine1.bindTopologyEventListener(slowListener1);
        
        logger.info("testVotingLoop: creating slowMachine2...");
        FullJR2VirtualInstanceBuilder slowBuilder2 = newBuilder();
        slowBuilder2.setDebugName("slow2")
            .useRepositoryOf(slowMachine1)
            .setConnectorPingTimeout(600 /* 600 sec timeout */)
            .setConnectorPingInterval(999 /* 999 sec interval: to disable it */)
            .setMinEventDelay(0);
        FullJR2VirtualInstance slowMachine2 = slowBuilder2.fullBuild();
        instances.add(slowMachine2);
        SimpleTopologyEventListener slowListener2 = new SimpleTopologyEventListener("slow2");
        slowMachine2.bindTopologyEventListener(slowListener2);
        
        logger.info("testVotingLoop: creating fastMachine...");
        FullJR2VirtualInstanceBuilder fastBuilder = newBuilder();
        fastBuilder.setDebugName("fast")
            .useRepositoryOf(slowMachine1)
            .setConnectorPingTimeout(600 /* 600 sec timeout */)
            .setConnectorPingInterval(999 /* 999 sec interval: to disable it */)
            .setMinEventDelay(0);
        FullJR2VirtualInstance fastMachine = fastBuilder.fullBuild();
        instances.add(fastMachine);
        SimpleTopologyEventListener fastListener = new SimpleTopologyEventListener("fast");
        fastMachine.bindTopologyEventListener(fastListener);
        HeartbeatHandler hhSlow1 = slowMachine1.getHeartbeatHandler();
        HeartbeatHandler hhSlow2 = slowMachine2.getHeartbeatHandler();
        HeartbeatHandler hhFast = fastMachine.getHeartbeatHandler();
        
        Thread.sleep(1000);
        logger.info("testVotingLoop: after some initial 1sec sleep no event should yet have been sent");
        assertFalse(fastMachine.getDiscoveryService().getTopology().isCurrent());
        assertFalse(slowMachine1.getDiscoveryService().getTopology().isCurrent());
        assertFalse(slowMachine2.getDiscoveryService().getTopology().isCurrent());
        assertNull(fastListener.getLastEvent());
        assertNull(slowListener1.getLastEvent());
        assertNull(slowListener2.getLastEvent());

        // prevent the slow machine from voting
        logger.info("testVotingLoop: stopping voting of slowMachine1...");
        slowMachine1.stopVoting();
        
        // now let all issue a heartbeat
        logger.info("testVotingLoop: letting slow1, slow2 and fast all issue 1 heartbeat");
        hhSlow1.issueHeartbeat();
        hhSlow2.issueHeartbeat();
        hhFast.issueHeartbeat();

        // now let the fast one start a new voting, to which
        // only the fast one will vote, the slow one doesn't.
        // that will cause a voting loop
        logger.info("testVotingLoop: let the fast one do a checkView, thus initiate a voting");
        hhFast.doCheckView();
        
        Calendar previousVotedAt = null;
        for(int i=0; i<5; i++) {
            logger.info("testVotingLoop: sleeping 1sec...");
            Thread.sleep(1000);
            logger.info("testVotingLoop: check to see that there is no voting loop...");
            // now check the ongoing votings
            ResourceResolverFactory factory = fastMachine.getResourceResolverFactory();
            ResourceResolver resourceResolver = factory
                    .getAdministrativeResourceResolver(null);
            try{
                List<VotingView> ongoingVotings = 
                        VotingHelper.listOpenNonWinningVotings(resourceResolver, fastMachine.getFullConfig());
                assertNotNull(ongoingVotings);
                assertEquals(1, ongoingVotings.size());
                VotingView ongoingVote = ongoingVotings.get(0);
                assertFalse(ongoingVote.isWinning());
                assertFalse(ongoingVote.hasVotedYes(slowMachine1.getSlingId()));
                assertTrue(ongoingVote.hasVotedYes(slowMachine2.getSlingId()));
                final Resource memberResource = ongoingVote.getResource().getChild("members").getChild(slowMachine2.getSlingId());
                final ModifiableValueMap memberMap = memberResource.adaptTo(ModifiableValueMap.class);
                Property vote = (Property) memberMap.get("vote");
                assertEquals(Boolean.TRUE, vote.getBoolean());
                Property votedAt = (Property) memberMap.get("votedAt");
                if (previousVotedAt==null) {
                    previousVotedAt = votedAt.getDate();
                } else {
                    assertEquals(previousVotedAt, votedAt.getDate());
                }
            } catch(Exception e) {
                resourceResolver.close();
                fail("Exception: "+e);
            }
        }
        
    }
    
}
