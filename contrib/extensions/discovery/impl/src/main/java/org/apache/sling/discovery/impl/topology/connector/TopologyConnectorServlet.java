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

import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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
@SlingServlet(paths = { "/libs/sling/topology/connector" })
@Property(name = "sling.auth.requirements", value = { "-/libs/sling/topology/connector" })
public class TopologyConnectorServlet extends SlingAllMethodsServlet {

    public static final String TOPOLOGY_CONNECTOR_PATH = "/libs/sling/topology/connector";

    private static final long serialVersionUID = 1300640476823585873L;

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

    protected void activate(final ComponentContext context) {
        whitelist.clear();
        String[] whitelistConfig = config.getTopologyConnectorWhitelist();
        for (int i = 0; i < whitelistConfig.length; i++) {
            String aWhitelistEntry = whitelistConfig[i];
            logger.info("activate: adding whitelist entry: " + aWhitelistEntry);
            whitelist.add(aWhitelistEntry);
        }
    }

    @Override
    protected void doPost(final SlingHttpServletRequest request,
            final SlingHttpServletResponse response) throws ServletException,
            IOException {

        if (!isWhitelisted(request)) {
            response.sendError(404); // in theory it would be 403==forbidden, but that would reveal that 
                                     // a resource would exist there in the first place
            return;
        }

        final String topologyAnnouncementJSON = request
                .getParameter("topologyAnnouncement");
        final String topologyDisconnect = request
                .getParameter("topologyDisconnect");
        if (topologyDisconnect != null) {
            announcementRegistry.unregisterAnnouncement(topologyDisconnect);
            return;
        }
        logger.debug("doPost: incoming topology announcement is: "
                + topologyAnnouncementJSON);
        final Announcement incomingTopologyAnnouncement;
        try {
            incomingTopologyAnnouncement = Announcement
                    .fromJSON(topologyAnnouncementJSON);
            incomingTopologyAnnouncement.removeInherited(clusterViewService
                    .getSlingId());

            if (clusterViewService.contains(incomingTopologyAnnouncement
                    .getOwnerId())) {
                logger.info("doPost: rejecting an announcement from an instance that is part of my cluster: "
                        + incomingTopologyAnnouncement);
                response.sendError(409);
                return;
            }
            if (clusterViewService.containsAny(incomingTopologyAnnouncement
                    .listInstances())) {
                logger.info("doPost: rejecting an announcement as it contains instance(s) that is/are part of my cluster: "
                        + incomingTopologyAnnouncement);
                response.sendError(409);
                return;
            }
            if (!announcementRegistry
                    .registerAnnouncement(incomingTopologyAnnouncement)) {
                logger.info("doPost: rejecting an announcement from an instance that I already see in my topology: "
                        + incomingTopologyAnnouncement);
                response.sendError(409);
                return;
            }

            Announcement replyAnnouncement = new Announcement(
                    clusterViewService.getSlingId());
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
            final String p = replyAnnouncement.asJSON();
            PrintWriter pw = response.getWriter();
            pw.print(p);
            pw.flush();
        } catch (JSONException e) {
            logger.error("doPost: Got a JSONException: " + e, e);
            response.sendError(500);
        }

    }

    /** Checks if the provided request's remote server is whitelisted **/
    private boolean isWhitelisted(final SlingHttpServletRequest request) {
        if (whitelist.contains(request.getRemoteAddr())) {
            return true;
        } else if (whitelist.contains(request.getRemoteHost())) {
            return true;
        }
        logger.info("isWhitelisted: rejecting " + request.getRemoteAddr()
                + ", " + request.getRemoteHost());
        return false;
    }

}
