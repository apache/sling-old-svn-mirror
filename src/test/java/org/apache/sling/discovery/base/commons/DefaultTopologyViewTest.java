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
package org.apache.sling.discovery.base.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.base.its.setup.TopologyHelper;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;
import org.apache.sling.discovery.commons.providers.spi.LocalClusterView;
import org.junit.Test;

import junitx.util.PrivateAccessor;

public class DefaultTopologyViewTest {

    @Test
    public void testForcedLeaderChangeCompare() throws Exception {
        // create view 1 with first instance the leader
        final String slingId1 = UUID.randomUUID().toString();
        final DefaultTopologyView view1 = TopologyHelper.createTopologyView(UUID
                .randomUUID().toString(), slingId1);
        final DefaultInstanceDescription id2 = TopologyHelper.addInstanceDescription(view1, TopologyHelper
                .createInstanceDescription(view1.getClusterViews().iterator()
                        .next()));
        final String slingId2 = id2.getSlingId();
        final DefaultInstanceDescription id3 = TopologyHelper.addInstanceDescription(view1, TopologyHelper
                .createInstanceDescription(view1.getClusterViews().iterator()
                        .next()));
        final String slingId3 = id3.getSlingId();
        
        // now create view 2 with exactly the same instances as above, but the second instance the leader
        DefaultTopologyView view2 = TopologyHelper.cloneTopologyView(view1, slingId2);
        // make sure we've chosen a new leader:
        assertNotEquals(view1.getClusterViews().iterator().next().getLeader().getSlingId(),
                view2.getClusterViews().iterator().next().getLeader().getSlingId());
        // and now test the compare method which should catch the leader change
        assertTrue(view1.compareTopology(view2)==Type.TOPOLOGY_CHANGED);
        
        // same thing now with view3 which takes slingId3 as the leader
        DefaultTopologyView view3 = TopologyHelper.cloneTopologyView(view1, slingId3);
        // make sure we've chosen a new leader:
        assertNotEquals(view1.getClusterViews().iterator().next().getLeader().getSlingId(),
                view3.getClusterViews().iterator().next().getLeader().getSlingId());
        // and now test the compare method which should catch the leader change
        assertTrue(view1.compareTopology(view3)==Type.TOPOLOGY_CHANGED);
    }
    
