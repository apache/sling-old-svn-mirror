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
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.topology.announcement.Announcement;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementFilter;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet which receives topology announcements at
 * /libs/sling/topology/connector (which is reachable without authorization)
 */
@SuppressWarnings("serial")
@SlingServlet(paths = { TopologyConnectorServlet.TOPOLOGY_CONNECTOR_PATH })
@Property(name = "sling.auth.requirements", value = { "-"+TopologyConnectorServlet.TOPOLOGY_CONNECTOR_PATH })
public class TopologyConnectorServlet extends SlingAllMethodsServlet {

    public static final String TOPOLOGY_CONNECTOR_PATH = "/libs/sling/topology/connector";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private ClusterViewService clusterViewService;

    @Reference
    private HeartbeatHandler heartbeatHandler;

    @Reference
    private Config config;

    /** the set of ips/hostnames which are allowed to connect to this servlet **/
    private final Set<String> whitelist = new HashSet<String>();

    private TopologyRequestValidator requestValidator;


    protected void activate(final ComponentContext context) {
        whitelist.clear();
        if (!config.isHmacEnabled()) {
            String[] whitelistConfig = config.getTopologyConnectorWhitelist();
            for (int i = 0; i < whitelistConfig.length; i++) {
                String aWhitelistEntry = whitelistConfig[i];
                logger.info("activate: adding whitelist entry: " + aWhitelistEntry);
                whitelist.add(aWhitelistEntry);
            }
        }
        requestValidator = new TopologyRequestValidator(config);
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        if (!isWhitelisted(request)) {
            // in theory it would be 403==forbidden, but that would reveal that
            // a resource would exist there in the first place
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        final String extension = pathInfo.getExtension();
        if (!"json".equals(extension)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        final String selector = pathInfo.getSelectorString();

        announcementRegistry.unregisterAnnouncement(selector);
    }

    @Override
    protected void doPut(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {

        if (!isWhitelisted(request)) {
            // in theory it would be 403==forbidden, but that would reveal that
            // a resource would exist there in the first place
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final RequestPathInfo pathInfo = request.getRequestPathInfo();
        final String extension = pathInfo.getExtension();
        if (!"json".equals(extension)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        final String selector = pathInfo.getSelectorString();

        String topologyAnnouncementJSON = requestValidator.decodeMessage(request);
    	if (logger.isDebugEnabled()) {
	        logger.debug("doPost: incoming topology announcement is: "
	                + topologyAnnouncementJSON);
    	}
        final Announcement incomingTopologyAnnouncement;
        try {
            incomingTopologyAnnouncement = Announcement
                    .fromJSON(topologyAnnouncementJSON);

            if (!incomingTopologyAnnouncement.getOwnerId().equals(selector)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String slingId = clusterViewService.getSlingId();
            if (slingId==null) {
            	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            	logger.info("doPut: no slingId available. Service not ready as expected at the moment.");
            	return;
            }
			incomingTopologyAnnouncement.removeInherited(slingId);

            final Announcement replyAnnouncement = new Announcement(
                    slingId);

            if (!incomingTopologyAnnouncement.isCorrectVersion()) {
                logger.warn("doPost: rejecting an announcement from an incompatible connector protocol version: "
                        + incomingTopologyAnnouncement);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } else if (clusterViewService.contains(incomingTopologyAnnouncement
                    .getOwnerId())) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("doPost: rejecting an announcement from an instance that is part of my cluster: "
	                        + incomingTopologyAnnouncement);
            	}
                // marking as 'loop'
                replyAnnouncement.setLoop(true);
            } else if (clusterViewService.containsAny(incomingTopologyAnnouncement
                    .listInstances())) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("doPost: rejecting an announcement as it contains instance(s) that is/are part of my cluster: "
	                        + incomingTopologyAnnouncement);
            	}
                // marking as 'loop'
                replyAnnouncement.setLoop(true);
            } else if (!announcementRegistry
                    .registerAnnouncement(incomingTopologyAnnouncement)) {
            	if (logger.isDebugEnabled()) {
	                logger.debug("doPost: rejecting an announcement from an instance that I already see in my topology: "
	                        + incomingTopologyAnnouncement);
            	}
                // marking as 'loop'
                replyAnnouncement.setLoop(true);
            } else {
                // normal, successful case: replying with the part of the topology which this instance sees
                replyAnnouncement.setLocalCluster(clusterViewService
                        .getClusterView());
                announcementRegistry.addAllExcept(replyAnnouncement,
                        new AnnouncementFilter() {

                            public boolean accept(final String receivingSlingId, Announcement announcement) {
                                if (announcement.getPrimaryKey().equals(
                                        incomingTopologyAnnouncement
                                                .getPrimaryKey())) {
                                    return false;
                                }
                                return true;
                            }
                        });
            }
            final String p = requestValidator.encodeMessage(replyAnnouncement.asJSON());
            requestValidator.trustMessage(response, request, p);
            final PrintWriter pw = response.getWriter();
            pw.print(p);
            pw.flush();
        } catch (JSONException e) {
            logger.error("doPost: Got a JSONException: " + e, e);
            response.sendError(500);
        }

    }

    /** Checks if the provided request's remote server is whitelisted **/
    private boolean isWhitelisted(final SlingHttpServletRequest request) {
        if (config.isHmacEnabled()) {
            return requestValidator.isTrusted(request);
        } else {
            if (whitelist.contains(request.getRemoteAddr())) {
                return true;
            } else if (whitelist.contains(request.getRemoteHost())) {
                return true;
            }
        }
        logger.info("isWhitelisted: rejecting " + request.getRemoteAddr()
                + ", " + request.getRemoteHost());
        return false;
    }

}
