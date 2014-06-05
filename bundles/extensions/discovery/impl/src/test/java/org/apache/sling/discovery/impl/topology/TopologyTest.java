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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.discovery.impl.topology.announcement.Announcement;
import org.junit.After;
import org.junit.Test;

public class TopologyTest {

    private final List<Instance> instances = new LinkedList<Instance>();
    
    @After
    public void tearDown() throws Exception {
        for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
            final Instance instance = it.next();
            instance.stop();
        }
    }
    
    @Test
    public void testTwoNodes() throws Throwable {
        Instance instance1 = TopologyTestHelper.createInstance(instances, "instance1");
        Instance instance2 = TopologyTestHelper.createInstance(instances, "instance2");
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
        
        new Connector(instance1, instance2);
        
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
        TopologyTestHelper.assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());
        TopologyTestHelper.assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());

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

        TopologyTestHelper.assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId(), instance2.getSlingId());
        TopologyTestHelper.assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance2.getSlingId());
        
        Thread.sleep(1000);
        instance1LocalAnnouncements = 
                instance1.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance1LocalAnnouncements.size());
        instance2LocalAnnouncements = 
                instance2.getAnnouncementRegistry().listLocalAnnouncements();
        assertEquals(0, instance2LocalAnnouncements.size());

        TopologyTestHelper.assertTopologyConsistsOf(instance1.getDiscoveryService().getTopology(), instance1.getSlingId());
        TopologyTestHelper.assertTopologyConsistsOf(instance2.getDiscoveryService().getTopology(), instance2.getSlingId());
    }
}
