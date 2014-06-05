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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import junitx.util.PrivateAccessor;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;
import org.apache.sling.discovery.impl.common.DefaultInstanceDescriptionImpl;
import org.apache.sling.discovery.impl.setup.Instance;

public class TopologyTestHelper {

    public static TopologyViewImpl createTopologyView(String clusterViewId,
            String slingId) {
        TopologyViewImpl t = new TopologyViewImpl();
        DefaultClusterViewImpl c = new DefaultClusterViewImpl(clusterViewId);
        DefaultInstanceDescriptionImpl i = new DefaultInstanceDescriptionImpl(
                c, true, false, slingId, new HashMap<String, String>());
        Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();
        instances.add(i);
        t.addInstances(instances);
        return t;
    }

    public static TopologyViewImpl cloneTopologyView(TopologyViewImpl original) {
        TopologyViewImpl t = new TopologyViewImpl();
        Iterator<ClusterView> it = original.getClusterViews().iterator();
        while (it.hasNext()) {
            DefaultClusterViewImpl c = (DefaultClusterViewImpl) it.next();
            t.addInstances(clone(c).getInstances());
        }
        return t;
    }

    public static DefaultClusterViewImpl clone(DefaultClusterViewImpl original) {
        DefaultClusterViewImpl c = new DefaultClusterViewImpl(original.getId());
        Iterator<InstanceDescription> it = original.getInstances().iterator();
        while (it.hasNext()) {
            DefaultInstanceDescriptionImpl id = (DefaultInstanceDescriptionImpl) it
                    .next();
            c.addInstanceDescription(cloneWOClusterView(id));
        }
        return c;
    }

    public static DefaultInstanceDescriptionImpl cloneWOClusterView(
            DefaultInstanceDescriptionImpl original) {
        DefaultInstanceDescriptionImpl id = new DefaultInstanceDescriptionImpl(
                null, original.isLeader(), original.isLocal(),
                original.getSlingId(), new HashMap<String, String>(
                        original.getProperties()));
        return id;
    }

    public static DefaultInstanceDescriptionImpl createAndAddInstanceDescription(
            TopologyViewImpl newView, ClusterView clusterView) {
        DefaultInstanceDescriptionImpl i = createInstanceDescription(clusterView);
        return addInstanceDescription(newView, i);
    }

    public static DefaultInstanceDescriptionImpl addInstanceDescription(
            TopologyViewImpl newView, DefaultInstanceDescriptionImpl i) {
        Collection<InstanceDescription> instances = new LinkedList<InstanceDescription>();
        instances.add(i);
        newView.addInstances(instances);
        return i;
    }

    public static DefaultInstanceDescriptionImpl createInstanceDescription(
            ClusterView clusterView) {
        return createInstanceDescription(UUID.randomUUID().toString(), false, clusterView);
    }
    
    public static DefaultInstanceDescriptionImpl createInstanceDescription(
            String instanceId, boolean isLocal, ClusterView clusterView) {
        if (!(clusterView instanceof DefaultClusterViewImpl)) {
            throw new IllegalArgumentException(
                    "Must pass a clusterView of type "
                            + DefaultClusterViewImpl.class);
        }
        DefaultInstanceDescriptionImpl i = new DefaultInstanceDescriptionImpl(
                (DefaultClusterViewImpl) clusterView, false, isLocal, instanceId, new HashMap<String, String>());
        return i;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getWriteableProperties(
            InstanceDescription instanceDescription)
            throws NoSuchFieldException {
        return (Map<String, String>) PrivateAccessor.getField(
                instanceDescription, "properties");
    }
    
    public static void assertTopologyConsistsOf(TopologyView topology, String... slingIds) {
        assertNotNull(topology);
        assertEquals(topology.getInstances().size(), slingIds.length);
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
    
    public static Instance createInstance(Collection<Instance> instances, String debugName) throws Exception {
        final Instance instance = Instance.newStandaloneInstance(debugName, true);
        instances.add(instance);
        return instance;
    }

}
