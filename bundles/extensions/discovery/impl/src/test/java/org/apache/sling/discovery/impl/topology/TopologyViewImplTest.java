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
package org.apache.sling.discovery.impl.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import junitx.util.PrivateAccessor;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;
import org.apache.sling.discovery.impl.common.DefaultInstanceDescriptionImpl;
import org.junit.Test;

public class TopologyViewImplTest {

    @Test
    public void testCompare() throws Exception {

        TopologyViewImpl newView = TopologyTestHelper.createTopologyView(UUID
                .randomUUID().toString(), UUID.randomUUID().toString());

        try {
            newView.compareTopology(null);
            fail("Should complain about null");
        } catch (Exception e) {
            // ok
        }

        TopologyViewImpl oldView = TopologyTestHelper
                .cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        DefaultInstanceDescriptionImpl id = TopologyTestHelper
                .createInstanceDescription(newView.getClusterViews().iterator()
                        .next());
        TopologyTestHelper.addInstanceDescription(newView, id);
        assertEquals(Type.TOPOLOGY_CHANGED, newView.compareTopology(oldView));

        assertEquals(2, newView.getInstances().size());
        // addInstanceDescription now no longer throws an exception if you add
        // the same
        // instance twice. this provides greater stability
        TopologyTestHelper.addInstanceDescription(newView, id);
        assertEquals(2, newView.getInstances().size());
        // try{
        // TopologyTestHelper.addInstanceDescription(newView, id);
        // fail("should not be able to add twice");
        // } catch(Exception e) {
        // // ok
        // }

        oldView = TopologyTestHelper.cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        TopologyTestHelper.getWriteableProperties(
                newView.getInstances().iterator().next()).put("a", "b");
        assertEquals(Type.PROPERTIES_CHANGED, newView.compareTopology(oldView));
        oldView = TopologyTestHelper.cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        TopologyTestHelper.getWriteableProperties(
                newView.getInstances().iterator().next()).put("a", "B");
        assertEquals(Type.PROPERTIES_CHANGED, newView.compareTopology(oldView));
        oldView = TopologyTestHelper.cloneTopologyView(newView);
        assertNull(newView.compareTopology(oldView));

        Map<String, String> p = TopologyTestHelper
                .getWriteableProperties(newView.getInstances().iterator()
                        .next());
        p.remove("a");
        p.put("a", "B");
        assertNull(newView.compareTopology(oldView));
        
        // now change the properties of the first instance but modify the second instance' cluster
        Iterator<InstanceDescription> it = newView.getInstances().iterator();
        DefaultInstanceDescriptionImpl firstInstance = (DefaultInstanceDescriptionImpl) it.next();
        assertNotNull(firstInstance);
        DefaultInstanceDescriptionImpl secondInstance = (DefaultInstanceDescriptionImpl) it.next();
        assertNotNull(secondInstance);
        TopologyTestHelper.getWriteableProperties(
                firstInstance).put("c", "d");
        DefaultClusterViewImpl cluster = new DefaultClusterViewImpl(UUID.randomUUID().toString());
        PrivateAccessor.setField(secondInstance, "clusterView", null);
        cluster.addInstanceDescription(secondInstance);
        assertEquals(Type.TOPOLOGY_CHANGED, newView.compareTopology(oldView));
    }

    @Test
    public void testFind() throws Exception {
        TopologyViewImpl newView = TopologyTestHelper.createTopologyView(UUID
                .randomUUID().toString(), UUID.randomUUID().toString());
        TopologyTestHelper.createAndAddInstanceDescription(newView, newView
                .getClusterViews().iterator().next());

        try {
            newView.findInstances(null);
            fail("should complain");
        } catch (IllegalArgumentException iae) {
            // ok
        }

        final DefaultInstanceDescriptionImpl id = TopologyTestHelper
                .createAndAddInstanceDescription(newView, newView
                        .getClusterViews().iterator().next());
        TopologyTestHelper.createAndAddInstanceDescription(newView, newView
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
        TopologyViewImpl newView = TopologyTestHelper.createTopologyView(UUID
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
