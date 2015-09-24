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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Property;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHelper;
import org.apache.sling.discovery.impl.cluster.voting.VotingView;
import org.apache.sling.discovery.impl.setup.Instance;
import org.junit.After;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class HeartbeatTest {
    
    class SimpleTopologyEventListener implements TopologyEventListener {

        private TopologyEvent lastEvent;
        private int eventCount;
        private final String name;

        public SimpleTopologyEventListener(String name) {
            this.name = name;
        }
        
        @Override
        public void handleTopologyEvent(TopologyEvent event) {
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
    
    Set<Instance> instances = new HashSet<Instance>();
    
    @After
    public void tearDown() throws Exception {
        Instance.setSingletonScheduler(null);
        Iterator<Instance> it = instances.iterator();
        while(it.hasNext()) {
            Instance i = it.next();
            i.stop();
        }
    }

    @Test
    public void testPartitioning() throws Throwable {
        doTestPartitioning(true);
    }
    
    @Test
    public void testPartitioningWithFailingScheduler() throws Throwable {
        installFailingScheduler();
        doTestPartitioning(false);
    }
    
    public void doTestPartitioning(boolean scheduler) throws Throwable {
        Instance slowMachine = Instance.newStandaloneInstance("/var/discovery/impl/", "slow", true, 5 /*5sec timeout*/, 
                999 /* 999sec interval: to disable it*/, 0, UUID.randomUUID().toString());
        assertEquals(1, slowMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(slowMachine.getSlingId(), slowMachine.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(slowMachine);
        Thread.sleep(10); // wait 10ms to ensure 'slowMachine' has the lowerst leaderElectionId (to become leader)
        SimpleTopologyEventListener slowListener = new SimpleTopologyEventListener("slow");
        slowMachine.bindTopologyEventListener(slowListener);
        Instance fastMachine1 = Instance.newClusterInstance("/var/discovery/impl/", "fast1", slowMachine, false, 5, 1, 0);
        assertEquals(1, fastMachine1.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine1.getSlingId(), fastMachine1.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine1);
        Thread.sleep(10); // wait 10ms to ensure 'fastMachine1' has the 2nd lowerst leaderElectionId (to become leader during partitioning)
        SimpleTopologyEventListener fastListener1 = new SimpleTopologyEventListener("fast1");
        fastMachine1.bindTopologyEventListener(fastListener1);
        Instance fastMachine2 = Instance.newClusterInstance("/var/discovery/impl/", "fast2", slowMachine, false, 5, 1, 0);
        assertEquals(1, fastMachine2.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine2.getSlingId(), fastMachine2.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine2);
        SimpleTopologyEventListener fastListener2 = new SimpleTopologyEventListener("fast2");
        fastMachine2.bindTopologyEventListener(fastListener2);
        Instance fastMachine3 = Instance.newClusterInstance("/var/discovery/impl/", "fast3", slowMachine, false, 5, 1, 0);
        assertEquals(1, fastMachine3.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine3.getSlingId(), fastMachine3.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine3);
        SimpleTopologyEventListener fastListener3 = new SimpleTopologyEventListener("fast3");
        fastMachine3.bindTopologyEventListener(fastListener3);
        Instance fastMachine4 = Instance.newClusterInstance("/var/discovery/impl/", "fast4", slowMachine, false, 5, 1, 0);
        assertEquals(1, fastMachine4.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(fastMachine4.getSlingId(), fastMachine4.getDiscoveryService().getTopology().getInstances().iterator().next().getSlingId());
        instances.add(fastMachine4);
        SimpleTopologyEventListener fastListener4 = new SimpleTopologyEventListener("fast4");
        fastMachine4.bindTopologyEventListener(fastListener4);
        
        HeartbeatHandler hhSlow = slowMachine.getHeartbeatHandler();
        for(int i=0; i<3; i++) {
            hhSlow.issueHeartbeat();
            hhSlow.checkView();
            if (!scheduler) {
                fastMachine1.getHeartbeatHandler().issueHeartbeat();
                fastMachine1.getHeartbeatHandler().checkView();
                fastMachine2.getHeartbeatHandler().issueHeartbeat();
                fastMachine2.getHeartbeatHandler().checkView();
                fastMachine3.getHeartbeatHandler().issueHeartbeat();
                fastMachine3.getHeartbeatHandler().checkView();
                fastMachine4.getHeartbeatHandler().issueHeartbeat();
                fastMachine4.getHeartbeatHandler().checkView();
            }
            Thread.sleep(1000);
        }
        
        // at this stage the 4 fast plus the slow instance should all see each other
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
        
        // after 7sec the slow instance' heartbeat should have timed out
        for(int i=0; i<7; i++) {
            if (!scheduler) {
                fastMachine1.getHeartbeatHandler().issueHeartbeat();
                fastMachine1.getHeartbeatHandler().checkView();
                fastMachine2.getHeartbeatHandler().issueHeartbeat();
                fastMachine2.getHeartbeatHandler().checkView();
                fastMachine3.getHeartbeatHandler().issueHeartbeat();
                fastMachine3.getHeartbeatHandler().checkView();
                fastMachine4.getHeartbeatHandler().issueHeartbeat();
                fastMachine4.getHeartbeatHandler().checkView();
            }
            Thread.sleep(1000);
        }
        
        // so the fast listeners should only see 4 instances remaining
        for(int i=0; i<5; i++) {
            Thread.sleep(2000);
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
            assertEquals(5, slowMachine.getDiscoveryService().getTopology().getInstances().size());
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
            Thread.sleep(200);
            assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, slowListener.getLastEvent().getType());
            assertEquals(2, slowListener.getEventCount());
            TopologyView slowTopo = slowMachine.getDiscoveryService().getTopology();
            assertNotNull(slowTopo);
            assertFalse(slowTopo.isCurrent());
            assertEquals(5, slowTopo.getInstances().size());
        }

        for(int i=0; i<3; i++) {
            hhSlow.issueHeartbeat();
            hhSlow.checkView();
            if (!scheduler) {
                fastMachine1.getHeartbeatHandler().issueHeartbeat();
                fastMachine1.getHeartbeatHandler().checkView();
                fastMachine2.getHeartbeatHandler().issueHeartbeat();
                fastMachine2.getHeartbeatHandler().checkView();
                fastMachine3.getHeartbeatHandler().issueHeartbeat();
                fastMachine3.getHeartbeatHandler().checkView();
                fastMachine4.getHeartbeatHandler().issueHeartbeat();
                fastMachine4.getHeartbeatHandler().checkView();
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
    @Test
    public void testSlowAndFastMachine() throws Throwable {
        doTestSlowAndFastMachine();
    }

    @Test
    public void testSlowAndFastMachineWithFailingScheduler() throws Throwable {
        installFailingScheduler();
        doTestSlowAndFastMachine();
    }

    public void doTestSlowAndFastMachine() throws Throwable {
        Instance slowMachine = Instance.newStandaloneInstance("/var/discovery/impl/", "slow", true, 5 /*5sec timeout*/, 
                999 /* 999sec interval: to disable it*/, 0, UUID.randomUUID().toString());
        instances.add(slowMachine);
        SimpleTopologyEventListener slowListener = new SimpleTopologyEventListener("slow");
        slowMachine.bindTopologyEventListener(slowListener);
        Instance fastMachine = Instance.newClusterInstance("/var/discovery/impl/", "fast", slowMachine, false, 5, 999, 0);
        instances.add(fastMachine);
        SimpleTopologyEventListener fastListener = new SimpleTopologyEventListener("fast");
        fastMachine.bindTopologyEventListener(fastListener);
        HeartbeatHandler hhSlow = slowMachine.getHeartbeatHandler();
        HeartbeatHandler hhFast = fastMachine.getHeartbeatHandler();
        
        Thread.sleep(1000);
        assertFalse(fastMachine.getDiscoveryService().getTopology().isCurrent());
        assertFalse(slowMachine.getDiscoveryService().getTopology().isCurrent());
        assertNull(fastListener.getLastEvent());
        assertNull(slowListener.getLastEvent());

        // make few rounds of heartbeats so that the two instances see each other
        for(int i=0; i<5; i++) {
            hhSlow.issueHeartbeat();
            hhSlow.checkView();
            hhFast.issueHeartbeat();
            hhFast.checkView();
            Thread.sleep(100);
        }
        slowMachine.dumpRepo();
        
        assertEquals(2, slowMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(2, fastMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, fastListener.getLastEvent().getType());
        assertEquals(1, fastListener.getEventCount());
        assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, slowListener.getLastEvent().getType());
        assertEquals(1, slowListener.getEventCount());
        
        // now let the slow machine be slow while the fast one updates as expected
        hhSlow.issueHeartbeat();
        for(int i=0; i<6; i++) {
            Thread.sleep(1500);
            hhFast.issueHeartbeat();
            hhFast.checkView();
        }
        fastMachine.dumpRepo();
        hhFast.checkView(); // one more for the start of the vote
        fastMachine.dumpRepo();
        hhFast.checkView(); // and one for the promotion

        // after 9 sec hhSlow's heartbeat will have timed out, so hhFast will not see hhSlow anymore
        fastMachine.dumpRepo();
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener.getLastEvent().getType());
        assertEquals(3, fastListener.getEventCount());
        assertEquals(1, fastMachine.getDiscoveryService().getTopology().getInstances().size());
        
        TopologyView topo = slowMachine.getDiscoveryService().getTopology();
        assertFalse(topo.isCurrent());
        
        // after those 6 sec, hhSlow does the check (6sec between heartbeat and check)
        hhSlow.checkView();
        slowMachine.dumpRepo();
        hhSlow.issueHeartbeat();
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener.getLastEvent().getType());
        assertEquals(3, fastListener.getEventCount());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, slowListener.getLastEvent().getType());
        assertEquals(2, slowListener.getEventCount());
        Thread.sleep(8000);
        // even after 8 sec the slow lsitener did not send a TOPOLOGY_CHANGED yet
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, slowListener.getLastEvent().getType());
        assertFalse(slowMachine.getDiscoveryService().getTopology().isCurrent());
        assertEquals(2, slowListener.getEventCount());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, fastListener.getLastEvent().getType());
        assertEquals(1, fastMachine.getDiscoveryService().getTopology().getInstances().size());
        assertEquals(3, fastListener.getEventCount());
        
        // make few rounds of heartbeats so that the two instances see each other again
        for(int i=0; i<4; i++) {
            hhFast.issueHeartbeat();
            hhFast.checkView();
            hhSlow.issueHeartbeat();
            hhSlow.checkView();
            Thread.sleep(1000);
        }
        
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
    
    private void installFailingScheduler() throws Exception {
        Instance.setSingletonScheduler(new Scheduler() {
            
            @Override
            public void removeJob(String name) throws NoSuchElementException {
                // nothing to do here
            }
            
            @Override
            public boolean fireJobAt(String name, Object job, Map<String, Serializable> config, Date date, int times, long period) {
                return false;
            }
            
            @Override
            public void fireJobAt(String name, Object job, Map<String, Serializable> config, Date date) throws Exception {
                throw new Exception("cos you are really worth it");
            }
            
            @Override
            public boolean fireJob(Object job, Map<String, Serializable> config, int times, long period) {
                return false;
            }
            
            @Override
            public void fireJob(Object job, Map<String, Serializable> config) throws Exception {
                throw new Exception("cos you are really worth it");
            }
            
            @Override
            public void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently,
                    boolean startImmediate) throws Exception {
                throw new Exception("cos you are really worth it");
            }
            
            @Override
            public void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently)
                    throws Exception {
                throw new Exception("cos you are really worth it");
            }
            
            @Override
            public void addJob(String name, Object job, Map<String, Serializable> config, String schedulingExpression,
                    boolean canRunConcurrently) throws Exception {
                throw new Exception("cos you are really worth it");
            }
        });
    }

    /**
     * SLING-5027 : test to reproduce the voting loop
     * (and verify that it's fixed)
     */
    @Test
    public void testVotingLoop() throws Throwable {
        Instance slowMachine1 = Instance.newStandaloneInstance("/var/discovery/impl/", "slow1", true, 600 /*600sec timeout*/, 
                999 /* 999sec interval: to disable it*/, 0, UUID.randomUUID().toString());
        instances.add(slowMachine1);
        SimpleTopologyEventListener slowListener1 = new SimpleTopologyEventListener("slow1");
        slowMachine1.bindTopologyEventListener(slowListener1);
        Instance slowMachine2 = Instance.newClusterInstance("/var/discovery/impl/", "slow2", slowMachine1, false, 600, 999, 0);
        instances.add(slowMachine2);
        SimpleTopologyEventListener slowListener2 = new SimpleTopologyEventListener("slow2");
        slowMachine2.bindTopologyEventListener(slowListener2);
        Instance fastMachine = Instance.newClusterInstance("/var/discovery/impl/", "fast", slowMachine1, false, 600, 999, 0);
        instances.add(fastMachine);
        SimpleTopologyEventListener fastListener = new SimpleTopologyEventListener("fast");
        fastMachine.bindTopologyEventListener(fastListener);
        HeartbeatHandler hhSlow1 = slowMachine1.getHeartbeatHandler();
        HeartbeatHandler hhSlow2 = slowMachine2.getHeartbeatHandler();
        HeartbeatHandler hhFast = fastMachine.getHeartbeatHandler();
        
        Thread.sleep(1000);
        assertFalse(fastMachine.getDiscoveryService().getTopology().isCurrent());
        assertFalse(slowMachine1.getDiscoveryService().getTopology().isCurrent());
        assertFalse(slowMachine2.getDiscoveryService().getTopology().isCurrent());
        assertNull(fastListener.getLastEvent());
        assertNull(slowListener1.getLastEvent());
        assertNull(slowListener2.getLastEvent());

        // prevent the slow machine from voting
        slowMachine1.stopVoting();
        
        // now let all issue a heartbeat
        hhSlow1.issueHeartbeat();
        hhSlow2.issueHeartbeat();
        hhFast.issueHeartbeat();

        // now let the fast one start a new voting, to which
        // only the fast one will vote, the slow one doesn't.
        // that will cause a voting loop
        hhFast.checkView();
        
        Calendar previousVotedAt = null;
        for(int i=0; i<5; i++) {
            Thread.sleep(1000);
            // now check the ongoing votings
            ResourceResolverFactory factory = fastMachine.getResourceResolverFactory();
            ResourceResolver resourceResolver = factory
                    .getAdministrativeResourceResolver(null);
            try{
                List<VotingView> ongoingVotings = 
                        VotingHelper.listOpenNonWinningVotings(resourceResolver, fastMachine.getConfig());
                assertNotNull(ongoingVotings);
                assertEquals(1, ongoingVotings.size());
                VotingView ongoingVote = ongoingVotings.get(0);
                assertFalse(ongoingVote.isWinning());
                assertFalse(ongoingVote.hasVotedOrIsInitiator(slowMachine1.getSlingId()));
                assertTrue(ongoingVote.hasVotedOrIsInitiator(slowMachine2.getSlingId()));
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
