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
package org.apache.sling.discovery.base.its.setup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.base.commons.DefaultTopologyView;
import org.apache.sling.discovery.commons.providers.DefaultClusterView;
import org.apache.sling.discovery.commons.providers.DefaultInstanceDescription;

import junitx.util.PrivateAccessor;

public class TopologyHelper {

    public static DefaultInstanceDescription createInstanceDescription(
            ClusterView clusterView) {
        return createInstanceDescription(UUID.randomUUID().toString(), false, clusterView);
    }
    
    public static DefaultInstanceDescription createInstanceDescription(
            String instanceId, boolean isLocal, ClusterView clusterView) {
        if (!(clusterView instanceof DefaultClusterView)) {
            throw new IllegalArgumentException(
                    "Must pass a clusterView of type "
                            + DefaultClusterView.class);
        }
        DefaultInstanceDescription i = new DefaultInstanceDescription(
                (DefaultClusterView) clusterView, false, isLocal, instanceId, new HashMap<String, String>());
        return i;
    }

    public static DefaultTopologyView createTopologyView(String clusterViewId,
            String slingId) {
        DefaultTopologyView t = new DefaultTopologyView();
        DefaultClusterView c = new DefaultClusterView(clusterViewId);
        DefaultInstanceDescription i = new DefaultInstanceDescription(
                c, true, false, slingId, new HashMap<String, String>());
        Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();
        instances.add(i);
        t.addInstances(instances);
        return t;
    }

    public static DefaultTopologyView cloneTopologyView(DefaultTopologyView original) {
        DefaultTopologyView t = new DefaultTopologyView();
        Iterator<ClusterView> it = original.getClusterViews().iterator();
        while (it.hasNext()) {
            DefaultClusterView c = (DefaultClusterView) it.next();
            t.addInstances(clone(c).getInstances());
        }
        return t;
    }

    public static DefaultClusterView clone(DefaultClusterView original) {
        DefaultClusterView c = new DefaultClusterView(original.getId());
        Iterator<InstanceDescription> it = original.getInstances().iterator();
        while (it.hasNext()) {
            DefaultInstanceDescription id = (DefaultInstanceDescription) it
                    .next();
            c.addInstanceDescription(cloneWOClusterView(id));
        }
        return c;
    }

    public static DefaultInstanceDescription cloneWOClusterView(
            DefaultInstanceDescription original) {
        DefaultInstanceDescription id = new DefaultInstanceDescription(
                null, original.isLeader(), original.isLocal(),
                original.getSlingId(), new HashMap<String, String>(
                        original.getProperties()));
        return id;
    }

    public static DefaultInstanceDescription createAndAddInstanceDescription(
            DefaultTopologyView newView, ClusterView clusterView) {
        DefaultInstanceDescription i = createInstanceDescription(clusterView);
        return addInstanceDescription(newView, i);
    }

    public static DefaultInstanceDescription addInstanceDescription(
            DefaultTopologyView newView, DefaultInstanceDescription i) {
        Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();
        instances.add(i);
        newView.addInstances(instances);
        return i;
    }

    public static DefaultTopologyView cloneTopologyView(DefaultTopologyView view,
            String newLeader) throws NoSuchFieldException {
        final DefaultTopologyView clone = cloneTopologyView(view);
        final DefaultClusterView cluster = (DefaultClusterView) clone.getClusterViews().iterator().next();
        for (Iterator it = cluster.getInstances().iterator(); it.hasNext();) {
            DefaultInstanceDescription id = (DefaultInstanceDescription) it.next();
            PrivateAccessor.setField(id, "isLeader", id.getSlingId().equals(newLeader));
        }
        return clone;
    }

    public static void assertTopologyConsistsOf(TopologyView topology, String... slingIds) {
        assertNotNull(topology);
        assertEquals(slingIds.length, topology.getInstances().size());
        for(int i=0; i<slingIds.length; i++) {
            final String aSlingId = slingIds[i];
            final Set<?> instances = topology.getInstances();
            boolean found = false;
            for (Iterator<?> it = instances.iterator(); it.hasNext();) {
                InstanceDescription anInstance = (InstanceDescription) it.next();
                if (anInstance.getSlingId().equals(aSlingId)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
    
}
