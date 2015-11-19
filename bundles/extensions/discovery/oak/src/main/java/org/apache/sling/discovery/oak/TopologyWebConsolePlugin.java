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
package org.apache.sling.discovery.oak;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.connectors.announcement.Announcement;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.base.connectors.announcement.CachedAnnouncement;
import org.apache.sling.discovery.base.connectors.ping.ConnectorRegistry;
import org.apache.sling.discovery.base.connectors.ping.TopologyConnectorClientInformation;
import org.apache.sling.discovery.commons.InstancesDiff;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteDescriptor;
import org.apache.sling.discovery.commons.providers.spi.base.OakBacklogClusterSyncService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple webconsole which gives an overview of the topology visible by the
 * discovery service
 */
@Component
@Service(value = { TopologyEventListener.class, Servlet.class })
@Properties({
    @Property(name=org.osgi.framework.Constants.SERVICE_DESCRIPTION,
            value="Apache Sling Web Console Plugin to display Background servlets and ExecutionEngine status"),
    @Property(name=WebConsoleConstants.PLUGIN_LABEL, value=TopologyWebConsolePlugin.LABEL),
    @Property(name=WebConsoleConstants.PLUGIN_TITLE, value=TopologyWebConsolePlugin.TITLE),
    @Property(name="felix.webconsole.category", value="Sling"),
    @Property(name="felix.webconsole.configprinter.modes", value={"zip"})
})
@SuppressWarnings("serial")
public class TopologyWebConsolePlugin extends AbstractWebConsolePlugin implements TopologyEventListener {

    public static final String LABEL = "topology";
    public static final String TITLE = "Topology Management";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** the truncated log of topology events, filtered by property change types. shown in webconsole **/
    private final List<String> propertyChangeLog = new LinkedList<String>();

    /** the truncated log of topology events, shown in webconsole **/
    private final List<String> topologyLog = new LinkedList<String>();

    /** the date format used in the truncated log of topology events **/
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    @Reference
    private ClusterViewService clusterViewService;

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private ConnectorRegistry connectorRegistry;

    @Reference
    protected ResourceResolverFactory resourceResolverFactory;

    @Reference
    private OakBacklogClusterSyncService clusterSyncService;

    private TopologyView currentView;
    
    private List<String> discoveryLiteHistory = new LinkedList<String>();

    /** 
     * keeps hold of the last DiscoveryLiteDescriptor that was added
     * to the discoveryLiteHistory - in order to de-duplicate as we go
     */
    private DiscoveryLiteDescriptor lastDiscoveryLiteDescriptor = null;
    
    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Activate
    @Override
    public void activate(final BundleContext bundleContext) {
        super.activate(bundleContext);
    }

    @Deactivate
    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void renderContent(final HttpServletRequest req, final HttpServletResponse res)
            throws ServletException, IOException {
        Object rawRoot = req.getAttribute(WebConsoleConstants.ATTR_PLUGIN_ROOT);
        if (!(rawRoot instanceof String)) {
            throw new ServletException("Illegal attr: "
                    + WebConsoleConstants.ATTR_PLUGIN_ROOT);
        }

        String root = rawRoot.toString();
        String pathInfo = req.getRequestURI().substring(root.length());

        final PrintWriter pw = res.getWriter();

        if (pathInfo.equals("")) {
            if ( this.currentView != null ) {
                renderOverview(pw,  currentView);
            } else {
                pw.println("<p class=\"statline ui-state-highlight\">No view available</p>");
                pw.println("<br/>");
                pw.println("No TOPOLOGY_INIT received yet, therefore no view available yet.");
            }
        } else {
            StringTokenizer st = new StringTokenizer(pathInfo, "/");
            final String nodeId = st.nextToken();
            renderProperties(pw, req.getContextPath(), nodeId);
        }
    }

