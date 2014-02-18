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
package org.apache.sling.discovery.impl.topology.announcement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.common.DefaultClusterViewImpl;
import org.apache.sling.discovery.impl.common.DefaultInstanceDescriptionImpl;
import org.apache.sling.discovery.impl.setup.MockFactory;
import org.apache.sling.discovery.impl.setup.OSGiFactory;
import org.apache.sling.discovery.impl.topology.TopologyTestHelper;
import org.junit.Before;
import org.junit.Test;

public class TopologyAnnouncementRegistryTest {

    private AnnouncementRegistryImpl registry;
    private String slingId;
    private ResourceResolverFactory resourceResolverFactory;
    private Config config;

    @Before
    public void setup() throws Exception {
        resourceResolverFactory = MockFactory
                .mockResourceResolverFactory();
        config = new Config() {
            public long getHeartbeatTimeout() {
                // 1s for fast tests
                return 1;
            };
        };
        slingId = UUID.randomUUID().toString();
        Session l = RepositoryProvider.instance().getRepository()
                .loginAdministrative(null);
        try {
            l.removeItem("/var");
            l.save();
            l.logout();
        } catch (Exception e) {
            l.refresh(false);
            l.logout();
        }
        registry = (AnnouncementRegistryImpl) OSGiFactory.createITopologyAnnouncementRegistry(
                resourceResolverFactory, config, slingId);
    }

