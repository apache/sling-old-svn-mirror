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
import org.apache.sling.discovery.impl.topology.connector.TopologyConnectorClientInformation.OriginInfo;
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

        final URL url = new URL("http://localhost:1234");
        final ClusterViewService cvs = i.getClusterViewService();
        try {
            c.registerOutgoingConnection(cvs, url, null);
            fail("should have complained");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            c.registerOutgoingConnection(null, url, OriginInfo.Programmatically);
            fail("should have complained");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            c.registerOutgoingConnection(cvs, null, OriginInfo.Config);
            fail("should have complained");
        } catch (IllegalArgumentException e) {
            // ok
        }
        TopologyConnectorClientInformation client = c
                .registerOutgoingConnection(cvs, url, OriginInfo.WebConsole);
        try {
            // should not be able to register same url twice
            client = c.registerOutgoingConnection(cvs, url,
                    OriginInfo.Programmatically);
            fail("should have complained");
        } catch (IllegalStateException e) {
            // ok
        }

        try {
            c.unregisterOutgoingConnection(null);
            fail("should have complained");
        } catch (IllegalArgumentException e) {
            // ok
        }

        c.unregisterOutgoingConnection(client.getId());
    }
}
