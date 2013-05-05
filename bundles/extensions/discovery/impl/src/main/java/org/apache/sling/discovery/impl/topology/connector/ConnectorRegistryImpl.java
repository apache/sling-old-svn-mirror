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

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = ConnectorRegistry.class)
public class ConnectorRegistryImpl implements ConnectorRegistry {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** A map of id-> topology connector clients currently registered/activate **/
    private final Map<String, TopologyConnectorClient> outgoingClientsMap = new HashMap<String, TopologyConnectorClient>();

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private Config config;

    /** the local port is added to the announcement as the serverInfo object **/
    private String port = "";

    @Activate
    protected void activate(final ComponentContext cc) {
        port = cc.getBundleContext().getProperty("org.osgi.service.http.port");
    }
    
    @Deactivate
    protected void deactivate() {
        synchronized (outgoingClientsMap) {
            for (Iterator<TopologyConnectorClient> it = outgoingClientsMap.values().iterator(); it.hasNext();) {
                final TopologyConnectorClient client = it.next();
                client.disconnect();
                it.remove();
            }
        }
    }
    
    public TopologyConnectorClientInformation registerOutgoingConnector(
            final ClusterViewService clusterViewService, final URL connectorUrl) {
        if (announcementRegistry == null) {
            logger.error("registerOutgoingConnection: announcementRegistry is null");
            return null;
        }
        TopologyConnectorClient client;
        synchronized (outgoingClientsMap) {
            for (Iterator<Entry<String, TopologyConnectorClient>> it = outgoingClientsMap
                    .entrySet().iterator(); it.hasNext();) {
                Entry<String, TopologyConnectorClient> entry = it.next();
                if (entry.getValue().getConnectorUrl().equals(connectorUrl)) {
                    throw new IllegalStateException(
                            "cannot register same url twice: " + connectorUrl);
                }
            }
            String serverInfo;
            try {
                serverInfo = InetAddress.getLocalHost().getCanonicalHostName()
                        + ":" + port;
            } catch (Exception e) {
                serverInfo = "localhost:" + port;
            }
            client = new TopologyConnectorClient(clusterViewService,
                    announcementRegistry, config, connectorUrl,
                    serverInfo);
            outgoingClientsMap.put(client.getId(), client);
        }
        client.ping();
        return client;
    }

    public Collection<TopologyConnectorClientInformation> listOutgoingConnectors() {
        final List<TopologyConnectorClientInformation> result = new ArrayList<TopologyConnectorClientInformation>();
        synchronized (outgoingClientsMap) {
            result.addAll(outgoingClientsMap.values());
        }
        return result;
    }

    public boolean unregisterOutgoingConnector(final String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id must not be null");
        }
        synchronized (outgoingClientsMap) {
            TopologyConnectorClient client = outgoingClientsMap.remove(id);
            if (client != null) {
                client.disconnect();
            }
            return client != null;
        }
    }

    public void pingOutgoingConnectors() {
        List<TopologyConnectorClient> outgoingTemplatesClone;
        synchronized (outgoingClientsMap) {
            outgoingTemplatesClone = new ArrayList<TopologyConnectorClient>(
                    outgoingClientsMap.values());
        }
        for (Iterator<TopologyConnectorClient> it = outgoingTemplatesClone
                .iterator(); it.hasNext();) {
            it.next().ping();
        }
    }

}
