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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.impl.Config;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.common.heartbeat.HeartbeatHandler;
import org.apache.sling.discovery.impl.topology.announcement.Announcement;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementFilter;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.impl.topology.connector.wl.SubnetWhitelistEntry;
import org.apache.sling.discovery.impl.topology.connector.wl.WhitelistEntry;
import org.apache.sling.discovery.impl.topology.connector.wl.WildcardWhitelistEntry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet which receives topology announcements at
 * /libs/sling/topology/connector*
 * without authorization (authorization is handled either via
 * hmac-signature with a shared key or via a flexible whitelist)
 */
@SuppressWarnings("serial")
@Component(immediate = true)
@Service(value=TopologyConnectorServlet.class)
public class TopologyConnectorServlet extends HttpServlet {

    /** 
     * prefix under which the topology connector servlet is registered -
     * the URL will consist of this prefix + "connector.slingId.json" 
     */
    private static final String TOPOLOGY_CONNECTOR_PREFIX = "/libs/sling/topology";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private ClusterViewService clusterViewService;

    @Reference
    private HeartbeatHandler heartbeatHandler;

    @Reference
    private HttpService httpService;
    
    @Reference
    private Config config;

    /** 
     * This list contains WhitelistEntry (ips/hostnames, cidr, wildcards),
     * each filtering some hostname/addresses that are allowed to connect to this servlet.
     **/
    private final List<WhitelistEntry> whitelist = new ArrayList<WhitelistEntry>();
    
    /** Set of plaintext whitelist entries - for faster lookups **/
    private final Set<String> plaintextWhitelist = new HashSet<String>();

    private TopologyRequestValidator requestValidator;

    @Activate
    protected void activate(final ComponentContext context) {
        whitelist.clear();
        if (!config.isHmacEnabled()) {
            String[] whitelistConfig = config.getTopologyConnectorWhitelist();
            initWhitelist(whitelistConfig);
        }
        requestValidator = new TopologyRequestValidator(config);
        
        try {
            httpService.registerServlet(TopologyConnectorServlet.TOPOLOGY_CONNECTOR_PREFIX, 
                    this, null, null);
            logger.info("activate: connector servlet registered at "+
                    TopologyConnectorServlet.TOPOLOGY_CONNECTOR_PREFIX);
        } catch (ServletException e) {
            logger.error("activate: ServletException while registering topology connector servlet: "+e, e);
        } catch (NamespaceException e) {
            logger.error("activate: NamespaceException while registering topology connector servlet: "+e, e);
        }
    }
    
    @Deactivate
    protected void deactivate() {
        httpService.unregister(TOPOLOGY_CONNECTOR_PREFIX);
    }

    void initWhitelist(String[] whitelistConfig) {
        if (whitelistConfig==null) {
            return;
        }
        for (int i = 0; i < whitelistConfig.length; i++) {
            String aWhitelistEntry = whitelistConfig[i];
            
            WhitelistEntry whitelistEntry = null;
            if (aWhitelistEntry.contains(".") && aWhitelistEntry.contains("/")) {
                // then this is a CIDR notation
                try{
                    whitelistEntry = new SubnetWhitelistEntry(aWhitelistEntry);
                } catch(Exception e) {
                    logger.error("activate: wrongly formatted CIDR subnet definition. Expected eg '1.2.3.4/24'. ignoring: "+aWhitelistEntry);
                    continue;
                }
            } else if (aWhitelistEntry.contains(".") && aWhitelistEntry.contains(" ")) {
                // then this is a IP/subnet-mask notation
                try{
                    final StringTokenizer st = new StringTokenizer(aWhitelistEntry, " ");
                    final String ip = st.nextToken();
                    if (st.hasMoreTokens()) {
                        final String mask = st.nextToken();
                        if (st.hasMoreTokens()) {
                            logger.error("activate: wrongly formatted ip subnet definition. Expected '10.1.2.3 255.0.0.0'. Ignoring: "+aWhitelistEntry);
                            continue;
                        }
                        whitelistEntry = new SubnetWhitelistEntry(ip, mask);
                    }
                } catch(Exception e) {
                    logger.error("activate: wrongly formatted ip subnet definition. Expected '10.1.2.3 255.0.0.0'. Ignoring: "+aWhitelistEntry);
                    continue;
                }
            }
            if (whitelistEntry==null) {
                if (aWhitelistEntry.contains("*") || aWhitelistEntry.contains("?")) {
                    whitelistEntry = new WildcardWhitelistEntry(aWhitelistEntry);
                } else {
                    plaintextWhitelist.add(aWhitelistEntry);
                }
            }
            logger.info("activate: adding whitelist entry: " + aWhitelistEntry);
            if (whitelistEntry!=null) {
                whitelist.add(whitelistEntry);
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isWhitelisted(request)) {
            // in theory it would be 403==forbidden, but that would reveal that
            // a resource would exist there in the first place
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final String[] pathInfo = request.getPathInfo().split("\\.");
        final String extension = pathInfo.length==3 ? pathInfo[2] : "";
        if (!"json".equals(extension)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        final String selector = pathInfo.length==3 ? pathInfo[1] : "";

        announcementRegistry.unregisterAnnouncement(selector);
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isWhitelisted(request)) {
            // in theory it would be 403==forbidden, but that would reveal that
            // a resource would exist there in the first place
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        final String[] pathInfo = request.getPathInfo().split("\\.");
        final String extension = pathInfo.length==3 ? pathInfo[2] : "";
        if (!"json".equals(extension)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        final String selector = pathInfo.length==3 ? pathInfo[1] : "";

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
            // gzip the response if the client accepts this
            final String acceptEncodingHeader = request.getHeader("Accept-Encoding");
            if (acceptEncodingHeader!=null && acceptEncodingHeader.contains("gzip")) {
                // tell the client that the content is gzipped:
                response.setHeader("Content-Encoding", "gzip");
                
                // then gzip the body
                final GZIPOutputStream gzipOut = new GZIPOutputStream(response.getOutputStream());
                gzipOut.write(p.getBytes("UTF-8"));
                gzipOut.close();
            } else {
                // otherwise plaintext
                final PrintWriter pw = response.getWriter();
                pw.print(p);
                pw.flush();
            }
        } catch (JSONException e) {
            logger.error("doPost: Got a JSONException: " + e, e);
            response.sendError(500);
        }

    }
    
    /** Checks if the provided request's remote server is whitelisted **/
    boolean isWhitelisted(final HttpServletRequest request) {
        if (config.isHmacEnabled()) {
            final boolean isTrusted = requestValidator.isTrusted(request);
            if (!isTrusted) {
                logger.info("isWhitelisted: rejecting distrusted " + request.getRemoteAddr()
                        + ", " + request.getRemoteHost());
            }
            return isTrusted;
        }
        
        if (plaintextWhitelist.contains(request.getRemoteHost()) ||
                plaintextWhitelist.contains(request.getRemoteAddr())) {
            return true;
        }

        for (Iterator<WhitelistEntry> it = whitelist.iterator(); it.hasNext();) {
            WhitelistEntry whitelistEntry = it.next();
            if (whitelistEntry.accepts(request)) {
                return true;
            }
        }
        logger.info("isWhitelisted: rejecting " + request.getRemoteAddr()
                + ", " + request.getRemoteHost());
        return false;
    }

}
