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
package org.apache.sling.discovery.base.its;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.base.connectors.DummyVirtualInstanceBuilder;
import org.apache.sling.discovery.base.connectors.announcement.Announcement;
import org.apache.sling.discovery.base.its.setup.TopologyHelper;
import org.apache.sling.discovery.base.its.setup.VirtualConnector;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<VirtualInstance> instances = new LinkedList<VirtualInstance>();
    
    private VirtualInstanceBuilder newBuilder() {
        return new DummyVirtualInstanceBuilder();
    }
    
    @After
    public void tearDown() throws Exception {
        for (Iterator<VirtualInstance> it = instances.iterator(); it.hasNext();) {
            final VirtualInstance instance = it.next();
            instance.stop();
        }
    }
    
    @Test
    public void testTwoNodes() throws Throwable {
        VirtualInstanceBuilder builder1 = newBuilder()
                .newRepository("/var/discovery/impl/", true)
                .setDebugName("instance1")
                .setConnectorPingInterval(20)
                .setConnectorPingTimeout(200);
        VirtualInstance instance1 = builder1.build();
        instances.add(instance1);
        VirtualInstanceBuilder builder2 = newBuilder()
                .useRepositoryOf(builder1)
                .setDebugName("instance2")
                .setConnectorPingInterval(20)
                .setConnectorPingTimeout(200);
        VirtualInstance instance2 = builder2.build();
        instances.add(instance2);
        instance1.getConfig().setViewCheckTimeout(8);
        instance1.getConfig().setViewCheckInterval(1);
        instance2.getConfig().setViewCheckTimeout(2);
        instance2.getConfig().setViewCheckInterval(1);
        
        for(int i=0; i<5; i++) {
            instance1.heartbeatsAndCheckView();
            instance2.heartbeatsAndCheckView();
            Thread.sleep(500);
        }
        
        Set<InstanceDescription> instances1 = instance1.getDiscoveryService().getTopology().getInstances();
        Set<InstanceDescription> instances2 = instance2.getDiscoveryService().getTopology().getInstances();
        
        assertEquals(1, instances1.size());
        assertEquals(1, instances2.size());
        assertEquals(instance1.getSlingId(), instances1.iterator().next().getSlingId());
        assertEquals(instance2.getSlingId(), instances2.iterator().next().getSlingId());
        
        new VirtualConnector(instance1, instance2);
        
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
        TopologyHelper.assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());
        TopologyHelper.assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());

        instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance1LocalAnnouncements.size());
        instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance2LocalAnnouncements.size());

        Thread.sleep(2200); // sleep of 2.2sec ensures instance2's heartbeat timeout (which is 2sec) hits
        
        instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(1, instance1LocalAnnouncements.size());
        instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance2LocalAnnouncements.size());

        logger.info("testTwoNodes: instance1: "+instance1.getSlingId());
        instance1.dumpRepo();
        logger.info("testTwoNodes: instance2: "+instance2.getSlingId());
        instance2.dumpRepo();
        TopologyHelper.assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());
        TopologyHelper.assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance2.getSlingId());
        
        Thread.sleep(6000); // another sleep 6s (2.2+6 = 8.2sec) ensures instance1's heartbeat timeout (which is 8sec) hits as well
        instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance1LocalAnnouncements.size());
        instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance2LocalAnnouncements.size());

        TopologyHelper.assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId());
        TopologyHelper.assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance2.getSlingId());
    }
}
