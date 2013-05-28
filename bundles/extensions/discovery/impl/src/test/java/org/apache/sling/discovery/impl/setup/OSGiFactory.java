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
package org.apache.sling.discovery.impl.setup;

import javax.jcr.Repository;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.cluster.ClusterViewServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistryImpl;
import org.apache.sling.discovery.impl.topology.connector.ConnectorRegistry;
import org.apache.sling.discovery.impl.topology.connector.ConnectorRegistryImpl;

public class OSGiFactory {

    // RepositoryProvider repoProvider = RepositoryProvider.instance();

    public static ClusterViewServiceImpl createClusterViewServiceImpl(
            String slingId, ResourceResolverFactory resourceResolverFactory, Config config)
            throws Exception {
        ClusterViewServiceImpl clusterViewService = new ClusterViewServiceImpl();
        PrivateAccessor.setField(clusterViewService, "resourceResolverFactory",
                resourceResolverFactory);
        PrivateAccessor.setField(clusterViewService, "settingsService",
                MockFactory.mockSlingSettingsService(slingId));
        PrivateAccessor.setField(clusterViewService, "config", config);
        return clusterViewService;
    }

    public static HeartbeatHandler createHeartbeatHandler(
            ResourceResolverFactory resourceResolverFactory, String slingId,
            AnnouncementRegistry topologyService,
            ConnectorRegistry connectorRegistry, Config config, Repository repository, Scheduler scheduler)
            throws Exception {
        HeartbeatHandler heartbeatHandler = new HeartbeatHandler();
        PrivateAccessor.setField(heartbeatHandler, "resourceResolverFactory",
                resourceResolverFactory);
        PrivateAccessor.setField(heartbeatHandler, "slingSettingsService",
                MockFactory.mockSlingSettingsService(slingId));
        PrivateAccessor.setField(heartbeatHandler, "announcementRegistry",
                topologyService);
        PrivateAccessor.setField(heartbeatHandler, "connectorRegistry",
                connectorRegistry);
        PrivateAccessor.setField(heartbeatHandler, "config", config);
        PrivateAccessor.setField(heartbeatHandler, "scheduler", scheduler);

        return heartbeatHandler;
    }

    public static DiscoveryServiceImpl createDiscoverService(String slingId,
            HeartbeatHandler heartbeatHandler,
            ClusterViewServiceImpl clusterViewService,
            AnnouncementRegistry topologyRegistry,
            ResourceResolverFactory resourceResolverFactory, Config config, 
            ConnectorRegistry connectorRegistry, Scheduler scheduler)
            throws Exception {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl();
        PrivateAccessor.setField(discoveryService, "settingsService",
                MockFactory.mockSlingSettingsService(slingId));
        PrivateAccessor.setField(discoveryService, "heartbeatHandler",
                heartbeatHandler);
        PrivateAccessor.setField(discoveryService, "clusterViewService",
                clusterViewService);
        PrivateAccessor.setField(discoveryService, "announcementRegistry",
                topologyRegistry);
        PrivateAccessor.setField(discoveryService, "resourceResolverFactory",
                resourceResolverFactory);
        PrivateAccessor.setField(discoveryService, "config", config);
        PrivateAccessor.setField(discoveryService, "connectorRegistry",
        		connectorRegistry);
        PrivateAccessor.setField(discoveryService, "scheduler",
        		scheduler);

        return discoveryService;
    }

    public static AnnouncementRegistry createITopologyAnnouncementRegistry(
            ResourceResolverFactory resourceResolverFactory, Config config, String slingId)
            throws Exception {
        AnnouncementRegistry topologyService = new AnnouncementRegistryImpl();

        PrivateAccessor.setField(topologyService, "resourceResolverFactory",
                resourceResolverFactory);
        PrivateAccessor.setField(topologyService, "settingsService",
                MockFactory.mockSlingSettingsService(slingId));
        PrivateAccessor.setField(topologyService, "config", config);

        return topologyService;
    }

    public static VotingHandler createVotingHandler(String slingId,
            ResourceResolverFactory resourceResolverFactory, Config config)
            throws Exception {
        VotingHandler votingHandler = new VotingHandler();
        PrivateAccessor.setField(votingHandler, "slingSettingsService",
                MockFactory.mockSlingSettingsService(slingId));
        PrivateAccessor.setField(votingHandler, "resolverFactory",
                resourceResolverFactory);
        PrivateAccessor.setField(votingHandler, "config", config);

        return votingHandler;
    }

    public static ConnectorRegistry createConnectorRegistry(
            AnnouncementRegistry topologyService, Config config)
            throws Exception {
        ConnectorRegistryImpl c = new ConnectorRegistryImpl();
        PrivateAccessor.setField(c, "announcementRegistry", topologyService);
        PrivateAccessor.setField(c, "config", config);
        return c;
    }
}
