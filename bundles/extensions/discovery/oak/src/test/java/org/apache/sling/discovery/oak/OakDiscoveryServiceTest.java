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

import org.apache.sling.discovery.commons.providers.base.DummyListener;
import org.apache.sling.discovery.commons.providers.spi.base.DescriptorHelper;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteDescriptorBuilder;
import org.apache.sling.discovery.commons.providers.spi.base.DummySlingSettingsService;
import org.apache.sling.discovery.commons.providers.spi.base.IdMapService;
import org.apache.sling.discovery.oak.its.setup.OakVirtualInstanceBuilder;
import org.junit.Test;

public class OakDiscoveryServiceTest {

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
        public long getBgTimeoutMillis() {
            return bgTimeoutMillis;
        }

        @Override
        public long getBgIntervalMillis() {
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
        discoveryService.handlePotentialTopologyChange();
        assertTrue(discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(1, listener.countEvents());
        discoveryService.unbindTopologyEventListener(listener);
        assertEquals(1, listener.countEvents());
        discoveryService.bindTopologyEventListener(listener);
        assertTrue(discoveryService.getViewStateManager().waitForAsyncEvents(2000));
        assertEquals(2, listener.countEvents()); // should now have gotten an INIT too
    }
    
}
