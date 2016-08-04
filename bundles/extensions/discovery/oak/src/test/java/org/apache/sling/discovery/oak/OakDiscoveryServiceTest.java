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
package org.apache.sling.discovery.oak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.commons.providers.base.DummyListener;
import org.apache.sling.discovery.commons.providers.spi.base.DescriptorHelper;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteDescriptorBuilder;
import org.apache.sling.discovery.commons.providers.spi.base.DummySlingSettingsService;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.discovery.oak.its.setup.OakVirtualInstanceBuilder;
import org.apache.sling.discovery.oak.its.setup.SimulatedLeaseCollection;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OakDiscoveryServiceTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final class SimpleCommonsConfig implements DiscoveryLiteConfig {
        
        private long bgIntervalMillis;
        private long bgTimeoutMillis;

        SimpleCommonsConfig(long bgIntervalMillis, long bgTimeoutMillis) {
            this.bgIntervalMillis = bgIntervalMillis;
            this.bgTimeoutMillis = bgTimeoutMillis;
        }
        
        @Override
        public String getSyncTokenPath() {
            return "/var/synctokens";
        }

        @Override
        public String getIdMapPath() {
            return "/var/idmap";
        }

        @Override
        public long getClusterSyncServiceTimeoutMillis() {
            return bgTimeoutMillis;
        }

        @Override
        public long getClusterSyncServiceIntervalMillis() {
            return bgIntervalMillis;
        }

    }

    @Test
    public void testBindBeforeActivate() throws Exception {
        OakVirtualInstanceBuilder builder = 
                (OakVirtualInstanceBuilder) new OakVirtualInstanceBuilder()
                .setDebugName("test")
                .newRepository("/foo/bar", true);
        String slingId = UUID.randomUUID().toString();;
        DiscoveryLiteDescriptorBuilder discoBuilder = new DiscoveryLiteDescriptorBuilder();
        discoBuilder.id("id").me(1).activeIds(1);
        // make sure the discovery-lite descriptor is marked as not final
        // such that the view is not already set before we want it to be
        discoBuilder.setFinal(false);
        DescriptorHelper.setDiscoveryLiteDescriptor(builder.getResourceResolverFactory(), 
                discoBuilder);
        IdMapService idMapService = IdMapService.testConstructor(new SimpleCommonsConfig(1000, -1), new DummySlingSettingsService(slingId), builder.getResourceResolverFactory());
        assertTrue(idMapService.waitForInit(2000));
        OakDiscoveryService discoveryService = (OakDiscoveryService) builder.getDiscoverService();
        assertNotNull(discoveryService);
        DummyListener listener = new DummyListener();
        for(int i=0; i<100; i++) {
            discoveryService.bindTopologyEventListener(listener);
            discoveryService.unbindTopologyEventListener(listener);
        }
        discoveryService.bindTopologyEventListener(listener);
        assertEquals(0, listener.countEvents());
        discoveryService.activate(null);
        assertEquals(0, listener.countEvents());
        // some more confusion...
        discoveryService.unbindTopologyEventListener(listener);
        discoveryService.bindTopologyEventListener(listener);
        // only set the final flag now - this makes sure that handlePotentialTopologyChange
        // will actually detect a valid new, different view and send out an event -
        // exactly as we want to
        discoBuilder.setFinal(true);
        DescriptorHelper.setDiscoveryLiteDescriptor(builder.getResourceResolverFactory(), 
                discoBuilder);
        discoveryService.checkForTopologyChange();
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(1, listener.countEvents());
        discoveryService.unbindTopologyEventListener(listener);
        assertEquals(1, listener.countEvents());
        discoveryService.bindTopologyEventListener(listener);
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(2, listener.countEvents()); // should now have gotten an INIT too
    }
    
    @Test
    public void testDescriptorSeqNumChange() throws Exception {
        logger.info("testDescriptorSeqNumChange: start");
        OakVirtualInstanceBuilder builder1 = 
                (OakVirtualInstanceBuilder) new OakVirtualInstanceBuilder()
                .setDebugName("instance1")
                .newRepository("/foo/barry/foo/", true)
                .setConnectorPingInterval(999)
                .setConnectorPingTimeout(999);
        VirtualInstance instance1 = builder1.build();
        OakVirtualInstanceBuilder builder2 = 
                (OakVirtualInstanceBuilder) new OakVirtualInstanceBuilder()
                .setDebugName("instance2")
                .useRepositoryOf(instance1)
                .setConnectorPingInterval(999)
                .setConnectorPingTimeout(999);
        VirtualInstance instance2 = builder2.build();
        logger.info("testDescriptorSeqNumChange: created both instances, binding listener...");
        
        DummyListener listener = new DummyListener();
        OakDiscoveryService discoveryService = (OakDiscoveryService) instance1.getDiscoveryService();
        discoveryService.bindTopologyEventListener(listener);
        
        logger.info("testDescriptorSeqNumChange: waiting 2sec, listener should not get anything yet");
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(0, listener.countEvents());
        
        logger.info("testDescriptorSeqNumChange: issuing 2 heartbeats with each instance should let the topology get established");
        instance1.heartbeatsAndCheckView();
        instance2.heartbeatsAndCheckView();
        instance1.heartbeatsAndCheckView();
        instance2.heartbeatsAndCheckView();

        logger.info("testDescriptorSeqNumChange: listener should get an event within 2sec from now at latest");
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(1, listener.countEvents());
        
        ResourceResolverFactory factory = instance1.getResourceResolverFactory();
        ResourceResolver resolver = factory.getAdministrativeResourceResolver(null);
        
        instance1.heartbeatsAndCheckView();
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(1, listener.countEvents());
        
        // increment the seqNum by 2 - simulating a coming and going instance
        // while we were sleeping
        SimulatedLeaseCollection c = builder1.getSimulatedLeaseCollection();
        c.incSeqNum(2);
        logger.info("testDescriptorSeqNumChange: incremented seqnum by 2 - issuing another heartbeat should trigger a topology change");
        instance1.heartbeatsAndCheckView();
        
        // due to the nature of the syncService/minEventDelay we now explicitly first sleep 2sec before waiting for async events for another 2sec
        logger.info("testDescriptorSeqNumChange: sleeping 2sec for topology change to happen");
        Thread.sleep(2000);
        logger.info("testDescriptorSeqNumChange: ensuring no async events are still in the pipe - for another 2sec");
        assertEquals(0, discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        logger.info("testDescriptorSeqNumChange: now listener should have received 3 events, it got: "+listener.countEvents());
        assertEquals(3, listener.countEvents());
    }

}
