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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.cluster.helpers.AcceptsMultiple;
import org.apache.sling.discovery.impl.cluster.helpers.AssertingTopologyEventListener;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.discovery.impl.setup.PropertyProviderImpl;
import org.apache.sling.discovery.impl.topology.announcement.Announcement;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementFilter;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterTest {
	
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private class SimpleClusterView {
    	
    	private Instance[] instances;

    	SimpleClusterView(Instance... instances) {
    		this.instances = instances;
    	}
    	
    	@Override
    	public String toString() {
    	    String instanceSlingIds = "";
    	    for(int i=0; i<instances.length; i++) {
    	        instanceSlingIds = instanceSlingIds + instances[i].slingId + ",";
    	    }
            return "an expected cluster with "+instances.length+" instances: "+instanceSlingIds;
    	}
    }

    Instance instance1;
    Instance instance2;
    Instance instance3;

    private String property1Value;

    protected String property2Value;

    private String property1Name;

    private String property2Name;
    Instance instance4;
    Instance instance5;
    Instance instance1Restarted;
    private Level logLevel;

    @Before
    public void setup() throws Exception {
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.TRACE);
        logger.debug("here we are");
        instance1 = Instance.newStandaloneInstance("firstInstance", true);
        instance2 = Instance.newClusterInstance("secondInstance", instance1,
                false);
    }

    @After
    public void tearDown() throws Exception {
        if (instance5 != null) {
            instance5.stop();
        }
        if (instance4 != null) {
            instance4.stop();
        }
        if (instance3 != null) {
            instance3.stop();
        }
        if (instance3 != null) {
            instance3.stop();
        }
        if (instance2 != null) {
        	instance2.stop();
        }
        if (instance1 != null) {
            instance1.stop();
        }
        if (instance1Restarted != null) {
            instance1Restarted.stop();
        }
        instance1Restarted = null;
        instance1 = null;
        instance2 = null;
        instance3 = null;
        instance4 = null;
        instance5 = null;
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        discoveryLogger.setLevel(logLevel);
    }
    
    /** test leader behaviour with ascending slingIds, SLING-3253 **/
    @Test
    public void testLeaderAsc() throws Throwable {
        logger.info("testLeaderAsc: start");
    	doTestLeader("000", "111");
        logger.info("testLeaderAsc: end");
    }

    /** test leader behaviour with descending slingIds, SLING-3253 **/
    @Test
    public void testLeaderDesc() throws Throwable {
        logger.info("testLeaderDesc: start");
    	doTestLeader("111", "000");
        logger.info("testLeaderDesc: end");
    }

    private void doTestLeader(String slingId1, String slingId2) throws Throwable {
        logger.info("doTestLeader("+slingId1+","+slingId2+"): start");
    	// stop 1 and 2 and create them with a lower heartbeat timeout
    	instance2.stopHeartbeats();
    	instance1.stopHeartbeats();
        instance2.stop();
        instance1.stop();
        instance1 = Instance.newStandaloneInstance("/var/discovery/impl/", "firstInstance", true, 30, 1, slingId1);
        // sleep so that the two dont have the same startup time, and thus leaderElectionId is lower for instance1
        logger.info("doTestLeader: 1st sleep 200ms");
        Thread.sleep(200);
        instance2 = Instance.newClusterInstance("/var/discovery/impl/", "secondInstance", instance1,
                false, 30, 1, slingId2);
        assertNotNull(instance1);
        assertNotNull(instance2);

        // the two instances are still isolated - hence they throw an exception
        try{
            instance1.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }
        try{
            instance2.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }

        // let the sync/voting happen
        for(int m=0; m<4; m++) {
            instance1.runHeartbeatOnce();
            instance2.runHeartbeatOnce();
            logger.info("doTestLeader: sleep 500ms");
            Thread.sleep(500);
        }
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        
        // now they must be in the same cluster, so in a cluster of size 1
        assertEquals(2, instance1.getClusterViewService().getClusterView().getInstances().size());
        assertEquals(2, instance2.getClusterViewService().getClusterView().getInstances().size());
        
        // the first instance should be the leader - since it was started first
        assertTrue(instance1.getLocalInstanceDescription().isLeader());
        assertFalse(instance2.getLocalInstanceDescription().isLeader());
        logger.info("doTestLeader("+slingId1+","+slingId2+"): end");
    }

    /**
     * Tests stale announcement reported in SLING-4139:
     * An instance which crashes but had announcements, never cleans up those announcements.
     * Thus on a restart, those announcements are still there, even if the connector
     * would no longer be in use (or point somewhere else etc).
     * That has various effects, one of them tested in this method: peers in the same cluster,
     * after the crashed/stopped instance restarts, will assume those stale announcements
     * as being correct and include them in the topology - hence reporting stale instances
     * (which can be old instances or even duplicates).
     */
    @Test
    public void testStaleAnnouncementsVisibleToClusterPeers4139() throws Throwable {
        logger.info("testStaleAnnouncementsVisibleToClusterPeers4139: start");
    	final String instance1SlingId = prepare4139();
        
        // remove topology connector from instance3 to instance1
        // -> corresponds to stop pinging
        // (nothing to assert additionally here)
        
        // start instance 1
        instance1Restarted = Instance.newClusterInstance("/var/discovery/impl/", "firstInstance", instance2,
                false, Integer.MAX_VALUE /* no timeout */, 1, instance1SlingId);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3);
        
        // facts: connector 3->1 does not exist actively anymore,
        //        instance 1+2 should build a cluster, 
        //        instance 3 should be isolated
        logger.info("instance1Restarted.dump: "+instance1Restarted.slingId);
        instance1Restarted.dumpRepo();
        
        logger.info("instance2.dump: "+instance2.slingId);
        instance2.dumpRepo();

        logger.info("instance3.dump: "+instance3.slingId);
        instance3.dumpRepo();

        assertTopology(instance1Restarted, new SimpleClusterView(instance1Restarted, instance2));
        assertTopology(instance3, new SimpleClusterView(instance3));
        assertTopology(instance2, new SimpleClusterView(instance1Restarted, instance2));
        instance1Restarted.stop();
        logger.info("testStaleAnnouncementsVisibleToClusterPeers4139: end");
    }
    
    /**
     * Tests a situation where a connector was done to instance1, which eventually
     * crashed, then the connector is done to instance2. Meanwhile instance1 
     * got restarted and this test assures that the instance3 is not reported
     * twice in the topology. Did not happen before 4139, but should never afterwards neither
     */
    @Test
    public void testDuplicateInstanceIn2Clusters4139() throws Throwable {
        logger.info("testDuplicateInstanceIn2Clusters4139: start");
        final String instance1SlingId = prepare4139();
        
        // remove topology connector from instance3 to instance1
        // -> corresponds to stop pinging
        // (nothing to assert additionally here)
        // instead, now start a connector from instance3 to instance2
        pingConnector(instance3, instance2);
        
        // start instance 1
        instance1Restarted = Instance.newClusterInstance("/var/discovery/impl/", "firstInstance", instance2,
                false, Integer.MAX_VALUE /* no timeout */, 1, instance1SlingId);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3);
        pingConnector(instance3, instance2);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3);
        pingConnector(instance3, instance2);
        logger.info("iteration 0");
        logger.info("instance1Restarted.slingId: "+instance1Restarted.slingId);
        logger.info("instance2.slingId: "+instance2.slingId);
        logger.info("instance3.slingId: "+instance3.slingId);
        instance1Restarted.dumpRepo();
        assertSameTopology(new SimpleClusterView(instance1Restarted, instance2), new SimpleClusterView(instance3));
        
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3);
        pingConnector(instance3, instance2);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3);
        pingConnector(instance3, instance2);
        logger.info("iteration 1");
        logger.info("instance1Restarted.slingId: "+instance1Restarted.slingId);
        logger.info("instance2.slingId: "+instance2.slingId);
        logger.info("instance3.slingId: "+instance3.slingId);
        instance1Restarted.dumpRepo();
        assertSameTopology(new SimpleClusterView(instance1Restarted, instance2), new SimpleClusterView(instance3));
        instance1Restarted.stop();

        logger.info("testDuplicateInstanceIn2Clusters4139: end");
    }
    
