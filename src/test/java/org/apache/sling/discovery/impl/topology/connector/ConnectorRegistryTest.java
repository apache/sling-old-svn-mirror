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
package org.apache.sling.discovery.impl.topology.connector;

import static org.junit.Assert.fail;

import java.net.URL;
import java.util.UUID;

import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.setup.Instance;
import org.apache.sling.discovery.impl.setup.MockFactory;
import org.apache.sling.discovery.impl.setup.OSGiFactory;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.junit.Test;

public class ConnectorRegistryTest {

    @Test
    public void testRegisterUnregister() throws Exception {
        Instance i = Instance.newStandaloneInstance("i", true);
        Config config = new Config() {
            @Override
            public long getHeartbeatTimeout() {
                return 20000;
            }
        };
        AnnouncementRegistry announcementRegistry = OSGiFactory
                .createITopologyAnnouncementRegistry(MockFactory
                        .mockResourceResolverFactory(), config, UUID.randomUUID()
                        .toString());

        ConnectorRegistry c = OSGiFactory.createConnectorRegistry(
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