    @Test
    public void testRegisterUnregister() throws Exception {
        try{
            registry.registerAnnouncement(null);
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        try{
            registry.unregisterAnnouncement(null);
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        try{
            registry.unregisterAnnouncement("");
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        
        try{
            new Announcement(null);
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        try{
            new Announcement("");
            fail("should complain"); 
        } catch(IllegalArgumentException iae) {
            // ok
        }
        
        Announcement ann = new Announcement(slingId);
        assertFalse(ann.isValid());
        assertFalse(registry.registerAnnouncement(ann)!=-1);
        
        DefaultClusterViewImpl localCluster = new DefaultClusterViewImpl(UUID.randomUUID().toString());
        ann.setLocalCluster(localCluster);
        assertFalse(ann.isValid());
        assertFalse(registry.registerAnnouncement(ann)!=-1);

        try{
            registry.listInstances(localCluster);
            fail("doing getInstances() on an empty cluster should throw an illegalstateexception");
        } catch(IllegalStateException ise) {
            // ok
        }
        
        DefaultInstanceDescriptionImpl instance = TopologyTestHelper.createInstanceDescription(ann.getOwnerId(), true, localCluster);
        assertEquals(instance.getSlingId(), ann.getOwnerId());
        assertTrue(ann.isValid());
        assertTrue(registry.registerAnnouncement(ann)!=-1);
        
        assertEquals(1, registry.listInstances(localCluster).size());
        
        registry.checkExpiredAnnouncements();
        assertEquals(1, registry.listInstances(localCluster).size());
        
        registry.unregisterAnnouncement(ann.getOwnerId());
        assertEquals(0, registry.listInstances(localCluster).size());
        assertTrue(ann.isValid());
        assertTrue(registry.registerAnnouncement(ann)!=-1);
        assertEquals(1, registry.listInstances(localCluster).size());

        Thread.sleep(1500);
        assertEquals(0, registry.listInstances(localCluster).size());
    
    }
    
    @Test
    public void testLists() throws Exception {
        try{
            registry.listAnnouncementsInSameCluster(null);
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        try{
            registry.listAnnouncementsInSameCluster(null);
            fail("should complain");
        } catch(IllegalArgumentException iae) {
            // ok
        }
        assertEquals(0, registry.listLocalAnnouncements().size());
        assertEquals(0, registry.listLocalIncomingAnnouncements().size());
        DefaultClusterViewImpl localCluster = new DefaultClusterViewImpl(UUID.randomUUID().toString());
        DefaultInstanceDescriptionImpl instance = TopologyTestHelper.createInstanceDescription(slingId, true, localCluster);        
        assertEquals(0, registry.listAnnouncementsInSameCluster(localCluster).size());
        assertEquals(0, registry.listLocalAnnouncements().size());
        assertEquals(0, registry.listLocalIncomingAnnouncements().size());
        
        Announcement ann = new Announcement(slingId);
        ann.setLocalCluster(localCluster);
        ann.setInherited(true);
        registry.registerAnnouncement(ann);
        assertEquals(1, registry.listAnnouncementsInSameCluster(localCluster).size());
        assertEquals(1, registry.listLocalAnnouncements().size());
        assertEquals(0, registry.listLocalIncomingAnnouncements().size());
        ann.setInherited(true);
        assertEquals(0, registry.listLocalIncomingAnnouncements().size());
        assertTrue(registry.hasActiveAnnouncement(slingId));
        assertFalse(registry.hasActiveAnnouncement(UUID.randomUUID().toString()));
        registry.unregisterAnnouncement(slingId);
        assertEquals(0, registry.listAnnouncementsInSameCluster(localCluster).size());
        assertEquals(0, registry.listLocalAnnouncements().size());
        assertEquals(0, registry.listLocalIncomingAnnouncements().size());
        assertFalse(registry.hasActiveAnnouncement(slingId));
        assertFalse(registry.hasActiveAnnouncement(UUID.randomUUID().toString()));
        ann.setInherited(false);
        registry.registerAnnouncement(ann);
        assertEquals(1, registry.listAnnouncementsInSameCluster(localCluster).size());
        assertEquals(1, registry.listLocalAnnouncements().size());
        assertEquals(1, registry.listLocalIncomingAnnouncements().size());
        assertTrue(registry.hasActiveAnnouncement(slingId));
        assertFalse(registry.hasActiveAnnouncement(UUID.randomUUID().toString()));
        registry.unregisterAnnouncement(slingId);
        assertEquals(0, registry.listAnnouncementsInSameCluster(localCluster).size());
        assertEquals(0, registry.listLocalAnnouncements().size());
        assertEquals(0, registry.listLocalIncomingAnnouncements().size());
        assertFalse(registry.hasActiveAnnouncement(slingId));
        assertFalse(registry.hasActiveAnnouncement(UUID.randomUUID().toString()));
        
        assertEquals(1, ann.listInstances().size());
        registry.addAllExcept(ann, localCluster, new AnnouncementFilter() {
            
            public boolean accept(String receivingSlingId, Announcement announcement) {
                assertNotNull(receivingSlingId);
                assertNotNull(announcement);
                return true;
            }
        });
        assertEquals(1, ann.listInstances().size());
        registry.registerAnnouncement(createAnnouncement(createCluster(3), 1, false));
        assertEquals(1, registry.listAnnouncementsInSameCluster(localCluster).size());
        assertEquals(3, registry.listInstances(localCluster).size());
        registry.addAllExcept(ann, localCluster, new AnnouncementFilter() {
            
            public boolean accept(String receivingSlingId, Announcement announcement) {
                assertNotNull(receivingSlingId);
                assertNotNull(announcement);
                return true;
            }
        });
        assertEquals(4, ann.listInstances().size());
        registry.registerAnnouncement(ann);
        assertEquals(2, registry.listAnnouncementsInSameCluster(localCluster).size());
    }
    
    private ClusterView createCluster(int numInstances) {
        DefaultClusterViewImpl localCluster = new DefaultClusterViewImpl(UUID.randomUUID().toString());
        for (int i = 0; i < numInstances; i++) {
            DefaultInstanceDescriptionImpl instance = TopologyTestHelper.createInstanceDescription(UUID.randomUUID().toString(), (i==0 ? true : false), localCluster);        
        }
        return localCluster;
    }
    
    private ClusterView createCluster(String... instanceIds) {
        DefaultClusterViewImpl localCluster = new DefaultClusterViewImpl(UUID.randomUUID().toString());
        for (int i = 0; i < instanceIds.length; i++) {
            DefaultInstanceDescriptionImpl instance = TopologyTestHelper.createInstanceDescription(instanceIds[i], (i==0 ? true : false), localCluster);        
        }
        return localCluster;
    }

    private Announcement createAnnouncement(ClusterView remoteCluster, int ownerIndex, boolean inherited) {
        List<InstanceDescription> instances = remoteCluster.getInstances();
        Announcement ann = new Announcement(instances.get(ownerIndex).getSlingId());
        ann.setInherited(inherited);
        ann.setLocalCluster(remoteCluster);
        return ann;
    }
    
    @Test
    public void testExpiry() throws InterruptedException, NoSuchFieldException {
        ClusterView cluster1 = createCluster(4);
        ClusterView cluster2 = createCluster(3);
        ClusterView cluster3 = createCluster(5);
        
        ClusterView myCluster = createCluster(slingId);
        
        Announcement ann1 = createAnnouncement(cluster1, 0, true);
        Announcement ann2 = createAnnouncement(cluster2, 1, true);
        Announcement ann3 = createAnnouncement(cluster3, 1, false);
        
        assertTrue(registry.registerAnnouncement(ann1)!=-1);
        assertTrue(registry.registerAnnouncement(ann2)!=-1);
        assertTrue(registry.registerAnnouncement(ann3)!=-1);
        assertTrue(registry.hasActiveAnnouncement(cluster1.getInstances().get(0).getSlingId()));
        assertTrue(registry.hasActiveAnnouncement(cluster2.getInstances().get(1).getSlingId()));
        assertTrue(registry.hasActiveAnnouncement(cluster3.getInstances().get(1).getSlingId()));
        assertEquals(3, registry.listAnnouncementsInSameCluster(myCluster).size());
        assertEquals(3, registry.listLocalAnnouncements().size());
        assertEquals(1, registry.listLocalIncomingAnnouncements().size());

        
        {
            Announcement testAnn = createAnnouncement(myCluster, 0, false);
            assertEquals(1, testAnn.listInstances().size());
            registry.addAllExcept(testAnn, myCluster, null);
            assertEquals(13, testAnn.listInstances().size());
        }

        
        Thread.sleep(1500);
        {
            Announcement testAnn = createAnnouncement(myCluster, 0, false);
            assertEquals(1, testAnn.listInstances().size());
            registry.addAllExcept(testAnn, myCluster, null);
            assertEquals(13, testAnn.listInstances().size());
        }
        assertTrue(registry.registerAnnouncement(ann3)!=-1);
        {
            Announcement testAnn = createAnnouncement(myCluster, 0, false);
            assertEquals(1, testAnn.listInstances().size());
            registry.addAllExcept(testAnn, myCluster, null);
            assertEquals(13, testAnn.listInstances().size());
        }
        
        registry.checkExpiredAnnouncements();
        
        assertEquals(1, registry.listAnnouncementsInSameCluster(myCluster).size());
        assertEquals(1, registry.listLocalAnnouncements().size());
        assertEquals(1, registry.listLocalIncomingAnnouncements().size());
        assertFalse(registry.hasActiveAnnouncement(cluster1.getInstances().get(0).getSlingId()));
        assertFalse(registry.hasActiveAnnouncement(cluster2.getInstances().get(1).getSlingId()));
        assertTrue(registry.hasActiveAnnouncement(cluster3.getInstances().get(1).getSlingId()));
        {
            Announcement testAnn = createAnnouncement(myCluster, 0, false);
            assertEquals(1, testAnn.listInstances().size());
            registry.addAllExcept(testAnn, myCluster, null);
            assertEquals(6, testAnn.listInstances().size());
        }
        
    }
    
    @Test
    public void testCluster() throws Exception {
        ClusterView cluster1 = createCluster(2);
        ClusterView cluster2 = createCluster(4);
        ClusterView cluster3 = createCluster(7);
        
        Announcement ann1 = createAnnouncement(cluster1, 1, true);
        Announcement ann2 = createAnnouncement(cluster2, 2, true);
        Announcement ann3 = createAnnouncement(cluster3, 3, false);
        
        final String instance1 = UUID.randomUUID().toString();
        final String instance2 = UUID.randomUUID().toString();
        final String instance3 = UUID.randomUUID().toString();
        ClusterView myCluster = createCluster(instance1, instance2, instance3);

        AnnouncementRegistryImpl registry1 = (AnnouncementRegistryImpl) OSGiFactory.createITopologyAnnouncementRegistry(
                resourceResolverFactory, config, instance1);
        AnnouncementRegistryImpl registry2 = (AnnouncementRegistryImpl) OSGiFactory.createITopologyAnnouncementRegistry(
                resourceResolverFactory, config, instance2);
        AnnouncementRegistryImpl registry3 = (AnnouncementRegistryImpl) OSGiFactory.createITopologyAnnouncementRegistry(
                resourceResolverFactory, config, instance3);

        assertTrue(registry1.registerAnnouncement(ann1)!=-1);
        assertTrue(registry2.registerAnnouncement(ann2)!=-1);
        assertTrue(registry3.registerAnnouncement(ann3)!=-1);
        
        assertTrue(registry1.hasActiveAnnouncement(cluster1.getInstances().get(1).getSlingId()));
        assertTrue(registry2.hasActiveAnnouncement(cluster2.getInstances().get(2).getSlingId()));
        assertTrue(registry3.hasActiveAnnouncement(cluster3.getInstances().get(3).getSlingId()));

        assertEquals(3, registry1.listAnnouncementsInSameCluster(myCluster).size());
        assertEquals(1, registry1.listLocalAnnouncements().size());
        assertEquals(0, registry1.listLocalIncomingAnnouncements().size());
        assertAnnouncements(registry1, myCluster, 4, 16);
        
        assertEquals(3, registry2.listAnnouncementsInSameCluster(myCluster).size());
        assertEquals(1, registry2.listLocalAnnouncements().size());
        assertEquals(0, registry2.listLocalIncomingAnnouncements().size());
        assertAnnouncements(registry2, myCluster, 4, 16);

        assertEquals(3, registry3.listAnnouncementsInSameCluster(myCluster).size());
        assertEquals(1, registry3.listLocalAnnouncements().size());
        assertEquals(1, registry3.listLocalIncomingAnnouncements().size());
        assertAnnouncements(registry3, myCluster, 4, 16);
        
        myCluster = createCluster(instance1, instance2);

        assertEquals(2, registry1.listAnnouncementsInSameCluster(myCluster).size());
        assertEquals(1, registry1.listLocalAnnouncements().size());
        assertEquals(0, registry1.listLocalIncomingAnnouncements().size());
        assertAnnouncements(registry1, myCluster, 3, 8);
        
        assertEquals(2, registry2.listAnnouncementsInSameCluster(myCluster).size());
        assertEquals(1, registry2.listLocalAnnouncements().size());
        assertEquals(0, registry2.listLocalIncomingAnnouncements().size());
        assertAnnouncements(registry2, myCluster, 3, 8);
        
        Thread.sleep(1500);
        assertAnnouncements(registry1, myCluster, 3, 8);
        assertAnnouncements(registry2, myCluster, 3, 8);
        registry1.checkExpiredAnnouncements();
        registry2.checkExpiredAnnouncements();
        assertAnnouncements(registry1, myCluster, 1, 2);
        assertAnnouncements(registry2, myCluster, 1, 2);
    }

    private void assertAnnouncements(AnnouncementRegistryImpl registry,
            ClusterView myCluster, int expectedNumAnnouncements, int expectedNumInstances) {
        Announcement ann = createAnnouncement(myCluster, 0, false);
        registry.addAllExcept(ann, myCluster, null);
        assertEquals(expectedNumInstances, ann.listInstances().size());
    }
    
}