    /**
     * Render the properties page of a particular instance
     */
    private void renderProperties(final PrintWriter pw, final String contextPath, final String nodeId) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("renderProperties: nodeId=" + nodeId);
    	}
        final TopologyView tv = this.currentView;
        @SuppressWarnings("unchecked")
        Set<InstanceDescription> instances = ( tv == null ? (Set<InstanceDescription>)Collections.EMPTY_SET :

                tv.findInstances(new InstanceFilter() {

                    public boolean accept(InstanceDescription instance) {
                        String slingId = instance.getSlingId();
                    	if (logger.isDebugEnabled()) {
	                        logger.debug("renderProperties/picks: slingId={}", slingId);
                    	}
                        return (slingId.equals(nodeId));
                    }
                }));

        if (instances != null && instances.size() == 1) {
            InstanceDescription instance = instances.iterator().next();
            pw.println("Properties of " + instance.getSlingId() + ":<br/>");

            pw.println("<table class=\"adapters nicetable ui-widget tablesorter\">");
            pw.println("<thead>");
            pw.println("<tr>");
            pw.println("<th class=\"header ui-widget-header\">Key</th>");
            pw.println("<th class=\"header ui-widget-header\">Value</th>");
            pw.println("</tr>");
            pw.println("</thead>");
            pw.println("<tbody>");
            boolean odd = true;
            for (Iterator<Entry<String, String>> it = instance.getProperties()
                    .entrySet().iterator(); it.hasNext();) {
                Entry<String, String> entry = it.next();
                String oddEven = odd ? "odd" : "even";
                odd = !odd;
                pw.println("<tr class=\"" + oddEven + " ui-state-default\">");

                pw.println("<td>" + entry.getKey() + "</td>");
                pw.println("<td>" + entry.getValue() + "</td>");

                pw.println("</tr>");
            }
            pw.println("</tbody>");
            pw.println("</table>");
        }
    }

    protected ResourceResolver getResourceResolver() throws LoginException {
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    /**
     * Render the overview of the entire topology
     */
    private void renderOverview(final PrintWriter pw, final TopologyView topology) {
        pw.println("<p class=\"statline ui-state-highlight\">Configuration</p>");
        pw.println("<br/>");
        pw.print("<a href=\"${appRoot}/configMgr/org.apache.sling.discovery.oak.Config\">Configure Discovery.Oak Service</a>");
        pw.println("<br/>");
        pw.println("<br/>");
        final String changing;
        if (!topology.isCurrent()) {
        	changing = " <b><i>CHANGING!</i> (the view is no longer current!)</b>";
        } else {
        	changing = "";
        }
        pw.println("<p class=\"statline ui-state-highlight\">Topology"+changing+"</p>");
        pw.println("<div class=\"ui-widget-header ui-corner-top buttonGroup\" style=\"height: 15px;\">");
        pw.println("<span style=\"float: left; margin-left: 1em;\">Instances in the topology</span>");
        pw.println("</div>");
        pw.println("<table class=\"adapters nicetable ui-widget tablesorter\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Sling id (click for properties)</th>");
        pw.println("<th class=\"header ui-widget-header\">ClusterView id</th>");
        pw.println("<th class=\"header ui-widget-header\">Local instance</th>");
        pw.println("<th class=\"header ui-widget-header\">Leader instance</th>");
        pw.println("<th class=\"header ui-widget-header\">In local cluster</th>");
        pw.println("<th class=\"header ui-widget-header\">Announced by instance</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        Set<ClusterView> clusters = topology.getClusterViews();
        ClusterView myCluster = topology.getLocalInstance().getClusterView();
        boolean odd = true;
        renderCluster(pw, myCluster, myCluster, odd, topology.isCurrent());

        for (Iterator<ClusterView> it = clusters.iterator(); it.hasNext();) {
            ClusterView clusterView = it.next();
            if (clusterView.equals(myCluster)) {
                // skip - I already rendered that
                continue;
            }
            odd = !odd;
            renderCluster(pw, clusterView, myCluster, odd, topology.isCurrent());
        }

        pw.println("</tbody>");
        pw.println("</table>");

        pw.println("<br/>");
        pw.println("<br/>");
        pw.println("<p class=\"statline ui-state-highlight\">Connectors</p>");
        listIncomingTopologyConnectors(pw);
        listOutgoingTopologyConnectors(pw);
        pw.println("<br/>");
        
        pw.println("<p class=\"statline ui-state-highlight\">Topology Change History</p>");
        pw.println("<pre>");
        for (Iterator<String> it = topologyLog
                .iterator(); it.hasNext();) {
            String aLogEntry = it.next();
            pw.println(aLogEntry);
        }
        pw.println("</pre>");
        pw.println("<br/>");
        pw.println("<p class=\"statline ui-state-highlight\">Property Change History</p>");
        pw.println("<pre>");
        for (Iterator<String> it = propertyChangeLog
                .iterator(); it.hasNext();) {
            String aLogEntry = it.next();
            pw.println(aLogEntry);
        }
        pw.println("</pre>");
        pw.println("</br>");

        pw.println("<p class=\"statline ui-state-highlight\">Oak Discovery-Lite Descriptor History</p>");
        updateDiscoveryLiteHistory();
        pw.println("<pre>");
        for (String discoLiteHistoryEntry : discoveryLiteHistory) {
            pw.println(discoLiteHistoryEntry);
        }
        pw.println("</pre>");
        pw.println("<br/>");

        pw.println("<p class=\"statline ui-state-highlight\">ClusterSyncService History</p>");
        pw.println("<pre>");
        for (String syncHistoryEntry : clusterSyncService.getSyncHistory()) {
            pw.println(syncHistoryEntry);
        }
        pw.println("</pre>");
        pw.println("<br/>");
    }

    /**
     * Render a particular cluster (into table rows)
     */
    private void renderCluster(final PrintWriter pw, final ClusterView renderCluster, final ClusterView localCluster, final boolean odd, final boolean current) {
        final Collection<Announcement> announcements = announcementRegistry.listAnnouncementsInSameCluster(localCluster);

        for (Iterator<InstanceDescription> it = renderCluster.getInstances()
                .iterator(); it.hasNext();) {
            final InstanceDescription instanceDescription = it.next();
            final boolean inLocalCluster = renderCluster == localCluster;
            Announcement parentAnnouncement = null;
            for (Iterator<Announcement> it2 = announcements.iterator(); it2
                    .hasNext();) {
                Announcement announcement = it2.next();
                for (Iterator<InstanceDescription> it3 = announcement
                        .listInstances().iterator(); it3.hasNext();) {
                    InstanceDescription announcedInstance = it3.next();
                    if (announcedInstance.getSlingId().equals(
                            instanceDescription.getSlingId())) {
                        parentAnnouncement = announcement;
                        break;
                    }
                }
            }

            final String oddEven = odd ? "odd" : "even";

            if (current && (inLocalCluster || (parentAnnouncement!=null))) {
                pw.println("<tr class=\"" + oddEven + " ui-state-default\">");
            } else {
                pw.println("<tr class=\"" + oddEven + " ui-state-error\">");
            }
            final boolean isLocal = instanceDescription.isLocal();
            final String slingId = instanceDescription.getSlingId();
            pw.print("<td>");
            if ( isLocal) {
                pw.print("<b>");
            }
            pw.print("<a href=\"");
            pw.print(this.getLabel());
            pw.print('/');
            pw.print(slingId);
            pw.print("\">");
            pw.print(slingId);
            pw.print("</a>");
            if ( isLocal) {
                pw.print("</b>");
            }
            pw.println("</td>");
            pw.println("<td>"
                    + (instanceDescription.getClusterView() == null ? "null"
                            : instanceDescription.getClusterView().getId())
                    + "</td>");
            pw.println("<td>" + (isLocal ? "<b>true</b>" : "false") + "</td>");
            pw.println("<td>"
                    + (instanceDescription.isLeader() ? "<b>true</b>" : "false")
                    + "</td>");
            if (inLocalCluster) {
                pw.println("<td>local</td>");
                pw.println("<td>n/a</td>");
            } else {
                pw.println("<td>remote</td>");
                if (parentAnnouncement != null) {
                    pw.println("<td>" + parentAnnouncement.getOwnerId()
                            + "</td>");
                } else {
                    pw.println("<td><b>(changing)</b></td>");
                }
            }
            pw.println("</tr>");
        }

    }

    /**
     * Render the outgoing topology connectors - including the header-div and table
     */
    private void listOutgoingTopologyConnectors(final PrintWriter pw) {
        boolean odd = false;
        pw.println("<div class=\"ui-widget-header ui-corner-top buttonGroup\" style=\"height: 15px;\">");
        pw.println("<span style=\"float: left; margin-left: 1em;\">Outgoing topology connectors</span>");
        pw.println("</div>");
        pw.println("<table class=\"adapters nicetable ui-widget tablesorter\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Connector url</th>");
        pw.println("<th class=\"header ui-widget-header\">Connected to slingId</th>");
        pw.println("<th class=\"header ui-widget-header\">Connector status</th>");
        pw.println("<th class=\"header ui-widget-header\">Last heartbeat&nbsp;</th>");
        pw.println("<th class=\"header ui-widget-header\">Next heartbeat&nbsp;</th>");
        pw.println("<th class=\"header ui-widget-header\">Request encoding&nbsp;</th>");
        pw.println("<th class=\"header ui-widget-header\">Response encoding&nbsp;</th>");
        // pw.println("<th class=\"header ui-widget-header\">Fallback connector urls</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        Collection<TopologyConnectorClientInformation> outgoingConnections = connectorRegistry
                .listOutgoingConnectors();
        for (Iterator<TopologyConnectorClientInformation> it = outgoingConnections
                .iterator(); it.hasNext();) {
            TopologyConnectorClientInformation topologyConnectorClient = it
                    .next();
            final String oddEven = odd ? "odd" : "even";
            odd = !odd;
            final String remoteSlingId = topologyConnectorClient.getRemoteSlingId();
            final boolean isConnected = topologyConnectorClient.isConnected() && remoteSlingId != null;
            final boolean autoStopped = topologyConnectorClient.isAutoStopped();
            final boolean representsLoop = topologyConnectorClient.representsLoop();
            if (isConnected || autoStopped || representsLoop) {
                pw.println("<tr class=\"" + oddEven + " ui-state-default\">");
            } else {
                pw.println("<tr class=\"" + oddEven + " ui-state-error\">");
            }
            pw.println("<td>"
                    + topologyConnectorClient.getConnectorUrl().toString()
                    + "</td>");
            if (autoStopped) {
            	pw.println("<td><b>auto-stopped</b></td>");
            	pw.println("<td><b>auto-stopped due to local-loop</b></td>");
            } else if (isConnected && !representsLoop) {
                pw.println("<td>" + remoteSlingId + "</td>");
                pw.println("<td>ok, in use</td>");
            } else if (representsLoop) {
                pw.println("<td>" + remoteSlingId + "</td>");
                pw.println("<td>ok, unused (loop or duplicate): standby</td>");
            } else {
                final int statusCode = topologyConnectorClient.getStatusCode();
                final String statusDetails = topologyConnectorClient.getStatusDetails();
                final String tooltipText;
                switch(statusCode) {
                case HttpServletResponse.SC_UNAUTHORIZED:
                    tooltipText = HttpServletResponse.SC_UNAUTHORIZED +
                        ": possible setup issue of discovery.oak on target instance, or wrong URL";
                    break;
                case HttpServletResponse.SC_NOT_FOUND:
                    tooltipText = HttpServletResponse.SC_NOT_FOUND +
                        ": possible white list rejection by target instance";
                    break;
                case -1:
                    tooltipText = "-1: check error log. possible connection refused.";
                    break;
                default:
                    tooltipText = null;
                }
                final String tooltip = tooltipText==null ? "" : (" title=\""+tooltipText+"\"");
                pw.println("<td><b>not connected</b></td>");
                pw.println("<td"+tooltip+"><b>not ok (HTTP Status-Code: "+statusCode+", "+statusDetails+")</b></td>");
            }
            pw.println("<td>"+beautifiedTimeDiff(topologyConnectorClient.getLastPingSent())+"</td>");
            pw.println("<td>"+beautifiedDueTime(topologyConnectorClient.getNextPingDue())+"</td>");
            pw.println("<td>"+topologyConnectorClient.getLastRequestEncoding()+"</td>");
            pw.println("<td>"+topologyConnectorClient.getLastResponseEncoding()+"</td>");
            // //TODO fallback urls are not yet implemented!
            // String fallbackConnectorUrls;
            // List<String> urls = topologyConnectorClient
            // .listFallbackConnectorUrls();
            // if (urls == null || urls.size() == 0) {
            // fallbackConnectorUrls = "n/a";
            // } else {
            // fallbackConnectorUrls = "";
            // for (Iterator<String> it2 = urls.iterator(); it2.hasNext();) {
            // String aFallbackConnectorUrl = it2.next();
            // fallbackConnectorUrls = fallbackConnectorUrls
            // + aFallbackConnectorUrl + "<br/>";
            // }
            // }
            // pw.println("<td>" + fallbackConnectorUrls + "</td>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
    }
    
    private String beautifiedDueTime(long secondsDue) {
        if (secondsDue<-1) {
            return "overdue";
        } else if (secondsDue<=0) {
            return "now-ish";
        } else if (secondsDue==1) {
            return "in 1 second";
        } else {
            int minsDue = (int) (secondsDue / 60);
            if (minsDue<5) {
                return "in "+secondsDue+" seconds";
            } else {
                return "in "+minsDue+" minutes";
            }
        }
    }

    private String beautifiedTimeDiff(long heartbeatTime) {
        final long diff = System.currentTimeMillis() - heartbeatTime;
        long seconds = (diff/1000);
        if (heartbeatTime<=0) {
            return "n/a";
        } else if (seconds==0) {
            return diff+" millis ago";
        } else if (seconds==1) {
            return "1 second ago";
        } else if (seconds<300) {
            // then print seconds
            return seconds+" seconds ago";
        } else {
            // then print seconds
            return (seconds/60)+" minute ago";
        }
    }

    /**
     * Render the incoming topology connectors - including the header-div and table
     */
    private void listIncomingTopologyConnectors(final PrintWriter pw) {
        boolean odd = false;
        pw.println("<div class=\"ui-widget-header ui-corner-top buttonGroup\" style=\"height: 15px;\">");
        pw.println("<span style=\"float: left; margin-left: 1em;\">Incoming topology connectors</span>");
        pw.println("</div>");
        pw.println("<table class=\"adapters nicetable ui-widget tablesorter\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Owner slingId</th>");
        pw.println("<th class=\"header ui-widget-header\">Server info</th>");
        pw.println("<th class=\"header ui-widget-header\">Last heartbeat</th>");
        pw.println("<th class=\"header ui-widget-header\">Timeout</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        Collection<CachedAnnouncement> incomingConnections = announcementRegistry.listLocalIncomingAnnouncements();
        for (Iterator<CachedAnnouncement> it = incomingConnections.iterator(); it
                .hasNext();) {
            CachedAnnouncement incomingCachedAnnouncement = it.next();
            Announcement incomingAnnouncement = incomingCachedAnnouncement.getAnnouncement();
            String oddEven = odd ? "odd" : "even";
            odd = !odd;

            pw.println("<tr class=\"" + oddEven + " ui-state-default\">");
            pw.println("<td>" + incomingAnnouncement.getOwnerId() + "</td>");
            if (incomingAnnouncement.getServerInfo() != null) {
                pw.println("<td>" + incomingAnnouncement.getServerInfo()
                        + "</td>");
            } else {
                pw.println("<td><i>n/a</i></td>");
            }
            pw.println("<td>"+beautifiedTimeDiff(incomingCachedAnnouncement.getLastPing())+"</td>");
            pw.println("<td>"+beautifiedDueTime(incomingCachedAnnouncement.getSecondsUntilTimeout())+"</td>");

            pw.println("</tr>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("<br/>");
        pw.println("<br/>");
    }

    /**
     * keep a truncated history of the log events for information purpose (to be shown in the webconsole)
     */
    public void handleTopologyEvent(final TopologyEvent event) {
        if (event.getType() == Type.PROPERTIES_CHANGED) {
            this.currentView = event.getNewView();

            StringBuilder sb = new StringBuilder();
            InstancesDiff instanceDiff = new InstancesDiff(event.getOldView(), event.getNewView());
            
            // there shouldn't be any instances added, but for paranoia reason:
            Collection<InstanceDescription> added = instanceDiff.added().get();
            if (!added.isEmpty()) {
                sb.append("instances were added:");
                for (InstanceDescription instance : added) {
                    sb.append(" ");
                    sb.append(instance.getSlingId());
                }
                sb.append(".");
            }
            
            // there shouldn't be any instances removed as well, but again for paranoia reason:
            Collection<InstanceDescription> removed = instanceDiff.removed().get();
            if (!removed.isEmpty()) {
                sb.append("instances were removed:");
                for (InstanceDescription instance : added) {
                    sb.append(" ");
                    sb.append(instance.getSlingId());
                }
                sb.append(".");
            }
            
            Set<InstanceDescription> newInstances = event.getNewView()
                    .getInstances();
            for (Iterator<InstanceDescription> it = newInstances.iterator(); it
                    .hasNext();) {
                final InstanceDescription newInstanceDescription = it.next();
                InstanceDescription oldInstanceDescription = findInstance(
                        event.getOldView(), newInstanceDescription.getSlingId());
                if (oldInstanceDescription == null) {
                    logger.error("handleTopologyEvent: got a property changed but did not find instance "
                            + newInstanceDescription
                            + " in oldview.. event="
                            + event);
                    sb.append("did not find instance in old view: " + newInstanceDescription.getSlingId() + ".");
                    continue;
                }

                Map<String, String> oldProps = oldInstanceDescription
                        .getProperties();
                Map<String, String> newProps = newInstanceDescription
                        .getProperties();
                StringBuilder diff = diff(oldProps, newProps);
                if (diff.length() > 0) {
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append("on instance "
                            + newInstanceDescription.getSlingId() + (newInstanceDescription.isLeader() ? " [isLeader]" : "") 
                            + ": " + diff + ". ");
                }
            }

            addEventLog(event.getType(), "details: "+sb.toString());
        } else if (event.getType() == Type.TOPOLOGY_INIT) {
            this.currentView = event.getNewView();
            StringBuilder details = new StringBuilder();
            for (Iterator<InstanceDescription> it = event.getNewView()
                    .getInstances().iterator(); it.hasNext();) {
                InstanceDescription newInstance = it.next();
                if (details.length() != 0) {
                    details.append(", ");
                }
                details.append(newInstance.getSlingId());
                if (newInstance.isLeader()) {
                    details.append(" [isLeader]");
                }
            }
            addEventLog(event.getType(),
                    "view: " + shortViewInfo(event.getNewView()) + ". "
                            + details);
        } else if (event.getType() == Type.TOPOLOGY_CHANGING) {
            this.currentView = event.getOldView();
            addEventLog(event.getType(),
                    "old view: " + shortViewInfo(event.getOldView()));
        } else {
            this.currentView = event.getNewView();
            if (event.getOldView() == null) {
                addEventLog(event.getType(),
                        "new view: " + shortViewInfo(event.getNewView()));
            } else {
                StringBuilder details = new StringBuilder();
                for (Iterator<InstanceDescription> it = event.getNewView()
                        .getInstances().iterator(); it.hasNext();) {
                    InstanceDescription newInstance = it.next();
                    if (findInstance(event.getOldView(),
                            newInstance.getSlingId()) == null) {
                        if (details.length() != 0) {
                            details.append(", ");
                        }
                        details.append(newInstance.getSlingId() + " joined");
                    }
                }
                for (Iterator<InstanceDescription> it = event.getOldView()
                        .getInstances().iterator(); it.hasNext();) {
                    InstanceDescription oldInstance = it.next();
                    if (findInstance(event.getNewView(),
                            oldInstance.getSlingId()) == null) {
                        if (details.length() != 0) {
                            details.append(", ");
                        }
                        details.append(oldInstance.getSlingId() + " left");
                    }
                }
                final InstanceDescription li = event.getNewView().getLocalInstance();
                if (li!=null) {
                    ClusterView clusterView = li.getClusterView();
                    if (clusterView!=null) {
                        final InstanceDescription leader = clusterView.getLeader();
                        if (leader!=null) {
                            if (details.length() !=0) {
                                details.append(", ");
                            }
                            details.append("[isLeader: "+leader.getSlingId()+"]");
                        }
                    }
                }
    
                addEventLog(
                        event.getType(),
                        "old view: " + shortViewInfo(event.getOldView())
                                + ", new view: "
                                + shortViewInfo(event.getNewView()) + ". "
                                + details);
            }
        }
        updateDiscoveryLiteHistory();
    }

    /**
     * find a particular instance in the topology
     */
    private InstanceDescription findInstance(final TopologyView view,
            final String slingId) {
        Set<InstanceDescription> foundInstances = view
                .findInstances(new InstanceFilter() {

                    public boolean accept(InstanceDescription instance) {
                        return instance.getSlingId().equals(slingId);
                    }
                });
        if (foundInstances.size() == 1) {
            return foundInstances.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * add a log entry and truncate the log entries if necessary
     */
    private synchronized void addEventLog(final Type type, final String info) {
        final String logEntry = getCurrentDateFormatted() + ": " + type + ". " + info;

        if (type == Type.PROPERTIES_CHANGED) {
            propertyChangeLog.add(logEntry);
            while (propertyChangeLog.size() > 12) {
                propertyChangeLog.remove(0);
            }
        } else {
            topologyLog.add(logEntry);
            while (topologyLog.size() > 12) {
                topologyLog.remove(0);
            }
        }

    }

    /**
     * if there's any change add a discoveryLite descriptor entry to the history, truncating if necessary
     */
    private synchronized void updateDiscoveryLiteHistory() {
        ResourceResolver resourceResolver = null;
        try{
            resourceResolver = getResourceResolver();
            DiscoveryLiteDescriptor descriptor = 
                    DiscoveryLiteDescriptor.getDescriptorFrom(resourceResolver);

            if (lastDiscoveryLiteDescriptor!=null && 
                    descriptor.getDescriptorStr().equals(lastDiscoveryLiteDescriptor.getDescriptorStr())) {
                // de-duplication - then there's nothing to update
                return;
            }
            final String logEntry = getCurrentDateFormatted() + ": " + descriptor.getDescriptorStr();
            lastDiscoveryLiteDescriptor = descriptor;
            discoveryLiteHistory.add(logEntry);
            while (discoveryLiteHistory.size() > 12) {
                discoveryLiteHistory.remove(0);
            }
        } catch(Exception e) {
            logger.error("addDiscoveryLiteHistoryEntry: Exception: "+e, e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
        
    }

    /**
     * compile a short information string of the topology, including
     * number of clusters and instances
     */
    private String shortViewInfo(final TopologyView view) {
        int clusters = view.getClusterViews().size();
        int instances = view.getInstances().size();
        return ((clusters == 1) ? "1 cluster" : clusters + " clusters") + ", "
                + ((instances == 1) ? "1 instance" : instances + " instances");
    }

    /**
     * calculate the difference between two sets of properties
     */
    private StringBuilder diff(final Map<String, String> oldProps,
            final Map<String, String> newProps) {
        final Set<String> oldKeys = new HashSet<String>(oldProps.keySet());
        final Set<String> newKeys = new HashSet<String>(newProps.keySet());

        StringBuilder sb = new StringBuilder();

        for (Iterator<String> it = oldKeys.iterator(); it.hasNext();) {
            String oldKey = it.next();
            if (newKeys.contains(oldKey)) {
                if (oldProps.get(oldKey).equals(newProps.get(oldKey))) {
                    // perfect
                } else {
                    sb.append("(" + oldKey + " changed from "
                            + oldProps.get(oldKey) + " to "
                            + newProps.get(oldKey) + ")");
                }
                newKeys.remove(oldKey);
            } else {
                sb.append("(" + oldKey + " was removed)");
            }
            it.remove();
        }
        for (Iterator<String> it = newKeys.iterator(); it.hasNext();) {
            String newKey = it.next();
            sb.append("(" + newKey + " was added)");
        }

        return sb;
    }

    public void printConfiguration( final PrintWriter pw ) {
        final TopologyView topology = this.currentView;

        pw.println(TITLE);
        pw.println("---------------------------------------");
        pw.println();
        if ( topology == null ) {
            pw.println("No topology available yet!");
            return;
        }
        pw.print("Topology");
        if (!topology.isCurrent()) {
            pw.print(" CHANGING! (the view is no longer current!)");
        }
        pw.println();
        pw.println();

        final Set<ClusterView> clusters = topology.getClusterViews();
        final ClusterView myCluster = topology.getLocalInstance().getClusterView();
        printCluster(pw, myCluster, myCluster);

        for (Iterator<ClusterView> it = clusters.iterator(); it.hasNext();) {
            ClusterView clusterView = it.next();
            if (clusterView.equals(myCluster)) {
                // skip - I already rendered that
                continue;
            }
            printCluster(pw, clusterView, myCluster);
        }

        pw.println();
        pw.println();

        final Collection<CachedAnnouncement> incomingConnections = announcementRegistry.listLocalIncomingAnnouncements();
        if ( incomingConnections.size() > 0 ) {
            pw.println("Incoming topology connectors");
            pw.println("---------------------------------------");

            for(final CachedAnnouncement incomingCachedAnnouncement : incomingConnections) {
                Announcement incomingAnnouncement = incomingCachedAnnouncement.getAnnouncement();
                pw.print("Owner Sling Id : ");
                pw.print(incomingAnnouncement.getOwnerId());
                pw.println();
                if (incomingAnnouncement.getServerInfo() != null) {
                    pw.print("Server Info : ");
                    pw.print(incomingAnnouncement.getServerInfo());
                    pw.println();
                }
                pw.println("Last heartbeat received : "+beautifiedTimeDiff(incomingCachedAnnouncement.getLastPing()));
                pw.println("Timeout : "+beautifiedDueTime(incomingCachedAnnouncement.getSecondsUntilTimeout()));

                pw.println();
            }
            pw.println();
            pw.println();
        }

        final Collection<TopologyConnectorClientInformation> outgoingConnections = connectorRegistry.listOutgoingConnectors();
        if ( outgoingConnections.size() > 0 ) {
            pw.println("Outgoing topology connectors");
            pw.println("---------------------------------------");

            for(final TopologyConnectorClientInformation topologyConnectorClient : outgoingConnections) {
                final String remoteSlingId = topologyConnectorClient.getRemoteSlingId();
                final boolean autoStopped = topologyConnectorClient.isAutoStopped();
                final boolean isConnected = topologyConnectorClient.isConnected() && remoteSlingId != null;
                pw.print("Connector URL : ");
                pw.print(topologyConnectorClient.getConnectorUrl());
                pw.println();

                if (autoStopped) {
                    pw.println("Conncted to Sling Id : auto-stopped");
                    pw.println("Connector status : auto-stopped due to local-loop");
                } else if (isConnected && !topologyConnectorClient.representsLoop()) {
                    pw.print("Connected to Sling Id : ");
                    pw.println(remoteSlingId);
                    pw.println("Connector status : ok, in use");
                } else if (topologyConnectorClient.representsLoop()) {
                    pw.print("Connected to Sling Id : ");
                    pw.println(remoteSlingId);
                    pw.println("Connector status : ok, unused (loop or duplicate): standby");
                } else {
                    final int statusCode = topologyConnectorClient.getStatusCode();
                    final String statusDetails = topologyConnectorClient.getStatusDetails();
                    final String tooltipText;
                    switch(statusCode) {
                        case HttpServletResponse.SC_UNAUTHORIZED:
                            tooltipText = HttpServletResponse.SC_UNAUTHORIZED +
                                ": possible setup issue of discovery.oak on target instance, or wrong URL";
                            break;
                        case HttpServletResponse.SC_NOT_FOUND:
                            tooltipText = HttpServletResponse.SC_NOT_FOUND +
                                ": possible white list rejection by target instance";
                            break;
                        case -1:
                            tooltipText = "-1: check error log. possible connection refused.";
                            break;
                        default:
                            tooltipText = null;
                    }
                    pw.println("Connected to Sling Id : not connected");
                    pw.print("Connector status : not ok");
                    if ( tooltipText != null ) {
                        pw.print(" (");
                        pw.print(tooltipText);
                        pw.print(")");
                    }
                    pw.print(" (HTTP StatusCode: "+statusCode+", "+statusDetails+")");
                    pw.println();
                    pw.println("Last heartbeat sent : "+beautifiedTimeDiff(topologyConnectorClient.getLastPingSent()));
                    pw.println("Next heartbeat due : "+beautifiedDueTime(topologyConnectorClient.getNextPingDue()));
                }
                pw.println();
            }
            pw.println();
            pw.println();
        }

        if ( topologyLog.size() > 0 ) {
            pw.println("Topology Change History");
            pw.println("---------------------------------------");
            for(final String aLogEntry : topologyLog) {
                pw.println(aLogEntry);
            }
            pw.println();
            pw.println();
        }

        if ( propertyChangeLog.size() > 0 ) {
            pw.println("Property Change History");
            pw.println("---------------------------------------");
            for(final String aLogEntry : propertyChangeLog) {
                pw.println(aLogEntry);
            }
            pw.println();
        }

        pw.println("Oak Discovery-Lite Descriptor History");
        pw.println("---------------------------------------");
        updateDiscoveryLiteHistory();
        for (String discoLiteHistoryEntry : discoveryLiteHistory) {
            pw.println(discoLiteHistoryEntry);
        }
        pw.println();
        pw.println();

        pw.println("ClusterSyncService History");
        pw.println("---------------------------------------");
        for (String syncHistoryEntry : clusterSyncService.getSyncHistory()) {
            pw.println(syncHistoryEntry);
        }
        pw.println();
        pw.println();
    }

    private String getCurrentDateFormatted() {
        return sdf.format(Calendar.getInstance().getTime());
    }

    /**
     * Render a particular cluster
     */
    private void printCluster(final PrintWriter pw, final ClusterView renderCluster, final ClusterView localCluster) {
        final Collection<Announcement> announcements = announcementRegistry.listAnnouncementsInSameCluster(localCluster);

        for(final InstanceDescription instanceDescription : renderCluster.getInstances() ) {
            final boolean inLocalCluster = renderCluster == localCluster;
            Announcement parentAnnouncement = null;
            for (Iterator<Announcement> it2 = announcements.iterator(); it2
                    .hasNext();) {
                Announcement announcement = it2.next();
                for (Iterator<InstanceDescription> it3 = announcement
                        .listInstances().iterator(); it3.hasNext();) {
                    InstanceDescription announcedInstance = it3.next();
                    if (announcedInstance.getSlingId().equals(
                            instanceDescription.getSlingId())) {
                        parentAnnouncement = announcement;
                        break;
                    }
                }
            }

            final boolean isLocal = instanceDescription.isLocal();
            final String slingId = instanceDescription.getSlingId();

            pw.print("Sling ID : ");
            pw.print(slingId);
            pw.println();
            pw.print("Cluster View ID : ");
            pw.print(instanceDescription.getClusterView() == null ? "null"
                            : instanceDescription.getClusterView().getId());
            pw.println();
            pw.print("Local instance : ");
            pw.print(isLocal);
            pw.println();
            pw.print("Leader instance : ");
            pw.print(instanceDescription.isLeader());
            pw.println();
            pw.print("In local cluster : ");
            if (inLocalCluster) {
                pw.print("local");
            } else {
                pw.print("remote");
            }
            pw.println();
            pw.print("Announced by : ");
            if (inLocalCluster) {
                pw.print("n/a");
            } else {
                if (parentAnnouncement != null) {
                    pw.print(parentAnnouncement.getOwnerId());
                } else {
                    pw.print("(changing)");
                }
            }
            pw.println();

            pw.println("Properties:");
            for(final Map.Entry<String, String> entry : instanceDescription.getProperties().entrySet()) {
                pw.print("- ");
                pw.print(entry.getKey());
                pw.print(" : ");
                pw.print(entry.getValue());
                pw.println();
            }
            pw.println();
            pw.println();
        }
    }
}
