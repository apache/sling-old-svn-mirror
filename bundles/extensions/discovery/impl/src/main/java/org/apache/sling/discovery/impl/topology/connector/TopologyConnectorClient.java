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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
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

    /** the information about this server **/
    private final String serverInfo;
    
    /** the status code of the last post **/
    private int lastStatusCode = -1;
    
    /** SLING-2882: whether or not to suppress ping warnings **/
    private boolean suppressPingWarnings_ = false;

    private TopologyRequestValidator requestValidator;

    TopologyConnectorClient(final ClusterViewService clusterViewService,
            final AnnouncementRegistry announcementRegistry, final Config config,
            final URL connectorUrl, final String serverInfo) {
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
        this.requestValidator = new TopologyRequestValidator(config);
        this.clusterViewService = clusterViewService;
        this.announcementRegistry = announcementRegistry;
        this.config = config;
        this.connectorUrl = connectorUrl;
        this.serverInfo = serverInfo;
        this.id = UUID.randomUUID();
    }

    /** ping the server and pass the announcements between the two **/
    void ping() {
        final String uri = connectorUrl.toString()+"."+clusterViewService.getSlingId()+".json";
    	if (logger.isDebugEnabled()) {
    		logger.debug("ping: connectorUrl=" + connectorUrl + ", complete uri=" + uri);
    	}
        HttpClient httpClient = new HttpClient();
        PutMethod method = new PutMethod(uri);
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
            final String p = requestValidator.encodeMessage(topologyAnnouncement.asJSON());

        	if (logger.isDebugEnabled()) {
        		logger.debug("ping: topologyAnnouncement json is: " + p);
        	}
        	requestValidator.trustMessage(method, p);
            method.setRequestEntity(new StringRequestEntity(p, "application/json", "UTF-8"));
            DefaultHttpMethodRetryHandler retryhandler = new DefaultHttpMethodRetryHandler(0, false);
            httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryhandler);
            httpClient.executeMethod(method);
        	if (logger.isDebugEnabled()) {
	            logger.debug("ping: done. code=" + method.getStatusCode() + " - "
	                    + method.getStatusText());
        	}
            lastStatusCode = method.getStatusCode();
            if (method.getStatusCode()==HttpServletResponse.SC_OK) {
                String responseBody = requestValidator.decodeMessage(method); // limiting to 16MB, should be way enough
            	if (logger.isDebugEnabled()) {
            		logger.debug("ping: response body=" + responseBody);
            	}
                if (responseBody!=null && responseBody.length()>0) {
                    Announcement inheritedAnnouncement = Announcement
                            .fromJSON(responseBody);
                    if (inheritedAnnouncement.isLoop()) {
                    	if (logger.isDebugEnabled()) {
	                        logger.debug("ping: connector response indicated a loop detected. not registering this announcement from "+
	                                    inheritedAnnouncement.getOwnerId());
                    	}
                    } else {
                        inheritedAnnouncement.setInherited(true);
                        if (!announcementRegistry
                                .registerAnnouncement(inheritedAnnouncement)) {
                        	if (logger.isDebugEnabled()) {
	                            logger.debug("ping: connector response is from an instance which I already see in my topology"
	                                    + inheritedAnnouncement);
                        	}
                            lastInheritedAnnouncement = null;
                            return;
                        }
                    }
                    lastInheritedAnnouncement = inheritedAnnouncement;
                } else {
                    lastInheritedAnnouncement = null;
                }
            } else {
                lastInheritedAnnouncement = null;
            }
        	// SLING-2882 : reset suppressPingWarnings_ flag in success case
    		suppressPingWarnings_ = false;
        } catch (URIException e) {
            logger.warn("ping: Got URIException: " + e + ", uri=" + uri);
            lastInheritedAnnouncement = null;
        } catch (IOException e) {
        	// SLING-2882 : set/check the suppressPingWarnings_ flag
        	if (suppressPingWarnings_) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("ping: got IOException: " + e + ", uri=" + uri);
        		}
        	} else {
        		suppressPingWarnings_ = true;
    			logger.warn("ping: got IOException [suppressing further warns]: " + e + ", uri=" + uri);
        	}
            lastInheritedAnnouncement = null;
        } catch (JSONException e) {
            logger.warn("ping: got JSONException: " + e);
            lastInheritedAnnouncement = null;
        } catch (RuntimeException re) {
            logger.warn("ping: got RuntimeException: " + re, re);
            lastInheritedAnnouncement = null;
        }
    }

    public int getStatusCode() {
        return lastStatusCode;
    }
    
    public URL getConnectorUrl() {
        return connectorUrl;
    }
    
    public boolean representsLoop() {
        if (lastInheritedAnnouncement == null) {
            return false;
        } else {
            return lastInheritedAnnouncement.isLoop();
        }
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

    public String getId() {
        return id.toString();
    }

    /** Disconnect this connector **/
    public void disconnect() {
        final String uri = connectorUrl.toString()+"."+clusterViewService.getSlingId()+".json";
    	if (logger.isDebugEnabled()) {
    		logger.debug("disconnect: connectorUrl=" + connectorUrl + ", complete uri="+uri);
    	}

        if (lastInheritedAnnouncement != null) {
            announcementRegistry
                    .unregisterAnnouncement(lastInheritedAnnouncement
                            .getOwnerId());
        }

        HttpClient httpClient = new HttpClient();
        DeleteMethod method = new DeleteMethod(uri);

        try {
            String userInfo = connectorUrl.getUserInfo();
            if (userInfo != null) {
                Credentials c = new UsernamePasswordCredentials(userInfo);
                httpClient.getState().setCredentials(
                        new AuthScope(method.getURI().getHost(), method
                                .getURI().getPort()), c);
            }

            requestValidator.trustMessage(method, null);
            httpClient.executeMethod(method);
        	if (logger.isDebugEnabled()) {
	            logger.debug("disconnect: done. code=" + method.getStatusCode()
	                    + " - " + method.getStatusText());
        	}
            // ignoring the actual statuscode though as there's little we can
            // do about it after this point
        } catch (URIException e) {
            logger.warn("disconnect: Got URIException: " + e);
        } catch (IOException e) {
            logger.warn("disconnect: got IOException: " + e);
        } catch (RuntimeException re) {
            logger.error("disconnect: got RuntimeException: " + re, re);
        }
    }
}
