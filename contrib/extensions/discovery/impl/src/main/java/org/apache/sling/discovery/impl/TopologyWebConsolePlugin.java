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
package org.apache.sling.discovery.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEvent.Type;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.discovery.impl.cluster.ClusterViewService;
import org.apache.sling.discovery.impl.topology.announcement.Announcement;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry;
import org.apache.sling.discovery.impl.topology.announcement.AnnouncementRegistry.ListScope;
import org.apache.sling.discovery.impl.topology.connector.ConnectorRegistry;
import org.apache.sling.discovery.impl.topology.connector.TopologyConnectorClientInformation;
import org.apache.sling.discovery.impl.topology.connector.TopologyConnectorClientInformation.OriginInfo;
import org.apache.sling.discovery.impl.topology.connector.TopologyConnectorServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple webconsole which gives an overview of the topology visible by the
 * discovery service
 */
@Service(value = { TopologyEventListener.class })
@Component(immediate = true)
@SuppressWarnings("serial")
public class TopologyWebConsolePlugin extends AbstractWebConsolePlugin implements TopologyEventListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** the url part where topology joins are posted to **/
    private static final String JOIN = "/join";

    /** the url part where topology disconnects are posted to **/
    private static final String DISCONNECT = "/disconnect/";

    /** the url part where topology explicit pings are posted to **/
    private static final String PING = "/ping/";

    /** the truncated log of topology events, filtered by property change types. shown in webconsole **/
    private final List<String> propertyChangeLog = new LinkedList<String>();

    /** the truncated log of topology events, shown in webconsole **/
    private final List<String> topologyLog = new LinkedList<String>();

    /** the date format used in the truncated log of topology events **/
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** the service registry where this webconsole is registered. used for deactivate/unregister **/
    private ServiceRegistration serviceRegistration;

    @Reference
    private ClusterViewService clusterViewService;

    @Reference
    private AnnouncementRegistry announcementRegistry;

    @Reference
    private ConnectorRegistry connectorRegistry;

    private TopologyView currentView;

    @Override
    public String getLabel() {
        return "topology";
    }

    @Override
    public String getTitle() {
        return "Topology Management";
    }

    @Activate
    @Override
    public void activate(final BundleContext bundleContext) {
        super.activate(bundleContext);
        logger.debug("activate: activating...");
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(
                org.osgi.framework.Constants.SERVICE_DESCRIPTION,
                "MEEEE Web Console Plugin to display Background servlets and ExecutionEngine status");
        props.put(org.osgi.framework.Constants.SERVICE_VENDOR,
                "The Apache Software Foundation");
        props.put(org.osgi.framework.Constants.SERVICE_PID, getClass()
                .getName());
        props.put(WebConsoleConstants.PLUGIN_LABEL, getLabel());

        serviceRegistration = bundleContext.registerService(
                WebConsoleConstants.SERVICE_NAME, this, props);
    }

    @Deactivate
    @Override
    public void deactivate() {
        super.deactivate();
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
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
            }
        } else {
            StringTokenizer st = new StringTokenizer(pathInfo, "/");
            final String nodeId = st.nextToken();
            renderProperties(pw, nodeId);
        }
    }

    /**
     * Render the properties page of a particular instance
     */
    private void renderProperties(final PrintWriter pw, final String nodeId) {
        logger.debug("renderProperties: nodeId=" + nodeId);
        final TopologyView tv = this.currentView;
        Set<InstanceDescription> instances = ( tv == null ? (Set<InstanceDescription>)Collections.EMPTY_SET :

                tv.findInstances(new InstanceFilter() {

                    public boolean accept(InstanceDescription instance) {
                        String slingId = instance.getSlingId();
                        logger.debug("renderProperties/picks: slingId="
                                + slingId);
                        return (slingId.equals(nodeId));
                    }
                }));

        if (instances != null && instances.size() == 1) {
            InstanceDescription instance = instances.iterator().next();
            pw.println("Properties of " + instance.getSlingId() + ":<br/>");

            pw.println("<table class=\"adapters nicetable ui-widget\">");
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

    /**
     * Render the overview of the entire topology
     */
    private void renderOverview(final PrintWriter pw, final TopologyView topology) {
        pw.println("<p class=\"statline ui-state-highlight\">Configuration</p>");
        pw.println("<br/>");
        pw.println("<a href=\"/system/console/configMgr/org.apache.sling.discovery.impl.Config\">Configure Discovery Service</a>");
        pw.println("<br/>");
        pw.println("<br/>");
        pw.println("<p class=\"statline ui-state-highlight\">Topology</p>");
        pw.println("<div class=\"ui-widget-header ui-corner-top buttonGroup\" style=\"height: 15px;\">");
        pw.println("<span style=\"float: left; margin-left: 1em;\">Instances in the topology</span>");
        pw.println("</div>");
        pw.println("<table class=\"adapters nicetable ui-widget\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Sling id (click for properties)</th>");
        pw.println("<th class=\"header ui-widget-header\">ClusterView id</th>");
        pw.println("<th class=\"header ui-widget-header\">Own (this) instance</th>");
        pw.println("<th class=\"header ui-widget-header\">Leader instance</th>");
        pw.println("<th class=\"header ui-widget-header\">In local cluster</th>");
        pw.println("<th class=\"header ui-widget-header\">Announced by</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        Set<ClusterView> clusters = topology.getClusterViews();
        ClusterView myCluster = topology.getLocalInstance().getClusterView();
        boolean odd = true;
        renderCluster(pw, myCluster, odd);

        for (Iterator<ClusterView> it = clusters.iterator(); it.hasNext();) {
            ClusterView clusterView = it.next();
            if (clusterView.equals(myCluster)) {
                // skip - I already rendered that
                continue;
            }
            odd = !odd;
            renderCluster(pw, clusterView, odd);
        }

        pw.println("</tbody>");
        pw.println("</table>");

        pw.println("<br/>");
        pw.println("<br/>");
        pw.println("<p class=\"statline ui-state-highlight\">Connectors</p>");
        listIncomingTopologyConnectors(pw);
        listOutgoingTopologyConnectors(pw);

        pw.println("<form action=\"/system/console/topology"
                + JOIN
                + "\" method=\"POST\" style=\"display: block; width: auto; min-height: 50px;\" scrolltop=\"0\" scrollleft=\"0\">");

        String url = "http://localhost:4502"
                + TopologyConnectorServlet.TOPOLOGY_CONNECTOR_PATH;

        pw.println("<br/>");
        pw.println("<br/>");
        pw.println("<p class=\"input\">"
                + "Join topology at: "
                + "<label>"
                + "<input class=\"ui-state-default ui-corner-all inputText\" type=\"text\" value=\""
                + url + "\" name=\"connectorUrl\" style=\"width: 300px\"/>"
                + "</label>");
        pw.println("<input class=\"ui-state-default ui-corner-all\" type=\"submit\" value=\"Join\">");
        pw.println("</form>");
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
    }

    /**
     * Render a particular cluster (into table rows)
     */
    private void renderCluster(final PrintWriter pw, final ClusterView cluster, final boolean odd) {
        final Collection<Announcement> announcements = announcementRegistry
                .listAnnouncements(ListScope.AllInSameCluster);

        for (Iterator<InstanceDescription> it = cluster.getInstances()
                .iterator(); it.hasNext();) {
            final InstanceDescription instanceDescription = it.next();
            final boolean inLocalCluster = clusterViewService.contains(instanceDescription.getSlingId());
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
            
            if (inLocalCluster || (parentAnnouncement!=null)) {
                pw.println("<tr class=\"" + oddEven + " ui-state-default\">");
            } else {
                pw.println("<tr class=\"" + oddEven + " ui-state-error\">");
            }
            final boolean isLocal = instanceDescription.isLocal();
            String slingId = instanceDescription.getSlingId();
            slingId = "<a href=\"/system/console/topology/" + slingId + "\">"
                    + slingId + "</a>";
            if (isLocal) {
                slingId = "<b>" + slingId + "</b>";
            }

            pw.println("<td>" + slingId + "</td>");
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
                    pw.println("<td><b>error</b></td>");
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
        pw.println("<table class=\"adapters nicetable ui-widget\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Connector url</th>");
        pw.println("<th class=\"header ui-widget-header\">Connected to slingId</th>");
        pw.println("<th class=\"header ui-widget-header\">Origin info</th>");
        pw.println("<th class=\"header ui-widget-header\">Persistent</th>");
        // pw.println("<th class=\"header ui-widget-header\">Fallback connector urls</th>");
        pw.println("<th class=\"header ui-widget-header\">Trigger a heartbeat</th>");
        pw.println("<th class=\"header ui-widget-header\">Disconnect</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        Collection<TopologyConnectorClientInformation> outgoingConnections = connectorRegistry
                .listOutgoingConnections();
        for (Iterator<TopologyConnectorClientInformation> it = outgoingConnections
                .iterator(); it.hasNext();) {
            TopologyConnectorClientInformation topologyConnectorClient = it
                    .next();
            final String oddEven = odd ? "odd" : "even";
            odd = !odd;
            final String remoteSlingId = topologyConnectorClient.getRemoteSlingId();
            final boolean isConnected = topologyConnectorClient.isConnected() && remoteSlingId != null;
            if (isConnected) {
                pw.println("<tr class=\"" + oddEven + " ui-state-default\">");
            } else {
                pw.println("<tr class=\"" + oddEven + " ui-state-error\">");
            }
            pw.println("<td>"
                    + topologyConnectorClient.getConnectorUrl().toString()
                    + "</td>");
            if (isConnected) {
                pw.println("<td>" + remoteSlingId + "</td>");
            } else {
                final int statusCode = topologyConnectorClient.getStatusCode();
                final String tooltipText;
                switch(statusCode) {
                case 401:
                    tooltipText = "401: possible setup issue of discovery.impl on target instance";
                    break;
                case 404:
                    tooltipText = "404: possible white list rejection by target instance";
                    break;
                case 409:
                    tooltipText = "409: target instance complains that we're already in the same topology";
                    break;
                case -1:
                    tooltipText = "-1: check error log. possible connection refused.";
                    break;
                default:
                    tooltipText = null;
                }
                final String tooltip = tooltipText==null ? "" : (" title=\""+tooltipText+"\"");
                pw.println("<td"+tooltip+"><b>not connected ("+statusCode+")</b></td>");
            }
            if (topologyConnectorClient.getOriginInfo() == OriginInfo.Config) {
                pw.println("<td>");
                pw.println("<a href=\"/system/console/configMgr/org.apache.sling.discovery.impl.Config\">Config</a>");
                pw.println("</td>");
                pw.println("<td>persistent</td>");
            } else {
                pw.println("<td>" + topologyConnectorClient.getOriginInfo()
                        + "</td>");
                if (topologyConnectorClient.getOriginInfo() == OriginInfo.WebConsole) {
                    pw.println("<td>not persistent</td>");
                } else {
                    pw.println("<td>n/a</td>");
                }
            }
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
            pw.println("<td>");
            final String id = topologyConnectorClient.getId();
            final String pingId = id+"-ping";
            pw.println("<form id=\"" + pingId
                    + "\" method=\"post\" action=\"/system/console/topology"
                    + PING + id + "\">");
            pw.println("<input type=\"hidden\" name=\"name\" value=\"value\" />");
            pw.println(" <a onclick=\"document.getElementById('" + pingId
                    + "').submit();\">click here to ping</a>");
            pw.println("</form>");
            pw.println("</td>");
            final String disconnectId = topologyConnectorClient.getId()+"-disconnect";
            pw.println("<td>");
            pw.println("<form id=\"" + disconnectId
                    + "\" method=\"post\" action=\"/system/console/topology"
                    + DISCONNECT + id + "\">");
            pw.println("<input type=\"hidden\" name=\"name\" value=\"value\" />");
            pw.println(" <a onclick=\"document.getElementById('" + disconnectId
                    + "').submit();\">click here to disconnect</a>");
            pw.println("</form>");
            pw.println("</td>");
            pw.println("</tr>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
    }

    /**
     * Render the incoming topology connectors - including the header-div and table
     */
    private void listIncomingTopologyConnectors(final PrintWriter pw) {
        boolean odd = false;
        pw.println("<div class=\"ui-widget-header ui-corner-top buttonGroup\" style=\"height: 15px;\">");
        pw.println("<span style=\"float: left; margin-left: 1em;\">Incoming topology connectors</span>");
        pw.println("</div>");
        pw.println("<table class=\"adapters nicetable ui-widget\">");
        pw.println("<thead>");
        pw.println("<tr>");
        pw.println("<th class=\"header ui-widget-header\">Owner slingId</th>");
        pw.println("<th class=\"header ui-widget-header\">Server info</th>");
        pw.println("<th class=\"header ui-widget-header\">Origin info</th>");
        pw.println("<th class=\"header ui-widget-header\">Persistent</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody>");

        Collection<Announcement> outgoingConnections = announcementRegistry
                .listAnnouncements(ListScope.OnlyInherited);
        for (Iterator<Announcement> it = outgoingConnections.iterator(); it
                .hasNext();) {
            Announcement incomingAnnouncement = it.next();
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

            if (incomingAnnouncement.getOriginInfo() == OriginInfo.Config) {
                pw.println("<td>Config (remote)</td>");
                pw.println("<td>persistent</td>");
            } else {
                pw.println("<td>" + incomingAnnouncement.getOriginInfo()
                        + " (remote)</td>");
                if (incomingAnnouncement.getOriginInfo() == OriginInfo.WebConsole) {
                    pw.println("<td>not persistent</td>");
                } else {
                    pw.println("<td>n/a</td>");
                }
            }
            pw.println("</tr>");
        }

        pw.println("</tbody>");
        pw.println("</table>");
        pw.println("<br/>");
        pw.println("<br/>");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        Object rawRoot = req.getAttribute(WebConsoleConstants.ATTR_PLUGIN_ROOT);
        if (!(rawRoot instanceof String)) {
            throw new ServletException("Illegal attr: "
                    + WebConsoleConstants.ATTR_PLUGIN_ROOT);
        }

        String root = rawRoot.toString();
        String pathInfo = req.getRequestURI().substring(root.length());
        String connectorUrl = req.getParameter("connectorUrl");
        if (JOIN.equals(pathInfo)) {
            logger.debug("doPost: " + JOIN + " called with connectorUrl="
                    + connectorUrl);
            try {
                connectorRegistry.registerOutgoingConnection(
                        clusterViewService, new URL(connectorUrl),
                        OriginInfo.WebConsole);
                resp.sendRedirect(root);
            } catch (Exception e) {
                logger.error("doPost: 500: " + e);
                resp.sendRedirect(root);
            }
        } else if (pathInfo != null && pathInfo.startsWith(PING)) {
            logger.debug("doPost: " + PING + " called with full info: "
                    + pathInfo);
            String id = pathInfo.substring(PING.length());
            logger.debug("doPost: id=" + id);
            connectorRegistry.pingOutgoingConnection(id);
            resp.sendRedirect(root);
        } else if (pathInfo != null && pathInfo.startsWith(DISCONNECT)) {
            logger.debug("doPost: " + DISCONNECT + " called with full info: "
                    + pathInfo);
            String id = pathInfo.substring(DISCONNECT.length());
            logger.debug("doPost: id=" + id);
            connectorRegistry.unregisterOutgoingConnection(id);
            resp.sendRedirect(root);
        } else {
            logger.error("doPost: invalid POST to " + pathInfo);
            resp.sendError(404);
        }
    }

    /**
     * keep a truncated history of the log events for information purpose (to be shown in the webconsole)
     */
    public void handleTopologyEvent(final TopologyEvent event) {
        this.currentView = event.getNewView();
        if (event.getType() == Type.PROPERTIES_CHANGED) {

            Set<InstanceDescription> newInstances = event.getNewView()
                    .getInstances();
            StringBuffer sb = new StringBuffer();
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
                    addEventLog(event.getType(), event.getType().toString());
                    return;
                }

                Map<String, String> oldProps = oldInstanceDescription
                        .getProperties();
                Map<String, String> newProps = newInstanceDescription
                        .getProperties();
                StringBuffer diff = diff(oldProps, newProps);
                if (diff.length() > 0) {
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append("on instance "
                            + newInstanceDescription.getSlingId() + ": " + diff);
                }
            }

            addEventLog(event.getType(), sb.toString());
        } else if (event.getType() == Type.TOPOLOGY_INIT) {
            StringBuffer details = new StringBuffer();
            for (Iterator<InstanceDescription> it = event.getNewView()
                    .getInstances().iterator(); it.hasNext();) {
                InstanceDescription newInstance = it.next();
                if (details.length() != 0) {
                    details.append(", ");
                }
                details.append(newInstance.getSlingId());
            }
            addEventLog(event.getType(),
                    "view: " + shortViewInfo(event.getNewView()) + ". "
                            + details);
        } else {
            if (event.getOldView() == null) {
                addEventLog(event.getType(),
                        "new view: " + shortViewInfo(event.getNewView()));
            } else {
                StringBuffer details = new StringBuffer();
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

                addEventLog(
                        event.getType(),
                        "old view: " + shortViewInfo(event.getOldView())
                                + ", new view: "
                                + shortViewInfo(event.getNewView()) + ". "
                                + details);
            }
        }
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
        final String dateStr = sdf.format(Calendar.getInstance().getTime());
        final String logEntry = dateStr + ": " + type + ". " + info;

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
    private StringBuffer diff(final Map<String, String> oldProps,
            final Map<String, String> newProps) {
        final Set<String> oldKeys = new HashSet<String>(oldProps.keySet());
        final Set<String> newKeys = new HashSet<String>(newProps.keySet());

        StringBuffer sb = new StringBuffer();

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
}