/*    ok, this test should do the following:
         * cluster A with instance 1 and instance 2
         * cluster B with instance 3 and instance 4
         * cluster C with instance 5
         
         * initially, instance3 is pinging instance1, and instance 5 is pinging instance1 as well (MAC hub)
          * that should result in instance3 and 5 to inherit the rest from instance1
         * then simulate load balancer switching from instance1 to instance2 - hence pings go to instance2 
         * 
         */
    @Test
    public void testConnectorSwitching4139() throws Throwable {
        final int MIN_EVENT_DELAY = 1;

        tearDown(); // reset any setup that was done - we start with a different setup than the default one
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.DEBUG);
        
        instance1 = Instance.newStandaloneInstance("/var/discovery/clusterA/", "instance1", true, 10 /* sec*/, 999, MIN_EVENT_DELAY);
        instance2 = Instance.newClusterInstance("/var/discovery/clusterA/", "instance2", instance1,
                false, 10 /* sec*/, 999, MIN_EVENT_DELAY);
        // now launch the remote instance
        instance3 = Instance.newStandaloneInstance("/var/discovery/clusterB/", "instance3", false, 10 /* sec*/, 999, MIN_EVENT_DELAY);
        instance4 = Instance.newClusterInstance("/var/discovery/clusterB/", "instance4", instance3,
                false, 10 /* sec*/, 999, MIN_EVENT_DELAY);
        instance5 = Instance.newStandaloneInstance("/var/discovery/clusterC/", "instance5", false, 10 /* sec*/, 999, MIN_EVENT_DELAY);

        // join the instances to form a cluster by sending out heartbeats
        runHeartbeatOnceWith(instance1, instance2, instance3, instance4, instance5);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1, instance2, instance3, instance4, instance5);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1, instance2, instance3, instance4, instance5);
        Thread.sleep(500);

        assertSameTopology(new SimpleClusterView(instance1, instance2));
        assertSameTopology(new SimpleClusterView(instance3, instance4));
        assertSameTopology(new SimpleClusterView(instance5));
        
        // create a topology connector from instance3 to instance1
        // -> corresponds to starting to ping
        runHeartbeatOnceWith(instance1, instance2, instance3, instance4, instance5);
        pingConnector(instance3, instance1);
        pingConnector(instance5, instance1);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1, instance2, instance3, instance4, instance5);
        pingConnector(instance3, instance1);
        pingConnector(instance5, instance1);
        Thread.sleep(500);
        
        // make asserts on the topology
        logger.info("testConnectorSwitching4139: instance1.slingId="+instance1.slingId);
        logger.info("testConnectorSwitching4139: instance2.slingId="+instance2.slingId);
        logger.info("testConnectorSwitching4139: instance3.slingId="+instance3.slingId);
        logger.info("testConnectorSwitching4139: instance4.slingId="+instance4.slingId);
        logger.info("testConnectorSwitching4139: instance5.slingId="+instance5.slingId);
        instance1.dumpRepo();
        
        assertSameTopology(new SimpleClusterView(instance1, instance2), 
                new SimpleClusterView(instance3, instance4), 
                new SimpleClusterView(instance5));
        
        // simulate a crash of instance1, resulting in load-balancer to switch the pings
        boolean success = false;
        for(int i=0; i<25; i++) {
            // loop for max 25 times, min 20 times
            runHeartbeatOnceWith(instance2, instance3, instance4, instance5);
            final boolean ping1 = pingConnector(instance3, instance2);
            final boolean ping2 = pingConnector(instance5, instance2);
            if (ping1 && ping2) {
                // both pings were fine - hence break
                success = true;
                logger.info("testConnectorSwitching4139: successfully switched all pings to instance2 after "+i+" rounds.");
                if (i<20) {
                    logger.info("testConnectorSwitching4139: min loop cnt not yet reached: i="+i);
                    Thread.sleep(1000); // 20x1000ms = 20sec max - (vs 10sec timeout) - should be enough for timing out
                    continue;
                }
                break;
            }
            logger.info("testConnectorSwitching4139: looping");
            Thread.sleep(1000); // 25x1000ms = 25sec max - (vs 10sec timeout)
            
        }
        assertTrue(success);
        // one final heartbeat
        runHeartbeatOnceWith(instance2, instance3, instance4, instance5);
        assertTrue(pingConnector(instance3, instance2));
        assertTrue(pingConnector(instance5, instance2));

        instance2.dumpRepo();

        assertSameTopology(new SimpleClusterView(instance2), 
                new SimpleClusterView(instance3, instance4), 
                new SimpleClusterView(instance5));

        // restart instance1, crash instance4
        instance4.stopHeartbeats();
        instance1Restarted = Instance.newClusterInstance("/var/discovery/clusterA/", "instance1", instance2,
                false, Integer.MAX_VALUE /* no timeout */, 1, instance1.slingId);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance5);
        assertTrue(pingConnector(instance3, instance2));
        assertTrue(pingConnector(instance5, instance2));
        success = false;
        for(int i=0; i<25; i++) {
            runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance5);
            assertTrue(pingConnector(instance3, instance2));
            assertTrue(pingConnector(instance5, instance2));
            final TopologyView topology = instance3.getDiscoveryService().getTopology();
            InstanceDescription i3 = null;
            for (Iterator<InstanceDescription> it = topology.getInstances().iterator(); it.hasNext();) {
                final InstanceDescription id = it.next();
                if (id.getSlingId().equals(instance3.slingId)) {
                    i3 = id;
                    break;
                }
            }
            assertNotNull(i3);
            assertEquals(instance3.slingId, i3.getSlingId());
            final ClusterView i3Cluster = i3.getClusterView();
            final int i3ClusterSize = i3Cluster.getInstances().size();
            if (i3ClusterSize==1) {
                if (i<20) {
                    logger.info("testConnectorSwitching4139: [2] min loop cnt not yet reached: i="+i);
                    Thread.sleep(500); // 20x500ms = 10sec max - (vs 5sec timeout) - should be enough for timing out
                    continue;
                }
                success = true;
                break;
            }
            logger.info("testConnectorSwitching4139: i3ClusterSize: "+i3ClusterSize);
            Thread.sleep(500);
        }

        logger.info("testConnectorSwitching4139: instance1Restarted.slingId="+instance1Restarted.slingId);
        logger.info("testConnectorSwitching4139: instance2.slingId="+instance2.slingId);
        logger.info("testConnectorSwitching4139: instance3.slingId="+instance3.slingId);
        logger.info("testConnectorSwitching4139: instance4.slingId="+instance4.slingId);
        logger.info("testConnectorSwitching4139: instance5.slingId="+instance5.slingId);
        instance1Restarted.dumpRepo();
        assertTrue(success);

        assertSameTopology(new SimpleClusterView(instance1Restarted, instance2), 
                new SimpleClusterView(instance3), 
                new SimpleClusterView(instance5));
        instance1Restarted.stop();

    }

    @Test
    public void testDuplicateInstance3726() throws Throwable {
        logger.info("testDuplicateInstance3726: start");
        final int MIN_EVENT_DELAY = 1;

        tearDown(); // reset any setup that was done - we start with a different setup than the default one
        final org.apache.log4j.Logger discoveryLogger = LogManager.getRootLogger().getLogger("org.apache.sling.discovery");
        logLevel = discoveryLogger.getLevel();
        discoveryLogger.setLevel(Level.DEBUG);
        
        instance1 = Instance.newStandaloneInstance("/var/discovery/clusterA/", "instance1", true, 15 /* sec*/, MIN_EVENT_DELAY);
        instance2 = Instance.newClusterInstance("/var/discovery/clusterA/", "instance2", instance1,
                false, 15 /* sec*/, MIN_EVENT_DELAY);
        // now launch the remote instance
        instance3 = Instance.newStandaloneInstance("/var/discovery/clusterB/", "instance3", false, 15 /* sec*/, MIN_EVENT_DELAY);
        instance5 = Instance.newStandaloneInstance("/var/discovery/clusterC/", "instance5", false, 15 /* sec*/, MIN_EVENT_DELAY);

        // join the instances to form a cluster by sending out heartbeats
        runHeartbeatOnceWith(instance1, instance2, instance3, instance5);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1, instance2, instance3, instance5);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance1, instance2, instance3, instance5);
        Thread.sleep(500);

        assertSameTopology(new SimpleClusterView(instance1, instance2));
        assertSameTopology(new SimpleClusterView(instance3));
        assertSameTopology(new SimpleClusterView(instance5));
        
        // create a topology connector from instance3 to instance1
        // -> corresponds to starting to ping
        pingConnector(instance3, instance1);
        pingConnector(instance5, instance1);
        pingConnector(instance3, instance1);
        pingConnector(instance5, instance1);
        
        // make asserts on the topology
        logger.info("testDuplicateInstance3726: instance1.slingId="+instance1.slingId);
        logger.info("testDuplicateInstance3726: instance2.slingId="+instance2.slingId);
        logger.info("testDuplicateInstance3726: instance3.slingId="+instance3.slingId);
        logger.info("testDuplicateInstance3726: instance5.slingId="+instance5.slingId);
        instance1.dumpRepo();
        
        assertSameTopology(new SimpleClusterView(instance1, instance2), 
                new SimpleClusterView(instance3/*, instance4*/), 
                new SimpleClusterView(instance5));
        
        // simulate a crash of instance1, resulting in load-balancer to switch the pings
        instance1.stopHeartbeats();
        boolean success = false;
        for(int i=0; i<25; i++) {
            // loop for max 25 times, min 20 times
            runHeartbeatOnceWith(instance2, instance3, /*instance4, */instance5);
            final boolean ping1 = pingConnector(instance3, instance2);
            final boolean ping2 = pingConnector(instance5, instance2);
            if (ping1 && ping2) {
                // both pings were fine - hence break
                success = true;
                logger.info("testDuplicateInstance3726: successfully switched all pings to instance2 after "+i+" rounds.");
                if (i<20) {
                    logger.info("testDuplicateInstance3726: min loop cnt not yet reached: i="+i);
                    Thread.sleep(1000); // 20x1000ms = 20sec max - (vs 15sec timeout) - should be enough for timing out
                    continue;
                }
                break;
            }
            logger.info("testDuplicateInstance3726: looping");
            Thread.sleep(1000); // 25x1000ms = 25sec max - (vs 15sec timeout)
            
        }
        assertTrue(success);
        // one final heartbeat
        runHeartbeatOnceWith(instance2, instance3, instance5);
        assertTrue(pingConnector(instance3, instance2));
        assertTrue(pingConnector(instance5, instance2));

        instance2.dumpRepo();

        assertSameTopology(new SimpleClusterView(instance2), 
                new SimpleClusterView(instance3), 
                new SimpleClusterView(instance5));

        // restart instance1, start instance4
        instance1Restarted = Instance.newClusterInstance("/var/discovery/clusterA/", "instance1", instance2,
                false, Integer.MAX_VALUE /* no timeout */, 1, instance1.slingId);
        instance4 = Instance.newClusterInstance("/var/discovery/clusterB/", "instance4", instance3,
                false, 30 /* sec*/, MIN_EVENT_DELAY);
        for(int i=0; i<3; i++) {
            runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance4, instance5);
            assertTrue(pingConnector(instance3, instance2));
            assertTrue(pingConnector(instance5, instance2));
        }

        instance1Restarted.dumpRepo();
        logger.info("testDuplicateInstance3726: instance1Restarted.slingId="+instance1Restarted.slingId);
        logger.info("testDuplicateInstance3726: instance2.slingId="+instance2.slingId);
        logger.info("testDuplicateInstance3726: instance3.slingId="+instance3.slingId);
        logger.info("testDuplicateInstance3726: instance4.slingId="+instance4.slingId);
        logger.info("testDuplicateInstance3726: instance5.slingId="+instance5.slingId);
        assertTrue(success);

        assertSameTopology(new SimpleClusterView(instance1Restarted, instance2), 
                new SimpleClusterView(instance3, instance4), 
                new SimpleClusterView(instance5));
        instance1Restarted.stop();
        logger.info("testDuplicateInstance3726: end");
    }

    private void assertSameTopology(SimpleClusterView... clusters) throws UndefinedClusterViewException {
        if (clusters==null) {
            return;
        }
        for(int i=0; i<clusters.length; i++) { // go through all clusters 
            final SimpleClusterView aCluster = clusters[i];
            assertSameClusterIds(aCluster.instances);
            for(int j=0; j<aCluster.instances.length; j++) { // and all instances therein
                final Instance anInstance = aCluster.instances[j];
                assertTopology(anInstance, clusters); // an verify that they all see the same
                for(int k=0; k<clusters.length; k++) {
                    final SimpleClusterView otherCluster = clusters[k];
                    if (aCluster==otherCluster) {
                        continue; // then ignore this one
                    }
                    for(int m=0; m<otherCluster.instances.length; m++) {
                        assertNotSameClusterIds(anInstance, otherCluster.instances[m]);
                    }
                }
            }
        }
    }

    private void runHeartbeatOnceWith(Instance... instances) {
        if (instances==null) {
            return;
        }
        for(int i=0; i<instances.length; i++) {
            instances[i].runHeartbeatOnce();
        }
    }

    /**
     * Tests a situation where a connector was done to instance1, which eventually
     * crashed, then the connector is done to instance4 (which is in a separate, 3rd cluster). 
     * Meanwhile instance1 got restarted and this test assures that the instance3 is not reported
     * twice in the topology. This used to happen prior to SLING-4139
     */
    @Test
    public void testStaleInstanceIn3Clusters4139() throws Throwable {
        logger.info("testStaleInstanceIn3Clusters4139: start");
        final String instance1SlingId = prepare4139();
        
        // remove topology connector from instance3 to instance1
        // -> corresponds to stop pinging
        // (nothing to assert additionally here)
        
        // start instance4 in a separate cluster
        instance4 = Instance.newStandaloneInstance("/var/discovery/implremote4/", "remoteInstance4", false, Integer.MAX_VALUE /* no timeout */, 1);
        try{
            instance4.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }
        
        // instead, now start a connector from instance3 to instance2
        instance4.runHeartbeatOnce();
        instance4.runHeartbeatOnce();
        pingConnector(instance3, instance4);
        
        // start instance 1
        instance1Restarted = Instance.newClusterInstance("/var/discovery/impl/", "firstInstance", instance2,
                false, Integer.MAX_VALUE /* no timeout */, 1, instance1SlingId);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance4);
        pingConnector(instance3, instance4);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance4);
        pingConnector(instance3, instance4);
        logger.info("iteration 0");
        logger.info("instance1Restarted.slingId: "+instance1Restarted.slingId);
        logger.info("instance2.slingId: "+instance2.slingId);
        logger.info("instance3.slingId: "+instance3.slingId);
        logger.info("instance4.slingId: "+instance4.slingId);
        instance1Restarted.dumpRepo();
        assertSameTopology(
                new SimpleClusterView(instance3),
                new SimpleClusterView(instance4));
        assertSameTopology(new SimpleClusterView(instance1Restarted, instance2));
        
        Thread.sleep(100);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance4);
        pingConnector(instance3, instance4);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance4);
        pingConnector(instance3, instance4);
        logger.info("iteration 1");
        logger.info("instance1Restarted.slingId: "+instance1Restarted.slingId);
        logger.info("instance2.slingId: "+instance2.slingId);
        logger.info("instance3.slingId: "+instance3.slingId);
        logger.info("instance4.slingId: "+instance4.slingId);
        instance1Restarted.dumpRepo();
        assertSameTopology(new SimpleClusterView(instance1Restarted, instance2));
        assertSameTopology(
                new SimpleClusterView(instance3),
                new SimpleClusterView(instance4));

        Thread.sleep(100);
        runHeartbeatOnceWith(instance1Restarted, instance2, instance3, instance4);
        pingConnector(instance3, instance4);
        
        // now the situation should be as follows:
        logger.info("iteration 2");
        logger.info("instance1Restarted.slingId: "+instance1Restarted.slingId);
        logger.info("instance2.slingId: "+instance2.slingId);
        logger.info("instance3.slingId: "+instance3.slingId);
        logger.info("instance4.slingId: "+instance4.slingId);
        instance1Restarted.dumpRepo();
        assertSameTopology(new SimpleClusterView(instance1Restarted, instance2));
        assertSameTopology(
                new SimpleClusterView(instance3),
                new SimpleClusterView(instance4));
        instance1Restarted.stop();

        logger.info("testStaleInstanceIn3Clusters4139: end");
    }
    
    /**
     * Preparation steps for SLING-4139 tests:
     * Creates two clusters: A: with instance1 and 2, B with instance 3
     * instance 3 creates a connector to instance 1
     * then instance 1 is killed (crashes)
     * @return the slingId of the original (crashed) instance1
     */
	private String prepare4139() throws Throwable, Exception,
			InterruptedException {
	    tearDown(); // stop anything running..
        instance1 = Instance.newStandaloneInstance("/var/discovery/impl/", "firstInstance", true, Integer.MAX_VALUE /* no timeout */, 1);
        instance2 = Instance.newClusterInstance("/var/discovery/impl/", "secondInstance", instance1,
                false, Integer.MAX_VALUE /* no timeout */, 1);
        // join the two instances to form a cluster by sending out heartbeats
        runHeartbeatOnceWith(instance1, instance2);
        Thread.sleep(100);
        runHeartbeatOnceWith(instance1, instance2);
        Thread.sleep(100);
        runHeartbeatOnceWith(instance1, instance2);
        assertSameClusterIds(instance1, instance2);
        
        // now launch the remote instance
        instance3 = Instance.newStandaloneInstance("/var/discovery/implremote/", "remoteInstance", false, Integer.MAX_VALUE /* no timeout */, 1);
        assertSameClusterIds(instance1, instance2);
        try{
            instance3.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException ue) {
            // ok
        }
        assertEquals(0, instance1.getAnnouncementRegistry().listLocalAnnouncements().size());
        assertEquals(0, instance1.getAnnouncementRegistry().listLocalIncomingAnnouncements().size());
        assertEquals(0, instance2.getAnnouncementRegistry().listLocalAnnouncements().size());
        assertEquals(0, instance2.getAnnouncementRegistry().listLocalIncomingAnnouncements().size());
        assertEquals(0, instance3.getAnnouncementRegistry().listLocalAnnouncements().size());
        assertEquals(0, instance3.getAnnouncementRegistry().listLocalIncomingAnnouncements().size());
        
        // create a topology connector from instance3 to instance1
        // -> corresponds to starting to ping
        instance3.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        pingConnector(instance3, instance1);
        // make asserts on the topology
        instance1.dumpRepo();
        assertSameTopology(new SimpleClusterView(instance1, instance2), new SimpleClusterView(instance3));
        
        // kill instance 1
        logger.info("instance1.slingId="+instance1.slingId);
        logger.info("instance2.slingId="+instance2.slingId);
        logger.info("instance3.slingId="+instance3.slingId);
        final String instance1SlingId = instance1.slingId;
        instance1.stopHeartbeats(); // and have instance3 no longer pinging instance1
        instance1.stop(); // otherwise it will have itself still registered with the observation manager and fiddle with future events..
        instance1 = null; // set to null to early fail if anyone still assumes (original) instance1 is up form now on
        instance2.getConfig().setHeartbeatTimeout(1); // set instance2's heartbeatTimeout to 1 sec to time out instance1 quickly!
        instance3.getConfig().setHeartbeatTimeout(1); // set instance3's heartbeatTimeout to 1 sec to time out instance1 quickly!
        Thread.sleep(500);
        runHeartbeatOnceWith(instance2, instance3);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance2, instance3);
        Thread.sleep(500);
        runHeartbeatOnceWith(instance2, instance3);
        // instance 2 should now be alone - in fact, 3 should be alone as well
        instance2.dumpRepo();
        assertTopology(instance2, new SimpleClusterView(instance2));
        assertTopology(instance3, new SimpleClusterView(instance3));
        instance2.getConfig().setHeartbeatTimeout(Integer.MAX_VALUE /* no timeout */); // set instance2's heartbeatTimeout back to Integer.MAX_VALUE /* no timeout */
        instance3.getConfig().setHeartbeatTimeout(Integer.MAX_VALUE /* no timeout */); // set instance3's heartbeatTimeout back to Integer.MAX_VALUE /* no timeout */
		return instance1SlingId;
	}
    
    private void assertNotSameClusterIds(Instance... instances) throws UndefinedClusterViewException {
    	if (instances==null) {
    		fail("must not pass empty set of instances here");
    	}
    	if (instances.length<=1) {
    		fail("must not pass 0 or 1 instance only");
    	}
        final String clusterId1 = instances[0].getClusterViewService()
                .getClusterView().getId();
        for(int i=1; i<instances.length; i++) {
	        final String otherClusterId = instances[i].getClusterViewService()
	                .getClusterView().getId();
	        // cluster ids must NOT be the same
	        assertNotEquals(clusterId1, otherClusterId);
        }
        if (instances.length>2) {
        	final Instance[] subset = new Instance[instances.length-1];
        	System.arraycopy(instances, 0, subset, 1, instances.length-1);
        	assertNotSameClusterIds(subset);
        }
	}

	private void assertSameClusterIds(Instance... instances) throws UndefinedClusterViewException {
    	if (instances==null) {
            // then there is nothing to compare
            return;
    	}
    	if (instances.length==1) {
    	    // then there is nothing to compare
    	    return;
    	}
        final String clusterId1 = instances[0].getClusterViewService()
                .getClusterView().getId();
        for(int i=1; i<instances.length; i++) {
	        final String otherClusterId = instances[i].getClusterViewService()
	                .getClusterView().getId();
	        // cluster ids must be the same
	        if (!clusterId1.equals(otherClusterId)) {
	            logger.error("assertSameClusterIds: instances[0]: "+instances[0]);
	            logger.error("assertSameClusterIds: instances["+i+"]: "+instances[i]);
	            fail("mismatch in clusterIds: expected to equal: clusterId1="+clusterId1+", otherClusterId="+otherClusterId);
	        }
        }
	}

	private void assertTopology(Instance instance, SimpleClusterView... assertedClusterViews) {
    	final TopologyView topology = instance.getDiscoveryService().getTopology();
    	logger.info("assertTopology: instance "+instance.slingId+" sees topology: "+topology+", expected: "+assertedClusterViews);
    	assertNotNull(topology);
    	if (assertedClusterViews.length!=topology.getClusterViews().size()) {
            dumpFailureDetails(topology, assertedClusterViews);
    	    fail("instance "+instance.slingId+ " expected "+assertedClusterViews.length+", got: "+topology.getClusterViews().size());
    	}
    	final Set<ClusterView> actualClusters = new HashSet<ClusterView>(topology.getClusterViews());
    	for(int i=0; i<assertedClusterViews.length; i++) {
    		final SimpleClusterView assertedClusterView = assertedClusterViews[i];
    		boolean foundMatch = false;
    		for (Iterator<ClusterView> it = actualClusters.iterator(); it
					.hasNext();) {
				final ClusterView actualClusterView = it.next();
				if (matches(assertedClusterView, actualClusterView)) {
					it.remove();
					foundMatch = true;
					break;
				}
			}
    		if (!foundMatch) {
    		    dumpFailureDetails(topology, assertedClusterViews);
    			fail("instance "+instance.slingId+ " could not find a match in the topology with instance="+instance.slingId+" and clusterViews="+assertedClusterViews.length);
    		}
    	}
    	assertEquals("not all asserted clusterviews are in the actual view with instance="+instance+" and clusterViews="+assertedClusterViews, actualClusters.size(), 0);
	}

    private void dumpFailureDetails(TopologyView topology, SimpleClusterView... assertedClusterViews) {
        logger.error("assertTopology: expected: "+assertedClusterViews.length);
        for(int j=0; j<assertedClusterViews.length; j++) {
            logger.error("assertTopology:  ["+j+"]: "+assertedClusterViews[j].toString());
        }
        final Set<ClusterView> clusterViews = topology.getClusterViews();
        final Set<InstanceDescription> instances = topology.getInstances();
        logger.error("assertTopology: actual: "+clusterViews.size()+" clusters with a total of "+instances.size()+" instances");
        for (Iterator<ClusterView> it = clusterViews.iterator(); it.hasNext();) {
            final ClusterView aCluster = it.next();
            logger.error("assertTopology:  a cluster: "+aCluster.getId());
            for (Iterator<InstanceDescription> it2 = aCluster.getInstances().iterator(); it2.hasNext();) {
                final InstanceDescription id = it2.next();
                logger.error("assertTopology:   - an instance "+id.getSlingId());
            }
        }
        logger.error("assertTopology: list of all instances: "+instances.size());
        for (Iterator<InstanceDescription> it = instances.iterator(); it.hasNext();) {
            final InstanceDescription id = it.next();
            logger.error("assertTopology: - an instance: "+id.getSlingId());
        }
    }

	private boolean matches(SimpleClusterView assertedClusterView,
			ClusterView actualClusterView) {
		assertNotNull(assertedClusterView);
		assertNotNull(actualClusterView);
		if (assertedClusterView.instances.length!=actualClusterView.getInstances().size()) {
			return false;
		}
		final Set<InstanceDescription> actualInstances = new HashSet<InstanceDescription>(actualClusterView.getInstances());
		outerLoop:for(int i=0; i<assertedClusterView.instances.length; i++) {
			final Instance assertedInstance = assertedClusterView.instances[i];
			for (Iterator<InstanceDescription> it = actualInstances.iterator(); it
					.hasNext();) {
				final InstanceDescription anActualInstance = it.next();
				if (assertedInstance.slingId.equals(anActualInstance.getSlingId())) {
					continue outerLoop;
				}
			}
			return false;
		}
		return true;
	}

	private boolean pingConnector(final Instance from, final Instance to) throws UndefinedClusterViewException {
	    final Announcement fromAnnouncement = createFromAnnouncement(from);
	    Announcement replyAnnouncement = null;
	    try{
            replyAnnouncement = ping(to, fromAnnouncement);
	    } catch(AssertionError e) {
	        logger.warn("pingConnector: ping failed, assertionError: "+e);
	        return false;
	    } catch (UndefinedClusterViewException e) {
            logger.warn("pingConnector: ping failed, currently the cluster view is undefined: "+e);
            return false;
        }
        registerReplyAnnouncement(from, replyAnnouncement);
        return true;
    }

	private void registerReplyAnnouncement(Instance from,
			Announcement inheritedAnnouncement) {
		final AnnouncementRegistry announcementRegistry = from.getAnnouncementRegistry();
        if (inheritedAnnouncement.isLoop()) {
        	fail("loop detected");
        	// we dont currently support loops here in the junit tests
        	return;
        } else {
            inheritedAnnouncement.setInherited(true);
            if (announcementRegistry
                    .registerAnnouncement(inheritedAnnouncement)==-1) {
                logger.info("ping: connector response is from an instance which I already see in my topology"
                        + inheritedAnnouncement);
                return;
            }
        }
//        resultingAnnouncement = inheritedAnnouncement;
//        statusDetails = null;
	}

	private Announcement ping(Instance to, final Announcement incomingTopologyAnnouncement) 
	        throws UndefinedClusterViewException {
		final String slingId = to.slingId;
		final ClusterViewService clusterViewService = to.getClusterViewService();
		final AnnouncementRegistry announcementRegistry = to.getAnnouncementRegistry();
		
		incomingTopologyAnnouncement.removeInherited(slingId);

        final Announcement replyAnnouncement = new Announcement(
                slingId);

        long backoffInterval = -1;
        if (!incomingTopologyAnnouncement.isCorrectVersion()) {
        	fail("incorrect version");
            return null; // never reached
        } else if (clusterViewService.contains(incomingTopologyAnnouncement
                .getOwnerId())) {
        	fail("loop=true");
            return null; // never reached
        } else if (clusterViewService.containsAny(incomingTopologyAnnouncement
                .listInstances())) {
        	fail("incoming announcement contains instances that are part of my cluster");
            return null; // never reached
        } else {
            backoffInterval = announcementRegistry
                    .registerAnnouncement(incomingTopologyAnnouncement);
            if (backoffInterval==-1) {
            	fail("rejecting an announcement from an instance that I already see in my topology: ");
                return null; // never reached
            } else {
                // normal, successful case: replying with the part of the topology which this instance sees
                final ClusterView clusterView = clusterViewService
                        .getClusterView();
                replyAnnouncement.setLocalCluster(clusterView);
                announcementRegistry.addAllExcept(replyAnnouncement, clusterView,
                        new AnnouncementFilter() {

                            public boolean accept(final String receivingSlingId, Announcement announcement) {
                                if (announcement.getPrimaryKey().equals(
                                        incomingTopologyAnnouncement
                                                .getPrimaryKey())) {
                                    return false;
                                }
                                return true;
                            }
                        });
                return replyAnnouncement;
            }
        }
	}

	private Announcement createFromAnnouncement(final Instance from) throws UndefinedClusterViewException {
		// TODO: refactor TopologyConnectorClient to avoid duplicating code from there (ping())
		Announcement topologyAnnouncement = new Announcement(from.slingId);
        topologyAnnouncement.setServerInfo(from.slingId);
        final ClusterView clusterView = from.getClusterViewService().getClusterView();
        topologyAnnouncement.setLocalCluster(clusterView);
        from.getAnnouncementRegistry().addAllExcept(topologyAnnouncement, clusterView, new AnnouncementFilter() {
            
            public boolean accept(final String receivingSlingId, final Announcement announcement) {
                // filter out announcements that are of old cluster instances
                // which I dont really have in my cluster view at the moment
                final Iterator<InstanceDescription> it = 
                        clusterView.getInstances().iterator();
                while(it.hasNext()) {
                    final InstanceDescription instance = it.next();
                    if (instance.getSlingId().equals(receivingSlingId)) {
                        // then I have the receiving instance in my cluster view
                        // all fine then
                        return true;
                    }
                }
                // looks like I dont have the receiving instance in my cluster view
                // then I should also not propagate that announcement anywhere
                return false;
            }
        });
        return topologyAnnouncement;
	}

	@Test
    public void testStableClusterId() throws Throwable {
        logger.info("testStableClusterId: start");
    	// stop 1 and 2 and create them with a lower heartbeat timeout
    	instance2.stopHeartbeats();
    	instance1.stopHeartbeats();
        instance2.stop();
        instance1.stop();
	// SLING-4302 : first set the heartbeatTimeout to 100 sec - large enough to work on all CI instances
        instance1 = Instance.newStandaloneInstance("/var/discovery/impl/", "firstInstance", true, 100, 1);
        instance2 = Instance.newClusterInstance("/var/discovery/impl/", "secondInstance", instance1,
                false, 100, 1);
        assertNotNull(instance1);
        assertNotNull(instance2);

        try{
            instance1.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }
        try{
            instance2.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }

        // let the sync/voting happen
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        
        String newClusterId1 = instance1.getClusterViewService()
                .getClusterView().getId();
        String newClusterId2 = instance2.getClusterViewService()
                .getClusterView().getId();
        // both cluster ids must be the same
        assertEquals(newClusterId1, newClusterId1);
        
        instance1.dumpRepo();
        assertEquals(2, instance1.getClusterViewService().getClusterView().getInstances().size());
        assertEquals(2, instance2.getClusterViewService().getClusterView().getInstances().size());
        
        // let instance2 'die' by now longer doing heartbeats
	// SLING-4302 : then set the heartbeatTimeouts back to 1 sec to have them properly time out with the sleeps applied below
        instance2.getConfig().setHeartbeatTimeout(1);
        instance1.getConfig().setHeartbeatTimeout(1);
        instance2.stopHeartbeats(); // would actually not be necessary as it was never started.. this test only runs heartbeats manually
        instance1.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        // the cluster should now have size 1
        assertEquals(1, instance1.getClusterViewService().getClusterView().getInstances().size());
        // the instance 2 should be in isolated mode as it is no longer in the established view
        // hence null
        try{
            instance2.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }

        // but the cluster id must have remained stable
        instance1.dumpRepo();
        String actualClusterId = instance1.getClusterViewService()
                .getClusterView().getId();
        logger.info("expected cluster id: "+newClusterId1);
        logger.info("actual   cluster id: "+actualClusterId);
		assertEquals(newClusterId1, actualClusterId);
        logger.info("testStableClusterId: end");
    }
    
    @Test
    public void testClusterView() throws Exception {
        logger.info("testClusterView: start");
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNull(instance3);
        instance3 = Instance.newClusterInstance("thirdInstance", instance1,
                false);
        assertNotNull(instance3);

        assertEquals(instance1.getSlingId(), instance1.getClusterViewService()
                .getSlingId());
        assertEquals(instance2.getSlingId(), instance2.getClusterViewService()
                .getSlingId());
        assertEquals(instance3.getSlingId(), instance3.getClusterViewService()
                .getSlingId());

        try{
            instance1.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }
        try{
            instance2.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }
        try{
            instance3.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }

        instance1.dumpRepo();

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();

        instance1.dumpRepo();
        logger.info("testClusterView: 1st 2s sleep");
        Thread.sleep(2000);

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        logger.info("testClusterView: 2nd 2s sleep");
        Thread.sleep(2000);

        instance1.dumpRepo();
        String clusterId1 = instance1.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId1=" + clusterId1);
        String clusterId2 = instance2.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId2=" + clusterId2);
        String clusterId3 = instance3.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId3=" + clusterId3);
        assertEquals(clusterId1, clusterId2);
        assertEquals(clusterId1, clusterId3);

        assertEquals(3, instance1.getClusterViewService().getClusterView()
                .getInstances().size());
        assertEquals(3, instance2.getClusterViewService().getClusterView()
                .getInstances().size());
        assertEquals(3, instance3.getClusterViewService().getClusterView()
                .getInstances().size());
        logger.info("testClusterView: end");
    }

    @Test
    public void testAdditionalInstance() throws Throwable {
        logger.info("testAdditionalInstance: start");
        assertNotNull(instance1);
        assertNotNull(instance2);

        assertEquals(instance1.getSlingId(), instance1.getClusterViewService()
                .getSlingId());
        assertEquals(instance2.getSlingId(), instance2.getClusterViewService()
                .getSlingId());

        try{
            instance1.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }
        try{
            instance2.getClusterViewService().getClusterView();
            fail("should complain");
        } catch(UndefinedClusterViewException e) {
            // ok
        }

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();

        instance1.dumpRepo();
        logger.info("testAdditionalInstance: 1st 2s sleep");
        Thread.sleep(2000);

        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        logger.info("testAdditionalInstance: 2nd 2s sleep");
        Thread.sleep(2000);

        instance1.dumpRepo();
        String clusterId1 = instance1.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId1=" + clusterId1);
        String clusterId2 = instance2.getClusterViewService().getClusterView()
                .getId();
        logger.info("clusterId2=" + clusterId2);
        assertEquals(clusterId1, clusterId2);

        assertEquals(2, instance1.getClusterViewService().getClusterView()
                .getInstances().size());
        assertEquals(2, instance2.getClusterViewService().getClusterView()
                .getInstances().size());

        AssertingTopologyEventListener assertingTopologyEventListener = new AssertingTopologyEventListener();
        assertingTopologyEventListener.addExpected(Type.TOPOLOGY_INIT);
        assertEquals(1, assertingTopologyEventListener.getRemainingExpectedCount());
        instance1.bindTopologyEventListener(assertingTopologyEventListener);
        Thread.sleep(500); // SLING-4755: async event sending requires some minimal wait time nowadays
        assertEquals(0, assertingTopologyEventListener.getRemainingExpectedCount());

        // startup instance 3
        AcceptsMultiple acceptsMultiple = new AcceptsMultiple(
                Type.TOPOLOGY_CHANGING, Type.TOPOLOGY_CHANGED);
        assertingTopologyEventListener.addExpected(acceptsMultiple);
        assertingTopologyEventListener.addExpected(acceptsMultiple);
        instance3 = Instance.newClusterInstance("thirdInstance", instance1,
                false);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        logger.info("testAdditionalInstance: 3rd 2s sleep");
        Thread.sleep(2000);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        logger.info("testAdditionalInstance: 4th 2s sleep");
        Thread.sleep(2000);
        assertEquals(1, acceptsMultiple.getEventCnt(Type.TOPOLOGY_CHANGING));
        assertEquals(1, acceptsMultiple.getEventCnt(Type.TOPOLOGY_CHANGED));
        logger.info("testAdditionalInstance: end");
    }

    @Test
    public void testPropertyProviders() throws Throwable {
        logger.info("testPropertyProviders: start");
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        assertNull(instance3);
        instance3 = Instance.newClusterInstance("thirdInstance", instance1,
                false);
        instance3.runHeartbeatOnce();
        logger.info("testPropertyProviders: 1st 2s sleep");
        Thread.sleep(2000);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        logger.info("testPropertyProviders: 2nd 2s sleep");
        Thread.sleep(2000);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        instance3.runHeartbeatOnce();
        logger.info("testPropertyProviders: 3rd 2s sleep");
        Thread.sleep(2000);

        property1Value = UUID.randomUUID().toString();
        property1Name = UUID.randomUUID().toString();
        PropertyProviderImpl pp1 = new PropertyProviderImpl();
        pp1.setProperty(property1Name, property1Value);
        instance1.bindPropertyProvider(pp1, property1Name);

        property2Value = UUID.randomUUID().toString();
        property2Name = UUID.randomUUID().toString();
        PropertyProviderImpl pp2 = new PropertyProviderImpl();
        pp2.setProperty(property2Name, property2Value);
        instance2.bindPropertyProvider(pp2, property2Name);

        assertPropertyValues();

        property1Value = UUID.randomUUID().toString();
        pp1.setProperty(property1Name, property1Value);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();

        assertPropertyValues();
        assertNull(instance1.getClusterViewService().getClusterView()
                .getInstances().get(0)
                .getProperty(UUID.randomUUID().toString()));
        assertNull(instance2.getClusterViewService().getClusterView()
                .getInstances().get(0)
                .getProperty(UUID.randomUUID().toString()));
        logger.info("testPropertyProviders: end");
    }

    private void assertPropertyValues() throws UndefinedClusterViewException {
        assertPropertyValues(instance1.getSlingId(), property1Name,
                property1Value);
        assertPropertyValues(instance2.getSlingId(), property2Name,
                property2Value);
    }

    private void assertPropertyValues(String slingId, String name, String value) throws UndefinedClusterViewException {
        assertEquals(value, getInstance(instance1, slingId).getProperty(name));
        assertEquals(value, getInstance(instance2, slingId).getProperty(name));
    }

    private InstanceDescription getInstance(Instance instance, String slingId) throws UndefinedClusterViewException {
        Iterator<InstanceDescription> it = instance.getClusterViewService()
                .getClusterView().getInstances().iterator();
        while (it.hasNext()) {
            InstanceDescription id = it.next();
            if (id.getSlingId().equals(slingId)) {
                return id;
            }
        }
        throw new IllegalStateException("instance not found: instance="
                + instance + ", slingId=" + slingId);
    }
    
    class LongRunningListener implements TopologyEventListener {
        
        String failMsg = null;
        
        boolean initReceived = false;
        int noninitReceived;

        private Semaphore changedSemaphore = new Semaphore(0);
        
        public void assertNoFail() {
            if (failMsg!=null) {
                fail(failMsg);
            }
        }
        
        public Semaphore getChangedSemaphore() {
            return changedSemaphore;
        }
        
        public void handleTopologyEvent(TopologyEvent event) {
            if (failMsg!=null) {
                failMsg += "/ Already failed, got another event; "+event;
                return;
            }
            if (!initReceived) {
                if (event.getType()!=Type.TOPOLOGY_INIT) {
                    failMsg = "Expected TOPOLOGY_INIT first, got: "+event.getType();
                    return;
                }
                initReceived = true;
                return;
            }
            if (event.getType()==Type.TOPOLOGY_CHANGED) {
                try {
                    changedSemaphore.acquire();
                } catch (InterruptedException e) {
                    throw new Error("don't interrupt me pls: "+e);
                }
            }
            noninitReceived++;
        }
    }
    
    /**
     * Test plan:
     *  * have a discoveryservice with two listeners registered
     *  * one of them (the 'first' one) is long running
     *  * during one of the topology changes, when the first
     *    one is hit, deactivate the discovery service
     *  * that deactivation used to block (SLING-4755) due
     *    to synchronized(lock) which was blocked by the
     *    long running listener. With having asynchronous
     *    event sending this should no longer be the case
     *  * also, once asserted that deactivation finished,
     *    and that the first listener is still busy, make
     *    sure that once the first listener finishes, that
     *    the second listener still gets the event
     * @throws Throwable 
     */
    @Test
    public void testLongRunningListener() throws Throwable {
        // let the instance1 become alone, instance2 is idle
        instance1.getConfig().setHeartbeatTimeout(2);
        instance2.getConfig().setHeartbeatTimeout(2);
        logger.info("testLongRunningListener : letting instance2 remain silent from now on");
        instance1.runHeartbeatOnce();
        Thread.sleep(1500);
        instance1.runHeartbeatOnce();
        Thread.sleep(1500);
        instance1.runHeartbeatOnce();
        Thread.sleep(1500);
        instance1.runHeartbeatOnce();
        logger.info("testLongRunningListener : instance 2 should now be considered dead");
//        instance1.dumpRepo();
        
        LongRunningListener longRunningListener1 = new LongRunningListener();
        AssertingTopologyEventListener fastListener2 = new AssertingTopologyEventListener();
        fastListener2.addExpected(Type.TOPOLOGY_INIT);
        longRunningListener1.assertNoFail();
        assertEquals(1, fastListener2.getRemainingExpectedCount());
        logger.info("testLongRunningListener : binding longRunningListener1 ...");
        instance1.bindTopologyEventListener(longRunningListener1);
        logger.info("testLongRunningListener : binding fastListener2 ...");
        instance1.bindTopologyEventListener(fastListener2);
        logger.info("testLongRunningListener : waiting a bit for longRunningListener1 to receive the TOPOLOGY_INIT event");
        Thread.sleep(2500); // SLING-4755: async event sending requires some minimal wait time nowadays
        assertEquals(0, fastListener2.getRemainingExpectedCount());
        assertTrue(longRunningListener1.initReceived);
        
        // after INIT, now do an actual change where listener1 will do a long-running handling
        fastListener2.addExpected(Type.TOPOLOGY_CHANGING);
        fastListener2.addExpected(Type.TOPOLOGY_CHANGED);
        instance1.getConfig().setHeartbeatTimeout(10);
        instance2.getConfig().setHeartbeatTimeout(10);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(500);
        instance1.runHeartbeatOnce();
        instance2.runHeartbeatOnce();
        Thread.sleep(500);
        
        instance1.dumpRepo();
        longRunningListener1.assertNoFail();
        // nothing unexpected should arrive at listener2:
        assertEquals(0, fastListener2.getUnexpectedCount());
        // however, listener2 should only get one (CHANGING) event, cos the CHANGED event is still blocked
        assertEquals(1, fastListener2.getRemainingExpectedCount());
        // and also listener2 should only get CHANGING, the CHANGED is blocked via changedSemaphore
        assertEquals(1, longRunningListener1.noninitReceived);
        assertTrue(longRunningListener1.getChangedSemaphore().hasQueuedThreads());
        Thread.sleep(2000);
        // even after a 2sec sleep things should be unchanged:
        assertEquals(0, fastListener2.getUnexpectedCount());
        assertEquals(1, fastListener2.getRemainingExpectedCount());
        assertEquals(1, longRunningListener1.noninitReceived);
        assertTrue(longRunningListener1.getChangedSemaphore().hasQueuedThreads());
        
        // now let's simulate SLING-4755: deactivation while longRunningListener1 does long processing
        // - which is simulated by waiting on changedSemaphore.
        final List<Exception> asyncException = new LinkedList<Exception>();
        Thread th = new Thread(new Runnable() {

            public void run() {
                try {
                    instance1.stop();
                } catch (Exception e) {
                    synchronized(asyncException) {
                        asyncException.add(e);
                    }
                }
            }
            
        });
        th.start();
        logger.info("Waiting max 4 sec...");
        th.join(4000);
        logger.info("Done waiting max 4 sec...");
        if (th.isAlive()) {
            logger.warn("Thread still alive: "+th.isAlive());
            // release before issuing fail as otherwise test will block forever
            longRunningListener1.getChangedSemaphore().release();
            fail("Thread was still alive");
        }
        logger.info("Thread was no longer alive: "+th.isAlive());
        synchronized(asyncException) {
            logger.info("Async exceptions: "+asyncException.size());
            if (asyncException.size()!=0) {
                // release before issuing fail as otherwise test will block forever
                longRunningListener1.getChangedSemaphore().release();
                fail("async exceptions: "+asyncException.size()+", first: "+asyncException.get(0));
            }
        }
        
        // now the test consists of
        // a) the fact that we reached this place without unlocking the changedSemaphore
        // b) when we now unlock the changedSemaphore the remaining events should flush through
        longRunningListener1.getChangedSemaphore().release();
        Thread.sleep(500);// shouldn't take long and then things should have flushed:
        assertEquals(0, fastListener2.getUnexpectedCount());
        assertEquals(0, fastListener2.getRemainingExpectedCount());
        assertEquals(2, longRunningListener1.noninitReceived);
        assertFalse(longRunningListener1.getChangedSemaphore().hasQueuedThreads());
    }

    
}