    @Test
    public void testComparelocalClusterSyncTokenId() throws Exception {
        String clusterViewId = UUID.randomUUID().toString();
        String slingId = UUID.randomUUID().toString();
        String syncTokenId = UUID.randomUUID().toString();

        DefaultTopologyView t1 = createSingleInstanceTopology(slingId, clusterViewId, syncTokenId);
        DefaultTopologyView t2 = createSingleInstanceTopology(slingId, clusterViewId, syncTokenId);

        assertNull(t1.compareTopology(t2));
        assertNull(t2.compareTopology(t1));
        
        DefaultTopologyView t3 = createSingleInstanceTopology(slingId, clusterViewId, UUID.randomUUID().toString());
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t1.compareTopology(t3));
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t3.compareTopology(t1));
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t2.compareTopology(t3));
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t3.compareTopology(t2));
        
        DefaultTopologyView t4 = createSingleInstanceTopology(slingId, clusterViewId, null);
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t1.compareTopology(t4));
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t4.compareTopology(t1));
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t2.compareTopology(t4));
        assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, t4.compareTopology(t2));

        DefaultTopologyView t5 = createSingleInstanceTopology(slingId, clusterViewId, null);
        assertNull(t5.compareTopology(t4));
        assertNull(t4.compareTopology(t5));
    }

    private DefaultTopologyView createSingleInstanceTopology(String slingId, String clusterViewId, String syncTokenId) {
        LocalClusterView clusterView = new LocalClusterView(clusterViewId, syncTokenId);
        DefaultInstanceDescription instance = 
                TopologyHelper.createInstanceDescription(slingId, true, clusterView);
        DefaultTopologyView t = new DefaultTopologyView();
        t.setLocalClusterView(clusterView);
        return t;
    }
    
    @Test
    public void testCompare() throws Exception {

        DefaultTopologyView newView = TopologyHelper.createTopologyView(UUID
                .randomUUID().toString(), UUID.randomUUID().toString());

        try {
            newView.compareTopology(null);
            fail("Should complain about null");
        } catch (Exception e) {
            // ok
        }

        DefaultTopologyView oldView = TopologyHelper
                .cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        DefaultInstanceDescription id = TopologyHelper
                .createInstanceDescription(newView.getClusterViews().iterator()
                        .next());
        TopologyHelper.addInstanceDescription(newView, id);
        assertEquals(Type.TOPOLOGY_CHANGED, newView.compareTopology(oldView));

        assertEquals(2, newView.getInstances().size());
        // addInstanceDescription now no longer throws an exception if you add
        // the same
        // instance twice. this provides greater stability
        TopologyHelper.addInstanceDescription(newView, id);
        assertEquals(2, newView.getInstances().size());
        // try{
        // TopologyTestHelper.addInstanceDescription(newView, id);
        // fail("should not be able to add twice");
        // } catch(Exception e) {
        // // ok
        // }

        oldView = TopologyHelper.cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        DefaultInstanceDescription instance = (DefaultInstanceDescription) newView.getInstances().iterator().next();
        instance.setProperty("a", "b");
        assertEquals(Type.PROPERTIES_CHANGED, newView.compareTopology(oldView));
        oldView = TopologyHelper.cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        instance.setProperty("a", "B");
        assertEquals(Type.PROPERTIES_CHANGED, newView.compareTopology(oldView));
        oldView = TopologyHelper.cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        instance.setProperty("a", "B");
        assertNull(newView.compareTopology(oldView));
        
        // now change the properties of the first instance but modify the second instance' cluster
        Iterator<InstanceDescription> it = newView.getInstances().iterator();
        DefaultInstanceDescription firstInstance = (DefaultInstanceDescription) it.next();
        assertNotNull(firstInstance);
        DefaultInstanceDescription secondInstance = (DefaultInstanceDescription) it.next();
        assertNotNull(secondInstance);
        firstInstance.setProperty("c", "d");
        DefaultClusterView cluster = new DefaultClusterView(UUID.randomUUID().toString());
        PrivateAccessor.setField(secondInstance, "clusterView", null);
        cluster.addInstanceDescription(secondInstance);
        assertEquals(Type.TOPOLOGY_CHANGED, newView.compareTopology(oldView));
    }

    @Test
    public void testFind() throws Exception {
        DefaultTopologyView newView = TopologyHelper.createTopologyView(UUID
                .randomUUID().toString(), UUID.randomUUID().toString());
        TopologyHelper.createAndAddInstanceDescription(newView, newView
                .getClusterViews().iterator().next());

        try {
            newView.findInstances(null);
            fail("should complain");
        } catch (IllegalArgumentException iae) {
            // ok
        }

        final DefaultInstanceDescription id = TopologyHelper
                .createAndAddInstanceDescription(newView, newView
                        .getClusterViews().iterator().next());
        TopologyHelper.createAndAddInstanceDescription(newView, newView
                .getClusterViews().iterator().next());
        assertEquals(4, newView.findInstances(new InstanceFilter() {

            public boolean accept(InstanceDescription instance) {
                return true;
            }
        }).size());
        assertEquals(1, newView.findInstances(new InstanceFilter() {

            public boolean accept(InstanceDescription instance) {
                return instance.getSlingId().equals(id.getSlingId());
            }
        }).size());
        assertEquals(1, newView.findInstances(new InstanceFilter() {

            public boolean accept(InstanceDescription instance) {
                return instance.isLeader();
            }
        }).size());
        assertEquals(1, newView.findInstances(new InstanceFilter() {
            boolean first = true;

            public boolean accept(InstanceDescription instance) {
                if (!first) {
                    return false;
                }
                first = false;
                return true;
            }
        }).size());
    }

    @Test
    public void testGetInstances() throws Exception {
        DefaultTopologyView newView = TopologyHelper.createTopologyView(UUID
                .randomUUID().toString(), UUID.randomUUID().toString());

        Set<InstanceDescription> instances = newView.getInstances();
        assertNotNull(instances);

        try {
            instances.remove(instances.iterator().next());
            fail("list should not be modifiable");
        } catch (Exception e) {
            // ok
        }

    }

}
