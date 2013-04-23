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

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.topology.announcement.Announcement;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementFilter;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A topology connector client is used for sending (pinging) a remote topology
 * connector servlet and exchanging announcements with it
 */
public class TopologyConnectorClient implements
        TopologyConnectorClientInformation {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** the endpoint url **/
    private final URL connectorUrl;

    /** the cluster view service **/
    private final ClusterViewService clusterViewService;

    /** the config service to user **/
    private final Config config;

    /** the id of this connection **/
    private final UUID id;

    /** the announcement registry **/
    private final AnnouncementRegistry announcementRegistry;

    /** the last inherited announcement **/
    private Announcement lastInheritedAnnouncement;

    /** the information as to where this connector came from **/
    private final OriginInfo originInfo;

    /** the information about this server **/
    private final String serverInfo;
    
    /** the status code of the last post **/
    private int lastStatusCode = -1;

    TopologyConnectorClient(final ClusterViewService clusterViewService,
            final AnnouncementRegistry announcementRegistry, final Config config,
            final URL connectorUrl, final OriginInfo originInfo, final String serverInfo) {
        if (clusterViewService == null) {
            throw new IllegalArgumentException(
                    "clusterViewService must not be null");
        }
        if (announcementRegistry == null) {
            throw new IllegalArgumentException(
                    "announcementRegistry must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (connectorUrl == null) {
            throw new IllegalArgumentException("connectorUrl must not be null");
        }
        if (originInfo == null) {
            throw new IllegalArgumentException("originInfo must not be null");
        }
        this.clusterViewService = clusterViewService;
        this.announcementRegistry = announcementRegistry;
        this.config = config;
        this.connectorUrl = connectorUrl;
        this.originInfo = originInfo;
        this.serverInfo = serverInfo;
        this.id = UUID.randomUUID();
    }

    /** ping the server and pass the announcements between the two **/
    void ping() {
        logger.debug("ping: connectorUrl=" + connectorUrl);
        HttpClient httpClient = new HttpClient();
        PostMethod method = new PostMethod(connectorUrl.toString());

        try {
            String userInfo = connectorUrl.getUserInfo();
            if (userInfo != null) {
                Credentials c = new UsernamePasswordCredentials(userInfo);
                httpClient.getState().setCredentials(
                        new AuthScope(method.getURI().getHost(), method
                                .getURI().getPort()), c);
            }

            Announcement topologyAnnouncement = new Announcement(
                    clusterViewService.getSlingId());
            topologyAnnouncement.setOriginInfo(originInfo);
            topologyAnnouncement.setServerInfo(serverInfo);
            topologyAnnouncement.setLocalCluster(clusterViewService
                    .getClusterView());
            announcementRegistry.addAllExcept(topologyAnnouncement, new AnnouncementFilter() {
                
                public boolean accept(final String receivingSlingId, final Announcement announcement) {
                    // filter out announcements that are of old cluster instances
                    // which I dont really have in my cluster view at the moment
                    final Iterator<InstanceDescription> it = 
                            clusterViewService.getClusterView().getInstances().iterator();
                    while(it.hasNext()) {
                        final InstanceDescription instance = it.next();
                        if (instance.getSlingId().equals(receivingSlingId)) {
                            // then I have the receiving instance in my cluster view
                            // all fine then
                            return true;
                        }
                    }
                    // looks like I dont have the receiving instance in my cluster view
                    // then I should also not propagate that announcement anywhere
                    return false;
                }
            });
            final String p = topologyAnnouncement.asJSON();

            logger.debug("ping: topologyAnnouncement json is: " + p);
            method.addParameter("topologyAnnouncement", p);
            httpClient.executeMethod(method);
            logger.debug("ping: done. code=" + method.getStatusCode() + " - "
                    + method.getStatusText());
            lastStatusCode = method.getStatusCode();
            if (method.getStatusCode()==200) {
                String responseBody = method.getResponseBodyAsString(16*1024*1024); // limiting to 16MB, should be way enough
                logger.debug("ping: response body=" + responseBody);
                Announcement inheritedAnnouncement = Announcement
                        .fromJSON(responseBody);
                inheritedAnnouncement.setInherited(true);
                if (!announcementRegistry
                        .registerAnnouncement(inheritedAnnouncement)) {
                    logger.info("ping: connector response is from an instance which I already see in my topology"
                            + inheritedAnnouncement);
                    lastInheritedAnnouncement = null;
                    return;
                }
                lastInheritedAnnouncement = inheritedAnnouncement;
            } else {
                lastInheritedAnnouncement = null;
            }
        } catch (URIException e) {
            logger.warn("ping: Got URIException: " + e);
        } catch (IOException e) {
            logger.warn("ping: got IOException: " + e);
        } catch (JSONException e) {
            logger.warn("ping: got JSONException: " + e);
        } catch (RuntimeException re) {
            logger.error("ping: got RuntimeException: " + re, re);
        }
    }

    public int getStatusCode() {
        return lastStatusCode;
    }
    
    public URL getConnectorUrl() {
        return connectorUrl;
    }

    public boolean isConnected() {
        if (lastInheritedAnnouncement == null) {
            return false;
        } else {
            return !lastInheritedAnnouncement.hasExpired(config);
        }
    }

    public String getRemoteSlingId() {
        if (lastInheritedAnnouncement == null) {
            return null;
        } else {
            return lastInheritedAnnouncement.getOwnerId();
        }
    }

    public OriginInfo getOriginInfo() {
        return originInfo;
    }

    public String getId() {
        return id.toString();
    }

    /** Disconnect this connector **/
    public void disconnect() {
        logger.debug("disconnect: connectorUrl=" + connectorUrl);

        if (lastInheritedAnnouncement != null) {
            announcementRegistry
                    .unregisterAnnouncement(lastInheritedAnnouncement
                            .getOwnerId());
        }

        HttpClient httpClient = new HttpClient();
        PostMethod method = new PostMethod(connectorUrl.toString());

        try {
            String userInfo = connectorUrl.getUserInfo();
            if (userInfo != null) {
                Credentials c = new UsernamePasswordCredentials(userInfo);
                httpClient.getState().setCredentials(
                        new AuthScope(method.getURI().getHost(), method
                                .getURI().getPort()), c);
            }

            method.addParameter("topologyDisconnect",
                    clusterViewService.getSlingId());
            httpClient.executeMethod(method);
            logger.debug("disconnect: done. code=" + method.getStatusCode()
                    + " - " + method.getStatusText());
        } catch (URIException e) {
            logger.warn("disconnect: Got URIException: " + e);
        } catch (IOException e) {
            logger.warn("disconnect: got IOException: " + e);
        } catch (RuntimeException re) {
            logger.error("disconnect: got RuntimeException: " + re, re);
        }
    }
}
