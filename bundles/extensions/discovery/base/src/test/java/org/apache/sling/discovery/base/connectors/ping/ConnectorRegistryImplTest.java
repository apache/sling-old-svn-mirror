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
package org.apache.sling.discovery.base.connectors.ping;

import static org.junit.Assert.fail;

import java.net.URL;
import java.util.UUID;

import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.base.connectors.DummyVirtualInstanceBuilder;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistryImpl;
import org.apache.sling.discovery.base.its.setup.VirtualInstance;
import org.apache.sling.discovery.base.its.setup.VirtualInstanceBuilder;
import org.apache.sling.discovery.base.its.setup.mock.MockFactory;
import org.apache.sling.discovery.base.its.setup.mock.SimpleConnectorConfig;
import org.apache.sling.discovery.commons.providers.spi.base.DummySlingSettingsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConnectorRegistryImplTest {

    private VirtualInstance i;

    public VirtualInstanceBuilder newBuilder() {
        return new DummyVirtualInstanceBuilder();
    }
    
    @Before
    public void setup() throws Exception {
        VirtualInstanceBuilder builder = newBuilder()
                .newRepository("/var/discovery/impl/", true)
                .setDebugName("i")
                .setConnectorPingInterval(20)
                .setConnectorPingTimeout(20);
        i = builder.build();
    }
    
    @After
    public void teardown() throws Exception {
        if (i!=null) {
            try {
                i.stopViewChecker();
            } catch (Throwable e) {
                e.printStackTrace();
                i.stop();
                throw new RuntimeException(e);
            }
            i.stop();
        }
    }
    
    @Test
    public void testRegisterUnregister() throws Exception {
        BaseConfig config = new SimpleConnectorConfig() {
            @Override
            public long getConnectorPingTimeout() {
                return 20000;
            }
        };
        AnnouncementRegistryImpl announcementRegistry = AnnouncementRegistryImpl.testConstructorAndActivate(
                MockFactory.mockResourceResolverFactory(), new DummySlingSettingsService(UUID.randomUUID().toString()), config);

        ConnectorRegistry c = ConnectorRegistryImpl.testConstructor(
                announcementRegistry, config);

        final URL url = new URL("http://localhost:1234/connector");
        final ClusterViewService cvs = i.getClusterViewService();
        try {
            c.registerOutgoingConnector(null, url);
            fail("should have complained");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            c.registerOutgoingConnector(cvs, null);
            fail("should have complained");
        } catch (IllegalArgumentException e) {
            // ok
        }
        TopologyConnectorClientInformation client = c
                .registerOutgoingConnector(cvs, url);
        try {
            // should not be able to register same url twice
            client = c.registerOutgoingConnector(cvs, url);
            // ok - no longer complains - SLING-3446
        } catch (IllegalStateException e) {
            fail("should no longer be thrown"); // SLING-3446
        }

        try {
            c.unregisterOutgoingConnector(null);
            fail("should have complained");
        } catch (IllegalArgumentException e) {
            // ok
        }

        c.unregisterOutgoingConnector(client.getId());
    }
}
