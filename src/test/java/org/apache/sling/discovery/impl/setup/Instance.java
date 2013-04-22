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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.DiscoveryServiceImpl;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.cluster.ClusterViewServiceImpl;
import org.apache.sling.discovery.impl.cluster.voting.VotingHandler;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.impl.topology.connector.ConnectorRegistry;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Instance {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final String slingId = UUID.randomUUID().toString();

    ClusterViewServiceImpl clusterViewService;

    private final ResourceResolverFactory resourceResolverFactory;

    private final HeartbeatHandler heartbeatHandler;

    private final OSGiMock osgiMock;

    private final DiscoveryServiceImpl discoveryService;

    private final AnnouncementRegistry announcementRegistry;

    private final ConnectorRegistry connectorRegistry;

    private final VotingHandler votingHandler;

    @SuppressWarnings("unused")
    private final String debugName;

    private ResourceResolver resourceResolver;

    private int serviceId = 999;

    private Instance(String debugName,
            ResourceResolverFactory resourceResolverFactory, boolean resetRepo)
            throws Exception {
        this.debugName = debugName;

        osgiMock = new OSGiMock();

        this.resourceResolverFactory = resourceResolverFactory;

        Config config = new Config() {
            @Override
            public long getHeartbeatTimeout() {
                return 20000;
            }
        };

        clusterViewService = OSGiFactory.createClusterViewServiceImpl(slingId,
                resourceResolverFactory, config);
        announcementRegistry = OSGiFactory.createITopologyAnnouncementRegistry(
                resourceResolverFactory, config, slingId);
        connectorRegistry = OSGiFactory.createConnectorRegistry(
                announcementRegistry, config);
        heartbeatHandler = OSGiFactory.createHeartbeatHandler(
                resourceResolverFactory, slingId, announcementRegistry,
                connectorRegistry, config,
                resourceResolverFactory.getAdministrativeResourceResolver(null)
                        .adaptTo(Repository.class));

        discoveryService = OSGiFactory.createDiscoverService(slingId,
                heartbeatHandler, clusterViewService, announcementRegistry,
                resourceResolverFactory, config);

        votingHandler = OSGiFactory.createVotingHandler(slingId,
                resourceResolverFactory, config);

        osgiMock.addService(clusterViewService);
        osgiMock.addService(heartbeatHandler);
        osgiMock.addService(discoveryService);
        osgiMock.addService(announcementRegistry);
        osgiMock.addService(votingHandler);

        resourceResolver = resourceResolverFactory
                .getAdministrativeResourceResolver(null);
        Session session = resourceResolver.adaptTo(Session.class);
        System.out
                .println("GOING TO REGISTER LISTENER WITH SESSION " + session);
        ObservationManager observationManager = session.getWorkspace()
                .getObservationManager();

        observationManager.addEventListener(
                new EventListener() {

                    public void onEvent(EventIterator events) {
                        try {
                            while (events.hasNext()) {
                                Event event = events.nextEvent();
                                Properties properties = new Properties();
                                String topic;
                                if (event.getType() == Event.NODE_ADDED) {
                                    topic = SlingConstants.TOPIC_RESOURCE_ADDED;
                                } else if (event.getType() == Event.NODE_MOVED) {
                                    topic = SlingConstants.TOPIC_RESOURCE_CHANGED;
                                } else if (event.getType() == Event.NODE_REMOVED) {
                                    topic = SlingConstants.TOPIC_RESOURCE_REMOVED;
                                } else {
                                    topic = SlingConstants.TOPIC_RESOURCE_CHANGED;
                                }
                                try {
                                    properties.put("path", event.getPath());
                                    org.osgi.service.event.Event osgiEvent = new org.osgi.service.event.Event(
                                            topic, properties);
                                    votingHandler.handleEvent(osgiEvent);
                                } catch (RepositoryException e) {
                                    logger.warn("RepositoryException: " + e, e);
                                }
                            }
                        } catch (Throwable th) {
                            try {
                                dumpRepo();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            logger.error(
                                    "Throwable occurred in onEvent: " + th, th);
                        }
                    }
                }, Event.NODE_ADDED | Event.NODE_REMOVED | Event.NODE_MOVED
                        | Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED
                        | Event.PROPERTY_REMOVED | Event.PERSIST, "/", true,
                null,
                null, false);

        osgiMock.activateAll(resetRepo);
    }

    public static Instance newStandaloneInstance(String debugName,
            boolean resetRepo) throws Exception {
        ResourceResolverFactory resourceResolverFactory = MockFactory
                .mockResourceResolverFactory();
        return new Instance(debugName, resourceResolverFactory, resetRepo);
    }

    public static Instance newClusterInstance(String debugName, Instance other,
            boolean resetRepo) throws Exception {
        return new Instance(debugName, other.resourceResolverFactory, resetRepo);
    }

    public void bindPropertyProvider(PropertyProvider propertyProvider,
            String... propertyNames) throws Throwable {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.SERVICE_ID, (long) serviceId++);
        props.put(PropertyProvider.PROPERTY_PROPERTIES, propertyNames);

        PrivateAccessor.invoke(discoveryService, "bindPropertyProvider",
                new Class[] { PropertyProvider.class, Map.class },
                new Object[] { propertyProvider, props });
    }

    public String getSlingId() {
        return slingId;
    }

    public ClusterViewService getClusterViewService() {
        return clusterViewService;
    }

    public void runHeartbeatOnce() {
        heartbeatHandler.run();
    }

    public void dumpRepo() throws Exception {
        Session session = resourceResolverFactory
                .getAdministrativeResourceResolver(null).adaptTo(Session.class);
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: start");
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: repo = " + session.getRepository());
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: |");

        dump(session.getRootNode());

        // session.logout();
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: end");
        logger.info("dumpRepo: |");
        logger.info("dumpRepo: |");

        session.logout();
    }

    private void dump(Node node) throws RepositoryException {
        if (node.getPath().equals("/jcr:system")
                || node.getPath().equals("/rep:policy")) {
            // ignore that one
            return;
        }

        PropertyIterator pi = node.getProperties();
        StringBuffer sb = new StringBuffer();
        while (pi.hasNext()) {
            Property p = pi.nextProperty();
            sb.append(" ");
            sb.append(p.getName());
            sb.append("=");
            if (p.getType() == PropertyType.BOOLEAN) {
                sb.append(p.getBoolean());
            } else if (p.getType() == PropertyType.STRING) {
                sb.append(p.getString());
            } else if (p.getType() == PropertyType.DATE) {
                sb.append(p.getDate().getTime());
            } else {
                sb.append("<unknown type=" + p.getType() + "/>");
            }
        }

        logger.info("dump: node=" + node + "   - properties:" + sb);
        NodeIterator it = node.getNodes();
        while (it.hasNext()) {
            Node child = it.nextNode();
            dump(child);
        }
    }

    public void stop() throws LoginException {
        if (resourceResolver != null) {
            resourceResolver.close();
        }
    }

    public void bindTopologyEventListener(TopologyEventListener eventListener)
            throws Throwable {
        PrivateAccessor.invoke(discoveryService, "bindTopologyEventListener",
                new Class[] { TopologyEventListener.class },
                new Object[] { eventListener });
    }

}
