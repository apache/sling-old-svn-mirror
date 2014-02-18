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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.discovery.impl.topology.announcement.Announcement;
import org.apache.sling.discovery.impl.topology.connector.TopologyConnectorClientInformation;
import org.junit.Test;

public class TopologyTest {

    class Connector {
        
        private final Instance from;
        private final Instance to;
        private final int jettyPort;
        private final TopologyConnectorClientInformation connectorInfo;

        Connector(Instance from, Instance to) throws Throwable {
            this.from = from;
            this.to = to;
            to.startJetty();
            this.jettyPort = to.getJettyPort();
            this.connectorInfo = from.connectTo("http://localhost:"+jettyPort+"/system/console/topology/connector");
        }
    }
    
    private Instance createInstance(String debugName) throws Exception {
        return Instance.newStandaloneInstance(debugName, true);
    }

    private Connector createConnector(Instance instance1, Instance instance2) throws Throwable {
        return new Connector(instance1, instance2);
    }
    
    @Test
    public void testTwoNodes() throws Throwable {
        Instance instance1 = createInstance("instance1");
        Instance instance2 = createInstance("instance2");
        instance1.getConfig().setHeartbeatTimeout(2);
        instance1.getConfig().setHeartbeatInterval(1);
        instance2.getConfig().setHeartbeatTimeout(1);
        instance2.getConfig().setHeartbeatInterval(1);
        
        Set<InstanceDescription> instances1 = instance1.getDiscoveryService().getTopology().getInstances();
        Set<InstanceDescription> instances2 = instance2.getDiscoveryService().getTopology().getInstances();
        
        assertEquals(1, instances1.size());
        assertEquals(1, instances2.size());
        assertEquals(instance1.getSlingId(), instances1.iterator().next().getSlingId());
        assertEquals(instance2.getSlingId(), instances2.iterator().next().getSlingId());
        
        Connector connector = createConnector(instance1, instance2);
        
        // check instance 1's announcements
        Collection<Announcement> instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance1LocalAnnouncements.size());
        Announcement instance1LocalAnnouncement = instance1LocalAnnouncements.iterator().next();
        assertEquals(instance2.getSlingId(), instance1LocalAnnouncement.getOwnerId());
        assertEquals(true, instance1LocalAnnouncement.isInherited());

        // check instance 2's announcements
        Collection<Announcement> instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance2LocalAnnouncements.size());
        Announcement instance2LocalAnnouncement = instance2LocalAnnouncements.iterator().next();
        assertEquals(instance1.getSlingId(), instance2LocalAnnouncement.getOwnerId());
        assertEquals(false, instance2LocalAnnouncement.isInherited());
        
        // check topology
        assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());
        assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());

        instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance1LocalAnnouncements.size());
        instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance2LocalAnnouncements.size());

        Thread.sleep(1500);
        
        instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance1LocalAnnouncements.size());
        instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance2LocalAnnouncements.size());

        assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());
        assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance2.getSlingId());
        
        Thread.sleep(1000);
        instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance1LocalAnnouncements.size());
        instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance2LocalAnnouncements.size());

        assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId());
        assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance2.getSlingId());
    }

    private void assertTopologyConsistsOf(TopologyView topology,
            String... slingIds) {
        assertNotNull(topology);
        assertEquals(topology.getInstances().size(), slingIds.length);
        for(int i=0; i<slingIds.length; i++) {
            final String aSlingId = slingIds[i];
            final Set instances = topology.getInstances();
            boolean found = false;
            for (Iterator it = instances.iterator(); it.hasNext();) {
                InstanceDescription anInstance = (InstanceDescription) it.next();
                if (anInstance.getSlingId().equals(aSlingId)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
    
    @Test
    public void testLargeTopologyWithHub() throws Throwable {
        final int TEST_SIZE = 100;
        Instance hub = createInstance("hub");
        
        List<String> slingIds = new LinkedList<String>();
        slingIds.add(hub.getSlingId());
        for(int i=0; i<TEST_SIZE; i++) {
            Instance instance = createInstance("instance"+i);
            Connector connector = createConnector(instance, hub);
            slingIds.add(instance.getSlingId());
        }
        assertTopologyConsistsOf(hub.getDiscoveryService().getTopology(), slingIds.toArray(new String[slingIds.size()]));
    }

}
