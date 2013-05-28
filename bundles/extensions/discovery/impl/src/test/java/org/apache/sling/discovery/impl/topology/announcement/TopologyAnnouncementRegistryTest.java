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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
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

    @Before
    public void setup() throws Exception {
        final ResourceResolverFactory resourceResolverFactory = MockFactory
                .mockResourceResolverFactory();
        final Config config = new Config() {
            public long getHeartbeatTimeout() {
                // 1s for fast tests
                return 1;
            };
        };
        final String slingId = UUID.randomUUID().toString();
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
        
        Announcement ann = new Announcement("foo");
        assertFalse(ann.isValid());
        assertFalse(registry.registerAnnouncement(ann));
        
        DefaultClusterViewImpl localCluster = new DefaultClusterViewImpl(UUID.randomUUID().toString());
        ann.setLocalCluster(localCluster);
        assertFalse(ann.isValid());
        assertFalse(registry.registerAnnouncement(ann));

        assertEquals(0, registry.listInstances().size());
        
        DefaultInstanceDescriptionImpl instance = TopologyTestHelper.createInstanceDescription(localCluster);
        assertTrue(ann.isValid());
        assertTrue(registry.registerAnnouncement(ann));
        
        assertEquals(1, registry.listInstances().size());
        
        registry.checkExpiredAnnouncements();
        assertEquals(1, registry.listInstances().size());
        
        registry.unregisterAnnouncement(ann.getOwnerId());
        assertEquals(0, registry.listInstances().size());
        assertTrue(ann.isValid());
        assertTrue(registry.registerAnnouncement(ann));
        assertEquals(1, registry.listInstances().size());

        Thread.sleep(1500);
        assertEquals(0, registry.listInstances().size());
    
    }
}
